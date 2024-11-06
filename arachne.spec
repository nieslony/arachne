%global selinuxtype targeted
%global moduletype contrib
%global modulename arachne

Name:           arachne
Version:        1.3.14

Release:        1
License:        GPLv3
Source0:         %{name}-%{version}.tar.gz
Summary:        Administration server for openVPN
BuildArch:      noarch
Url:            https://github.com/nieslony/arachne

%if 0%{?fedora} >= 40
BuildRequires:  maven-openjdk21
BuildRequires:  java-21-openjdk-devel
%else
BuildRequires:  maven-openjdk17
%endif
BuildRequires:  java-17-openjdk-devel
BuildRequires:  systemd-rpm-macros
BuildRequires:  selinux-policy-devel
BuildRequires:  pkgconfig(systemd)
%{?selinux_requires}

Requires:       java-17-openjdk-headless
Requires:       openvpn
Requires:       openvpn-plugin-arachne
Requires:       arachne-dbus

Recommends:     httpd

%description
Administration server for openVPN

%prep
%setup
#-n % source_dir

%build
mvn --no-transfer-progress -DskipGenerateGitProperties=true package
make -f /usr/share/selinux/devel/Makefile arachne.pp

%install
mkdir -pv %{buildroot}/%{_datadir}/%{name}
mkdir -pv %{buildroot}/%{_unitdir}
install -v %{_builddir}/%{?buildsubdir}/target/Arachne.jar %{buildroot}/%{_datadir}/%{name}
install -v %{name}.service %{buildroot}/%{_unitdir}

install -d %{buildroot}%{_datadir}/selinux/packages
install -m 0644 arachne.pp %{buildroot}%{_datadir}/selinux/packages

%pre
%selinux_relabel_pre -s %{selinuxtype}

%post
%selinux_modules_install -s %{selinuxtype} %{_datadir}/selinux/packages/arachne.pp
semanage boolean --modify --on httpd_can_network_connect_db
getent group arachne  || groupadd --system arachne
getent passwd arachne || \
    useradd \
        --comment "Arachne openVPN Administrator" \
        --home-dir /var/lib/arachne \
        --create-home \
        --gid arachne \
        --system \
        --shell /bin/false \
        arachne
mkdir -pv /var/lib/arachne/{arachneconfig,vpnconfig}
chown -v arachne.arachne /var/lib/arachne/{arachneconfig,vpnconfig}
ln -fsv \
    /var/lib/arachne/vpnconfig/openvpn-user-server.conf \
    /etc/openvpn/server/arachne-user.conf
ln -fsv \
    /var/lib/arachne/vpnconfig/openvpn-site-server.conf \
    /etc/openvpn/server/arachne-site.conf

%postun
if [ $1 -eq 0 ]; then
    %selinux_modules_uninstall -s %{selinuxtype} arachne || :
fi

%posttrans
%selinux_relabel_post -s %{selinuxtype} || :

%files
%{_unitdir}/%{name}.service
%dir %{_datadir}/%{name}/
%{_datadir}/%{name}/Arachne.jar
%license LICENSE
%attr(0644,root,root) %{_datadir}/selinux/packages/arachne.pp

%changelog
* Wed Nov 06 2024 Claas Nieslony <github@nieslony.at> 1.3.14-1
- Add dependency (github@nieslony.at)

* Wed Nov 06 2024 Claas Nieslony <github@nieslony.at> 1.3.13-1
- Fix: skip generation of git props (github@nieslony.at)
- Don't create git.properties only in development env (github@nieslony.at)

