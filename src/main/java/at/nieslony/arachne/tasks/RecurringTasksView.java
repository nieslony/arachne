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
package at.nieslony.arachne.tasks;

import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.utils.ArachneTimeUnit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.time.Duration;
import java.time.LocalTime;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.CastUtils;

/**
 *
 * @author claas
 */
@Route(value = "recurring-tasks", layout = ViewTemplate.class)
@PageTitle("Recurring Tasks | Arachne")
@RolesAllowed("ADMIN")
public class RecurringTasksView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(RecurringTaskModel.class);

    public RecurringTasksView(RecurringTasksRepository recurringTaskRepository) {
        Grid<RecurringTaskModel> grid = new Grid<>();
        grid
                .addColumn((source) -> {
                    try {
                        Class c = Class.forName(source.getClassName());
                        return getTaskName(CastUtils.cast(c));
                    } catch (ClassNotFoundException ex) {
                        return "Unknown Class: " + source.getClassName();
                    }
                })
                .setHeader("Name");
        grid
                .addColumn((source) -> {
                    Integer interval = source.getRecurringInterval();
                    if (interval != null && interval != 0) {
                        return "%d %s"
                                .formatted(
                                        source.getRecurringInterval(),
                                        source.getTimeUnit().toString()
                                );
                    } else {
                        return "";
                    }
                })
                .setHeader("Recurring Interval");
        grid
                .addColumn((source) -> {
                    Boolean startAtFixTime = source.getStartAtFixTime();
                    if (startAtFixTime == null || !startAtFixTime) {
                        return "";
                    }
                    String startAt = source.getStartAt();
                    if (startAt != null && !startAt.isEmpty()) {
                        return startAt;
                    } else {
                        return "";
                    }
                })
                .setHeader("Start at");
        grid.addComponentColumn((source) -> {
            Button editButton = new Button("Edit...", (t) -> {
                Dialog editDialog = createEditDialog(source, (m) -> {
                    recurringTaskRepository.save(m);
                    grid.getDataProvider().refreshItem(m);
                });
                editDialog.open();
            });
            return editButton;
        });

        grid.setItems(recurringTaskRepository.findAll());

        add(grid);
    }

    private String getTaskName(Class<? extends Task> c) {
        if (c.isAnnotationPresent(TaskDescription.class)) {
            TaskDescription descr = c.getAnnotation(TaskDescription.class);
            return descr.name();
        } else {
            return c.getSimpleName();
        }
    }

    private Dialog createEditDialog(
            RecurringTaskModel model,
            Consumer<RecurringTaskModel> onSave) {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("Edit Task");

        Binder<RecurringTaskModel> binder = new Binder<>();

        Checkbox repeatTaskField = new Checkbox("Repeat Task");
        repeatTaskField.setWidthFull();
        repeatTaskField.setValue(Boolean.TRUE);
        binder.bind(
                repeatTaskField,
                RecurringTaskModel::getRepeatTask,
                RecurringTaskModel::setRepeatTask
        );

        IntegerField intervalField = new IntegerField("Interval");
        intervalField.setStepButtonsVisible(true);
        binder.bind(
                intervalField,
                RecurringTaskModel::getRecurringInterval,
                RecurringTaskModel::setRecurringInterval
        );

        Select<ArachneTimeUnit> timeUnitField = new Select<>();
        timeUnitField.setItems(ArachneTimeUnit.values());
        binder.bind(
                timeUnitField,
                RecurringTaskModel::getTimeUnit,
                RecurringTaskModel::setTimeUnit
        );

        HorizontalLayout intervalLayout
                = new HorizontalLayout(intervalField, timeUnitField);
        intervalLayout.setAlignItems(Alignment.BASELINE);
        intervalLayout.setWidthFull();
        intervalLayout.setFlexGrow(
                1.0,
                intervalField,
                timeUnitField
        );

        Checkbox fixStartTimeField = new Checkbox("Start at fix time:");
        fixStartTimeField.setValue(Boolean.TRUE);
        binder.bind(
                fixStartTimeField,
                RecurringTaskModel::getStartAtFixTime,
                RecurringTaskModel::setStartAtFixTime
        );

        TimePicker startAtField = new TimePicker();
        startAtField.setStep(Duration.ofMinutes(15));
        binder.bind(
                startAtField,
                (source) -> {
                    RecurringTaskModel.Time time = source.getStartAtAsTime();
                    if (time != null) {
                        return LocalTime.of(
                                time.hour(),
                                time.min(),
                                time.sec()
                        );
                    }
                    return null;
                },
                (source, value) -> {
                    source.setStartAt(
                            "%02d:%02d:%02d"
                                    .formatted(value.getHour(),
                                            value.getMinute(),
                                            value.getSecond()
                                    )
                    );
                }
        );

        HorizontalLayout startTimeLayout = new HorizontalLayout(
                fixStartTimeField,
                startAtField
        );
        startTimeLayout.setAlignItems(Alignment.BASELINE);
        startTimeLayout.setWidthFull();
        startTimeLayout.setFlexGrow(
                1.0,
                fixStartTimeField,
                startAtField
        );

        Button okButton = new Button("OK", (e) -> {
            try {
                binder.writeBean(model);
            } catch (ValidationException ex) {
                logger.error("Cannot write recurring task settings: " + ex.getMessage());
            }
            onSave.accept(model);
            dlg.close();
        });
        okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", (e) -> {
            dlg.close();
        });

        repeatTaskField.addValueChangeListener((e) -> {
            boolean enabled = e.getValue();
            intervalField.setEnabled(enabled);
            timeUnitField.setEnabled(enabled);
            fixStartTimeField.setEnabled(enabled);
            startAtField.setEnabled(enabled && fixStartTimeField.getValue());
        });

        fixStartTimeField.addValueChangeListener((e) -> {
            startAtField.setEnabled(e.getValue());
        });

        binder.setBean(model);

        dlg.add(new VerticalLayout(
                repeatTaskField,
                intervalLayout,
                startTimeLayout
        ));

        dlg.getFooter().add(okButton, cancelButton);

        return dlg;
    }
}
