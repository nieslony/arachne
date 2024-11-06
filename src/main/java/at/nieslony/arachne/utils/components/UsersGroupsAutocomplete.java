/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils.components;

import at.nieslony.arachne.ldap.LdapSettings;
import com.vaadin.componentfactory.Autocomplete;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
public class UsersGroupsAutocomplete extends Autocomplete {

    private static final Logger logger = LoggerFactory.getLogger(UsersGroupsAutocomplete.class);

    public enum CompleteMode {
        NULL, USERS, GROUPS
    }

    private final LdapSettings ldapSettings;
    private final int maxValues;
    private CompleteMode completeMode;

    public UsersGroupsAutocomplete(LdapSettings ldapSettings, int maxValues) {
        super(maxValues);
        this.ldapSettings = ldapSettings;
        this.completeMode = CompleteMode.NULL;
        this.maxValues = maxValues;

        addChangeListener(event
                -> setOptions(complete(event.getValue()))
        );
        addAutocompleteValueAppliedListener((newValue) -> {
            setValue(cleanValue(newValue.getValue()));
        });
    }

    public void setCompleteMode(CompleteMode mode) {
        completeMode = mode;
    }

    @Override
    public String getValue() {
        return cleanValue(super.getValue());
    }

    public void setValue(String v) {
        if (v == null) {
            super.setValue("");
        } else {
            super.setValue(v);
        }
    }

    private String cleanValue(String value) {
        if (value != null) {
            return value.replaceAll("\\s*\\(.*\\)", "");
        } else {
            return value;
        }
    }

    private List<String> complete(String pattern) {
        logger.info("Completing " + pattern);
        if (pattern == null || pattern.isEmpty()) {
            return new LinkedList<>();
        }
        List<String> options = ldapSettings != null && ldapSettings.isValid()
                ? switch (completeMode) {
            case GROUPS ->
                ldapSettings.findGroupsPretty("*" + pattern + "*", maxValues);
            case USERS ->
                ldapSettings.findUsersPretty("*" + pattern + "*", maxValues);
            case NULL ->
                new LinkedList<>();
        } : new LinkedList<>();
        logger.info("Found: " + options.toString());
        return options;
    }
}
