/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils;

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
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 * @param <T>
 */
public class MagicEditableListBox<T extends Object>
        extends AbstractCompositeField<VerticalLayout, MagicEditableListBox<T>, List<T>>
        implements HasSize {

    private static final Logger logger = LoggerFactory.getLogger(MagicEditableListBox.class);

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
            okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            Button cancelButton = new Button("Cancel", (e) -> {
                close();
            });

            getFooter().add(cancelButton, okButton);

            if (valueEditor instanceof HasValidator<E> hasVal) {
                okButton.setEnabled(false);
                hasVal.addValidationStatusChangeListener((e) -> {
                    okButton.setEnabled(e.getNewStatus());
                });
            }

            valueEditor.setValue(value);
        }
    }

    private void init(String label) {
        NativeLabel mlbLabel = new NativeLabel(label);
        mlbLabel.addClassNames(
                LumoUtility.FontSize.SMALL,
                LumoUtility.FontWeight.BOLD,
                LumoUtility.TextColor.BODY
        );

        itemsField = new ListBox<>();
        itemsField.setHeight(16, Unit.EM);
        itemsField.getStyle()
                .setBorder("1px solid var(--lumo-primary-color)")
                .setBackground("var(--lumo-primary-color-10pct)");
        itemsField.setWidthFull();

        Button addButton = new Button(
                VaadinIcon.PLUS.create(),
                (e) -> editValue(null)
        );
        addButton.setTooltipText("Add");

        Button removeButton = new Button(
                VaadinIcon.DEL_A.create(),
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

        HorizontalLayout buttonLayout = new HorizontalLayout(
                addButton,
                removeButton,
                clearButton,
                editButton
        );

        itemsField.addValueChangeListener((vcl) -> {
            boolean enable = !vcl.getHasValue().isEmpty();
            removeButton.setEnabled(enable);
            editButton.setEnabled(enable);
        });

        getContent().add(
                mlbLabel,
                itemsField,
                buttonLayout
        );
    }

    @Override
    protected void setPresentationValue(List<T> v) {
        itemsField.setItems(v);
    }

    void editValue(T value) {
        String title = value == null
                ? "Add " + labelString
                : "Edit " + labelString;
        if (value == null) {
            try {
                value = valueClass.getDeclaredConstructor().newInstance();
            } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
                logger.error("cannot create new instance: " + ex.getMessage());
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
}
