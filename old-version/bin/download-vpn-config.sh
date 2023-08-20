#!/bin/bash

function usage {
    cat <<EOF
Usage:    
    
download-vpn-config.sh [ -d ] -a auth_TYPE -u DOWNLOAD_URL | -b OPEN_VPN_ADMIN_BASE_URL

    auth_TYPE   krb5

EOF
}

while [ -n "$1" ]; do
    case "$1" in
        -u)
            shift
            URL=$1
            ;;
        -b)
            shift
            URL="$1/download/add-vpn-to-networkmanager.sh"
            ;;
        -a)
            shift
            case $1 in
                krb5)
                    AUTH="--negotiate --user :"
                    ;;
                *)
                    echo "Invalid auth type: $1"
                    usage
                    exit 2
            esac
            ;;
        -d)
            DL_ONLY="yes"
            ;;
         *)
            echo $1
            usage
            exit 2
            ;;            
    esac
    shift
done

if [ -z "$URL" ]; then
    echo "Error please give me an url"
    usage
    exit 2
fi
if [ -z "$AUTH" ]; then
    echo "Please give me an auth type"
    usage
    exit 2
fi

PID_FILE=$XDG_RUNTIME_DIR/download-openvpn.pid

DEBUG=1

function debug() {
    if [ "$DEBUG" = "1" ]; then
        echo $@
    fi
}

if [ -e "$PID_FILE" ]; then
    echo $PID_FILE exists, somebody else is downloading VPN script.
    exit 0
fi

TMP_FILE=$( mktemp )
trap "{ rm -v $PID_FILE $TMP_FILE ; }" EXIT SIGTERM

touch $PID_FILE
touch $TMP_FILE
chmod 700 $TMP_FILE
if [ -z "$DEBUG" ]; then
    $DL_CMD="$DL_CMD --silent"
fi

debug Downloading $URL...
DL_CMD="curl --silent --fail --retry 1 $AUTH $URL"

debug Executing $DL_CMD
$DL_CMD > $TMP_FILE 
DL_OK=$?
if [ "$DL_OK" = "0" -a -s "$TMP_FILE" ] ; then
    if [ -n "$DL_ONLY" ]; then
        cat $TMP_FILE
    else
        debug Setting up VPN connection...
        bash < $TMP_FILE
    fi
else
    echo Error $DL_OK: Hmmmm, something went wrong.
    exit 1
fi

exit 0
