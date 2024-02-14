/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 *
 * @author claas
 */
public class ShowNotification {

    public static void info(String headerText) {
        info(headerText, null);
    }

    public static void info(String headerText, String msgText) {
        Notification notification = new Notification();

        Icon icon = VaadinIcon.CHECK_CIRCLE.create();
        icon.setColor("var(--lumo-success-color)");

        var header = new Div(headerText);
        header.getStyle()
                .set("font-weight", "600")
                .setColor("var(--lumo-success-text-color)");

        HorizontalLayout layout = new HorizontalLayout();
        if (msgText != null) {
            Div msg = new Div(msgText);
            Div content = new Div(header, msg);
            layout.add(
                    icon,
                    content,
                    createCloseButton(notification)
            );
        } else {
            layout.add(
                    icon,
                    header,
                    createCloseButton(notification)
            );
        }

        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        notification.add(layout);
        notification.setDuration(5000);
        notification.setPosition(Notification.Position.MIDDLE);

        notification.open();
    }

    public static void error(String headerText, String msgText) {
        Notification notification = new Notification();

        Icon icon = VaadinIcon.WARNING.create();

        var header = new Div(headerText);
        header.getStyle()
                .set("font-weight", "600");

        var msg = new Div(msgText);
        var content = new Div(header, msg);

        HorizontalLayout layout = new HorizontalLayout(
                icon,
                content,
                createCloseButton(notification)
        );
        notification.add(layout);
        notification.setPosition(Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);

        notification.open();
    }

    private static Button createCloseButton(Notification notification) {
        Button closeBtn = new Button(VaadinIcon.CLOSE_SMALL.create(),
                clickEvent -> notification.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        return closeBtn;
    }
}
