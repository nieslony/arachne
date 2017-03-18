%if 0%{?suse_version}
%define webappsdir /srv/tomcat/webapps
%define webappuser root
%define webappgroup root
%else 
%define webappsdir /var/lib/tomcat/webapps
%define webappuser tomcat
%define webappgroup tomcat
%endif

%define destdir %{webappsdir}/OpenVPN_Admin
%define libdir %{destdir}/WEB-INF/lib
%define debug_package %{nil}

Name:       OpenVPN_Admin
Version:    0.2.3
Release:    3
Summary:    Web application for administering openVPN

License:    GPL-2.0+
URL:        http://www.nieslony.site/OpenVPN_Admin
Source0:    %{name}-%{version}.tar.gz

BuildRoot:  %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)

BuildRequires:  ant bouncycastle tomcat

%if 0%{?fedora}
BuildRequires:  java-1.8.0-openjdk-devel tomcat-el-3.0-api
%endif
%if 0%{?suse_version}
BuildRequires:  java-1_8_0-openjdk  java-1_8_0-openjdk-headless  libgcj-devel   tomcat-el-3_0-api
%endif
%if 0%{?centos_version}
BuildRequires:  java-1.8.0-openjdk-devel tomcat-el-2.2-api
%endif
 
%package server
Summary:	OpenVPN_Admin server
BuildArch:	noarch
Requires:	tomcat bouncycastle openvpn

%package config-downloader
Summary:	OpenVPN_Admin downloader for NetworkManager config
BuildArch:	noarch
Requires:	curl NetworkManager NetworkManager-openvpn

%description server
Tomcat Web application for administering openVPN

%description config-downloader
Configuration downloader for OpenVPN_Admin

%description
Tomcat Web application for administering openVPN


%prep
%setup -q

%build
ant -Droot=%{_builddir}/%{name}-%{version}

%install 
ant install -Droot=%{_builddir}/%{name}-%{version} -Dinstall-dir=%{buildroot}/%{webappsdir}/%{name}

mkdir -pv %{buildroot}/usr/bin
install bin/download-vpn-config.sh %{buildroot}/usr/bin

%clean
ant clean 

%post server
ln -sfv /usr/share/java/bcprov.jar %{libdir}

%preun server
if [ $1 = 0 ] ; then
	rm -vf %{libdir}/bcprov.jar
else
	echo Do not remove %{libdir}/bcprov.jar, still needed.
fi

%files server
%defattr(-, %{webappuser}, %{webappgroup}, -)
%attr(755,  %{webappuser}, %{webappgroup}) %{webappsdir}/%{name}/WEB-INF/bin/*.sh
%webappsdir/%{name}

%files config-downloader
/usr/bin/download-vpn-config.sh

%changelog
* Sun May 01 2016 Claas Nieslony <claas@nieslony.at> 0.1.0
- Initial version
