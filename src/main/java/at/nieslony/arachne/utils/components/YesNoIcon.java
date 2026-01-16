/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils.components;

import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

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
        yesIcon.addThemeVariants(ButtonVariant.AURA_TERTIARY);
        yesIcon.setVisible(true);
        yesIcon.setEnabled(false);

        noIcon = new Button(VaadinIcon.CLOSE.create());
        noIcon.addThemeVariants(
                ButtonVariant.AURA_DANGER,
                ButtonVariant.AURA_TERTIARY
        );
        noIcon.setVisible(false);
        noIcon.setEnabled(false);

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
