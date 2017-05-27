%if 0%{?suse_version}
%define webappsdir /srv/tomcat/webapps
%define webappuser root
%define webappgroup root
%else 
%define webappsdir /var/lib/tomcat/webapps
%define webappuser tomcat
%define webappgroup tomcat
%endif

%define destdir %{webappsdir}/arachne
%define libdir %{destdir}/WEB-INF/lib
%define debug_package %{nil}

Name:       arachne
Version:    0.3.0
Release:    1
Summary:    Web application for administering openVPN

License:    GPL-3.0+
URL:        http://www.nieslony.site/OpenVPN_Admin
Source0:    %{name}-%{version}.tar.gz

BuildRoot:  %{_tmppath}/%{name}-%{version}-%{release}-root

BuildRequires:  ant bouncycastle tomcat python

%if 0%{?fedora}
BuildRequires:  java-1.8.0-openjdk-devel tomcat-el-3.0-api
%endif
%if 0%{?centos_version}
BuildRequires:  java-1.8.0-openjdk-devel tomcat-el-2.2-api
%endif
%if 0%{?suse_version}
BuildRequires:  java-1_8_0-openjdk-devel tomcat-el-3_0-api
%endif
 
%package server
Summary:	arachne server
BuildArch:	noarch
Requires:	tomcat bouncycastle openvpn postgresql-jdbc

%package config-downloader
Summary:	arachne downloader for NetworkManager config
BuildArch:	noarch
Requires:	curl NetworkManager NetworkManager-openvpn

%description server
Tomcat Web application for administering openVPN

%description config-downloader
Configuration downloader for OpenVPN_Admin

%description
Tomcat Web application for administering openVPN


%prep
%setup 

%build
ant dist -Droot=%{_builddir}/%{name}-%{version}

%install 
ant install -Droot=%{_builddir}/%{name}-%{version} -Dinstall-root=%{buildroot}

mkdir -pv %{buildroot}/usr/bin %{buildroot}/%_defaultdocdir/%{name}
install bin/download-vpn-config.sh %{buildroot}/usr/bin
install apache/arachne-redhat.conf %{buildroot}/%_defaultdocdir/%{name}/arachne.conf
install COPYING-GPL3        %{buildroot}/%_defaultdocdir/%{name}

mkdir -pv %{buildroot}/var/lib/arachne
%clean
ant clean 

%post server
ln -sfv \
	/usr/share/java/bcprov.jar \
	/usr/share/java/postgresql-jdbc.jar \
	%{libdir}

%preun server
if [ $1 = 0 ] ; then
	rm -vf %{libdir}/bcprov.jar
	rm -vf %{libdir}/postgresql-jdbc.jar
else
	echo Do not remove %{libdir}/bcprov.jar, still needed.
fi

%files server
%defattr(-, %{webappuser}, %{webappgroup}, -)
%attr(755,  %{webappuser}, %{webappgroup}) %{webappsdir}/%{name}/WEB-INF/bin/*.sh
%webappsdir/%{name}

%attr(664, root, root) %_defaultdocdir/%{name}/*
%attr(770, %{webappuser}, %{webappgroup}) /var/lib/arachne

%files config-downloader
/usr/bin/download-vpn-config.sh

%changelog
* Sun May 01 2016 Claas Nieslony <claas@nieslony.at> 0.1.0
- Initial version
