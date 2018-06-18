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
import at.nieslony.utils.classfinder.StaticMemberBean;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.bouncycastle.operator.OperatorCreationException;

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
                | IOException | OperatorCreationException | SQLException ex) {

            logger.warning(String.format("Cannot reload CRL: %s", ex.getMessage()));
        }
    }
}
