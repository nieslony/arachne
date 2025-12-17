/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.ssh;

import com.jcraft.jsch.JSchException;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
public class AddSshKeyDialog extends Dialog {

    private static final Logger logger = LoggerFactory.getLogger(AddSshKeyDialog.class);

    public AddSshKeyDialog(Consumer<SshKeyEntity> onOk) {
        super("Create SSH Key Pair");
        ComboBox<SshKeyType> sshKeyType = new ComboBox<>("Key Type", SshKeyType.values());
        sshKeyType.setWidthFull();

        ComboBox<Integer> sshKeySize = new ComboBox<>("Key Size");
        sshKeySize.setWidthFull();
        sshKeySize.setItemLabelGenerator((v) -> String.valueOf(v));

        TextField comment = new TextField("Comment");
        comment.setRequired(true);
        comment.setValueChangeMode(ValueChangeMode.EAGER);

        Button okButton = new Button("OK", (e) -> {
            try {
                SshKeyEntity entity = SshKeyEntity.create(
                        sshKeyType.getValue(),
                        sshKeySize.getValue(),
                        comment.getValue()
                );
                onOk.accept(entity);
            } catch (JSchException ex) {
                logger.error("Cannot create key pair: " + ex.getMessage());
            }
            close();
        });
        okButton.addThemeVariants(ButtonVariant.AURA_PRIMARY);
        okButton.setAutofocus(true);

        Button cancelButton = new Button("Cancel", (e) -> close());

        sshKeyType.addValueChangeListener((e) -> {
            SshKeyType keyType = e.getValue();
            sshKeySize.setItems(keyType.getKeySizes());
            sshKeySize.setValue(keyType.getKeySizes().get(0));
        });

        comment.addValueChangeListener(
                (e) -> {
                    okButton.setEnabled(!e.getValue().isEmpty());
                }
        );

        sshKeyType.setValue(SshKeyType.RSA);
        comment.setValue("arachne");

        VerticalLayout layout = new VerticalLayout(
                sshKeyType,
                sshKeySize,
                comment
        );

        layout.setPadding(
                false);
        layout.setSpacing(
                false);
        add(layout);

        getFooter()
                .add(
                        cancelButton,
                        okButton
                );
    }
}
