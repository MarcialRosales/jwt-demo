#Json Web Token demonstration

## Introduction
Demonstrate how to secure enterprise applications (REST-api, no UI apps) using Json Web Token using symmetrical and asymmetrical keys. Typically, in the enterprise world, end-users do not decide about what they can or cannot do, i.e. their roles. Instead, end-users are centrally managed by an identity provider along with their roles (in case of RBAC - role based access control). This project demonstrate how we can secure traditional enterprise applications using JWT.   

## Enterprise Authorization scenarios
There are 3 authorization scenarios we can encounter in enterprise-like applications. But before we talk about authorization we should first talk about authentication which is outside of JWT discussion but it deserves bringing to our attention. There must be a login service where our users authenticate (by presenting their credentials like username/password) and they get back a JWT token. The JWT token encapsulates who this user is and what this user is entitled to do, i.e. roles, claims, whatever we want to call it.
Clarification: For demonstration purposes, we have provided an application called `token-service` which issues tokens, the same tokens that a login service would issue upon a successful authentication. Our `token-service` does not authenticate users, it directly issue digitally signed tokens.

Once the user has a JWT token, the user can access applications secured with JWT. We are now going to see 3 typical authorization scenarios.

First scenario is where the client (`AnyRestClient`) sends a request to an application which requires the user to have a given role.
```
    <AnyRestClient> ----http(with client_JWT)---> Gateway (requires ADMIN role)
```
In this scenario, the `gateway` authorizes requests based on the client's role found in the client's token.

The second scenario is where the client (`AnyRestClient`) sends a request whose purpose is to access/manipulate a client's resource, e.g. its account, its messages, etc. The client's resources are served by a downstream service called `resource-service`. But the client does not directly interact with the `resource-service` but via a `gateway` app.
```
    <AnyRestClient> ----http(with client_JWT)---> Gateway --http(with client_JWT)------> resource-service
```
In this scenario, the `resource-service` authorizes requests based on the client's JWT because it has the roles used by the `resource-service` to decide whether it accepts the request. e.g. `resource.read` or `resource.write`.

And the third scenario is where the client sends a request and the `gateway` application needs to call a downstream infrastructure service, e.g. a notification service, a cache service, etc. This scenario does not necessarily need to be triggered by a client's action, it can also be triggered by other type of events like a time event or a message arrival event.
```
    <AnyRestClient> ---http(with client_JWT)---> Gateway ---http(with gateway_JWT)----> backend-service (triggered by user)
                                                 Gateway ---http(with gateway_JWT)----> backend-service (triggered by other event)
```
In this scenario, the `backend-service` does not serve any client's resource hence it does not really need a client's JWT. Instead, it expects a JWT token which has `aud` = `backend-service`.

In this demonstration project we are leveraging 2 authorization methods. One is the traditional role-based authorization which compares the user's roles contained within a JWT Token with the roles expected by the application. The second method is based on the [JWT Specification](https://tools.ietf.org/html/rfc7519) which relies on the `aud` claim :

> aud: ... The principal intended to process the JWT MUST be identified with the value of the audience claim. If the principal processing the claim does not identify itself with the identifier in the aud claim value then the JWT MUST be rejected.


## Authorization scenarios demonstration

Our rest client (curl, Postman, or whatever you prefer) talks to the `Gateway` which runs on port 8080. This application expects all requests to provide, at least, a signed JWT token (a.k.a. JWS) with at least these 2 claims: `sub` the Subject and `aud` the Audience which must match the name of our gateway application (i.e. `gateway`).

This demonstration project supports both, symmetrical and asymmetrical keys. The `token-service` is capable of issuing JWT tokens signed using both type of keys. The applications `gateway`, `resource-service` and `backend-service` are also configured to support both types of signatures. We can select which type of signature we want to use by activating the right Spring profile. It has 2 profiles: `symmetrical` and `asymmetrical`, being the former the default one.
The signing key is configured in `application.yml#jwt.key` property. If the key is symmetrical, all applications including the `token-service` must use the same key. If the key is asymmetrical, the `token-service` uses the private key (found in the  `private.key` file) and the applications use the public key (found in the file `public.key`).

