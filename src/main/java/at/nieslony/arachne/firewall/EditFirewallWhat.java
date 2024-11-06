/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.firewall;

import at.nieslony.arachne.utils.net.TransportProtocol;
import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.HasValidator;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValidationStatusChangeEvent;
import com.vaadin.flow.data.binder.ValidationStatusChangeListener;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import java.util.ArrayList;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
class EditFirewallWhat extends AbstractCompositeField<VerticalLayout, EditFirewallWhat, FirewallWhat>
        implements HasValidator<FirewallWhat> {

    private static final Logger logger = LoggerFactory.getLogger(EditFirewallWhat.class);

    private final Select<FirewallWhat.Type> whatType;
    private final Binder<FirewallWhat> binder;
    private final Collection<ValidationStatusChangeListener<FirewallWhat>> validationStatusListeners = new ArrayList<>();

    EditFirewallWhat() {
        super(new FirewallWhat());
        binder = new Binder<>();
        var firewalldServices = FirewalldService.getAllServices();

        whatType = new Select<>();
        whatType.setItems(FirewallWhat.Type.values());
        whatType.setLabel("What Type");
        whatType.setWidthFull();
        binder.forField(whatType)
                .bind(FirewallWhat::getType, FirewallWhat::setType);

        IntegerField portField = new IntegerField("Port");
        portField.setMin(1);
        portField.setMax(65535);
        portField.setWidth(8, Unit.EM);
        portField.setStepButtonsVisible(true);
        portField.setValueChangeMode(ValueChangeMode.EAGER);
        binder.forField(portField)
                .bind(FirewallWhat::getPort, FirewallWhat::setPort);

        Select<TransportProtocol> portProtocolSelect = new Select<>();
        portProtocolSelect.setItems(TransportProtocol.values());
        binder.forField(portProtocolSelect)
                .bind(FirewallWhat::getPortProtocol, FirewallWhat::setPortProtocol);

        HorizontalLayout portEdit = new HorizontalLayout(
                portField,
                new Text("/"),
                portProtocolSelect
        );
        portEdit.setMargin(false);
        portEdit.setPadding(false);
        portEdit.setAlignItems(FlexComponent.Alignment.BASELINE);
        portEdit.setVisible(false);
        portEdit.setWidthFull();

        IntegerField portFromField = new IntegerField("Port from");
        portFromField.setMin(1);
        portFromField.setMax(65535);
        portFromField.setMinWidth(9, Unit.EM);
        portFromField.setStepButtonsVisible(true);
        portFromField.setValueChangeMode(ValueChangeMode.EAGER);
        binder.forField(portFromField)
                .bind(FirewallWhat::getPortFrom, FirewallWhat::setPortFrom);

        IntegerField portToField = new IntegerField("Port to");
        portToField.setMin(1);
        portToField.setMax(65535);
        portToField.setMinWidth(9, Unit.EM);
        portToField.setStepButtonsVisible(true);
        portToField.setValueChangeMode(ValueChangeMode.EAGER);
        binder.forField(portToField)
                .bind(FirewallWhat::getPortTo, FirewallWhat::setPortTo);

        Select<TransportProtocol> portRangeProtocolSelect = new Select<>();
        portRangeProtocolSelect.setItems(TransportProtocol.values());
        binder.forField(portRangeProtocolSelect)
                .bind(FirewallWhat::getPortRangeProtocol, FirewallWhat::setPortRangeProtocol);

        HorizontalLayout portRangeEdit = new HorizontalLayout(portFromField, new Text("-"), portToField, new Text("/"), portRangeProtocolSelect);
        portRangeEdit.setMargin(false);
        portRangeEdit.setPadding(false);
        portRangeEdit.setAlignItems(FlexComponent.Alignment.BASELINE);
        portRangeEdit.setVisible(false);
        portRangeEdit.setWidthFull();

        ComboBox<FirewalldService> firewalldServiceSelect
                = new ComboBox<>();
        firewalldServiceSelect.setLabel("FirewallD Service");
        firewalldServiceSelect.setItems(firewalldServices);
        firewalldServiceSelect.setItemLabelGenerator(FirewalldService::getShortDescription);
        firewalldServiceSelect.setWidthFull();
        firewalldServiceSelect.setAllowCustomValue(false);
        firewalldServiceSelect.setRequired(true);
        firewalldServiceSelect.setVisible(false);
        binder.forField(firewalldServiceSelect)
                .bind(
                        (FirewallWhat source)
                        -> FirewalldService.getService(source.getService()),
                        (FirewallWhat dest, FirewalldService value)
                        -> dest.setService(value.getName())
                );

        var content = getContent();
        content.add(
                whatType,
                firewalldServiceSelect,
                portEdit,
                portRangeEdit
        );
        content.setMargin(false);
        content.setPadding(false);
        content.setMinWidth(30, Unit.EM);

        whatType.addValueChangeListener(e -> {
            portEdit.setVisible(false);
            portRangeEdit.setVisible(false);
            firewalldServiceSelect.setVisible(false);
            switch (e.getHasValue().getValue()) {
                case OnePort ->
                    portEdit.setVisible(true);
                case PortRange ->
                    portRangeEdit.setVisible(true);
                case Service ->
                    firewalldServiceSelect.setVisible(true);
                case Everything -> {
                }
            }
            binder.validate();
        });

        binder.setBean(getValue());
        binder.validate();

        binder.addValueChangeListener((e) -> {
            FirewallWhat newValue = binder.getBean();
            setModelValue(newValue, true);
            binder.validate();
        });

        binder.addStatusChangeListener((sce) -> {
            var event = new ValidationStatusChangeEvent<>(this, binder.isValid());
            validationStatusListeners.forEach(
                    listener -> {
                        listener.validationStatusChanged(event);
                    }
            );
        });
    }

    @Override
    protected void setPresentationValue(FirewallWhat t) {
        if (t.getService() == null || t.getService().isEmpty()) {
            t.setService(FirewalldService.getService("http").getName());
        }
        binder.setBean(t);
        binder.validate();
    }

    @Override
    public Validator<FirewallWhat> getDefaultValidator() {
        return (value, context) -> {
            Boolean valid = binder.isValid();
            return valid ? ValidationResult.ok() : ValidationResult.error("Invalid value");
        };
    }

    @Override
    public Registration addValidationStatusChangeListener(
            ValidationStatusChangeListener<FirewallWhat> listener) {
        validationStatusListeners.add(listener);
        return () -> validationStatusListeners.remove(listener);
    }
}
