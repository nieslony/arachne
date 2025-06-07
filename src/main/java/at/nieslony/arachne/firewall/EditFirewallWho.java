/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.firewall;

import at.nieslony.arachne.ldap.LdapSettings;
import at.nieslony.arachne.usermatcher.EverybodyMatcher;
import at.nieslony.arachne.usermatcher.LdapGroupUserMatcher;
import at.nieslony.arachne.usermatcher.UserMatcherCollector;
import at.nieslony.arachne.usermatcher.UserMatcherInfo;
import at.nieslony.arachne.usermatcher.UsernameMatcher;
import at.nieslony.arachne.utils.components.LdapAutoComplete;
import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.HasValidator;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValidationStatusChangeEvent;
import com.vaadin.flow.data.binder.ValidationStatusChangeListener;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.shared.Registration;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author claas
 */
public class EditFirewallWho extends AbstractCompositeField<VerticalLayout, EditFirewallWho, FirewallWho>
        implements HasValidator<FirewallWho> {

    private Binder<FirewallWho> binder;
    private final Collection<ValidationStatusChangeListener<FirewallWho>> validationStatusListeners = new ArrayList<>();
    private final UserMatcherCollector userMatcherCollector;

    public EditFirewallWho(
            UserMatcherCollector userMatcherCollector,
            LdapSettings ldapSettings
    ) {
        super(new FirewallWho());
        this.userMatcherCollector = userMatcherCollector;
        binder = new Binder<>();

        Select<UserMatcherInfo> userMatchersSelect = new Select<>();
        userMatchersSelect.setLabel("User Matcher");
        userMatchersSelect.setItems(
                userMatcherCollector.getAllUserMatcherInfo()
                        .stream()
                        .filter(c -> !c.getClassName().equals(EverybodyMatcher.class.getName()))
                        .toList()
        );
        userMatchersSelect.setEmptySelectionAllowed(false);
        userMatchersSelect.setWidthFull();
        binder.forField(userMatchersSelect)
                .asRequired()
                .bind(
                        rr -> {
                            return new UserMatcherInfo(rr.getUserMatcherClassName());
                        },
                        (rr, v) -> {
                            rr.setUserMatcherClassName(v.getClassName());
                        }
                );

        TextField parameterField = new TextField();
        LdapAutoComplete parameterFieldComplete = new LdapAutoComplete(
                parameterField,
                ldapSettings
        );
        parameterField.setWidthFull();
        binder.forField(parameterField)
                .withValidator(
                        text -> {
                            String label = userMatchersSelect.getValue().getParameterLabel();
                            if (label == null || label.isEmpty()) {
                                return true;
                            }
                            return !parameterField.getValue().isEmpty();
                        },
                        "Value required")
                .bind(FirewallWho::getParameter, FirewallWho::setParameter);

        var content = getContent();
        getContent().add(
                userMatchersSelect,
                parameterField
        );
        content.setMargin(false);
        content.setPadding(false);
        content.setMinWidth(30, Unit.EM);

        binder.addValueChangeListener((e) -> {
            FirewallWho newValue = binder.getBean();
            setModelValue(newValue, true);

            binder.validate();
        });

        binder.addStatusChangeListener((sce) -> {
            var event = new ValidationStatusChangeEvent<>(this, binder.isValid());
            validationStatusListeners.forEach(
                    listener -> {
                        listener.validationStatusChanged(event);
                    }
            );
        });

        userMatchersSelect.addValueChangeListener((e) -> {
            String labelTxt = e.getValue().getParameterLabel();
            parameterField.setLabel(labelTxt);
            if (labelTxt != null && !labelTxt.isEmpty()) {
                parameterField.setVisible(true);
                binder.validate();
            } else {
                parameterField.setVisible(false);
            }
            String className = e.getValue().getClassName();
            if (className == null) {
                parameterFieldComplete.setCompleteMode(
                        LdapAutoComplete.CompleteMode.NULL
                );
            } else if (className.equals(UsernameMatcher.class.getName())) {
                parameterFieldComplete.setCompleteMode(
                        LdapAutoComplete.CompleteMode.USERS
                );
            } else if (className.equals(LdapGroupUserMatcher.class.getName())) {
                parameterFieldComplete.setCompleteMode(
                        LdapAutoComplete.CompleteMode.GROUPS
                );
            } else {
                parameterFieldComplete.setCompleteMode(
                        LdapAutoComplete.CompleteMode.NULL
                );
            }
            binder.validate();
        });
    }

    @Override
    protected void setPresentationValue(FirewallWho value) {
        if (value.getUserMatcherClassName() == null || value.getUserMatcherClassName().isEmpty()) {
            value.setUserMatcherClassName(
                    userMatcherCollector.getAllUserMatcherInfo().get(0).getClassName()
            );
        }

        binder.setBean(value);
        binder.validate();
    }

    @Override
    public Validator<FirewallWho> getDefaultValidator() {
        return (value, context) -> {
            Boolean valid = binder.isValid();
            return valid ? ValidationResult.ok() : ValidationResult.error("Invalid value");
        };
    }

    @Override
    public Registration addValidationStatusChangeListener(
            ValidationStatusChangeListener<FirewallWho> listener) {
        validationStatusListeners.add(listener);
        return () -> validationStatusListeners.remove(listener);
    }
}
