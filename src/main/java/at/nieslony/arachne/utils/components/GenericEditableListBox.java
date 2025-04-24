/*
 * Copyright (C) 2023 claas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package at.nieslony.arachne.utils.components;

import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.value.HasValueChangeMode;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

/**
 *
 * @author claas
 */
@Slf4j
public class GenericEditableListBox<T extends Object, TE extends Component & HasValue<?, T>>
        extends AbstractCompositeField<VerticalLayout, GenericEditableListBox<T, TE>, List<T>>
        implements HasSize {

    private ListBox<T> itemsField;
    private Binder<T> binder;
    private Button clearButton;
    private Button loadDefaultsButton;
    private Supplier<List<T>> defaultsSupplier = null;
    private final TE editField;

    public GenericEditableListBox(
            String label,
            TE editField
    ) {
        super(new LinkedList<>());
        this.editField = editField;
        init(label);
    }

    private void init(String label) {
        binder = new Binder<>();

        itemsField = new ListBox<>();
        itemsField.setHeight(16, Unit.EM);
        itemsField.getStyle()
                .setBorder("1px solid var(--lumo-primary-color)")
                .setBackground("var(--lumo-primary-color-10pct)");

        NativeLabel elbLabel = new NativeLabel(label);
        elbLabel.addClassNames(
                LumoUtility.FontSize.SMALL,
                LumoUtility.FontWeight.BOLD,
                LumoUtility.TextColor.BODY
        );

        if (editField instanceof HasValueChangeMode hvm) {
            hvm.setValueChangeMode(ValueChangeMode.EAGER);
        }

        Button addButton = new Button(
                VaadinIcon.PLUS.create(),
                e -> {
                    List<T> items = new LinkedList<>(getValue());
                    var newItem = editField.getValue();
                    items.add(newItem);
                    itemsField.setItems(items);
                    itemsField.setValue(newItem);
                    setModelValue(new LinkedList<>(items), true);
                });
        addButton.setTooltipText("Add");
        addButton.setEnabled(false);
        Button updateButton = new Button(
                VaadinIcon.REFRESH.create(),
                e -> {
                    List<T> items = new LinkedList<>(getValue());
                    var newItem = editField.getValue();
                    items.remove(itemsField.getValue());
                    items.add(newItem);
                    itemsField.setItems(items);
                    itemsField.setValue(newItem);
                    setModelValue(new LinkedList<>(items), true);
                });
        updateButton.setTooltipText("Update");
        updateButton.setEnabled(false);
        Button removeButton = new Button(
                VaadinIcon.MINUS.create(),
                e -> {
                    List<T> items = new LinkedList<>(getValue());
                    items.remove(itemsField.getValue());
                    itemsField.setItems(items);
                    if (!items.isEmpty()) {
                        itemsField.setValue(items.get(0));
                    }
                    setModelValue(new LinkedList<>(items), true);
                });
        removeButton.setTooltipText("Delete");
        removeButton.setEnabled(false);
        clearButton = new Button(VaadinIcon.TRASH.create(),
                (t) -> {
                    List<T> items = new LinkedList<>(getValue());
                    items.clear();
                    itemsField.setItems(items);
                    setModelValue(new LinkedList<>(items), true);
                }
        );
        clearButton.setTooltipText("Delete All");
        clearButton.setEnabled(false);
        loadDefaultsButton = new Button(
                VaadinIcon.DOWNLOAD.create(),
                (e) -> {
                    if (defaultsSupplier != null) {
                        setValue(defaultsSupplier.get());
                    }
                }
        );
        loadDefaultsButton.setVisible(false);

        getContent().add(
                elbLabel,
                itemsField,
                editField,
                new HorizontalLayout(
                        addButton,
                        updateButton,
                        removeButton,
                        clearButton,
                        loadDefaultsButton
                )
        );
        itemsField.setWidthFull();
        if (editField instanceof HasSize hs) {
            hs.setWidthFull();
        }
        addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderRadius.MEDIUM
        );

        AtomicReference<T> edit = new AtomicReference<>();
        binder.forField(editField)
                .withValidator(getValidator())
                .asRequired()
                .bind(
                        ip -> {
                            return edit.get();
                        },
                        (ip, v) -> {
                            edit.set(v);
                        }
                );

        itemsField.addValueChangeListener((e) -> {
            if (e.getValue() != null) {
                editField.setValue(e.getValue());
                updateButton.setEnabled(true);
                removeButton.setEnabled(true);
            } else {
                editField.clear();
                updateButton.setEnabled(false);
                removeButton.setEnabled(false);
            }
        });

        binder.addStatusChangeListener((sce) -> {
            addButton.setEnabled(!sce.hasValidationErrors());
            updateButton.setEnabled(
                    !sce.hasValidationErrors() && itemsField.getValue() != null
            );
        });

        getStyle().setBorder("1px solid var(--lumo-contrast-10pct)");
    }

    protected Validator<T> getValidator() {
        if (editField instanceof HasValidation ef) {
            return (t, vc) -> ef.isInvalid()
                    ? ValidationResult.error(ef.getErrorMessage())
                    : ValidationResult.ok();
        } else {
            return (t, vc) -> ObjectUtils.isEmpty(t)
                    ? ValidationResult.error("Empty value not allowed")
                    : ValidationResult.ok();
        }
    }

    @Override
    protected void setPresentationValue(List<T> v) {
        if (v != null) {
            itemsField.setItems(v);
            clearButton.setEnabled(!v.isEmpty());
            getElement().setPropertyList("value", v);
        }
    }

    public void setDefaultValuesSupplier(Supplier<List<T>> defaultsSupplier) {
        setDefaultValuesSupplier(null, defaultsSupplier);
    }

    public void setDefaultValuesSupplier(String toolTipText, Supplier<List<T>> defaultsSupplier) {
        this.defaultsSupplier = defaultsSupplier;
        loadDefaultsButton.setVisible(true);
        if (toolTipText == null || toolTipText.isEmpty()) {
            loadDefaultsButton.setTooltipText("Load default values");
        } else {
            loadDefaultsButton.setTooltipText(toolTipText);
        }
    }
}