* Tue Nov 05 2024 Claas Nieslony <github@nieslony.at> 1.3.12-1
- Depend on maven-openjdk21 on newer fedoras (github@nieslony.at)
- Bump dependency versions (github@nieslony.at)
- Add drop down indicators (github@nieslony.at)
- Bump versions of vaadin and spring boot (github@nieslony.at)
- Make Kerberos first external auth (github@nieslony.at)
- Bump dependency versions (github@nieslony.at)
- Configure remember password for NetworkManager (github@nieslony.at)
- Introduce field stringContent (github@nieslony.at)
- Fix: typo (github@nieslony.at)
- Try deserializing first, then valueOf(String) (github@nieslony.at)
- Add version info from git (github@nieslony.at)
- Add favicon (github@nieslony.at)
- Add icon to AboutDialog (github@nieslony.at)
- Fix: read string and primitives (github@nieslony.at)
- Ignore if LdapSettings are not valid (github@nieslony.at)
- Check if LdapSettings are valid (github@nieslony.at)
- Write settings content as string id possible (github@nieslony.at)
- Add TastRestController.java (github@nieslony.at)
- Add About dialog (github@nieslony.at)
- Ignore params annotated with AuthenticationPrincipal (github@nieslony.at)
- Fix: add id of method (github@nieslony.at)
- Include username in client config (github@nieslony.at)

* Mon Sep 02 2024 Claas Nieslony <github@nieslony.at> 1.3.11-1
- Bump dependency versions (github@nieslony.at)
- Remove log message (github@nieslony.at)
- Remove log (github@nieslony.at)
- Validate hostname (github@nieslony.at)
- Replace ldap urls edit with EditableListBox (github@nieslony.at)
- Add UrlField.java (github@nieslony.at)
- Make EditableListBox more fexible (github@nieslony.at)
- Copy API URL to clipboard (github@nieslony.at)
- Docu update (github@nieslony.at)
- Fix: read LDAP servers from ldapSettings (github@nieslony.at)
- Add token based auth (github@nieslony.at)
- Fix default value (github@nieslony.at)
- Fix: findUser (github@nieslony.at)
- Add task RefreshLdapUsers (github@nieslony.at)
- Make createRandomPassword static (github@nieslony.at)
- Enable no limit on findUsers (github@nieslony.at)
- Fix: update LDAP cache (github@nieslony.at)
- Label Kerberos logon more clearly (github@nieslony.at)
- Dont't write location if preAuth is disabled (github@nieslony.at)
- Create authentication token on /api/auth (github@nieslony.at)
- Add methods encryptData, decryptData, createSignature, verifySignature
  (github@nieslony.at)
- Begin implementing auth token (github@nieslony.at)
- Bump depenency versions (github@nieslony.at)
- Code cleanup (github@nieslony.at)
- No persisten sessions (github@nieslony.at)
- Split ArachneUserDetailsService into InternalUserDetailsService and
  LdapUserDetailsService (github@nieslony.at)
- Rename class ArachneUser → UserModel (github@nieslony.at)
- typo (github@nieslony.at)
- Fix: handle null value (github@nieslony.at)
- Issues fixed with new spring-security-kerberos, cleanup (github@nieslony.at)
- Add reqrite option UnsafeAllow3F (github@nieslony.at)
- Fix: ldapSettings==null (github@nieslony.at)
- Create package at.nieslony.arachne.utils.components (github@nieslony.at)
- Add src/main/frontend (github@nieslony.at)
- Bump vaadin version (github@nieslony.at)
- Rename class (github@nieslony.at)
- - switch from CentOS stream9 to AlmaLinux 9 - optimize custom provisioners
  (github@nieslony.at)
- Update and rename README.txt -> README. (github@nieslony.at)
- Update and rename README.txt -> README.md (github@nieslony.at)
- Bump spring-security-kerberos, spring-boot-starter-parent
  (github@nieslony.at)
- Ignore src/main/bundles (github@nieslony.at)
- Bump vaadin version (github@nieslony.at)
- Open API docs in new tab (github@nieslony.at)
- Bump vaadin vertsion to 24.4 (github@nieslony.at)
- Bump dependency versions (github@nieslony.at)
- . (github@nieslony.at)
- Bump vaadin version (github@nieslony.at)
- Fix: update value (github@nieslony.at)
- Bump vaadin and spring versions (github@nieslony.at)
- Replace attachment name (github@nieslony.at)
- Handle MailSendException (github@nieslony.at)
- Bump version of bouncycastle (github@nieslony.at)
- Add icons, set button as primary (github@nieslony.at)
- Add form flag (github@nieslony.at)
- Add form flag (github@nieslony.at)
- Don't remove attribute from invalid session (github@nieslony.at)
- Remove Unauthenticated.java (github@nieslony.at)
- Add filter to handle status 401 if no negotiation header supplied
  (github@nieslony.at)
