#!/bin/bash

ARACHNE_VERSION=$( mvn help:evaluate -Dexpression=project.version | grep -v '\[' )
RPM_VERSION=$( echo $ARACHNE_VERSION | sed -e s/-/./g )

mvn assembly:single || exit 1
mv -v \
    target/arachne-$ARACHNE_VERSION-distribution.tar.xz \
    target/arachne-$RPM_VERSION.tar.xz || exit 1

cat arachne.spec \
    | sed -e "s/@@version@@/$RPM_VERSION/" \
    > target/arachne.spec
