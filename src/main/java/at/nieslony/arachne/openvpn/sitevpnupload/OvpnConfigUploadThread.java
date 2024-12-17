/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn.sitevpnupload;

import at.nieslony.arachne.openvpn.OpenVpnController;
import com.jcraft.jsch.JSchException;
import java.io.IOException;
import java.io.StringWriter;
import org.springframework.beans.factory.BeanFactory;

/**
 *
 * @author claas
 */
public class OvpnConfigUploadThread extends ConfigUploadThread {

    public OvpnConfigUploadThread(
            SiteUploadSettings uploadSettings,
            BeanFactory beanFactory
    ) {
        super(uploadSettings, beanFactory);
    }

    private CommandReturn createConfigFolder() throws JSchException, IOException, CommandException {
        String command = "mkdir -pv " + uploadSettings.getVpnSite().getDestinationFolder();
        return execCommand(command);
    }

    private CommandReturn uploadConfiguration() throws JSchException, IOException, CommandException {
        OpenVpnController openVpnRestController = beanFactory.getBean(OpenVpnController.class);
        String configFN = "%s/%s".formatted(
                uploadSettings.getVpnSite().getDestinationFolder(),
                openVpnRestController.getOpenVpnSiteRemoteConfigName(
                        openVpnSiteSettings,
                        uploadSettings.getVpnSite()
                )
        );
        StringWriter configWriter = new StringWriter();
        openVpnRestController.writeOpenVpnSiteRemoteConfig(
                uploadSettings.getVpnSite().getId(),
                configWriter
        );

        String command
                = """
                cat <<EOF_config > ${CONF_FILE} || exit 1
                ${CONFIG}
                EOF_config
                chmod 600 ${CONF_FILE} || exit 1
                chown root:root ${CONF_FILE} || exit 1
                """
                        .replace("${CONFIG}", configWriter.toString())
                        .replace("${CONF_FILE}", configFN);
        return execCommand(command);
    }

    private CommandReturn restartOpenVpn() throws JSchException, IOException, CommandException {
        String command = "systemctl restart openvpn-client@" + uploadSettings.getVpnSite().getName();
        return execCommand(command);
    }

    private CommandReturn enableOpenVpn() throws JSchException, IOException, CommandException {
        String command = "systemctl enable openvpn-client@" + uploadSettings.getVpnSite().getName();
        return execCommand(command);
    }

    @Override
    protected void addCommandDescriptors() {
        comdLineDescriptors.add(new CommandDescriptor(
                "Create configuration folder",
                () -> createConfigFolder()
        ));
        comdLineDescriptors.add(new CommandDescriptor(
                "Upload configuration",
                () -> uploadConfiguration()
        ));
        if (uploadSettings.getVpnSite().isRestartOpenVpn()) {
            comdLineDescriptors.add(new CommandDescriptor(
                    "Restart OpenVPN",
                    () -> restartOpenVpn()
            ));
        }
        if (uploadSettings.getVpnSite().isEnableOpenVpn()) {
            comdLineDescriptors.add(new CommandDescriptor(
                    "EnableOpenVPN",
                    () -> enableOpenVpn()
            ));
        }
    }
}
