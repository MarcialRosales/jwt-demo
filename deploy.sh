#!/bin/bash

set -eu

cf target

echo "Push token-service ..."
pushd jwt-token-service
mvn package
mvn resources:resources
cf push -f target/manifest.yml

tokenService=`cf app token-service | grep urls | awk '{print $2}'`
popd

echo "Generate signing key ..."
key=`curl $tokenService/key`
if [! -d tmp ]; then
 mkdir tmp
fi
echo $key > tmp/key

echo "Building jwt-helper ..."
pushd jwt-helper
mvn install
popd

echo "Push backend-service ..."
pushd backend-service
mvn package
mvn resources:resources -Djwt_secret=$key

cf push -f target/manifest.yml
backendService=`cf app backend-service | grep urls | awk '{print $2}'`
popd

echo "Push resource-service ..."
pushd resource-service
mvn package
mvn resources:resources -Djwt_secret=$key

cf push -f target/manifest.yml
resourceService=`cf app resource-service | grep urls | awk '{print $2}'`
popd

echo "Issue Token for gateway to access backend-service"
backendToken=`curl -s -X POST -H "Content-Type: multipart/form-data" -F claims='{"aud":"backend", "sub":"gateway" }' -F symkey=$key localhost:8081/token`

echo "Push gateway ..."
pushd gateway-app
mvn package
mvn resources:resources -Djwt_secret=$key -Dbackend_url=http://$backendService -Dbackend_token=$backendToken -Dresource_url=http://$resourceService

cf push -f target/manifest.yml
gateway=`cf app gateway | grep urls | awk '{print $2}'`
popd

echo "Deployment completed. Gateway is at ${gateway}"
