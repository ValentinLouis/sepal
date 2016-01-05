#!/usr/bin/env bash
cd ../../sepal-php
rebuild=false
if [ -f ./docker/binary/sepal-php.tar.gz ]; then
    found=$(find . \
        -newer ./docker/binary/sepal-php.tar.gz \
        -print -quit)
    if [ ! -z "$found" ]; then
        rebuild=true
    fi
else
    rebuild=true
fi
echo ${rebuild}
