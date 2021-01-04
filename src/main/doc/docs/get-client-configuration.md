# Get client configuration

Arachne provides two types of client configuration:

 1. classic openVPN configuration file (.pvpn)

 1. Shell script that adds or modifies a NetworkManager configuration

Both configuration types can be retrieved in several ways. To add a
configuration to NetworkManager save the file, make it executable und execute
it.


## Download from administration page

A user with admin role can download client configurations for any user.

 1. Login as user with role admin

 1. navigate to *View/Edit users*

 1. Find the user you want to download the configuration for

 1. Click *Actions* and select the prefered download option


## Download from user welcopme page

A user without admin role but wuth user role can download his/her personal
from user the welcome page.

 1. Login as user wuth user role

 1. Click on the crow web to download tzhe NetworkManager installer or on the
    openVPN icon to download .ovpn file


## Direct download from download URL

A user with user role can download his/her personal configuration direct from
download url.

 - NetworkManager installer: *http(s)://arachne.example.com:8080/arachne/download/add-vpn-to-networkmanager.sh*

 - .ovpn file: *http(s)://arachne.example.com:8080/arachne/download/client-config.ovpn*


## Automatic download with configuration downloader (Linux only)

For Linux users there's a small system tray application that can download and
update a configuration automatically.

To install the configuration downloader on the client:

 1. Add repositorý

 1. install package arachne_configdownloader

 After next login you will see ![Screenshot](img/IconConfigDownloader.png) in
 your system tray. Right click on the icon to open the context menu where you
 can

 - edit the configuration downloaders settings

 - download the configuration just now

 - open arachne's administration page in web browser

 - quit the downloader

