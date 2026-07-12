/*
 * Copyright (C) 2026 claas
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package at.nieslony.arachne.onetimeview;

import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author claas
 */
@Route(value = "one-time-views", layout = ViewTemplate.class)
@PageTitle("One Time Views")
@RolesAllowed("ADMIN")
@Slf4j
public class OneTimeViewOverview extends TabSheet {

    @Autowired
    OneTimeViewRepository oneTimeViewRepository;

    @Autowired
    Settings settings;

    private Grid<OneTimeViewModel> otvList;

    @PostConstruct
    public void init() {
        add("Registered One Time Views", createOtvListTab());
        add("Settings", createSettingsTab());

        setWidthFull();
    }

    private Component createOtvListTab() {
        otvList = new Grid<>();
        otvList.addColumn(OneTimeViewModel::getId)
                .setHeader("Id")
                .setSortable(true);
        otvList.addColumn(OneTimeViewModel::getUsername)
                .setHeader("Valid for User")
                .setSortable(true);
        otvList.addColumn(OneTimeViewModel::getView)
                .setHeader("View")
                .setSortable(true);
        otvList.addColumn(OneTimeViewModel::getView)
                .setHeader("Valid until")
                .setSortable(true);
        otvList.setItems(oneTimeViewRepository.findAll());
        otvList.setHeightFull();

        return otvList;
    }

    private Component createSettingsTab() {
        OneTimeViewSettings oneTimeViewSettings = settings.getSettings(OneTimeViewSettings.class);
        Binder<OneTimeViewSettings> binder = new Binder<>();

        IntegerField validDaysField = new IntegerField("Valid days");
        validDaysField.setMin(1);
        validDaysField.setMax(31);
        validDaysField.setRequired(true);
        validDaysField.setStepButtonsVisible(true);
        binder.forField(validDaysField)
                .bind(OneTimeViewSettings::getValidDays, OneTimeViewSettings::setValidDays);

        Checkbox ignoreWeekendField = new Checkbox("Ignore Weekends");
        binder.forField(ignoreWeekendField)
                .bind(OneTimeViewSettings::isIgnoreWeekends, OneTimeViewSettings::setIgnoreWeekends);

        Button saveButton = new Button("Save", e -> {
            try {
                oneTimeViewSettings.save(settings);
            } catch (SettingsException ex) {
                log.error("Cannot save settings: " + ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);

        binder.setBean(oneTimeViewSettings);

        VerticalLayout layout = new VerticalLayout(
                validDaysField,
                ignoreWeekendField
        );
        layout.setMargin(false);
        layout.setPadding(false);

        return layout;
    }
}
