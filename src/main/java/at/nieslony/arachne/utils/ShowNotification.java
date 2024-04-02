/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.dom.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
public class ShowNotification {

    private static final Logger logger = LoggerFactory.getLogger(ShowNotification.class);

    public static void info(String msgText) {
        createInfo(msgText, new Text("")).open();
    }

    public static void info(String headerText, String msgText) {
        createInfo(headerText, new Text(msgText)).open();
    }

    public static void info(String headerText, Component msg) {
        createInfo(headerText, msg).open();
    }

    public static Notification createInfo(String headerText) {
        return createInfo(headerText, new Text(""));
    }

    public static Notification createInfo(String headerText, String msgText) {
        return createInfo(headerText, new Text(msgText));
    }

    public static Notification createInfo(String headerText, Component msg) {
        Notification notification = createNotification(
                VaadinIcon.CHECK_CIRCLE,
                headerText,
                "var(--lumo-success-text-color)",
                msg);
        notification.setDuration(10000);

        return notification;
    }

    public static void error(String headerText, String msgText) {
        createError(headerText, new Text(msgText)).open();
    }

    public static void error(String headerText, Component msg) {
        createError(headerText, msg).open();
    }

    public static Notification createError(String headerText, String msgText) {
        return createError(headerText, new Text(msgText));
    }

    public static Notification createError(String headerText, Component msg) {
        Notification notification = createNotification(
                VaadinIcon.WARNING,
                headerText,
                "var(--lumo-base-text-color)",
                msg);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);

        return notification;
    }

    private static Notification createNotification(
            VaadinIcon vaadinIcon,
            String headerText,
            String headerColor,
            Component msg) {
        Notification notification = new Notification();
        HorizontalLayout layout = new HorizontalLayout();

        Icon icon = vaadinIcon.create();
        icon.setColor(headerColor);

        var header = new Div(headerText);
        header.getStyle()
                .setFontWeight(Style.FontWeight.BOLD)
                .setColor(headerColor);
        layout.add(
                icon,
                new Div(header, msg),
                createCloseButton(notification)
        );
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        notification.add(layout);
        notification.setPosition(Notification.Position.MIDDLE);

        return notification;
    }

    private static Button createCloseButton(Notification notification) {
        Button closeBtn = new Button(VaadinIcon.CLOSE_SMALL.create(),
                clickEvent -> notification.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        return closeBtn;
    }
}
