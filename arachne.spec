Name:       arachne
Version:    0.3.0
Release:    0
License:    GPLv3
Source:     %{name}-%{version}.tar.gz
Summary:    Administration server for openVPN

%define artifactId $( xpath -q -e '/project/artifactId/text()' pom.xml )
%define artifactVersion $( xpath -q -e '/project/version/text()' pom.xml )
%define arachne_jar_name %{artifactId}-%{artifactVersion}.jar

BuildRequires:  maven
BuildRequires:  java-17-openjdk-devel
BuildRequires:  systemd-rpm-macros

Requires:       java-17-openjdk

%description
Administration server for openVPN

%prep
%setup

%build
# mvn package

%install
mkdir -pv %{buildroot}/%{_datadir}/%{name}
install -v %{_builddir}/%{name}-%{version}/target/%{arachne_jar_name} %{buildroot}/%{_datadir}/%{name}
install -v %{name}.service %{buildroot}/%{_unitdir}

%files
%{_unitdir}/%{name}.service
%dir %{_datadir}/%{name}
%license LICENSE
