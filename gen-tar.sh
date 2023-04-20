#!/bin/bash

VERSION=$( xpath -q -e '/project/version/text()' pom.xml | sed -e 's/-/_/g' )
FORMAT=tar.gz
NAME="arachne-$VERSION"

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

git archive \
    --format=$FORMAT \
    --prefix=$NAME/ \
    --output=target/$NAME.$FORMAT \
    --add-file=target/arachne.spec \
    HEAD . ':!arachne.spec'
