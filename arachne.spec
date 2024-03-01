%global selinuxtype targeted
%global moduletype contrib
%global modulename arachne

Name:           arachne
Version:        0.1.3.git_2403011547_2b59921

Release:        1
License:        GPLv3
Source0:         %{name}-%{version}.tar.gz
Summary:        Administration server for openVPN
BuildArch:      noarch
Url:            https://github.com/nieslony/arachne

BuildRequires:  maven-openjdk17
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
mvn --no-transfer-progress package
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
    /etc/openvpn/server/arachne.conf

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
* Fri Mar 01 2024 Claas Nieslony <github@nieslony.at>
- Specify destination host on upload (github@nieslony.at)
- Remove some debugging code (github@nieslony.at)
- - enable IPs allowed\n- enable DNS check (github@nieslony.at)
- Replace constructor by with... (github@nieslony.at)

* Wed Feb 21 2024 Claas Nieslony <github@nieslony.at>
- Add dependency (github@nieslony.at)

* Wed Feb 21 2024 Claas Nieslony <github@nieslony.at>
- Support copr (github@nieslony.at)
- Add supported files (github@nieslony.at)
- Fix: enable whitelist on non default page (github@nieslony.at)
- use EditableListBox's default button (github@nieslony.at)
- use EditableListBox's default button (github@nieslony.at)
- Fix:page title (github@nieslony.at)
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
- Write plugin site configuration (github@nieslony.at)
- Remove debug message (github@nieslony.at)
- Fix: disable/enable cpomponents (github@nieslony.at)
- Handle empty sskKeys (github@nieslony.at)
- Handle null value (github@nieslony.at)
- Write plugin config for site VPN, renme method (github@nieslony.at)
- Remove useless import (github@nieslony.at)
- Add createInfo(String headerText) (github@nieslony.at)
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
- Add @PreDestroy (github@nieslony.at)
- Enable polling (github@nieslony.at)
- Configure status update interval (claas@nieslony.at)
- Implement listener for signal handler (claas@nieslony.at)
- Make member final (claas@nieslony.at)
- Enable push (claas@nieslony.at)
- Add signal handler (claas@nieslony.at)
- removver logging (github@nieslony.at)
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

