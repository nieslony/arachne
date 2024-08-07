/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.firewall;

import static at.nieslony.arachne.firewall.FirewallWhere.Type.Hostname;
import static at.nieslony.arachne.firewall.FirewallWhere.Type.MxRecord;
import static at.nieslony.arachne.firewall.FirewallWhere.Type.ServiceRecord;
import static at.nieslony.arachne.firewall.FirewallWhere.Type.Subnet;
import at.nieslony.arachne.utils.net.DnsServiceName;
import at.nieslony.arachne.utils.net.NetMask;
import at.nieslony.arachne.utils.net.NetUtils;
import at.nieslony.arachne.utils.net.TransportProtocol;
import at.nieslony.arachne.utils.validators.HostnameValidator;
import at.nieslony.arachne.utils.validators.IgnoringInvisibleOrDisabledValidator;
import at.nieslony.arachne.utils.validators.MxRecordValidator;
import at.nieslony.arachne.utils.validators.RequiredIfVisibleValidator;
import at.nieslony.arachne.utils.validators.ServiceRecordValidator;
import at.nieslony.arachne.utils.validators.SubnetValidator;
import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.HasValidator;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValidationStatusChangeEvent;
import com.vaadin.flow.data.binder.ValidationStatusChangeListener;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.renderer.LitRenderer;
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
class EditFirewallWhere extends AbstractCompositeField<VerticalLayout, EditFirewallWhere, FirewallWhere>
        implements HasValidator<FirewallWhere> {

    private static final Logger logger = LoggerFactory.getLogger(EditFirewallWhere.class);

    private final Select<FirewallWhere.Type> whereTypeSelect;
    private final Binder<FirewallWhere> binder;
    private final Collection<ValidationStatusChangeListener<FirewallWhere>> validationStatusListeners = new ArrayList<>();

    public EditFirewallWhere() {
        super(new FirewallWhere());
        binder = new Binder<>();

        whereTypeSelect = new Select<>();
        whereTypeSelect.setLabel("Where Type");
        whereTypeSelect.setItems(FirewallWhere.Type.values());
        whereTypeSelect.setWidthFull();
        binder.forField(whereTypeSelect)
                .bind(FirewallWhere::getType, FirewallWhere::setType);

        TextField hostnameField = new TextField("Hostname");
        hostnameField.setClearButtonVisible(true);
        hostnameField.setVisible(false);
        hostnameField.setWidthFull();
        hostnameField.setValueChangeMode(ValueChangeMode.EAGER);
        binder.forField(hostnameField)
                .asRequired(new RequiredIfVisibleValidator())
                .withValidator(new IgnoringInvisibleOrDisabledValidator<>(
                        new HostnameValidator()
                                .withResolvableRequired(true)
                                .withIpAllowed(true)
                ))
                .bind(FirewallWhere::getHostname, FirewallWhere::setHostname);

        TextField subnetField = new TextField("Subnet");
        subnetField.setClearButtonVisible(true);
        subnetField.setVisible(false);
        subnetField.setValueChangeMode(ValueChangeMode.EAGER);
        binder.forField(subnetField)
                .bind(FirewallWhere::getSubnet, FirewallWhere::setSubnet);

        ComboBox<Integer> netMaskField = new ComboBox<>();
        netMaskField.setItems(
                IntStream
                        .rangeClosed(1, 32)
                        .boxed()
                        .collect(Collectors.toList())
        );
        netMaskField.setLabel("Subnet Mask");
        netMaskField.setWidth(20, Unit.EM);
        netMaskField.setVisible(false);
        netMaskField.setItemLabelGenerator((value) -> NetMask.format(value));
        binder.forField(netMaskField)
                .bind(FirewallWhere::getSubnetMask, FirewallWhere::setSubnetMask);
        binder.forField(subnetField)
                .asRequired(new RequiredIfVisibleValidator())
                .withValidator(new IgnoringInvisibleOrDisabledValidator<>(
                        new SubnetValidator(() -> netMaskField.getValue())
                ))
                .bind(FirewallWhere::getSubnet, FirewallWhere::setSubnet);

        HorizontalLayout networkEdit = new HorizontalLayout(
                subnetField,
                new Text("/"),
                netMaskField
        );
        networkEdit.setFlexGrow(1, subnetField);
        networkEdit.setWidthFull();
        networkEdit.setAlignItems(FlexComponent.Alignment.BASELINE);
        networkEdit.setVisible(false);

        TextField serviceRecDomainField = new TextField("Domain");
        serviceRecDomainField.setValueChangeMode(ValueChangeMode.EAGER);
        serviceRecDomainField.setVisible(false);
        serviceRecDomainField.setClearButtonVisible(true);
        binder.forField(serviceRecDomainField)
                .asRequired(new RequiredIfVisibleValidator())
                .withValidator(new IgnoringInvisibleOrDisabledValidator<>(
                        new HostnameValidator())
                )
                .bind(FirewallWhere::getServiceRecDomain, FirewallWhere::setServiceRecDomain);

        ComboBox<DnsServiceName> serviceRecNameField = new ComboBox<>("Service");
        serviceRecNameField.setWidth(10, Unit.EM);
        serviceRecNameField.setItemLabelGenerator(srv -> srv.name());
        serviceRecNameField.setVisible(false);
        serviceRecNameField.setRenderer(
                LitRenderer.<DnsServiceName>of(
                        """
                        <div>
                            ${item.name}
                            <div style=\"font-size: var(--lumo-font-size-s); color: var(--lumo-secondary-text-color);\">
                                ${item.description}
                            </div>
                        </div>
                    """)
                        .withProperty("name", DnsServiceName::name)
                        .withProperty("description", DnsServiceName::description)
        );
        serviceRecNameField.setItems(
                (item, filterString)
                -> item.name().contains(filterString) || item.description().contains(filterString),
                DnsServiceName.getKnownServices().values().stream()
                        .sorted((t1, t2) -> t1.name().compareTo(t2.name()))
                        .toList()
        );
        serviceRecNameField.getStyle()
                .set("--vaadin-combo-box-overlay-width", "20em");

        Select<TransportProtocol> serviceRecProtocolField = new Select<>();
        serviceRecProtocolField.setLabel("Protocol");
        serviceRecProtocolField.setItems(TransportProtocol.values());
        serviceRecProtocolField.setWidth(6, Unit.EM);
        serviceRecProtocolField.setEmptySelectionAllowed(false);
        serviceRecProtocolField.setVisible(false);
        binder.forField(serviceRecProtocolField)
                .bind(FirewallWhere::getServiceRecProtocol, FirewallWhere::setServiceRecProtocol);
        binder.forField(serviceRecNameField)
                .withValidator(new IgnoringInvisibleOrDisabledValidator<>(new ServiceRecordValidator(
                        serviceRecDomainField,
                        serviceRecProtocolField
                )))
                .bind(
                        (where) -> DnsServiceName.getService(where.getServiceRecName()),
                        ((where, value) -> where.setServiceRecName(value.name()))
                );

        HorizontalLayout serviceRecEdit = new HorizontalLayout(
                serviceRecDomainField,
                serviceRecNameField,
                serviceRecProtocolField
        );
        serviceRecEdit.setFlexGrow(2, serviceRecDomainField);
        serviceRecEdit.setFlexGrow(1, serviceRecNameField);
        serviceRecEdit.setVisible(false);
        serviceRecEdit.setWidthFull();

        TextField mxRecDomain = new TextField("Domain");
        mxRecDomain.setWidthFull();
        mxRecDomain.setValueChangeMode(ValueChangeMode.EAGER);
        binder.forField(mxRecDomain)
                .asRequired(new RequiredIfVisibleValidator())
                .withValidator(new IgnoringInvisibleOrDisabledValidator<>(
                        new HostnameValidator())
                )
                .withValidator(new MxRecordValidator())
                .bind(FirewallWhere::getMxDomain, FirewallWhere::setMxDomain);

        var content = getContent();
        getContent().add(
                whereTypeSelect,
                hostnameField,
                networkEdit,
                serviceRecEdit,
                mxRecDomain
        );
        content.setMargin(false);
        content.setPadding(false);
        content.setMinWidth(30, Unit.EM);

        binder.addValueChangeListener((e) -> {
            FirewallWhere newValue = binder.getBean();
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

        whereTypeSelect.addValueChangeListener((e) -> {
            hostnameField.setVisible(false);
            networkEdit.setVisible(false);
            subnetField.setVisible(false);
            netMaskField.setVisible(false);
            serviceRecEdit.setVisible(false);
            serviceRecDomainField.setVisible(false);
            serviceRecNameField.setVisible(false);
            serviceRecProtocolField.setVisible(false);
            mxRecDomain.setVisible(false);
            switch (e.getValue()) {
                case Hostname ->
                    hostnameField.setVisible(true);
                case Subnet -> {
                    networkEdit.setVisible(true);
                    subnetField.setVisible(true);
                    netMaskField.setVisible(true);
                }
                case ServiceRecord -> {
                    serviceRecEdit.setVisible(true);
                    serviceRecDomainField.setVisible(true);
                    serviceRecNameField.setVisible(true);
                    serviceRecProtocolField.setVisible(true);
                    if (serviceRecDomainField.getValue() == null
                            || "".equals(serviceRecDomainField.getValue())) {
                        serviceRecDomainField.setValue(NetUtils.myDomain());
                    }
                    if (serviceRecProtocolField.getValue() == null) {
                        serviceRecProtocolField.setValue(TransportProtocol.TCP);
                    }
                }
                case MxRecord -> {
                    mxRecDomain.setVisible(true);

                    if (mxRecDomain.getValue() == null
                            || mxRecDomain.getValue().isEmpty()) {
                        mxRecDomain.setValue(NetUtils.myDomain());
                    }
                }
            }
            binder.validate();
        });

        serviceRecDomainField.addValueChangeListener((e) -> binder.validate());
        serviceRecProtocolField.addValueChangeListener((e) -> binder.validate());
    }

    @Override
    protected void setPresentationValue(FirewallWhere value) {
        if (value.getServiceRecName() == null || value.getServiceRecName().isEmpty()) {
            value.setServiceRecName(
                    DnsServiceName
                            .getKnownServices()
                            .values()
                            .stream()
                            .findFirst()
                            .get()
                            .name());
        }
        logger.info("Set value " + value.toString());
        binder.setBean(value);
        binder.validate();
    }

    @Override
    public Validator<FirewallWhere> getDefaultValidator() {
        return (value, context) -> {
            Boolean valid = binder.isValid();
            return valid ? ValidationResult.ok() : ValidationResult.error("Invalid value");
        };
    }

    @Override
    public Registration addValidationStatusChangeListener(
            ValidationStatusChangeListener<FirewallWhere> listener) {
        validationStatusListeners.add(listener);
        return () -> validationStatusListeners.remove(listener);
    }
}
