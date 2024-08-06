#/bin/bash

curl http://www.dns-sd.org/ServiceTypes.html \
	| awk -F '</b> +' '/<b>[a-z\\-]+/ { gsub(/<b>/, ""); print $1 "\t" $2; }' \
        | tail -n +2 \
	> known-dns-srv-types.csv
