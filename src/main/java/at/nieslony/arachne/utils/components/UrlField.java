/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils.components;

import at.nieslony.arachne.utils.net.MutableUrl;
import at.nieslony.arachne.utils.net.UrlParseException;
import at.nieslony.arachne.utils.validators.HostnameValidator;
import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.HasValueChangeMode;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author claas
 * @param <UT>
 */
@Slf4j
public class UrlField<UT extends MutableUrl>
        extends AbstractCompositeField<HorizontalLayout, UrlField<UT>, MutableUrl>
        implements HasSize, HasValidation, HasValueChangeMode {

    private final Select<MutableUrl.SchemaDescr> schema;
    private final TextField hostname;
    private final IntegerField port;
    private final List<MutableUrl.SchemaDescr> schemata;
    private ValueChangeMode valueChangeMode;
    Binder<UT> binder = new Binder<>();

    public UrlField(UT emptyValue) {
        super(null);
        this.schemata = emptyValue.getAllowedSchemata();
        binder = new Binder<>();

        schema = new Select<>();
        schema.setLabel("Schema");
        schema.setItems(schemata);
        schema.setItemLabelGenerator((item) -> item.name());
        schema.setWidth(6, Unit.EM);
        binder.forField(schema)
                .bind(
                        newSchema -> this.schemata.stream()
                                .filter(
                                        sch -> sch.name().equals(newSchema.getSchema())
                                )
                                .findFirst()
                                .orElse(null),
                        (urlP, sch) -> {
                            try {
                                urlP.setSchema(sch.name());
                            } catch (UrlParseException ex) {
                                log.warn("Cannot set URL schema: " + ex.getMessage());
                            }
                        }
                );

        hostname = new TextField("Hostname");
        hostname.setWidthFull();
        hostname.setValueChangeMode(ValueChangeMode.EAGER);
        binder.forField(hostname)
                .withValidator(new HostnameValidator())
                .bind(MutableUrl::getHost, MutableUrl::setHost);

        port = new IntegerField("Port");
        port.setMin(1);
        port.setMax(65535);
        port.setStepButtonsVisible(true);
        port.setWidth(8, Unit.EM);
        binder.forField(port)
                .asRequired()
                .bind(MutableUrl::getPort, MutableUrl::setPort);

        HorizontalLayout layout = getContent();
        layout.add(
                schema,
                new Text("://"),
                hostname,
                new Text(":"),
                port
        );
        layout.setAlignItems(FlexComponent.Alignment.BASELINE);
        layout.setFlexGrow(0, schema, port);
        layout.setFlexGrow(1, hostname);
        layout.setMargin(false);
        layout.setPadding(false);

        binder.setBean(emptyValue);
        binder.addValueChangeListener((e) -> {
            MutableUrl newValue = binder.getBean();
            MutableUrl oldValue = getValue();
            setModelValue(newValue, false);
            AbstractField.ComponentValueChangeEvent<UrlField, MutableUrl> event
                    = new AbstractField.ComponentValueChangeEvent<>(
                            this,
                            this,
                            oldValue,
                            e.isFromClient()
                    );
            binder.validate();
            fireEvent(event);
        });
        binder.validate();
    }

    @Override
    protected void setPresentationValue(MutableUrl url) {
        if (url == null) {
            schema.setValue(schemata.getFirst());
            hostname.setValue("example.com");
            port.setValue(schemata.getFirst().defaultPort());
        } else {
            hostname.setValue(url.getHost());
            port.setValue(url.getPort());
        }
    }

    @Override
    public ValueChangeMode getValueChangeMode() {
        return valueChangeMode;
    }

    @Override
    public void setValueChangeMode(ValueChangeMode vcm) {
        valueChangeMode = vcm;
        hostname.setValueChangeMode(valueChangeMode);
        port.setValueChangeMode(valueChangeMode);
    }

    @Override
    public boolean isInvalid() {
        log.debug("isInvalid: getValue != null: %b, hostname.isValid: %b, port: %b"
                .formatted(schema.getValue() == null, hostname.isInvalid(), port.isInvalid())
        );
        return schema.getValue() == null || hostname.isInvalid() || port.isInvalid();
    }

    @Override
    public void setErrorMessage(String string) {
    }

    @Override
    public String getErrorMessage() {
        return "";
    }

    @Override
    public void setInvalid(boolean bln) {
    }
}