In the following sections we are going to generate tokens (via the `token-service`) to test various scenarios. If the applications are running with the `symmetrical` profile remember to take the symmetrical key from one of the `application.yml#jwt.key` property. In the contrary, if you are testing with the `asymmetrical` profile use the `private.key` file. For further details, check out the section [JWT Token Service](#json-web-token-service-jwt-token-service)

We have provided a script, `start.sh`, that launches all 4 applications. By default, it will launch them with the `symmetrical` profile. To launch with the `asymmetrical` do: `./start.sh asymmetrical`.

### Scenario 1. Non-authenticated request should get back a `401` status code.
Our first scenario attempts to access our `gateway` application without any tokens.

Launch the `gateway` application (if you have already executed the script `start.sh` it is already running on port 8080) and try this request.

```
curl localhost:8080
```
Produces:
```
{"timestamp":1481534933500,"status":401,"error":"Unauthorized","message":"Authentication Failed: JWT token not found","path":"/"}
```

### Scenario 2. Authenticated request with both, `aud` and `sub` claims, and signed with the same key as configured in the `application.yml` should get back a `200` status code and a greeting message.
In order to access the gateway, we need to have a valid token. Let's launch `token-service`. This service exposes one simple rest endpoint which takes one request parameter which is the key we want to use to sign the token. We are going to copy into the clipboard the symmetrical key configured in our gateway app (`application.yml#jwt.key`).

```
trdFmDVIKGhC8wR7be36Jyve3lqQRLTI
```
Next we are going to submit the following request to `jwt-token-service`. See that we are declaring 2 claims, `aud` and `sub`.
```
symkey=trdFmDVIKGhC8wR7be36Jyve3lqQRLTI
token=`curl -X POST -H "Content-Type: multipart/form-data" -F claims='{ "aud":"gateway", "sub":"bob" }' -F symkey=$symkey localhost:8081/token`
```

And finally, we are going to submit a request to the `gateway` using the token we just obtained.

```
curl -H "Authorization: Bearer $token" localhost:8080
```
Produces:
```
hello bob
```

### Scenario 3. Invalid signatures should get back `401` status code.
Let's try an invalid signature. Simply change the last character of the token.
```
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJnYXRld2F5Iiwic3ViIjoiYm9iIn0.-UDM8eThnUL_0rDZcGbmjMTjHUOIJx9R1q" localhost:8080
```
Produces:
```
{"timestamp":1481535342194,"status":401,"error":"Unauthorized","message":"Authentication Failed: JWT signature does not match locally computed signature. JWT validity cannot be asserted and should not be trusted.","path":"/"}
```

### Scenario 4. JWT without a matching audience should get back `401` status code.
The `gateway` expects a token whose `aud` claim matches its name, `gateway`. What happens if we send a token which has a different audience.

Request token with `aud: other`
```
symkey=trdFmDVIKGhC8wR7be36Jyve3lqQRLTI
token=`curl -X POST -H "Content-Type: multipart/form-data" -F claims='{ "aud":"other", "sub":"bob" }' -F symkey=$symkey localhost:8081/token`
```
Send request to gateway:
```
curl -H "Authorization: Bearer $token" localhost:8080
```
Produces:
```
{"timestamp":1481535529491,"status":401,"error":"Unauthorized","message":"Authentication Failed: Expected aud claim to be: gateway, but was: other.","path":"/"}
```

### Scenario 5. Role-based Authorization
So far we have reached endpoints which does not require further checks besides the `aud`. But the `gateway` exposes another endpoint, `/admin` which requires the user to have the `ADMIN` role. This role is contained in an agreed JWT claim. The name of this claim is configurable (`application.yml#jwt.claimName`) by default the name is `roles` (comma-separated values).

Let's try first sending a request which has no roles (`$token` variable created in the previous scenario).
```
curl -H "Authorization: Bearer $token" localhost:8080/admin
```
Produces:
```
{"timestamp":1481537257128,"status":403,"error":"Forbidden","exception":"org.springframework.security.access.AccessDeniedException","message":"Access is denied","path":"/admin"}
```

Now, let's request a token with `roles: ADMIN`.
```
symkey=trdFmDVIKGhC8wR7be36Jyve3lqQRLTI
token=`curl -X POST -H "Content-Type: multipart/form-data" -F claims='{ "aud":"gateway", "sub":"bob", "roles": "ADMIN" }' -F symkey=$symkey localhost:8081/token`
```

Send `/admin` request to the `gateway` app:
```
curl -H "Authorization: Bearer $token" localhost:8080/admin
```

### Scenario 6. Securing downstream client resource (e.g. a service that deals with user's information)
The `gateway` endpoints we have seen so far do not delegate to other endpoints. In this scenario, the client is sending a request which either access or manipulates its own resource, say its account, or its messages, etc. The client's resource are served by another service called `resource-service`. The `resource-service` is our downstream service that the `gateway` will call.

The flow is as follows:
```
 <RestClient> ---/resource(with client_JWT)---> Gateway ---/resource(with client_JWT)----> resource-service
```
The `resource-service` exposes one endpoint `/resource` and 2 operations, `GET` and `POST`. The former requires the role `resource.read` and the latter `resource.write`. If our RestClient has the role `resource.read` it will be allowed to do a GET on `/resource`. Likewise, the role `resource.write` will allow to do a PUT on `/resource`.

Lets create a token for our user `Bob` with the roles `ADMIN,resource.read`:
```
symkey=trdFmDVIKGhC8wR7be36Jyve3lqQRLTI
token=`curl -X POST -H "Content-Type: multipart/form-data" -F claims='{ "aud":"gateway", "sub":"bob", "roles": "ADMIN,resource.read" }' -F symkey=$symkey localhost:8081/token`
```

Let's launch the resource-service first. Now we send a `GET` request:
```
curl  -H "Authorization: Bearer $token" localhost:8080/resource
```
It should succeed (it returns nothing).

But if we send a `POST` request, it should fail because our user hasn't got yet the `resource.write` role.
```
curl -X POST -d '' -H  "Authorization: Bearer $token" localhost:8080/resource
```
Produces:
```
{"timestamp":1481543136869,"status":403,"error":"Forbidden","exception":"org.springframework.security.access.AccessDeniedException","message":"No message available","path":"/backend"}
```

### Scenario 7. Securing Service-to-Service calls where the downstream service is a not a client resource (e.g. an infrastructure service)
In the previous scenario, the gateway called a downstream service which served clients' resources. But there are other scenarios where our `gateway` application needs to call infrastructure services, i.e. services which does not server clients' resources.
`backend-service` is our downstream service that the `gateway` will call. This service requires the caller to pass a JWT with the `aud` claim equal to `backend`.

The flow is as follows:
```
 <RestClient> ---/backend(with client_JWT)---> Gateway ---/(with gateway_JWT)----> backend-service
```
See how the `gateway` does not send the client's token to the `backend-service`. Instead it uses the token that authorizes it to talk to the `backend-service`.

`gateway` is already configured with a valid token. Check out `application.yml#backend.key` property.

Lets launch first the `backend-service` and then send a `/backend` request to the `gateway`. See that we can use any of the valid JWT we used in the previous sections.
```
curl -H "Authorization: Bearer $token" localhost:8080/backend
```

## Json Web Token service (`jwt-token-service`)

Rest service that issues JWT tokens to facilitate testing.

### Generate a symmetrical key
```
symkey=`curl localhost:8081/key`
```

### Generate token using symmetrical key
Assuming we have executed the previous command, we have the symmetrical key in the variable `symkey`.
```
token=`curl -X POST -H "Content-Type: multipart/form-data" -F claims='{"aud":"gateway", "sub":"bob" }' -F symkey=$symkey localhost:8081/token`
```

### Verify token signed using a symmetrical key
Assuming you have executed the previous command, we have the token in the variable `token` and the symmetrical key in the variable `symkey`.

```
curl -X POST -H "Content-Type: multipart/form-data" -F symkey=$symkey -F token=$token localhost:8081/verify
```
Produces:
```
{"aud":"gateway","sub":"bob"}
```

### Generate token using asymmetrical key (RSA)
First we need to generate a public/private key-pair. The following commands will produce 2 files: `private.key` and `public.key` that will use to sign and verify the JWT.
```
openssl req  -nodes -new -x509  -keyout server.key -out server.cert
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in server.key -out private.key
openssl x509 -pubkey -noout -in server.cert > public.key
```
Note: Java's ``PKCS8EncodedKeySpec`` class only understands `PKCS8` format. OpenSSL generates the private key in a different format.

Now we generate a token and we digitally sign it using the `private.key` file.
```
token=`curl -s -X POST -H "Content-Type: multipart/form-data" -F claims='{"aud":"gateway", "sub":"bob" }' -F asymkey=@private.key localhost:8081/token`
```

### Verify token signed using asymmetrical key (RSA)
Assuming you have executed the previous command, we have the token in the variable `token` and the public key in the `public.key` file.
```
curl -s -X POST -H "Content-Type: multipart/form-data" -F asymkey=@public.key -F token=$key localhost:8081/verify
```
Produces:
```
{"aud":"gateway","sub":"bob"}
```

## Deploy to Pivotal Cloud Foundry

### Secure applications in PCF using symmetrically signed tokens
So far we have been running the applications locally. Now we are going to deploy them, including the `token-service` to Pivotal Cloud Foundry. We have provided a script, `deploy.sh`, that generates a symmetrical key, configures the applications to use that symmetrical key and push the apps all in one go.
Before calling `deploy.sh` you must have a previously logged into CF (e.g. `cf login <targetURL>`).

To facilitate testing, we have provided a script, `generateTokens.sh`, that generates different tokens that we can use to test the  authorization scenarios described earlier. Once you execute the script, you can use them like this:

```
. ./generateTokens.sh
gateway=`cf app gateway | grep urls | awk '{print $2}'`
curl -H "Authorization: Bearer $symGatewayBob" http://$gateway
```

### Secure applications in PCF using asymmetrically signed tokens
So far you have tried various requests against the `gateway` running in PCF and secured using symmetrically signed tokens. Now we are going to reconfigure those applications to use an asymmetrical key instead. To do so, invoke the following script `setAsymKeysAndRestage.sh`. This script assumes you have executed the `deploy.sh` script.

`setAsymKeyAndRestage.sh` will use the key pair found in the files `private.key` and `public.key`.

Step by step:
1. execute `deploy.sh` if you have not already done it
2. execute `setAsymKeyAndRestage.sh`
3. `curl gateway.cfapps.pez.pivotal.io` should fail with `401 Unathorized` (assuming your gateway is running under gateway.cfapps.pez.pivotal.io)
4. `tokenService=token-service.cfapps.pez.pivotal.io` (assuming your token-service application is running under `token-service.cfapps.pez.pivotal.io`)
5. request a token
```
token=`curl -s -X POST -H "Content-Type: multipart/form-data" -F claims='{"aud":"gateway", "sub":"bob" }' -F asymkey=@private.key $tokenService/token`
```
6. `curl -H "Authorization: Bearer $token" gateway.cfapps.pez.pivotal.io` should suceed this time


### Provisioning Credentials using Environment variables
There are several ways we can use to provision credentials to an application in PCF. One way is to provide them via environment variables. This is the approached we have followed in this project. We declare the `jwt.key` property as an environment variable in the `manifest.yml` (see below).
```
env:
  JWT_KEY: eyJhbGciOiJSUzI1NiJ9.eyJhdWQiOiJnYXRld2F5Iiwic3ViIjoiYm9iIn0.unA8iC8Dea5F3aw8EO8QSEuIxiarDH8CRsq0AAAAV87BxMASnawDGjVjzg1p1yUNcQJ8U_q8sQ_InhAFdICwe10Fzc6QAAp1XzKcxSnWsaGshlupjeSdtoIAY_OcsU1JMMnpSvmwjJCR87snT1zp3Hi9je04fL_J-u2q1YqeXEw
```
(Spring Boot mapS JWT_KEY into jwt.key)

### Provisioning Credentials using User Provided Service
Another way to provision credentials is via [User Provided Services](https://docs.cloudfoundry.org/devguide/services/user-provided.html). This way allows us to separate the act of provisioning credentials from the act of pushing the application.  


## Limitations and further improvements

As we already know, tokens are validated by matching the `aud` claim against the ID of the application which is validating the token. This means that a token can only be used to access one application/resource. What about if we could issue a token that grants access to several applications? For instance, `aud: "backend-service, resource-service"` grants access to `backend-service` and `resource-service` apps. Or even better use wildcard domains, `aud: "resource-service, infra."` grants access to the `resource-service` app and also to all the applications that match the expression `infra.*` for instance, `infra.cache-service` or `infra.file-service`. To make this possible we would have to change the `JWTTokenValidator` class.

Applications are statically configured with a key. If the key were compromised, we would have to issue new tokens with a brand new key and reset the new key in all our applications. Let's briefly describe how we could improve our application.
Let's say we are using asymmetrical keys to sign the tokens. Our applications could download from `token-service` the public key every so often. As soon as the `token service` changed its private key, all applications would automatically detect the new public key.

Furthermore, we have statically configured the `gateway` application with the token it needs to access the `backend-service`. Ideally, tokens should be short-lived and applications should renew/refresh their tokens. In other words, applications which have assigned tokens, like the `gateway` application, should request a new token from the `token-service` either when the token is about to expire or when it detects the `token-service` has changed the signing key.
