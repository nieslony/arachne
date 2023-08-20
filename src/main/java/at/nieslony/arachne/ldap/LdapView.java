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
import at.nieslony.arachne.utils.HostnameValidator;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
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
import java.util.concurrent.atomic.AtomicReference;
import javax.naming.directory.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.AuthenticationException;
import org.springframework.ldap.InvalidNameException;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;

/**
 *
 * @author claas
 */
@Route(value = "ldap_settings", layout = ViewTemplate.class)
@PageTitle("LDAP Settings | Arachne")
@RolesAllowed("ADMIN")
public class LdapView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(LdapView.class);

    private final Settings settings;
    private final LdapSettings ldapSettings;
    private Binder<LdapSettings> binder;
    Button saveButton;

    public LdapView(Settings settings) {
        this.settings = settings;
        this.ldapSettings = new LdapSettings(settings);
        this.binder = new Binder();

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
                e -> binder.getBean().save(settings)
        );

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
        usersLayout.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderRadius.MEDIUM
        );

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
        groupsLayout.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderRadius.MEDIUM
        );

        MenuBar loadDefaultsMenu = new MenuBar();
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
        var ldapUrlsEditor = createUrlsEditor(ldapSettings);

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

        ComboBox<String> bindPrincipalField = new ComboBox<String>("Bind Principal");
        bindPrincipalField.setItems("");
        bindPrincipalField.setWidthFull();
        binder.forField(bindPrincipalField)
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
                    } catch (IOException | KeytabException ex) {

                    }
                }
        );
        HorizontalLayout principalsLayout = new HorizontalLayout(
                bindPrincipalField,
                readPrincipalsButton
        );
        principalsLayout.setWidthFull();
        principalsLayout.setFlexGrow(1, bindPrincipalField);
        principalsLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        bindType.addValueChangeListener(e -> {
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
        Notification notification = new Notification();
        notification.setDuration(5000);
        String msg;
        try {
            LdapTemplate templ = ldapSettings.getLdapTemplate();
            var res = templ.lookup(ldapSettings.getBaseDn());
            msg = "Connection successful";
        } catch (AuthenticationException ex) {
            msg = "Authentication failed: " + ex.getMessage();
            logger.error(msg);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (NameNotFoundException ex) {
            msg = "Name not found. Maybe wrong base dn";
            logger.error(msg);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (InvalidNameException ex) {
            msg = ex.getMessage();
            logger.error(msg);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            msg = ex.getMessage();
            logger.error(msg);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        notification.setText(msg);
        notification.open();
    }

    VerticalLayout createUrlsEditor(LdapSettings ldapSettings) {
        Binder binder = new Binder();

        Button guessFromDns = new Button("Guess URLs from DNS");

        ListBox<LdapUrl> ldapUrlsField = new ListBox<>();
        ldapUrlsField.setHeight(24, Unit.EX);
        ldapUrlsField.setItems(ldapSettings.getLdapUrls());
        ldapUrlsField.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.PRIMARY,
                LumoUtility.Background.PRIMARY_10
        );
        ldapUrlsField.setWidthFull();

        Select<LdapProtocol> protocolField = new Select<>();
        protocolField.setLabel("Protocol");
        protocolField.setItems(LdapProtocol.values());
        protocolField.setWidth(6, Unit.EM);
        protocolField.setValue(LdapProtocol.LDAP);

        TextField hostnameField = new TextField("LDAP Host");
        hostnameField.setMinWidth(24, Unit.EX);
        hostnameField.setPattern(
                "[a-z][a-z0-9\\-]*(\\.[a-z][a-z0-9\\-]*)*"
        );
        hostnameField.setClearButtonVisible(true);
        AtomicReference<String> hostname = new AtomicReference<>("");
        binder.forField(hostnameField)
                .withValidator(new HostnameValidator())
                .bind(
                        s -> hostname.get(),
                        (s, v) -> {
                            hostname.set(v.toString());
                        }
                );
        hostnameField.setValueChangeMode(ValueChangeMode.EAGER);

        IntegerField portField = new IntegerField("Port");
        portField.setMin(1);
        portField.setMax(65534);
        portField.setStepButtonsVisible(true);
        portField.setWidth(8, Unit.EM);
        portField.setValue(389);

        HorizontalLayout urlsLayout = new HorizontalLayout(
                protocolField,
                hostnameField,
                portField);
        urlsLayout.setFlexGrow(1, hostnameField);
        urlsLayout.setWidthFull();

        Button addUrlButton = new Button(
                "Add",
                e -> {
                    List<LdapUrl> ldapUrls = new LinkedList<>(ldapSettings.getLdapUrls());
                    logger.info(ldapUrls.toString());
                    ldapUrls.add(
                            new LdapUrl(
                                    protocolField.getValue(),
                                    hostnameField.getValue(),
                                    portField.getValue()
                            )
                    );
                    logger.info(ldapUrls.toString());
                    ldapUrlsField.setItems(ldapUrls);
                    ldapSettings.setLdapUrls(ldapUrls);
                });
        Button updateUrlButton = new Button(
                "Update",
                e -> {
                    List<LdapUrl> ldapUrls = new LinkedList<>(ldapSettings.getLdapUrls());
                    ldapUrls.remove(ldapUrlsField.getValue());
                    ldapUrls.add(
                            new LdapUrl(
                                    protocolField.getValue(),
                                    hostnameField.getValue(),
                                    portField.getValue()
                            )
                    );
                    ldapUrlsField.setItems(ldapUrls);
                    ldapSettings.setLdapUrls(ldapUrls);
                });
        Button removeUrlButton = new Button(
                "Remove", e -> {
                    List<LdapUrl> ldapUrls = new LinkedList<>(ldapSettings.getLdapUrls());
                    ldapUrls.remove(ldapUrlsField.getValue());
                    ldapUrlsField.setItems(ldapUrls);
                    ldapSettings.setLdapUrls(ldapUrls);
                });
        HorizontalLayout buttonsLayout = new HorizontalLayout(
                addUrlButton,
                updateUrlButton,
                removeUrlButton
        );

        guessFromDns.addClickListener((t) -> {
            ldapSettings.guessDefaultsFromDns(settings);
            ldapUrlsField.setItems(ldapSettings.getLdapUrls());
        });

        ldapUrlsField.addValueChangeListener(e -> {
            LdapUrl url = ldapUrlsField.getValue();
            if (url != null) {
                protocolField.setValue(url.getProtocol());
                hostnameField.setValue(url.getHost());
                portField.setValue(url.getPort());
            }
        });

        hostnameField.addValueChangeListener(e -> {
            if (e.getValue().isEmpty()) {
                addUrlButton.setEnabled(false);
                updateUrlButton.setEnabled(false);
            } else {
                addUrlButton.setEnabled(true);
                updateUrlButton.setEnabled(true);
            }
        });

        NativeLabel tryLdapUrlsLabel = new NativeLabel("Try LDAP URLs");
        tryLdapUrlsLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.BODY);
        VerticalLayout layout = new VerticalLayout(
                tryLdapUrlsLabel,
                guessFromDns,
                ldapUrlsField,
                urlsLayout,
                buttonsLayout
        );
        layout.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderRadius.MEDIUM
        );

        return layout;
    }

    boolean ldapUrlExists(
            LdapSettings ldapSettings,
            LdapProtocol protocol,
            String hostname,
            int port) {
        LdapUrl url = new LdapUrl(protocol, hostname, port);
        return !ldapSettings.getLdapUrls().contains(url);
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
            logger.error("LDAP search failed: " + ex.getMessage());
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
            logger.error("LDAP search failed: " + ex.getMessage());
        }
    }
}
