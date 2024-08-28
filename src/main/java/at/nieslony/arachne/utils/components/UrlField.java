/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils.components;

import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.shared.HasValidationProperties;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.HasValueChangeMode;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author claas
 */
@Slf4j
public class UrlField
        extends AbstractCompositeField<HorizontalLayout, UrlField, String>
        implements HasSize, HasValidationProperties, HasValueChangeMode {

    public record SchemaDescr(String name, int defaultPort) {

    }

    public static final SchemaDescr[] SCHEMATA_LDAP = new SchemaDescr[]{
        new UrlField.SchemaDescr("ldap", 389),
        new UrlField.SchemaDescr("ldaps", 636)
    };

    public static final SchemaDescr[] SCHEMATA_WWW = new SchemaDescr[]{
        new UrlField.SchemaDescr("http", 80),
        new UrlField.SchemaDescr("https", 443)
    };

    private final Select<SchemaDescr> schema;
    private final TextField hostname;
    private final IntegerField port;
    private final SchemaDescr schemata[];
    private ValueChangeMode valueChangeMode;

    public UrlField(SchemaDescr schemata[]) {
        super(new String());
        this.schemata = schemata;
        Binder<URI> binder = new Binder<>();

        schema = new Select<>();
        schema.setLabel("Schema");
        schema.setItems(schemata);
        schema.setItemLabelGenerator((item) -> item.name);
        schema.setWidth(6, Unit.EM);
        schema.setValue(schemata[0]);

        hostname = new TextField("Hostname");
        hostname.setWidthFull();
        hostname.setValue("example.com");

        port = new IntegerField("Port");
        port.setMin(1);
        port.setMax(65535);
        port.setStepButtonsVisible(true);
        port.setWidth(8, Unit.EM);
        port.setValue(schemata[0].defaultPort());

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

        schema.addValueChangeListener((e) -> {
            if (e.getValue() != null) {
                port.setValue(e.getValue().defaultPort);
                updateUrlValue();
            }
        });
        hostname.addValueChangeListener((e) -> {
            if (e.getValue() != null) {
                updateUrlValue();
            }
        });
        port.addValueChangeListener((e) -> {
            updateUrlValue();
        });
    }

    @Override
    protected void setPresentationValue(String uriStr) {
        if (uriStr == null || uriStr.isEmpty()) {
            schema.setValue(schemata[0]);
            hostname.setValue("example.com");
            port.setValue(schemata[0].defaultPort());
        } else
        try {
            URI uri = new URI(uriStr);
            for (var s : schemata) {
                if (s.name().equals(uri.getScheme())) {
                    schema.setValue(s);
                }
            }
            hostname.setValue(uri.getHost());
            port.setValue(uri.getPort());
        } catch (URISyntaxException ex) {
            log.error("Invalid URI %s: %s".formatted(uriStr, ex.getMessage()));
        }
    }

    private void updateUrlValue() {
        setModelValue(
                "%s://%s:%d".formatted(
                        schema.getValue().name(),
                        hostname.getValue(),
                        port.getValue()
                ),
                true
        );
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
}
