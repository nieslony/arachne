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
package at.nieslony.arachne.ldap;

import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.kerberos.KeytabException;
import at.nieslony.arachne.kerberos.KeytabFile;
import static at.nieslony.arachne.ldap.LdapSettings.LdapBindType.ANONYMOUS;
import static at.nieslony.arachne.ldap.LdapSettings.LdapBindType.BIND_DN;
import static at.nieslony.arachne.ldap.LdapSettings.LdapBindType.KEYTAB;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import at.nieslony.arachne.utils.components.EditableListBox;
import at.nieslony.arachne.utils.components.ShowNotification;
import at.nieslony.arachne.utils.components.UrlField;
import at.nieslony.arachne.utils.net.NetUtils;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.InvalidNameException;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.core.AuthenticationException;

/**
 *
 * @author claas
 */
@Route(value = "ldap-settings", layout = ViewTemplate.class)
@PageTitle("LDAP User Source")
@RolesAllowed("ADMIN")
public class LdapView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(LdapView.class);

    private final Settings settings;
    private final LdapSettings ldapSettings;
    private Binder<LdapSettings> binder;
    Button saveButton;

    public LdapView(Settings settings) {
        this.settings = settings;
        this.ldapSettings = settings.getSettings(LdapSettings.class);
        this.binder = new Binder<>();

        Checkbox enableLdapUserSource = new Checkbox("Enable LDAP user Source");
        enableLdapUserSource.setValue(true);
        binder.forField(enableLdapUserSource)
                .bind(LdapSettings::isEnableLdapUserSource, LdapSettings::setEnableLdapUserSource);

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Basics", createBasicsPage());
        tabSheet.add("Users and Groups", createUsersAndGroupsPage());
        tabSheet.setWidthFull();

        saveButton = new Button(
                "Save",
                e -> {
                    try {
                        binder.getBean().save(settings);
                    } catch (SettingsException ex) {
                        logger.error("Cannot save ldap settings: " + ex.getMessage());
                    }
                }
        );
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        enableLdapUserSource.addValueChangeListener(e -> {
            tabSheet.setVisible(e.getValue());
        });

        binder.setBean(ldapSettings);
        binder.validate();

        add(
                enableLdapUserSource,
                tabSheet,
                saveButton
        );
        setPadding(false);
    }

    final Component createUsersAndGroupsPage() {
        TextField usersOuField = new TextField("Users OU");
        binder.forField(usersOuField)
                .bind(LdapSettings::getUsersOu, LdapSettings::setUsersOu);

        TextField usersObjectClassField = new TextField("Users Object Class");
        binder.forField(usersObjectClassField)
                .bind(LdapSettings::getUsersObjectClass, LdapSettings::setUsersObjectClass);

        TextField usersAttrUsernameField = new TextField("Attribute Username");
        binder.forField(usersAttrUsernameField)
                .bind(LdapSettings::getUsersAttrUsername, LdapSettings::setUsersAttrUsername);

        Checkbox usersEnableCustomFilter = new Checkbox("Enable Custom Filter");
        binder.forField(usersEnableCustomFilter)
                .bind(LdapSettings::isUsersEnableCustomFilter, LdapSettings::setUsersEnableCustomFilter);

        TextField usersSearchFilterField = new TextField("Search Filter");

        TextField displayNameAttrField = new TextField("Attribute Display Name");
        binder.forField(displayNameAttrField)
                .bind(LdapSettings::getUsersAttrDisplayName, LdapSettings::setUsersAttrDisplayName);

        TextField emailAttrField = new TextField("Attribute E-Mail");
        binder.forField(emailAttrField)
                .bind(LdapSettings::getUsersAttrEmail, LdapSettings::setUsersAttrEmail);

        TextField testUserField = new TextField("Test and find user");
        testUserField.setWidthFull();

        Button testAndFindUserButton = new Button(
                "Find and Test",
                e -> testFindUser(testUserField.getValue())
        );
        HorizontalLayout testUserLayout = new HorizontalLayout(
                testUserField, testAndFindUserButton
        );
        testUserLayout.setFlexGrow(1, testUserField);
        testUserField.setWidthFull();
        testUserLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        FormLayout usersFormLayout = new FormLayout(
                usersOuField,
                usersObjectClassField,
                usersAttrUsernameField,
                usersEnableCustomFilter,
                usersSearchFilterField,
                displayNameAttrField,
                emailAttrField,
                testUserLayout
        );
        usersFormLayout.setColspan(usersOuField, 2);
        NativeLabel usersFormLabel = new NativeLabel("Users");
        usersFormLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.BODY);
        VerticalLayout usersLayout = new VerticalLayout(
                usersFormLabel,
                usersFormLayout
        );
        usersLayout.getStyle().setBorder("1px solid var(--lumo-contrast-10pct)");

        TextField groupsOu = new TextField("Groups OU");
        binder.forField(groupsOu)
                .bind(LdapSettings::getGroupsOu, LdapSettings::setGroupsOu);

        TextField groupsObjectclass = new TextField("Groups Object Class");
        binder.forField(groupsObjectclass)
                .bind(LdapSettings::getGroupsObjectClass, LdapSettings::setGroupsObjectClass);

        TextField groupsAttrName = new TextField("Attribute Name");
        binder.forField(groupsAttrName)
                .bind(LdapSettings::getGroupsAttrName, LdapSettings::setGroupsAttrName);

        Checkbox groupsEnableCustomFilter = new Checkbox("Enable Custom Filter");
        binder.forField(groupsEnableCustomFilter)
                .bind(LdapSettings::isGroupsEnableCustomFilter, LdapSettings::setGroupsEnableCustomFilter);

        TextField groupsSearchFilter = new TextField("Custom Search Filter");
        binder.forField(groupsSearchFilter)
                .bind(LdapSettings::getGroupsCustomFilter, LdapSettings::setGroupsCustomFilter);

        TextField groupsAttrDescription = new TextField("Attribute Description");
        binder.forField(groupsAttrDescription)
                .bind(LdapSettings::getGroupsAttrDescription, LdapSettings::setGroupsAttrDescription);

        TextField groupsAttrMember = new TextField("Members Attribute");
        binder.forField(groupsAttrMember)
                .bind(LdapSettings::getGroupsAttrMember, LdapSettings::setGroupsAttrMember);

        TextField testAndFindGroupField = new TextField("Test and find group");
        Button testAndFindGroupButton = new Button(
                "Find and Test",
                e -> testFindGroup(testAndFindGroupField.getValue())
        );
        HorizontalLayout testAndFindGroupLayout = new HorizontalLayout(
                testAndFindGroupField,
                testAndFindGroupButton
        );
        testAndFindGroupLayout.setFlexGrow(1, testAndFindGroupField);
        testAndFindGroupLayout.setWidthFull();
        testAndFindGroupLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        FormLayout groupsFormLayout = new FormLayout(
                groupsOu,
                groupsObjectclass,
                groupsAttrName,
                groupsEnableCustomFilter,
                groupsSearchFilter,
                groupsAttrDescription,
                groupsAttrMember,
                testAndFindGroupLayout
        );
        groupsFormLayout.setColspan(groupsOu, 2);
        NativeLabel groupsLabel = new NativeLabel("Groups");
        groupsLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.BODY);
        VerticalLayout groupsLayout = new VerticalLayout(
                groupsLabel,
                groupsFormLayout
        );
        groupsLayout.getStyle().setBorder("1px solid var(--lumo-contrast-10pct)");

        MenuBar loadDefaultsMenu = new MenuBar();
        loadDefaultsMenu.addThemeVariants(MenuBarVariant.LUMO_DROPDOWN_INDICATORS);
        MenuItem menuItem = loadDefaultsMenu.addItem("Load Defaults for...");
        SubMenu subMenu = menuItem.getSubMenu();
        subMenu.addItem("FreeIPA", e -> {
            usersOuField.setValue("cn=users,cn=accounts");
            usersObjectClassField.setValue("posixaccount");
            usersAttrUsernameField.setValue("krbCanonicalName");
            displayNameAttrField.setValue("displayName");
            emailAttrField.setValue("mail");

            groupsOu.setValue("cn=groups,cn=accounts");
            groupsObjectclass.setValue("posixgroup");
            groupsAttrName.setValue("cn");
            groupsAttrMember.setValue("member");
            groupsAttrDescription.setValue("description");
            groupsSearchFilter.setValue("");
        });

        usersEnableCustomFilter.addValueChangeListener(
                e -> {
                    if (ldapSettings.isUsersEnableCustomFilter()) {
                        usersSearchFilterField.setValue(
                                ldapSettings.getUsersCustomFilter()
                        );
                        usersSearchFilterField.setEnabled(true);
                    } else {
                        usersSearchFilterField.setValue(
                                ldapSettings.getUsersFilter()
                        );
                        usersSearchFilterField.setEnabled(false);
                    }
                }
        );

        groupsEnableCustomFilter.addValueChangeListener(
                e -> {
                    if (ldapSettings.isGroupsEnableCustomFilter()) {
                        groupsSearchFilter.setValue(
                                ldapSettings.getGroupsCustomFilter()
                        );
                        groupsSearchFilter.setEnabled(true);
                    } else {
                        groupsSearchFilter.setValue(
                                ldapSettings.getGroupsFilter()
                        );
                        groupsSearchFilter.setEnabled(false);
                    }
                }
        );

        FormLayout layout = new FormLayout(
                usersLayout,
                groupsLayout
        );

        return new VerticalLayout(
                loadDefaultsMenu,
                layout
        );
    }

    final Component createBasicsPage() {
        EditableListBox ldapUrlsEditor = new EditableListBox(
                "LDAP Sources",
                new UrlField(UrlField.SCHEMATA_LDAP)
        );
        ldapUrlsEditor.setDefaultValuesSupplier(
                "Guess from DNS",
                () -> {
                    List<String> ret = new LinkedList<>();
                    try {
                        var recs = NetUtils.srvLookup("ldap");
                        if (recs != null) {
                            ret.addAll(recs.stream()
                                    .map((r) -> "ldap://%s:%d".formatted(
                                    r.getHostname(),
                                    r.getPort())
                                    )
                                    .toList()
                            );
                        }
                    } catch (NamingException ex) {
                        logger.error("DNS lookup failed: " + ex.getMessage());
                    }
                    return ret;
                }
        );
        binder.forField(ldapUrlsEditor)
                .bind(
                        (source) -> source.getLdapUrls()
                                .stream()
                                .map((url) -> url.toString())
                                .toList(),
                        (bean, value) -> bean.setLdapUrls(
                                value.stream()
                                        .map((u) -> new LdapUrl(u))
                                        .toList()
                        )
                );

        TextField baseDnField = new TextField("Base DN");
        baseDnField.setWidthFull();
        baseDnField.setClearButtonVisible(true);
        binder.forField(baseDnField)
                .asRequired("Value Required")
                .bind(LdapSettings::getBaseDn, LdapSettings::setBaseDn);

        RadioButtonGroup<LdapSettings.LdapBindType> bindType
                = new RadioButtonGroup<>("Authentication Type");
        bindType.setItems(LdapSettings.LdapBindType.values());
        binder.forField(bindType)
                .bind(LdapSettings::getBindType, LdapSettings::setBindType);

        TextField bindDnField = new TextField("Bind DN");
        bindDnField.setWidthFull();
        bindDnField.setClearButtonVisible(true);
        binder.forField(bindDnField)
                .bind(LdapSettings::getBindDn, LdapSettings::setBindDn);

        PasswordField bindPasswordField = new PasswordField("Bind Password");
        bindPasswordField.setWidthFull();
        binder.forField(bindPasswordField)
                .bind(LdapSettings::getBindPassword, LdapSettings::setBindPassword);

        HorizontalLayout simpleBindLayout = new HorizontalLayout(
                bindDnField,
                bindPasswordField
        );
        simpleBindLayout.setWidthFull();

        TextField keytabPath = new TextField("Keytab Path");
        keytabPath.setWidthFull();
        binder.forField(keytabPath)
                .bind(LdapSettings::getKeytabPath, LdapSettings::setKeytabPath);

        ComboBox<String> bindPrincipalField = new ComboBox<>("Bind Principal");
        bindPrincipalField.setItems("");
        bindPrincipalField.setWidthFull();
        binder.forField(bindPrincipalField)
                .withValidator(
                        (value) -> {
                            return bindType.getValue() != LdapSettings.LdapBindType.KEYTAB
                            || (value != null && !value.isEmpty());
                        },
                        "Cannot be empty"
                )
                .bind(LdapSettings::getKerberosBindPricipal, LdapSettings::setKerberosBindPricipal);
        Button readPrincipalsButton = new Button(
                "Read Entries from Keytab",
                e -> {
                    try {
                        KeytabFile keytabFile = new KeytabFile(keytabPath.getValue());
                        Set<String> principals = keytabFile.getPrincipals();
                        Optional<String> principal = principals.stream().findFirst();
                        if (principal.isPresent()) {
                            bindPrincipalField.setItems(principals);
                            bindPrincipalField.setValue(principal.get());
                        }
                        binder.validate();
                    } catch (IOException | KeytabException ex) {
                        String exMsg = ex.getCause() != null
                        ? ex.getCause().getMessage()
                        : ex.getMessage();
                        String header = "Cannot read keytab %s: "
                                .formatted(
                                        keytabPath.getValue(),
                                        exMsg
                                );
                        logger.error(header + ex.getMessage());
                        ShowNotification.error(header, ex.getMessage());
                    }
                }
        );
        HorizontalLayout principalsLayout = new HorizontalLayout(
                bindPrincipalField,
                readPrincipalsButton
        );

        principalsLayout.setWidthFull();

        principalsLayout.setFlexGrow(
                1, bindPrincipalField);
        principalsLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        bindType.addValueChangeListener(e
                -> {
            switch (bindType.getValue()) {
                case ANONYMOUS -> {
                    bindDnField.setVisible(false);
                    bindPasswordField.setVisible(false);
                    keytabPath.setVisible(false);
                    bindPrincipalField.setVisible(false);
                    readPrincipalsButton.setVisible(false);
                }
                case BIND_DN -> {
                    bindDnField.setVisible(true);
                    bindPasswordField.setVisible(true);
                    keytabPath.setVisible(false);
                    bindPrincipalField.setVisible(false);
                    readPrincipalsButton.setVisible(false);
                }
                case KEYTAB -> {
                    bindDnField.setVisible(false);
                    bindPasswordField.setVisible(false);
                    keytabPath.setVisible(true);
                    bindPrincipalField.setVisible(true);
                    readPrincipalsButton.setVisible(true);
                }
            }
            binder.validate();
        });

        Button testConnectionButton = new Button(
                "Test Connection",
                e -> testLdapConnection(ldapSettings)
        );

        FormLayout layout = new FormLayout();

        layout.add(
                ldapUrlsEditor,
                new VerticalLayout(
                        baseDnField,
                        bindType,
                        simpleBindLayout,
                        keytabPath,
                        principalsLayout,
                        testConnectionButton
                )
        );

        return layout;
    }

    void testLdapConnection(LdapSettings ldapSettings) {
        try {
            LdapTemplate templ = ldapSettings.getLdapTemplate();
            templ.lookup(ldapSettings.getBaseDn());
            ShowNotification.info("Successfully connected");
        } catch (AuthenticationException ex) {
            logger.error("Authentication failed: " + ex.getMessage());
            ShowNotification.error("Connection failed", ex.getMessage());
        } catch (NameNotFoundException ex) {
            String header = "Name not found. Maybe wrong base dn. ";
            logger.error(header + ex.getMessage());
            ShowNotification.error(header, ex.getMessage());
        } catch (InvalidNameException ex) {
            logger.error(ex.getMessage());
            ShowNotification.error("Invalid Name", ex.getMessage());
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            ShowNotification.error("Connection failed", ex.getMessage());
        }
    }

    void testFindGroup(String groupname) {
        try {
            LdapTemplate ldap = ldapSettings.getLdapTemplate();

            String filter = ldapSettings.getGroupsFilter(groupname);
            var result = ldap.search(
                    ldapSettings.getGroupsOu(),
                    filter,
                    (AttributesMapper<Map<String, String>>) attrs -> {
                        Map<String, String> groupInfo = new HashMap<>();
                        logger.info(attrs.toString());
                        Attribute attr;
                        attr = attrs.get(ldapSettings.getGroupsAttrName());
                        if (attr != null) {
                            groupInfo.put("name", attr.get().toString());
                        }
                        attr = attrs.get(ldapSettings.getGroupsAttrDescription());
                        if (attr != null) {
                            groupInfo.put("description", attr.get().toString());
                        }
                        attr = attrs.get(ldapSettings.getGroupsAttrMember());
                        if (attr != null) {
                            groupInfo.put("mail", attr.get().toString());
                        }
                        return groupInfo;
                    }
            );

            Dialog dlg = new Dialog();
            dlg.setHeaderTitle("Search Result");

            String html = """
                        <dl>
                            <dt>Group Name:</dt>
                            <dd>%s</dd>
                            <dt>Description:</dt>
                            <dd>%s</dd>
                        </dl>
                          """.formatted(
                    result.get(0).get("name"),
                    result.get(0).get("description")
            );
            dlg.add(new Html(html));

            Button closeButton = new Button("Close", e -> dlg.close());
            closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            dlg.getFooter().add(closeButton);

            dlg.open();

        } catch (Exception ex) {
            String header = "LDAP search failed: ";
            logger.error(header + ex.getMessage());
            ShowNotification.error(header, ex.getMessage());
        }
    }

    void testFindUser(String username) {
        try {
            LdapTemplate ldap = ldapSettings.getLdapTemplate();
            String filter = ldapSettings.getUsersFilter(username);
            var result = ldap.search(
                    ldapSettings.getUsersOu(),
                    filter,
                    (AttributesMapper<Map<String, String>>) attrs -> {
                        Map<String, String> userInfo = new HashMap<>();
                        logger.info(attrs.toString());
                        Attribute attr;
                        attr = attrs.get(ldapSettings.getUsersAttrUsername());
                        if (attr != null) {
                            userInfo.put("uid", attr.get().toString());
                        }
                        attr = attrs.get(ldapSettings.getUsersAttrDisplayName());
                        if (attr != null) {
                            userInfo.put("displayName", attr.get().toString());
                        }
                        attr = attrs.get(ldapSettings.getUsersAttrEmail());
                        if (attr != null) {
                            userInfo.put("mail", attr.get().toString());
                        }
                        return userInfo;
                    }
            );

            Dialog dlg = new Dialog();
            dlg.setHeaderTitle("Search Result");

            String html = """
                        <dl>
                            <dt>Username:</dt>
                            <dd>%s</dd>
                            <dt>Display Name:</dt>
                            <dd>%s</dd>
                            <dt>E-Mail:</dt>
                            <dd>%s</dd>
                        </dl>
                          """.formatted(
                    result.get(0).get("uid"),
                    result.get(0).get("displayName"),
                    result.get(0).get("mail")
            );
            dlg.add(new Html(html));

            Button closeButton = new Button("Close", e -> dlg.close());
            closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            dlg.getFooter().add(closeButton);

            dlg.open();

        } catch (Exception ex) {
            String header = "LDAP search failed: ";
            logger.error(header + ex.getMessage());
            ShowNotification.error(header, ex.getMessage());
        }
    }
}
