%if 0%{?suse_version}
%define webappsdir /srv/tomcat/webapps
%define webappuser root
%define webappgroup root
%define docbookstylesheet /usr/share/xml/docbook/stylesheet/nwalsh5/1.78.1/xhtml5/chunk.xsl
%else
%define webappsdir /var/lib/tomcat/webapps
%define webappuser tomcat
%define webappgroup tomcat
%define docbookstylesheet /usr/share/sgml/docbook/xsl-ns-stylesheets/xhtml5/chunk.xsl
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
BuildRequires:  bouncycastle-pkix bouncycastle postgresql-jdbc
BuildRequires:  databasepropertiesstorage

%if 0%{?fedora}
BuildRequires:  java-1.8.0-openjdk-devel tomcat-el-3.0-api docbook5-style-xsl docbook5-schemas libxslt
BuildRequires:	lua ruby
%endif
%if 0%{?centos_version}
BuildRequires:  java-1.8.0-openjdk-devel tomcat-el-2.2-api docbook5-style-xsl docbook5-schemas
%endif
%if 0%{?suse_version}
BuildRequires:  java-1_8_0-openjdk-devel tomcat-el-3_0-api docbook_5 docbook5-xsl-stylesheets
%endif

%package server
Summary:	Arachne server
BuildArch:	noarch
Requires:	tomcat bouncycastle bouncycastle-pkix openvpn postgresql-jdbc myfaces-core primefaces arachne-doc
Requires:       apache-commons-digester apache-commons-codec databasepropertiesstorage openvpn-arachne-plugin
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
ant custom.doc -Droot=%{_builddir}/%{name}-%{version} -Ddocbook-stylesheet=%{docbookstylesheet}

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
mkdir -pv %{buildroot}/var/lib/arachne/vpnconfig
mkdir -pv %{buildroot}/var/lib/arachne/appconfig
mkdir -pv %{buildroot}/etc/openvpn/server

%if ! 0%{?suse_version}
ln -vs /var/lib/arachne/vpnconfig/arachne_uservpn.conf %{buildroot}/etc/openvpn/server
%endif


pushd %{buildroot}/%{webappsdir}/%{name}/
ln -sv %_defaultdocdir/%{name}-doc doc
popd

%post server
%if 0%{?centos_version}
mkdir -v %{libdir}
ln -svf \
    /usr/share/java/{bcpkix.jar,bcprov.jar} \
    /usr/share/java/{commons-beanutils.jar,commons-codec.jar,commons-collections.jar}  \
    /usr/share/java/{commons-digester.jar,commons-logging.jar} \
    /usr/share/java/{myfaces-api.jar,myfaces-impl.jar,myfaces-impl-shared.jar} \
    /usr/share/java/postgresql-jdbc.jar \
    /usr/share/java/primefaces.jar \
    /usr/share/java/databasepropertiesstorage.jar \
    %{libdir}
%endif

if [ $1 = 1 ]; then
    pushd %{webappsdir}/%{name}
    ln -vs WEB-INF/SetupWizard.xhtml .
    popd
fi

%preun
if [ "$1" = 0 ]; then
    rm -f %{webappsdir}/%{name}/web/SetupWizard.xhtml
fi

%files server
%defattr(-, %{webappuser}, %{webappgroup}, -)
%attr(755,  %{webappuser}, %{webappgroup}) %{webappsdir}/%{name}/WEB-INF/bin/*.sh
%webappsdir/%{name}

%attr(755, root, root)    %_defaultdocdir/%{name}
%attr(664, root, root)    %_defaultdocdir/%{name}/*
%attr(770, %{webappuser}, %{webappgroup}) /var/lib/arachne

%webappsdir/%{name}/doc

%if ! 0%{?suse_version}
/etc/openvpn/server/arachne_uservpn.conf
%endif

%files doc
%_defaultdocdir/%{name}-doc

%files config-downloader
/usr/bin/download-vpn-config.sh

%changelog
* Sun May 01 2016 Claas Nieslony <claas@nieslony.at> 0.1.0
- Initial version
