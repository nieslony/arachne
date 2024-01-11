# this is just a monotonically increasing number to preceed the git hash, to get incremented on every git bump
%global git_bump		0
%global git_commit		f3b0f8580041649bf300a962674a2cf3f3bd1385
%global git_shortcommit		%(c=%{git_commit}; echo ${c:0:7})
%global git_branch              ma

# don't strip binaries at all
%global __strip			/bin/true
%global debug_package		%{nil}

# don't byte compile the ./examples ...
%global __spec_install_post	/usr/lib/rpm/check-rpaths   /usr/lib/rpm/check-buildroot  \
				/usr/lib/rpm/brp-compress

# they warn against doing this ... :-\
%define _disable_source_fetch 0

%global selinuxtype targeted
%global moduletype contrib
%global modulename arachne

Name:           arachne
Version:        1.2.%{git_bump}.git.%{git_shortcommit}
# Version:        master

Release:        1
License:        GPLv3
Source:         master.zip
Summary:        Administration server for openVPN
BuildArch:      noarch

BuildRequires:  maven-openjdk17
BuildRequires:  java-17-openjdk-devel
BuildRequires:  systemd-rpm-macros
BuildRequires:  selinux-policy-devel
BuildRequires:  pkgconfig(systemd)
%{?selinux_requires}

Requires:       java-17-openjdk-headless
Requires:       openvpn
Requires:       openvpn-plugin-arachne

Recommends:     httpd

%description
Administration server for openVPN

%prep
%setup

%build
mvn --no-transfer-progress package
make -f /usr/share/selinux/devel/Makefile arachne.pp

%install
mkdir -pv %{buildroot}/%{_datadir}/%{name}
mkdir -pv %{buildroot}/%{_unitdir}
install -v %{_builddir}/%{name}-%{version}/target/Arachne.jar %{buildroot}/%{_datadir}/%{name}
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
