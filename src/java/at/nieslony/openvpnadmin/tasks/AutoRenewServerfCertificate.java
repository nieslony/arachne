/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.tasks;

import at.nieslony.openvpnadmin.beans.ManagementInterface;
import at.nieslony.openvpnadmin.beans.Pki;
import at.nieslony.openvpnadmin.beans.ServerCertificateSettings;
import at.nieslony.utils.classfinder.StaticMemberBean;
import java.util.Date;
import java.util.logging.Logger;
import org.bouncycastle.cert.X509CertificateHolder;

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
    private static ManagementInterface managementInterface;

    static public void setManagementInterface(ManagementInterface mi) {
        managementInterface = mi;
    }

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

    private static void renewServerCertificate() {

    }

    @Override
    public void run() {
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
