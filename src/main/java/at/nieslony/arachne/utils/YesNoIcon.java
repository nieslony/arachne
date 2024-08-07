/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils;

import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 *
 * @author claas
 */
public class YesNoIcon extends AbstractCompositeField<HorizontalLayout, YesNoIcon, Boolean> {

    private final Button yesIcon;
    private final Button noIcon;

    public YesNoIcon() {
        super(true);

        yesIcon = new Button(VaadinIcon.CHECK.create());
        yesIcon.addClassNames(
                LumoUtility.Padding.NONE,
                LumoUtility.Margin.NONE
        );
        yesIcon.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        yesIcon.setVisible(true);

        noIcon = new Button(VaadinIcon.CLOSE.create());
        noIcon.addClassNames(
                LumoUtility.Padding.NONE,
                LumoUtility.Margin.NONE
        );
        noIcon.addThemeVariants(ButtonVariant.LUMO_ERROR);
        noIcon.setVisible(false);

        getContent().add(yesIcon, noIcon);
        getContent().setMargin(false);
        getContent().setPadding(false);
    }

    @Override
    protected void setPresentationValue(Boolean status) {
        yesIcon.setVisible(status);
        noIcon.setVisible(!status);
    }
}
