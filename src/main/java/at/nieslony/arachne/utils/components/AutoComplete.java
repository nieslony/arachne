/*
 * Copyright (C) 2025 claas
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

import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.util.List;
import java.util.function.Function;
import lombok.extern.log4j.Log4j2;

/**
 *
 * @author claas
 */
@Log4j2
public class AutoComplete<T> extends Popover {

    private final TextField parent;

    private Function<T, String> valueConverter = (value) -> value.toString();
    private Function<String, List<T>> valueCompleter;

    public AutoComplete(TextField parent) {
        this.parent = parent;
        init();
    }

    public AutoComplete(TextField parent, Function<String, List<T>> completer) {
        this.parent = parent;
        this.valueCompleter = completer;
        init();
    }

    private void init() {
        setTarget(parent);
        setPosition(PopoverPosition.BOTTOM_START);
        setOpenOnClick(false);
        setOpenOnFocus(false);

        ListBox<T> values = new ListBox<>();
        values.setRenderer(new ComponentRenderer<>(value -> {
            String pattern = parent.getValue();
            String valueStr = value.toString()
                    .replaceAll("([*()@])", "\\\\$1")
                    .replaceAll("(?i)(%s)".formatted(pattern), "**$1**");
            return new Markdown(valueStr);
        }));

        parent.setValueChangeMode(ValueChangeMode.EAGER);
        parent.addValueChangeListener((e) -> {
            if (!e.isFromClient()) {
                return;
            }
            String pattern = e.getValue();
            if (!pattern.isEmpty()) {
                List<T> items = valueCompleter.apply(pattern);
                if (items != null) {
                    values.setItems(items);
                } else {
                    values.clear();
                }
                open();
            } else {
                close();
                values.clear();
            }
        });

        values.addValueChangeListener((e) -> {
            String value = (e == null || e.getValue() == null)
                    ? ""
                    : valueConverter.apply(e.getValue());
            parent.setValue(value);
            close();
        });

        add(values);
    }

    public void setValueCompleter(Function<String, List<T>> valueCompleter) {
        this.valueCompleter = valueCompleter;
    }

    public void setValueConverter(Function<T, String> converter) {
        valueConverter = converter;
    }
}
