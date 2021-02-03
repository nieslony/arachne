/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.tasks;

import at.nieslony.openvpnadmin.beans.Pki;
import at.nieslony.utils.classfinder.StaticMemberBean;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

/**
 *
 * @author claas
 */
@ScheduledTaskInfo(
        description = "Create new DH parameters file",
        name = "Create DH Params"
)
public class CreateDhParams
    implements ScheduledTask {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @StaticMemberBean
    static Pki pki;

    static public void setPki(Pki _pki) {
        pki = _pki;
    }

    @Override
    public void run() {
        FileWriter fw;
        String fn = pki.getDhFilename();
        logger.info(String.format("Writing DH params into file %s", fn));
        File f = new File(fn);
        f.getParentFile().mkdirs();

        try {
            fw = new FileWriter(fn);
            pki.writeDhParameters(new PrintWriter(fw));
            fw.close();
        }
        catch (IOException ex) {
            logger.warning(String.format("Cannot write DH params to %s: %s",
                    fn, ex.getMessage()));
        }
    }
}
