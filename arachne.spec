Name:           arachne
Version:        0.3.0
Release:        1
License:        GPLv3
Source:         %{name}-%{version}.tar.gz
Summary:        Administration server for openVPN
BuildArch:      noarch

BuildRequires:  maven-openjdk17
BuildRequires:  java-17-openjdk-devel
BuildRequires:  systemd-rpm-macros

Requires:       java-17-openjdk-headless
Requires:       openvpn

%description
Administration server for openVPN

%prep
%setup

%build
mvn --no-transfer-progress package

%install
mkdir -pv %{buildroot}/%{_datadir}/%{name}
mkdir -pv %{buildroot}/%{_unitdir}
install -v %{_builddir}/%{name}-%{version}/target/Arachne.jar %{buildroot}/%{_datadir}/%{name}
install -v %{name}.service %{buildroot}/%{_unitdir}

%post
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

%files
%{_unitdir}/%{name}.service
%dir %{_datadir}/%{name}/
%{_datadir}/%{name}/Arachne.jar
%license LICENSE
