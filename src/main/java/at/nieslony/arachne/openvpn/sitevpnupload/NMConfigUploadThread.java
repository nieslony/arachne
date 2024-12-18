/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn.sitevpnupload;

import at.nieslony.arachne.openvpn.VpnSiteRepository;
import at.nieslony.arachne.pki.Pki;
import at.nieslony.arachne.pki.PkiException;
import at.nieslony.arachne.settings.SettingsException;
import at.nieslony.arachne.utils.ShellQuote;
import at.nieslony.arachne.utils.net.TransportProtocol;
import com.jcraft.jsch.JSchException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;

/**
 *
 * @author claas
 */
public class NMConfigUploadThread extends ConfigUploadThread {

    private static final Logger logger = LoggerFactory.getLogger(NMConfigUploadThread.class);
    private final VpnSiteRepository vpnSiteRepository;

    public NMConfigUploadThread(
            SiteUploadSettings uploadSettings,
            BeanFactory beanFactory
    ) {
        super(uploadSettings, beanFactory);
        this.vpnSiteRepository = beanFactory.getBean(VpnSiteRepository.class);
    }

    private CommandReturn createConnection() throws JSchException, IOException, CommandException {
        String conUuid = uploadSettings.getVpnSite().getNetworkManagerConnectionUuid() != null
                ? uploadSettings.getVpnSite().getNetworkManagerConnectionUuid()
                : "";
        String conName = ShellQuote.shellQuote(uploadSettings.getVpnSite().getConnectionName());

        CommandReturn ret;
        String cmdCreateConnection
                = """
                if [ -z "${CON_UUID}" ]; then
                    # create new connection
                    if nmcli connection show ${CON_NAME} > /dev/zero 2>&1 ; then
                        echo There is already a connection named ${CON_NAME}.
                        exit 1
                    fi
                    nmcli connection add con-name ${CON_NAME} type vpn vpn-type openvpn > /dev/null
                        nmcli -g connection.uuid connection show ${CON_NAME}
                else
                    # update connection
                    found_uuid=$( nmcli -g connection.uuid connection show ${CON_NAME} 2> /dev/zero )
                    if [ -n "$found_uuid" ]; then
                        # connection found
                        if [ "${CON_UUID}" != "$found_uuid" ]; then
                            echo "
                                There is already a connection named \"${CON_NAME}\" with a different uuid.
                                Found: ($found_uuid)
                                Expected: ${CON_UUID}
                            "
                            exit 1
                        fi
                    else
                        # connection with name not found
                        if ! nmcli connection show ${CON_UUID} > /dev/null 2>&1 ; then
                            # no connection with uuid
                            nmcli connection add con-name ${CON_NAME} type vpn vpn-type openvpn > /dev/zero 2>&1
                            nmcli -g connection.uuid connection show ${CON_NAME}
                        fi
                    fi
                fi
                """
                        .replace("${CON_UUID}", conUuid)
                        .replace("${CON_NAME}", conName);

        ret = execCommand(cmdCreateConnection);
        if (ret.exitStatus() == 0 && !ret.stdout().isEmpty()) {
            conUuid = ret.stdout().strip();
            uploadSettings.getVpnSite().setNetworkManagerConnectionUuid(conUuid);
            vpnSiteRepository.save(uploadSettings.getVpnSite());
        }

        return ret;
    }

    private CommandReturn uploadCertificate() throws JSchException, IOException, CommandException {
        Pki pki = beanFactory.getBean(Pki.class);
        try {
            String command
                    = """
                mkdir -p "${CERT_FOLDER}" || exit 1
                cat <<EOF > ${CA_CERT_FN} || exit 1
                ${CA_CERT}
                EOF
                cat <<EOF > ${CERT_FN} || exit 1
                ${CERT}
                EOF
                cat <<EOF > ${KEY_FN} || exit 1
                ${KEY}
                EOF
                chmod 600 ${KEY_FN}
                """
                            .replace("${CERT_FOLDER}", uploadSettings.getVpnSite().getCertitifaceFolder())
                            .replace("${CA_CERT}", pki.getRootCertAsBase64())
                            .replace("${CERT}", pki.getUserCertAsBase64(uploadSettings.getVpnSite().getSiteHostname()))
                            .replace("${KEY}", pki.getUserKeyAsBase64(uploadSettings.getVpnSite().getSiteHostname()))
                            .replace("${CA_CERT_FN}", getCaCertFileName())
                            .replace("${CERT_FN}", getCertFileName())
                            .replace("${KEY_FN}", getKeyFileName());
            return execCommand(command);
        } catch (PkiException | SettingsException ex) {
            throw new CommandException("Cannot build upload command", ex.getMessage());
        }
    }

