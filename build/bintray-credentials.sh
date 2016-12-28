#!/bin/bash

mkdir -p ~/.bintray
eval "echo \"$(< ./build/bintray-credentials.template)\"" > ~/.bintray/.credentials
