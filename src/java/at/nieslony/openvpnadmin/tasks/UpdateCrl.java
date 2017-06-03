/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.tasks;

import at.nieslony.utils.classfinder.StaticMemberBean;
import at.nieslony.openvpnadmin.beans.Pki;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 *
 * @author claas
 */
@ScheduledTaskInfo(
        description = "Update CRL from certificate database",
        name = "Update CRL"
)
public class UpdateCrl
        implements ScheduledTask {

    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @StaticMemberBean
    static Pki pki;

    static public void setPki(Pki _pki) {
        pki = _pki;
    }

    @Override
    public void run() {
        if (pki == null) {
            logger.warning("Cannot get pki bean");
            return;
        }
        try {
            pki.createCrl();
            pki.updateCrlFromDb();
            String fn = pki.getCrlFilename();
            try (PrintWriter pw = new PrintWriter(fn)) {
                pki.writeCrl(pw);
            }
        } catch (CRLException | CertificateException | ClassNotFoundException
                | IOException | InvalidKeyException | NoSuchAlgorithmException
                | NoSuchProviderException | SQLException | SignatureException ex) {

            logger.warning(String.format("Cannot reload CRL: %s", ex.getMessage()));
        }
    }
}
