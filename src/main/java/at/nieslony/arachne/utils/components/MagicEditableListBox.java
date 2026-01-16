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
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.HasValidator;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author claas
 * @param <T>
 */
@Slf4j
public class MagicEditableListBox<T extends Object>
        extends AbstractCompositeField<VerticalLayout, MagicEditableListBox<T>, List<T>>
        implements HasSize {

    private ListBox<T> itemsField;
    private Button clearButton;

    private final Supplier<? extends HasValue<?, T>> valueEditorSupplier;
    private final Class<T> valueClass;
    private final String labelString;

    public MagicEditableListBox(
            Class<T> valueClass,
            String label,
            Supplier<? extends HasValue<?, T>> valueEditorSupplier
    ) {
        super(new LinkedList<>());
        this.valueClass = valueClass;
        this.valueEditorSupplier = valueEditorSupplier;
        this.labelString = label;
        init(label);
    }

    class EditDlg<E> extends Dialog {

        EditDlg(
                String title,
                MagicEditableListBox<E> listBox,
                E value,
                HasValue<?, E> valueEditor
        ) {
            setHeaderTitle(title);
            add((Component) valueEditor);

            Button okButton = new Button("OK", (e) -> {
                List<E> items = new LinkedList<>(listBox.getValue());
                if (value != null) {
                    items.remove(value);
                }
                E newValue = valueEditor.getValue();
                items.add(newValue);
                listBox.updateModelValue(new LinkedList<>(items));
                close();
            });
            okButton.addThemeVariants(ButtonVariant.AURA_PRIMARY);
            Button cancelButton = new Button("Cancel", (e) -> {
                close();
            });

            getFooter().add(cancelButton, okButton);

            if (valueEditor instanceof HasValidator<E> hasVal) {
                hasVal.addValidationStatusChangeListener((e) -> {
                    okButton.setEnabled(e.getNewStatus());
                });
            }

            valueEditor.setValue(value);
        }
    }

    private void init(String labelTxt) {
        NativeLabel label = new NativeLabel(labelTxt);

        itemsField = new ListBox<>();
        itemsField.setHeight(16, Unit.EM);
        itemsField.getStyle()
                .setBorder("1px solid var(--vaadin-border-color)")
                .setBackground("var(--vaadin-background-color)");
        itemsField.setWidthFull();

        Button addButton = new Button(
                VaadinIcon.PLUS.create(),
                (e) -> editValue(null)
        );
        addButton.setTooltipText("Add");

        Button removeButton = new Button(
                VaadinIcon.MINUS.create(),
                (e) -> {
                    List<T> values = new LinkedList<>(getValue());
                    values.remove(itemsField.getValue());
                    updateModelValue(values);
                }
        );
        removeButton.setTooltipText("Delete");
        removeButton.setEnabled(false);

        clearButton = new Button(
                VaadinIcon.TRASH.create(),
                (e) -> updateModelValue(new LinkedList<>())
        );
        clearButton.setTooltipText("Delete All");
        clearButton.setEnabled(false);

        Button editButton = new Button(
                VaadinIcon.EDIT.create(),
                (e) -> editValue(itemsField.getValue())
        );
        editButton.setTooltipText("Edit");
        editButton.setEnabled(false);

        getContent().add(
                label,
                itemsField,
                new HorizontalLayout(
                        addButton,
                        removeButton,
                        clearButton,
                        editButton
                )
        );
        getContent().setFlexGrow(1, itemsField);
        getContent().setMargin(false);
        getContent().setPadding(false);
        itemsField.setWidthFull();

        itemsField.addValueChangeListener((vcl) -> {
            boolean enable = !vcl.getHasValue().isEmpty();
            removeButton.setEnabled(enable);
            editButton.setEnabled(enable);
        });
    }

    @Override
    protected void setPresentationValue(List<T> v) {
        if (v != null) {
            itemsField.setItems(v);
            clearButton.setEnabled(!v.isEmpty() && isEnabled());
            getElement().setPropertyList("value", v);
        }
    }

    void editValue(T value) {
        String title = value == null
                ? "Add " + labelString
                : "Edit " + labelString;
        if (value == null) {
            try {
                value = valueClass.getDeclaredConstructor().newInstance();
            } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
                log.error("cannot create new instance: " + ex.getMessage());
                return;
            }
        }
        EditDlg<T> dlg = new EditDlg<>(
                title,
                this,
                value, valueEditorSupplier.get()
        );
        dlg.open();
    }

    void updateModelValue(List<T> items) {
        clearButton.setEnabled(!items.isEmpty());
        itemsField.setItems(items);
        setModelValue(items, true);
    }

    public void setItemRenderer(ComponentRenderer<?, T> renderer) {
        itemsField.setRenderer(renderer);
    }
}
