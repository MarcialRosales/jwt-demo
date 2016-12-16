#!/bin/bash

set -eu

cf target

tokenService=`cf app token-service | grep urls | awk '{print $2}'`
echo "token-service running at ${tokenService}"

echo "Setting up asymmetrical keys ..."
privateKey=$(cat private.key)
publicKey=$(cat public.key)

echo "Updating backend-service with Public Key and activating asymmetrical profile ..."
cf set-env backend-service JWT_KEY "${publicKey}"
cf set-env backend-service SPRING_PROFILES_ACTIVE asymmetrical
cf restage backend-service

echo "Updating resource-service with Public Key and activating asymmetrical profile ..."
cf set-env resource-service JWT_KEY "${publicKey}"
cf set-env resource-service SPRING_PROFILES_ACTIVE asymmetrical
cf restage resource-service

echo "Issue Token for gateway to access backend-service"
backendToken=`curl -s -X POST -H "Content-Type: multipart/form-data" -F claims='{"aud":"backend", "sub":"gateway" }' -F asymkey=@private.key localhost:8081/token`

echo "Updating gateway with Public Key and activating asymmetrical profile ..."

cf set-env gateway JWT_KEY "${publicKey}"
cf set-env gateway BACKEND_TOKEN "${backendToken}"
cf set-env gateway SPRING_PROFILES_ACTIVE asymmetrical
cf restage gateway

echo "Done!"
