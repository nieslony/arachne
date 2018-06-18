/*
 * Copyright (C) 2018 Claas Nieslony
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

package at.nieslony.openvpnadmin.tasks;

import at.nieslony.openvpnadmin.beans.Pki;
import at.nieslony.openvpnadmin.beans.ServerCertificateRenewer;
import at.nieslony.openvpnadmin.beans.ServerCertificateSettings;
import at.nieslony.utils.classfinder.StaticMemberBean;
import java.security.Provider;
import java.security.Security;
import java.util.Date;
import java.util.logging.Logger;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 *
 * @author claas
 */
@ScheduledTaskInfo(
        name = "Auto renew server certificate",
        description = "Auto renew server certifivcate after " +
            "specified percentage of lifetime and reload server")
public class AutoRenewServerfCertificate
        implements ScheduledTask
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @StaticMemberBean
    private static Pki pki;

    public static void setPki(Pki pki) {
        AutoRenewServerfCertificate.pki = pki;
    }

    @StaticMemberBean
    private static ServerCertificateSettings serverCertificateSettings;

    public static void setServerCertificateSettings(ServerCertificateSettings s) {
        serverCertificateSettings = s;
    }

    @StaticMemberBean
    private static ServerCertificateRenewer serverCertificateRenewer;

    public static void setServerCertificateRenewer(ServerCertificateRenewer scr) {
        serverCertificateRenewer = scr;
    }

    private static void renewServerCertificate() {
        serverCertificateRenewer.renewServerCertificate(serverCertificateSettings);
    }

    public static void registerBouncyCastle() {
        try {
            Provider provs[] = Security.getProviders();
            boolean found = false;
            for (Provider p: provs) {
                if (p.getName().equals(BouncyCastleProvider.PROVIDER_NAME)) {
                    logger.info("BouncyCastleProvider already added");
                    found = true;
                    break;
                }
            }
            if (!found) {
                logger.info("Adding new security privider: BouncyCastleProvider");
                Security.addProvider(new BouncyCastleProvider());
            }
        }
        catch (SecurityException ex) {
            logger.severe(String.format("Cannot add security provider: %s",
                    ex.getMessage()));
        }
    }

    @Override
    public void run() {
        registerBouncyCastle();

        X509CertificateHolder serverCert = pki.getServerCert();

        if (serverCert == null) {
            logger.warning("There's no server certificate.");

            return;
        }
        Date validFrom = serverCert.getNotBefore();
        Date validTo = serverCert.getNotAfter();
        Date now = new Date();

        long validityLength = validTo.getTime() - validFrom.getTime();
        long validityOver = now.getTime() - validFrom.getTime();

        logger.info(String.format("validity length: %d validity over: %d",
            validityLength, validityOver));

        long percent = validityOver * 100 / validityLength;
        logger.info(String.format("Server certificate lifetime: %d%%", percent));

        if (percent > serverCertificateSettings.getAutoUpdateLifetime()) {
            logger.info("Server certificate will expoire soon.");
            renewServerCertificate();
        }
        else {
            logger.info("Server certificate still valid.");
        }
    }
}
