/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn.sitevpnupload;

import at.nieslony.arachne.openvpn.OpenVpnSiteSettings;
import at.nieslony.arachne.openvpn.VpnSite;
import at.nieslony.arachne.settings.Settings;
import static at.nieslony.arachne.ssh.SshAuthType.PUBLIC_KEY;
import static at.nieslony.arachne.ssh.SshAuthType.USERNAME_PASSWORD;
import at.nieslony.arachne.ssh.SshKeyEntity;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;

/**
 *
 * @author claas
 */
public abstract class ConfigUploadThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(ConfigUploadThread.class);

    protected record CommandReturn(String stdout, String stderr, int exitStatus) {

    }

    protected class CommandException extends Exception {

        @Getter
        private final String what;

        @Getter
        private final String why;

        protected CommandException(String what, String why) {
            super("%s: %s".formatted(what, why));
            this.what = what;
            this.why = why;
        }
    }

    private final Dialog uploadingDlg;
    private final UI ui;
    private final VerticalLayout commandsItems;
    private final Paragraph successMessage;
    private final Paragraph errorHeader;
    private final Paragraph errorMessage;
    private final Button cancelButton;
    private final Button closeButton;

    protected final SiteConfigUploader.SiteUploadSettings uploadSettings;
    protected final OpenVpnSiteSettings openVpnSiteSettings;
    protected final VpnSite vpnSite;
    protected Session session = null;
    protected final List<CommandDescriptor> comdLineDescriptors;
    protected final BeanFactory beanFactory;

    protected class CommandDescriptor {

        @Getter
        private final String label;
        @Getter
        private final Callable<CommandReturn> command;
        @Getter
        @Setter
        private HorizontalLayout layout;

        protected CommandDescriptor(String label, Callable<CommandReturn> command) {
            this.label = label;
            this.command = command;
        }
    }

    public ConfigUploadThread(
            SiteConfigUploader.SiteUploadSettings uploadSettings,
            VpnSite vpnSite,
            BeanFactory beanFactory
    ) {
        super("Upload Site Configuration");
        this.ui = UI.getCurrent();
        this.uploadSettings = uploadSettings;
        this.vpnSite = vpnSite;
        this.comdLineDescriptors = new LinkedList<>();
        this.beanFactory = beanFactory;
        Settings settings = beanFactory.getBean(Settings.class);
        this.openVpnSiteSettings = settings.getSettings(OpenVpnSiteSettings.class);

        uploadingDlg = new Dialog("Uploading configuration to %s..."
                .formatted(vpnSite.getSiteHostname())
        );

        commandsItems = new VerticalLayout();

        successMessage = new Paragraph("");
        successMessage.getStyle()
                .set("color", "green");

        errorHeader = new Paragraph("");
        errorHeader.getStyle()
                .set("color", "red")
                .set("font-weight", "bold");

        errorMessage = new Paragraph("");
        errorMessage.getStyle()
                .set("color", "red");

        cancelButton = new Button(
                "Cancel",
                (e) -> {
                    uploadingDlg.close();
                    interrupt();
                }
        );
        closeButton = new Button(
                "Close",
                (e) -> uploadingDlg.close()
        );
        closeButton.setVisible(false);
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        uploadingDlg.add(
                commandsItems,
                successMessage,
                errorHeader,
                errorMessage
        );
        uploadingDlg.getFooter().add(cancelButton, closeButton);

    }

    protected CommandReturn execCommand(String command)
            throws JSchException, IOException {
        ChannelExec execChannel = (ChannelExec) session.openChannel("exec");
        OutputStream stdin = execChannel.getOutputStream();
        InputStream stderr = execChannel.getErrStream();
        InputStream stdout = execChannel.getInputStream();
        if (uploadSettings.isSudoRequired()) {
            String cmdWithSudo = """
                         sudo -s -- <<EOF_sudo
                         %s
                         EOF_sudo
                         """.formatted(command);
            execChannel.setCommand(cmdWithSudo);
            try (PrintWriter pw = new PrintWriter(stdin)) {
                pw.println(uploadSettings.getPassword());
            }
        } else {
            execChannel.setCommand(command);
        }
        execChannel.setPty(true);
        execChannel.connect();
        stdin.flush();

        StringBuilder stdoutBuilder = new StringBuilder();
        StringBuilder stderrBuilder = new StringBuilder();
        byte[] buf = new byte[1024];
        while (true) {
            while (stdout.available() > 0) {
                int i = stdout.read(buf, 0, 1024);
                if (i < 0) {
                    break;
                }
                stdoutBuilder.append(new String(buf, 0, i));
            }
            while (stderr.available() > 0) {
                int i = stderr.read(buf, 0, 1024);
                if (i < 0) {
                    break;
                }
                stderrBuilder.append(new String(buf, 0, i));
            }
            if (execChannel.isClosed()) {
                break;
            }
        }

        execChannel.disconnect();
        var ret = new CommandReturn(
                stdoutBuilder.toString(),
                stderrBuilder.toString(),
                execChannel.getExitStatus()
        );
        return ret;
    }

    abstract protected void addCommandDescriptors();

    @Override
    public void run() {
        ui.access(() -> uploadingDlg.open());

        JSch ssh = new JSch();

        try {
            comdLineDescriptors.add(new CommandDescriptor(
                    "Connect to " + uploadSettings.getUploadToHost(),
                    () -> {
                        try {
                            session = ssh.getSession(
                                    uploadSettings.getUsername(),
                                    uploadSettings.getUploadToHost()
                            );
                            switch (uploadSettings.getSshAuthType()) {
                                case USERNAME_PASSWORD ->
                                    session.setPassword(uploadSettings.getPassword());
                                case PUBLIC_KEY -> {
                                    SshKeyEntity sshKey = uploadSettings.getSshKey();
                                    ssh.addIdentity(
                                            sshKey.getComment(),
                                            sshKey.getPrivateKey().getBytes(),
                                            sshKey.getPublicKey().getBytes(),
                                            "".getBytes());
                                }
                            }
                            session.setConfig("StrictHostKeyChecking", "no");
                            session.connect();
                            logger.info("Connected.");
                        } catch (JSchException ex) {
                            throw new CommandException(
                                    "Cannot connect to " + uploadSettings.getUploadToHost(),
                                    ex.getMessage()
                            );
                        }
                        return null;
                    }
            ));

            addCommandDescriptors();

            comdLineDescriptors.forEach((item) -> {
                HorizontalLayout layout = new HorizontalLayout(
                        VaadinIcon.HOURGLASS.create(),
                        new Text(item.getLabel())
                );
                ui.access(() -> commandsItems.add(layout));
                item.setLayout(layout);
            });

            AtomicReference<HorizontalLayout> layout = new AtomicReference<>();
            try {
                for (var cmd : comdLineDescriptors) {
                    layout.set(cmd.layout);
                    ui.access(() -> {
                        var curIcon = cmd.layout.getComponentAt(0);
                        var newIcon = VaadinIcon.ARROWS_LONG_RIGHT.create();
                        newIcon.addClassName("--lumo-primary-color");
                        cmd.layout.replace(curIcon, newIcon);
                    });
                    var ret = cmd.command.call();
                    if (ret != null && ret.exitStatus() != 0) {
                        String msg = !ret.stderr().isEmpty()
                                ? ret.stderr()
                                : ret.stdout();
                        throw new CommandException("Step failed: " + cmd.getLabel(), msg);
                    }
                    ui.access(() -> {
                        var curIcon = cmd.layout.getComponentAt(0);
                        var newIcon = VaadinIcon.CHECK.create();
                        newIcon.addClassName("--lumo-success-color");
                        cmd.layout.replace(curIcon, newIcon);
                    });
                }
                ui.access(() -> successMessage.setText("Configuration successfully uploaded"));
            } catch (CommandException ex) {
                ui.access(() -> {
                    var curIcon = layout.get().getComponentAt(0);
                    var newIcon = VaadinIcon.CLOSE.create();
                    layout.get().replace(curIcon, newIcon);
                    errorHeader.setText(ex.getWhat());
                    errorMessage.setText(ex.getWhy());
                });
            } catch (Exception ex) {
                ui.access(() -> {
                    var curIcon = layout.get().getComponentAt(0);
                    var newIcon = VaadinIcon.CLOSE.create();
                    layout.get().replace(curIcon, newIcon);
                    errorMessage.setText(ex.getMessage());
                });
                StringWriter wr = new StringWriter();
                ex.printStackTrace(new PrintWriter(wr));
                logger.error(ex.getMessage());
                logger.error(wr.toString());
            }
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }

        ui.access(() -> {
            cancelButton.setVisible(false);
            closeButton.setVisible(true);
        });
    }
}
