#!/bin/bash

set -e # fail fast
#set -x # print commands

key=$(cat tmp/key)
tokenService=`cf app token-service | grep urls | awk '{print $2}'`

symGatewayBob=`curl -s -X POST -H "Content-Type: multipart/form-data" -F claims='{"aud":"gateway", "sub":"bob" }' -F symkey=key http://$tokenService/token`
symGatewayBobAdmin=`curl -s -X POST -H "Content-Type: multipart/form-data" -F claims='{"aud":"gateway", "sub":"bob", "roles" : "ADMIN" }' -F symkey=key http://$tokenService/token`
symGatewayBobAdminResourceRead=`curl -s -X POST -H "Content-Type: multipart/form-data" -F claims='{"aud":"gateway", "sub":"bob", "roles" : "ADMIN,resource.read" }' -F symkey=key http://$tokenService/token`
symGatewayBobFull=`curl -s -X POST -H "Content-Type: multipart/form-data" -F claims='{"aud":"gateway", "sub":"bob", "roles" : "ADMIN,resource.read,resource.write" }' -F symkey=key http://$tokenService/token`

#asymGatewayBob=`curl -s -X POST -H "Content-Type: multipart/form-data" -F claims='{"aud":"gateway", "sub":"bob" }' -F asymkey=@private.key http://$tokenService/token`
#asymGatewayBobAdmin=`curl -s -X POST -H "Content-Type: multipart/form-data" -F claims='{"aud":"gateway", "sub":"bob", "roles" : "ADMIN" }' -F asymkey=@private.key http://$tokenService/token`
#asymGatewayBobAdminResourceRead=`curl -s -X POST -H "Content-Type: multipart/form-data" -F claims='{"aud":"gateway", "sub":"bob", "roles" : "ADMIN,resource.read" }' -F asymkey=@private.key http://$tokenService/token`
#asymGatewayBobFull=`curl -s -X POST -H "Content-Type: multipart/form-data" -F claims='{"aud":"gateway", "sub":"bob", "roles" : "ADMIN,resource.read,resource.write" }' -F asymkey=@private.key http://$tokenService/token`

export symGatewayBob symGatewayBobAdmin symGatewayBobAdminResourceRead symGatewayBobFull
#export asymGatewayBob asymGatewayBobAdmin asymGatewayBobAdminResourceRead asymGatewayBobFull
