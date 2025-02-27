# Customising VM and additional provisioning scripts

Symlink or create

- additional provisioning tasks e.g. FreeIPA/AD join. Supported extensions:

    - .rb directly inserted ruby code

    - .yml ansible playbooks

    - .sh shell scripts

- ansible.cfg for Ansible playbooks

- extra_vars.yml for Ansible extra variables

The tasks are executed in alphabetic order.

# Integrate in Vagrant test environment

VAGRANT_DIR=$HOME/Vagrant

ln -sv $VAGRANT_DIR/ansible/network.yml 10-network.yml
ln -sv $VAGRANT_DIR/ansible/join-ipa-domain.yml 20-join-ipa-domain.yml
ln -sv $VAGRANT_DIR/ansible/roles/webserver.yml 30-webserver-roles.yml
ln -sv $VAGRANT_DIR/ansible/ansible.cfg ansible.cfg

cat <<EOF > extra_vars.yml
static_network:
    "192.168.120.0": { ip: "192.168.120.250/24", dns: ["192.168.120.254"], gw: "192.168.120.254" }
webserver_constrained_delegation_enabled: false
EOF

cat <<EOF > 05-private-network.rb
config.proxy.http = "http://192.168.121.1:3128"
config.proxy.https = "http://192.168.121.1:3128"
config.proxy.no_proxy = "localhost,127.0.0.1,.lab"

config.vm.network :private_network, :ip => "192.168.120.250",
                :libvirt__network_name => "Lab_Linux_Internal",
                :libvirt__autostart => "true",
                :libvirt__forward_mode => "route",
                :hostname => true,
                :autoconfig => false
EOF
