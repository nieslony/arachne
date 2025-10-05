#!/bin/bash

SIZES="16 32 48 64 120 144 152 180 192 512"

for size in $SIZES ; do
	for image in arachne arachne_dark ; do
		IN_NAME=${image}.svg
		OUT_NAME=${image}_${size}.png
		echo Creating $OUT_NAME...
		convert -background transparent -resize ${size}x${size} $IN_NAME $OUT_NAME
	done
done
