# Arachne
Arachne is a web administration tool for OpenVPN user VPNs.

## Compile Arachne manually

In a production environment it's recommended to use the precompiled packages. To
compile arachne manually you need:
  - maven
  - Java Development Kit 17

### Build Arachne
    mvn package

### Run Arachne
    ./target/Arachne.jar

## Installation on CentOS and Fedora

There are precompiled packages for CentOS and Fedora.

1. add copr repository

       dnf copr enable nieslony/arachne

1. add EPEL (CentOS only)

       dnf install epel-release

1. install Arachne

       dnf install arachne

    Arachne and all its dependencies will be installed.

1. enable and start arachne

       systemctl enable --now arachne arachne-dbus

1. enable port 8080 and 8443 in firewall

       firewall-cmd --add-port 8080/tcp --add-port 8443/tcp

    It's strongly recommended to run arachne behind a reverse proxy like
    Apache HTTPD.

1. connect to arachne: http://arachne.example.com:8080/arachne and follow the
   installation wizard to create Arachne's CA and the the administor user.

## Configuration

1. login as the formally created admin user

### LDAP User Source

1. enable _Enable Ldap User Source_

1. select _LDAP User Source_ on the left

1. on the _Basics_ tab

   1. get LDAP servers from SRV records in DNS or add your LDAP servers manually

   1. modify or confirm the base DN

   1. select the authentication mode against your LDAP servers. For bind with
      Kerberos you need a keytab file. If you plan to enable Kerberos
      authentication on your webserver you can reuse the keytab file.

1. on the _Users and Groups_ tab

   1. get the default attributes for FreeIPA or define your own mappings

   1. you can test the maaping by entering a username/groupname and clicking
      _Find and Test_

1. save the configuration

### Roles

There are the following roles:

- __Administrator__ An Administrator can modify all settings and get backups,
  but he cannot login to the VPN.

- __User__ A User can login to the VPN

- __Backup Operator__ A Backup Operator can download backups

Click _Add..._ to add a new role rule. Roles can be bound to users, groups and
everybody.

### External Authentication

Arachne can make use of an external authentication service instead of using
internal users.

#### Kerberos

The internal Kerberos authentication is currently not recommended.

#### Pre Authentication

If Arachne is runng bind a reverse proxy e.g. Apache HTTPD with AJP you can
forward the authentication to the proxy and trust the remote user provides by an
environment variable. Don't forget to enable the AJP connector.

## Integrated Tomcat

Tomcat settings can be found in the left menu under _Integrated Tomcat_,

### AJP Connector

If you enable the AJP connector a configuration file for Apache https is
created with provided AJP password. You can copy or symlink
_${workdir}/arachneconfig/arachne.conf_ to the Apache configuration folder.

### HTTPS

If HTTPS is enabled the integrated tomcat reads the sercer certificate from
_${workdir}/arachneconfig/server.crt_ and the private key from
_${workdir}/arachneconfig/server.key_. _server.crt_ and _server.key_ may be
symlinks e.g. to certificates maintained by certmonger (FreeIPA).

## Backup and Restore

As a user with the role _Administrator_ or _Backup Operator_ you can download
a backup from: http://araxchne.example.com:8080/arachne/api/backup With this
backup you can restore Arachne in case of a disaster recovery on the first page
of the installation wizard. But you cannot restore individual settings.

## REST API

Arachne supports a REST API for some tasks. Have a look at
http://arachne.example.com:8080/arachne/api-index for the supported API calls.
For most calls the _Administrator_ role is required. All sent or received data
is in JSON format.
