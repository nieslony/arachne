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
package at.nieslony.arachne.utils;

import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
public class EditableListBox
        extends AbstractCompositeField<VerticalLayout, EditableListBox, List<String>> {

    private static final Logger logger = LoggerFactory.getLogger(EditableListBox.class);

    private ListBox<String> itemsField;
    private List<String> items;
    private Binder<String> binder;
    private TextField editField;

    public EditableListBox(String label) {
        super(new LinkedList<>());
        binder = new Binder<>();
        items = new LinkedList<>();

        itemsField = new ListBox<>();
        itemsField.setHeight(16, Unit.EM);
        itemsField.getStyle()
                .setBorder("1px solid var(--lumo-primary-color)")
                .setBackground("var(--lumo-primary-color-10pct)");

        NativeLabel elbLabel = new NativeLabel(label);
        elbLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.BODY);

        editField = new TextField();
        editField.setValueChangeMode(ValueChangeMode.EAGER);
        Button addButton = new Button(
                "Add",
                e -> {
                    items.add(editField.getValue());
                    itemsField.setItems(items);
                });
        Button updateButton = new Button(
                "Update",
                e -> {
                    items.remove(itemsField.getValue());
                    items.add(editField.getValue());
                    itemsField.setItems(items);
                });
        updateButton.setEnabled(false);
        Button removeButton = new Button(
                "Remove",
                e -> {
                    items.remove(itemsField.getValue());
                    itemsField.setItems(items);
                });
        removeButton.setEnabled(false);

        getContent().add(
                elbLabel,
                itemsField,
                editField,
                new HorizontalLayout(
                        addButton,
                        updateButton,
                        removeButton
                )
        );
        itemsField.setWidthFull();
        editField.setWidthFull();
        addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderRadius.MEDIUM
        );

        AtomicReference<String> edit = new AtomicReference<>("");
        binder.forField(editField)
                .withValidator(getValidator())
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
                editField.setValue("");
                updateButton.setEnabled(false);
                removeButton.setEnabled(false);
            }
        });

        getStyle().setBorder("1px solid var(--lumo-contrast-10pct)");
    }

    @Override
    public void setValue(List<String> items) {
        this.items.clear();
        this.items.addAll(items);
        itemsField.setItems(this.items);
    }

    @Override
    public List<String> getValue() {
        return new LinkedList<>(items);
    }

    protected Validator<String> getValidator() {
        return (t, vc) -> ValidationResult.ok();
    }

    @Override
    protected void setPresentationValue(List<String> v) {
        items.clear();
        if (v != null) {
            items.addAll(v);
        }
        itemsField.setItems(items);
    }
}