    private CommandReturn configureConnection() throws JSchException, IOException, CommandException {
        String[] vpnData = {
            "ca = " + getCaCertFileName(),
            "cert = " + getCertFileName(),
            "cert-pass-flags = 4",
            "connection-type = tls",
            "dev-type = tun",
            "float = no",
            "key = " + getKeyFileName(),
            "mssfix = no",
            "proto-tcp = "
            + (openVpnSiteSettings.getListenProtocol() == TransportProtocol.TCP
            ? "yes"
            : "no"),
            "remote = " + openVpnSiteSettings.getConnectToHost(),
            "remote-random = no",
            "tun-ipv6 = no"
        };
        StringBuilder strBuilder = new StringBuilder();
        strBuilder
                .append("nmcli connection modify ").append(uploadSettings.getVpnSite().getNetworkManagerConnectionUuid())
                .append(" connection.id ").append(uploadSettings.getVpnSite().getConnectionName())
                .append(" connection.autoconnect ").append(uploadSettings.getVpnSite().isEnableConnection() ? "yes" : "no")
                .append(" ipv4.dns-priority -1")
                .append(" ipv4.dns-search ").append(String.join(",", uploadSettings.getVpnSite().getPushSearchDomains()))
                .append(" ipv4.dns ").append(String.join(",", uploadSettings.getVpnSite().getPushDnsServers()))
                .append(" vpn.data \"").append(String.join(",", vpnData)).append("\"");

        //  ipv4.dns 192.168.120.20 vpn.data "ca = /etc/openvpn/client/ca.pem, cert = /etc/openvpn/client/cert.pem, cert-pass-flags = 4, connection-type = tls, dev-type = tun, float = no, key = /etc/openvpn/client/key.pem, mssfix = no, proto-tcp = yes, remote = 192.168.100.83, remote-random = no, tun-ipv6 = no"
        String command = strBuilder.toString();
        return execCommand(command);
    }

    private CommandReturn restartConnection() throws JSchException, IOException, CommandException {
        String command
                = """
                nmcli connection down ${CON_UUID}
                nmcli connection up ${CON_UUID}
                """.replace("${CON_UUID}", uploadSettings.getVpnSite().getNetworkManagerConnectionUuid());

        return execCommand(command);
    }

    @Override
    protected void addCommandDescriptors() {
        comdLineDescriptors.add(new CommandDescriptor("Create connection", () -> createConnection()));
        comdLineDescriptors.add(new CommandDescriptor("Upload certificate", () -> uploadCertificate()));
        comdLineDescriptors.add(new CommandDescriptor("Configure connection", () -> configureConnection()));
        if (uploadSettings.getVpnSite().isEnableConnection()) {
            comdLineDescriptors.add(new CommandDescriptor("Restart connection", () -> restartConnection()));
        }
    }

    private String getCaCertFileName() {
        return "%s/arachne_ca_%s.crt"
                .formatted(
                        uploadSettings.getVpnSite().getCertitifaceFolder(),
                        openVpnSiteSettings.getConnectToHost()
                );
    }

    private String getCertFileName() {
        return "%s/%s.crt"
                .formatted(
                        uploadSettings.getVpnSite().getCertitifaceFolder(),
                        uploadSettings.getVpnSite().getSiteHostname()
                );
    }

    private String getKeyFileName() {
        return "%s/%s.key"
                .formatted(
                        uploadSettings.getVpnSite().getCertitifaceFolder(),
                        uploadSettings.getVpnSite().getSiteHostname()
                );
    }
}
