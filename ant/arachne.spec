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
Version:    @@VERSION@@
Release:    1
Summary:    Web application for administering openVPN

License:    GPL-3.0+
URL:        http://www.nieslony.site/OpenVPN_Admin
Source0:    %{name}-%{version}.tar.gz

BuildRoot:  %{_tmppath}/%{name}-%{version}-%{release}-root

BuildRequires:  ant bouncycastle tomcat python primefaces myfaces-core 
BuildRequires:  bouncycastle-pkix bouncycastle postgresql-jdbc docbook5-style-xsl docbook5-schemas

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
Summary:	Arachne server
BuildArch:	noarch
Requires:	tomcat bouncycastle openvpn postgresql-jdbc myfaces-core primefaces arachne-doc
Obsoletes:      OpenVPN_Admin-server

%package config-downloader
Summary:	Arachne downloader for NetworkManager config
BuildArch:	noarch
Requires:	curl NetworkManager NetworkManager-openvpn
Obsoletes:      OpenVPN_Admin-config-downloader

%package doc
Summary:	Documentation for Arachne
BuildArch:	noarch

%description doc
HTML documentation for arachne

%description server
Tomcat Web application for administering openVPN

%description config-downloader
Command line tool for downloading configuration file from arachne

%description
Tomcat Web application for administering openVPN

%prep
%setup 

%build
ant dist       -Droot=%{_builddir}/%{name}-%{version}
ant custom.doc -Droot=%{_builddir}/%{name}-%{version}

%install 
ant install -Droot=%{_builddir}/%{name}-%{version} -Dinstall-root=%{buildroot} -Dwebapps.dir=%{webappsdir}
mkdir -vp %{buildroot}/%_defaultdocdir
mv -v %{buildroot}/%{webappsdir}/%{name}/doc %{buildroot}/%_defaultdocdir/%{name}-doc

mkdir -pv %{buildroot}/usr/bin %{buildroot}/%_defaultdocdir/%{name}
install bin/download-vpn-config.sh %{buildroot}/usr/bin
%if 0%{?suse_version}
install apache/arachne-suse.conf %{buildroot}/%_defaultdocdir/%{name}/arachne.conf
%else
install apache/arachne-redhat.conf %{buildroot}/%_defaultdocdir/%{name}/arachne.conf
%endif
install COPYING-GPL3        %{buildroot}/%_defaultdocdir/%{name}

mkdir -pv %{buildroot}/var/lib/arachne

%clean
[ %{buildroot} != "/" ] && rm -rf %{buildroot}

%post server
ln -sfv \
	/usr/share/java/primefaces.jar \
	/usr/share/java/jsf-api.jar \
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

%attr(755, root, root)    %_defaultdocdir/%{name}
%attr(664, root, root)    %_defaultdocdir/%{name}/*
%attr(770, %{webappuser}, %{webappgroup}) /var/lib/arachne

%files doc
%_defaultdocdir/%{name}-doc

%files config-downloader
/usr/bin/download-vpn-config.sh

%changelog
* Sun May 01 2016 Claas Nieslony <claas@nieslony.at> 0.1.0
- Initial version
