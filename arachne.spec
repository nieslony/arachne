Name:           arachne
Version:        @@version@@
Release:        1
Summary:        Web administrator for openVPN
License:        GPL-3.0+
URL:            http://www.nieslony.site/arachne
Source0:        %{name}-%{version}.tar.xz
BuildRequires:  maven
BuildRequires:  java-17-devel
BuildArch:      noarch

%description
Web application to administer an openVPN server.

%prep
%setup -q -n %{name}-r0.2-SNAPSHOT

%build
mvn package
