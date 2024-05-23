/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn.sitevpnupload;

import at.nieslony.arachne.openvpn.OpenVpnRestController;
import at.nieslony.arachne.openvpn.VpnSite;
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
            SiteConfigUploader.SiteUploadSettings uploadSettings,
            VpnSite vpnSite,
            BeanFactory beanFactory
    ) {
        super(uploadSettings, vpnSite, beanFactory);
    }

    private CommandReturn createConfigFolder() throws JSchException, IOException, CommandException {
        String command = "mkdir -pv " + uploadSettings.getDestinationFolder();
        return execCommand(command);
    }

    private CommandReturn uploadConfiguration() throws JSchException, IOException, CommandException {
        OpenVpnRestController openVpnRestController = beanFactory.getBean(OpenVpnRestController.class);
        String configFN = "%s/%s".formatted(
                uploadSettings.getDestinationFolder(),
                openVpnRestController.getOpenVpnSiteRemoteConfigName(openVpnSiteSettings, vpnSite)
        );
        StringWriter configWriter = new StringWriter();
        openVpnRestController.writeOpenVpnSiteRemoteConfig(vpnSite.getId(), configWriter);

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
        String command = "systemctl restart openvpn-client@" + vpnSite.getName();
        return execCommand(command);
    }

    private CommandReturn enableOpenVpn() throws JSchException, IOException, CommandException {
        String command = "systemctl enable openvpn-client@" + vpnSite.getName();
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
        if (uploadSettings.isRestartOpenVpn()) {
            comdLineDescriptors.add(new CommandDescriptor(
                    "Restart OpenVPN",
                    () -> restartOpenVpn()
            ));
        }
        if (uploadSettings.isEnableOpenVpn()) {
            comdLineDescriptors.add(new CommandDescriptor(
                    "EnableOpenVPN",
                    () -> enableOpenVpn()
            ));
        }
    }
}
