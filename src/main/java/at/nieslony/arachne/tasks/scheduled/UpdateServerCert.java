/*
 * Copyright (C) 2023 claas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package at.nieslony.arachne.tasks.scheduled;

import at.nieslony.arachne.openvpn.OpenVpnRestController;
import at.nieslony.arachne.openvpn.OpenVpnUserSettings;
import at.nieslony.arachne.openvpnmanagement.OpenVpnManagement;
import at.nieslony.arachne.openvpnmanagement.OpenVpnManagementException;
import at.nieslony.arachne.pki.Pki;
import at.nieslony.arachne.pki.PkiSettings;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.tasks.RecurringTaskDescription;
import at.nieslony.arachne.tasks.Task;
import at.nieslony.arachne.tasks.TaskDescription;
import at.nieslony.arachne.utils.ArachneTimeUnit;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;

/**
 *
 * @author claas
 */
@TaskDescription(name = "Update Server Certificate")
@RecurringTaskDescription(
        defaulnterval = 7,
        timeUnit = ArachneTimeUnit.DAY,
        startAt = "01:00:00"
)
public class UpdateServerCert extends Task {

    private static final Logger logger = LoggerFactory.getLogger(UpdateServerCert.class);

    @Override
    public String run(BeanFactory beanFactory) throws Exception {
        Pki pki = beanFactory.getBean(Pki.class);
        Settings settings = beanFactory.getBean(Settings.class);
        OpenVpnUserSettings openVpnUserSettings = new OpenVpnUserSettings(settings);
        PkiSettings pkiSettings = new PkiSettings(settings);

        X509Certificate serverCert = pki.getServerCert();
        Calendar cal = Calendar.getInstance();
        logger.info("Now: " + cal.getTime().toString());
        cal.setTime(serverCert.getNotAfter());
        logger.info("Cert valid until: " + cal.getTime().toString());
        cal.add(Calendar.DATE, -pkiSettings.getServerCertRenewDays());
        logger.info("Renew after: " + cal.getTime().toString());
        if (cal.before(Calendar.getInstance())) {
            pki.createServerCert();
            OpenVpnRestController openVpnRestController
                    = beanFactory.getBean(OpenVpnRestController.class);
            openVpnRestController.writeOpenVpnUserServerConfig(openVpnUserSettings);

            OpenVpnManagement openVpnManagement = beanFactory.getBean(OpenVpnManagement.class);
            try {
                openVpnManagement.restartServer();
                return "Server Certitificate renewed, openVPN server restarted";
            } catch (OpenVpnManagementException ex) {
                return "Server Certificate renewed but openVPN Server restart failed: "
                        + ex.getMessage();
            }

        } else {
            return "Server Certificate will be renewed on " + cal.getTime().toString();
        }
    }
}
