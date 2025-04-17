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

import at.nieslony.arachne.ldap.LdapController;
import at.nieslony.arachne.ldap.LdapSettings;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;

/**
 *
 * @author claas
 */
public class LdapAutoComplete extends AutoComplete<LdapController.PrettyResult> {

    final private LdapController ldapController = LdapController.getInstance();

    public enum CompleteMode {
        NULL, USERS, GROUPS
    }

    private final LdapSettings ldapSettings;

    private CompleteMode completeMode = CompleteMode.NULL;

    public LdapAutoComplete(TextField parent, LdapSettings ldapSettings) {
        super(parent);
        this.ldapSettings = ldapSettings;
        setValueConverter((value) -> value.name());
        setValueCompleter((v) -> onComplete(v));
    }

    private List<LdapController.PrettyResult> onComplete(String value) {
        return switch (completeMode) {
            case GROUPS ->
                ldapController.findGroupsPretty(
                ldapSettings,
                "*" + value + "*",
                5);
            case USERS ->
                ldapController.findUsersPretty(
                ldapSettings,
                "*" + value + "*",
                5);
            default ->
                null;
        };
    }

    public void setCompleteMode(CompleteMode mode) {
        completeMode = mode;
    }
}