- Create SSO view with redirect (github@nieslony.at)
- Change redirext text (github@nieslony.at)
- Test and try :-( (claas@nieslony.at)

* Thu Mar 21 2024 Claas Nieslony <github@nieslony.at> 1.3.5.git_2403211001_016b011-1
- Bump dependencies (github@nieslony.at)
- add extra_vars (github@nieslony.at)
>>>>>>> openvpn-site
- Specify destination host on upload (github@nieslony.at)
- Remove some debugging code (github@nieslony.at)
- - enable IPs allowed\n- enable DNS check (github@nieslony.at)
- Replace constructor by with... (github@nieslony.at)
<<<<<<< HEAD

* Wed Feb 21 2024 Claas Nieslony <github@nieslony.at>
- Add dependency (github@nieslony.at)

* Wed Feb 21 2024 Claas Nieslony <github@nieslony.at>
=======
>>>>>>> openvpn-site
- Support copr (github@nieslony.at)
- Add supported files (github@nieslony.at)
- Fix: enable whitelist on non default page (github@nieslony.at)
- use EditableListBox's default button (github@nieslony.at)
- use EditableListBox's default button (github@nieslony.at)
- Fix:page title (github@nieslony.at)
<<<<<<< HEAD
- Show page title above content (github@nieslony.at)
- Set default values (github@nieslony.at)
- Replace button text by icon, add optional default value supplier
  (github@nieslony.at)
- Merge from branch origin/openvpn-site (github@nieslony.at)
- Merge from branch origin/master (github@nieslony.at)
- - bump vaadin version\n- revert to older version of autocomplete
  (github@nieslony.at)
- Fix: styling (github@nieslony.at)
- Make use of EditableListbox (github@nieslony.at)
- Validate ip/prefix without supplier (github@nieslony.at)
- Remove the arachne (github@nieslony.at)
=======
>>>>>>> openvpn-site
- Write plugin site configuration (github@nieslony.at)
- Remove debug message (github@nieslony.at)
- Fix: disable/enable cpomponents (github@nieslony.at)
- Handle empty sskKeys (github@nieslony.at)
- Handle null value (github@nieslony.at)
- Write plugin config for site VPN, renme method (github@nieslony.at)
- Remove useless import (github@nieslony.at)
- Add createInfo(String headerText) (github@nieslony.at)
<<<<<<< HEAD
- Adjust widget width (github@nieslony.at)
- Add icon (github@nieslony.at)
- Handle json read and write differently (github@nieslony.at)
- Remove debug message (github@nieslony.at)
- Many GUI improvements for API index (github@nieslony.at)
- Show Lists (github@nieslony.at)
- Hide getLdapTemplate from Json (github@nieslony.at)
- Fix links, show possible enum values (github@nieslony.at)
- Hide some methdos from json (github@nieslony.at)
- Hide methods from json, make TemplateConfigType public (github@nieslony.at)
- TomcatView: GUI improvements (github@nieslony.at)
- Enable message to info notofications (github@nieslony.at)
- Bump dependency versions (github@nieslony.at)
- Show more information (github@nieslony.at)
- Add SiteNav item (github@nieslony.at)
- Replace FileDownloadWrapper by DynamicFileDownloader (github@nieslony.at)
- standard fole name (github@nieslony.at)
- Start woth API index (github@nieslony.at)
- Add getOpenVpnSiteRemoiteConfigName (github@nieslony.at)
- Add Detach listener (github@nieslony.at)
- Fix: permissions on admin home page (github@nieslony.at)
=======
- Replace FileDownloadWrapper by DynamicFileDownloader (github@nieslony.at)
- standard fole name (github@nieslony.at)
- Add getOpenVpnSiteRemoiteConfigName (github@nieslony.at)
- Add Detach listener (github@nieslony.at)
>>>>>>> openvpn-site
- Add @PreDestroy (github@nieslony.at)
- Enable polling (github@nieslony.at)
- Configure status update interval (claas@nieslony.at)
- Implement listener for signal handler (claas@nieslony.at)
- Make member final (claas@nieslony.at)
- Enable push (claas@nieslony.at)
- Add signal handler (claas@nieslony.at)
<<<<<<< HEAD
- removver logging (github@nieslony.at)
=======
>>>>>>> openvpn-site
- Remove old openvpn management (claas@nieslony.at)
- Replace OpenVpnManagement by ArachneDbus (claas@nieslony.at)
- Replace OpenVpnManagement by ArachneDbus (claas@nieslony.at)
- Add method ServerStatus (claas@nieslony.at)
- Restart via new dbus interface (claas@nieslony.at)
- - don't restart after writing server config\n- add openvpn status and pid
  file (claas@nieslony.at)
- Add openvpn run dir (claas@nieslony.at)
- Add dbus-java (claas@nieslony.at)
- Add ArachneDbus (claas@nieslony.at)
- Enable multiline strings (claas@nieslony.at)
- set value after adding listener (claas@nieslony.at)
- - enable SSH key auth - show notifications on config upload/failure
  (claas@nieslony.at)
- Add SSHkey to backup (claas@nieslony.at)
- Fix: Configuration upload (claas@nieslony.at)
- SSH key moved to SshKeyRepository (claas@nieslony.at)
- Improve GUI (claas@nieslony.at)
- Add and delete SSH keys (claas@nieslony.at)
- Add package at.nieslony.arachne.ssh (claas@nieslony.at)
- Handle enabled/disabled on (non-)default site (claas@nieslony.at)
- Change client -> site (claas@nieslony.at)
- Remove useless code (claas@nieslony.at)
- Create site with builder (claas@nieslony.at)
- . (claas@nieslony.at)
- Mark some methods as @Transactional (claas@nieslony.at)
- Rearrange imports (claas@nieslony.at)
- Add clear button (claas@nieslony.at)
- Make use of ticket cache (claas@nieslony.at)
- Add getDefaultSearchDomains() (claas@nieslony.at)
- Copy config to remote host (claas@nieslony.at)
- Fixes (github@nieslony.at)
- Save status (github@nieslony.at)
- Add SiteConfigUploader.java (github@nieslony.at)
- Introduce enum SshAuthType (github@nieslony.at)
- Add jsch (github@nieslony.at)
- Add validators (github@nieslony.at)
- Get netmask from value if no Supplier provided (github@nieslony.at)
- Simplify handling of disabling components (github@nieslony.at)
- Download remote config (github@nieslony.at)
- Show create and remote config (github@nieslony.at)
- Site saved ⇒ not modified (github@nieslony.at)
- Some ignore some fields for default site (github@nieslony.at)
- Make VpnSite outer class (github@nieslony.at)
- Fix: save site when saving vpn; update list after renaming site
  (github@nieslony.at)
- Add properties (github@nieslony.at)
- Save site settings (github@nieslony.at)
- Make valueChangeListeneer work (github@nieslony.at)
- Write server config (github@nieslony.at)
- Change default for keepalive (github@nieslony.at)
- Edit push DNS servers and routes (github@nieslony.at)
- Save OpenVpnSite (github@nieslony.at)
- Remove site (github@nieslony.at)
- Switch to settings API, add new site (github@nieslony.at)
- Change site name (github@nieslony.at)
- Add more settings (github@nieslony.at)
- Add OpenVpnSiteSettings.java (github@nieslony.at)
- Add empty OpenVpnSiteView.java (github@nieslony.at)

* Wed May 01 2024 Claas Nieslony <github@nieslony.at> 1.3.10-1
- Document Tomcat Settings (github@nieslony.at)
- Enable PreAuth with HTTP header (github@nieslony.at)
- Configure Tomcat HTTPS (github@nieslony.at)

* Wed May 01 2024 Claas Nieslony <github@nieslony.at> 1.3.9-1
- Bump dependencies (github@nieslony.at)
- Bump to vaadin 24.3.9 (github@nieslony.at)
- set key file permissisons (github@nieslony.at)
- Enable SSL on port 8443, create RSA key and certificate if not exists
  (github@nieslony.at)
- Add version template (github@nieslony.at)

* Fri Mar 29 2024 Claas Nieslony <github@nieslony.at> 1.3.8-1
- add version

* Fri Mar 29 2024 Claas Nieslony <github@nieslony.at> 1.3.7-1
- add version

* Fri Mar 29 2024 Claas Nieslony <github@nieslony.at> 1.3.6-1
- Bump dependencies (github@nieslony.at)

<<<<<<< HEAD
=======
* Thu Mar 14 2024 Claas Nieslony <github@nieslony.at>
- Bump dependencies (github@nieslony.at)
- Handle unavailable LDAP server on authentication (github@nieslony.at)
- Allow components as message text (github@nieslony.at)

* Sun Mar 10 2024 Claas Nieslony <github@nieslony.at>
- Fix: shell syntax (github@nieslony.at)
- UserMatcher matches user not username now\n- LdapGroupUserMatcher retruns
  false for not LDAP users (github@nieslony.at)
- Bump dependencies (github@nieslony.at)
- Fix: make member final (github@nieslony.at)
- API requires role (github@nieslony.at)
- Add some documentation (github@nieslony.at)
- Show page title above content (github@nieslony.at)
- Set default values (github@nieslony.at)
- Replace button text by icon, add optional default value supplier
  (github@nieslony.at)
- Merge from branch origin/openvpn-site (github@nieslony.at)
- Merge from branch origin/master (github@nieslony.at)
- - bump vaadin version\n- revert to older version of autocomplete
  (github@nieslony.at)
- Fix: styling (github@nieslony.at)
- Make use of EditableListbox (github@nieslony.at)
- Validate ip/prefix without supplier (github@nieslony.at)
- Remove the arachne (github@nieslony.at)
- Adjust widget width (github@nieslony.at)
- Add icon (github@nieslony.at)
- Handle json read and write differently (github@nieslony.at)
- Remove debug message (github@nieslony.at)
- Many GUI improvements for API index (github@nieslony.at)
- Show Lists (github@nieslony.at)
- Hide getLdapTemplate from Json (github@nieslony.at)
- Fix links, show possible enum values (github@nieslony.at)
- Hide some methdos from json (github@nieslony.at)
- Hide methods from json, make TemplateConfigType public (github@nieslony.at)
- TomcatView: GUI improvements (github@nieslony.at)
- Enable message to info notofications (github@nieslony.at)
- Bump dependency versions (github@nieslony.at)
- Show more information (github@nieslony.at)
- Add SiteNav item (github@nieslony.at)
- Start woth API index (github@nieslony.at)
- Fix: permissions on admin home page (github@nieslony.at)
- removver logging (github@nieslony.at)

>>>>>>> openvpn-site
* Thu Jan 25 2024 Claas Nieslony <github@nieslony.at> 1.3.3-1
-

* Thu Jan 25 2024 Claas Nieslony <github@nieslony.at>
-

* Sun Jan 21 2024 Claas Nieslony <github@nieslony.at> 1.3.2-1
-

* Wed Jan 17 2024 Claas Nieslony <claas@nieslony.at> 1.3.1-1
- Bump arachne version to 1.3.1 (claas@nieslony.at)
- Tell details about plugin location (claas@nieslony.at)
- Change plugin search order (claas@nieslony.at)
- Fix: open management console with password (claas@nieslony.at)
- Remove useless code (claas@nieslony.at)
- Write management passwort file (github@nieslony.at)
- Set default button (claas@nieslony.at)
- Fix: user context menu (claas@nieslony.at)
- Fix: add annotation EnableMethodSecurity (claas@nieslony.at)

* Fri Jan 12 2024 Claas Nieslony <claas@nieslony.at> 1.3-1
- new package built with tito

* Thu Jan 11 2024 Claas Nieslony <github@nieslony.at>
- Initial changelog

