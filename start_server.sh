#!/bin/bash

./setup_dev_mtls.sh
./gradlew --no-daemon bootRun -Djava.security.egd=file:/dev/urandom $@
