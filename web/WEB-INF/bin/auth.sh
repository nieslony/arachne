#!/bin/bash

PWD_FILE=$2
AUTH_URL=$1
MACHINE=$( echo $AUTH_URL | sed -e 's/^https\?:\/\/\([a-z.\-]\+\).*/\1/g' )

echo "--- BEGIN authentication script ---"
echo "Reading username and password from $PWD_FILE"
echo "Trying to get URL $AUTH_URL ..."

HTTP_STATUS=$(
    echo "machine $MACHINE login $( cat $PWD_FILE | head -1 ) password $( cat $PWD_FILE | tail -1 )" |
        curl --silent --head --netrc-file /dev/stdin $AUTH_URL |
        head -1 |
        cut -f 2 -d " "
)

echo "HTTP exit status: $HTTP_STATUS"

if [ "$HTTP_STATUS" = "200" ]; then
    echo Authentication successfull.
    RET=0
else
    echo Authentication failed.
    RET=1
fi

echo Exit with code $RET
echo "--- END authentication script ---"

exit $RET
