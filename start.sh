#!/bin/bash

set -eu

profile=symmetrical

echo "Starting token-service ..."
pushd jwt-token-service
mvn spring-boot:run -Drun.profiles=$profile &
popd


echo "Building jwt-starter ..."
pushd jwt-starter
mvn install
popd

echo "Push backend-service ..."
pushd backend-service
mvn spring-boot:run -Drun.profiles=$profile &

popd

echo "Push resource-service ..."
pushd resource-service
mvn package
mvn spring-boot:run -Drun.profiles=$profile &

popd


echo "Push gateway ..."
pushd gateway-app
mvn spring-boot:run -Drun.profiles=$profile &

popd
