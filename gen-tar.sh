#!/bin/bash

VERSION=$( xpath -q -e '/project/version/text()' pom.xml | sed -e 's/-/_/g' )
FORMAT=tar.gz
NAME="arachne-$VERSION"
TAR_FILE=target/$NAME.$FORMAT

function patch_spec {
    cat arachne.spec | \
        awk -F: -v version=$VERSION '
            /^Version:/ {
                print "Version: " version;
                next;
            }
            { print $0; }
            END { print ""; }
            '
}

patch_spec > target/arachne.spec
rm -vf $TAR_FILE
echo Creating $TAR_FILE

git archive \
    --format=$FORMAT \
    --prefix=$NAME/ \
    --output=$TAR_FILE \
    --add-file=target/arachne.spec \
    HEAD . ':!arachne.spec'

ls -l $TAR_FILE