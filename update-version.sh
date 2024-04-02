#!/bin/bash

function get_git_version {
    cur_version=$(
        cat arachne.spec | awk '/^Version:/ { sub(/\.git_.*/, "", $2); print $2; }'
    )
    echo -n "$cur_version.git_$(date +%y%m%d%H%M)_$(git rev-parse --short HEAD)"
}

if [ -z "$1" ]; then
    VERSION="$( get_git_version )"
else
    VERSION="$1"
fi

echo $VERSION

tito tag --use-version="$VERSION"
