/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn.vpnsite;

import at.nieslony.arachne.utils.net.NetMask;
import at.nieslony.arachne.utils.validators.IgnoringInvisibleOrDisabledValidator;
import at.nieslony.arachne.utils.validators.RequiredIfVisibleValidator;
import at.nieslony.arachne.utils.validators.SubnetValidator;
import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.HasValidator;
import com.vaadin.flow.data.binder.ValidationStatusChangeEvent;
import com.vaadin.flow.data.binder.ValidationStatusChangeListener;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
public class EditRemoteNetwork extends AbstractCompositeField<FormLayout, EditRemoteNetwork, RemoteNetwork>
        implements HasValidator<RemoteNetwork> {

    private static final Logger logger = LoggerFactory.getLogger(EditRemoteNetwork.class);

    private final Binder<RemoteNetwork> binder;
    private final Collection<ValidationStatusChangeListener<RemoteNetwork>> validationStatusListeners = new ArrayList<>();

    public EditRemoteNetwork() {
        super(new RemoteNetwork());

        binder = new Binder<>();

        TextField networkField = new TextField("Network");
        networkField.setValueChangeMode(ValueChangeMode.EAGER);

        ComboBox<Integer> netMaskField = new ComboBox<>();
        netMaskField.setItems(
                IntStream
                        .range(1, 32)
                        .boxed()
                        .collect(Collectors.toList())
        );
        netMaskField.setItemLabelGenerator((value) -> NetMask.format(value));
        netMaskField.setLabel("Subnet Mask");
        binder.forField(networkField)
                .asRequired(new RequiredIfVisibleValidator())
                .withValidator(new IgnoringInvisibleOrDisabledValidator<>(
                        new SubnetValidator(() -> netMaskField.getValue())
                ))
                .bind(
                        RemoteNetwork::getAddress,
                        RemoteNetwork::setAddress
                );
        binder.forField(netMaskField)
                .bind(RemoteNetwork::getMask, RemoteNetwork::setMask);

        TextField name = new TextField("Name");
        name.setValueChangeMode(ValueChangeMode.EAGER);
        binder.bind(
                name,
                RemoteNetwork::getName,
                RemoteNetwork::setName
        );

        getContent().add(networkField, netMaskField);
        getContent().add(name, 2);
        getContent().setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("50pt", 2)
        );
        binder.addValueChangeListener((e) -> {
            var newValue = binder.getBean();
            setModelValue(newValue, true);
            binder.validate();
        });

        binder.addStatusChangeListener((sce) -> {
            var event = new ValidationStatusChangeEvent<>(this, binder.isValid());
            logger.info("New validation status: " + event.getNewStatus() + " binder: " + binder.isValid());
            validationStatusListeners.forEach(
                    listener -> {
                        listener.validationStatusChanged(event);
                    }
            );
        });

        binder.setBean(new RemoteNetwork());
        binder.validate();
    }

    @Override
    protected void setPresentationValue(RemoteNetwork remoteNetwork) {
        binder.setBean(remoteNetwork);
        binder.validate();
    }

    @Override
    public Registration addValidationStatusChangeListener(
            ValidationStatusChangeListener<RemoteNetwork> listener
    ) {
        validationStatusListeners.add(listener);
        return () -> validationStatusListeners.remove(listener);
    }
}
