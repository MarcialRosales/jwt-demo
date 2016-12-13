#Json Web Token demonstration

Demonstrate how to secure applications using Json Web Token using symmetrical and asymmetrical keys.

There are 2 authorization scenarios. One scenario where the client (`AnyRestClient`) sends a request whose purpose is to access/manipulate a client's resource, e.g. its account, its messages, etc. The client's resources are served by a downstream service called `resource-service`. But the client does not directly interact with the `resource-service` but via a `gateway` app.
```
    <AnyRestClient> ----http(with client_JWT)---> Gateway --http(with client_JWT)------> resource-service
```
In this scenario, the `resource-service` authorizes requests based on the client's JWT because it has the roles used by the `resource-service` to decide whether it accepts the request. e.g. `resource.read` or `resource.write`.

A second scenario is where the client sends a request and the `gateway` application needs to call a downstream infrastructure service, e.g. a notification service, a cache service, etc. This scenario does not necessarily need to be triggered by a client's action, it can also be triggered by other type of events like a time event or a message arrival event.
```
    <AnyRestClient> ---http(with client_JWT)---> Gateway ---http(with gateway_JWT)----> backend-service
```
In this scenario, the `backend-service` does not serve any client's resource hence it does not really need a client's JWT. Instead, it expects a JWT token which has `aud` = `backend-service`.

In this demonstration project we are leveraging 2 authorization methods. One is the traditional role-based authorization which compares the user's roles contained within a JWT Token with the roles expected by the application. The second method is based on the JWT Specification :

>The principal intended to process the JWT MUST be identified with the value of the audience claim. If the principal processing the claim does not identify itself with the identifier in the aud claim value then the JWT MUST be rejected.


## Authorization scenarios

Our rest client (curl, Postman, or whatever you prefer) talks to the `Gateway` which runs on port 8080. This application expects all requests to provide, at least, a signed JWT token (a.k.a. JWS) with at least these 2 claims: `sub` the Subject and `aud` the Audience which must match the name of our gateway application (i.e. `gateway`).

This demonstration project supports both, symmetrical and asymmetrical keys. The `token-service` is capable of issuing JWT tokens signed using both type of keys. The applications `gateway`, `resource-service` and `backend-service` are also configured to support both types of signatures. We can select which type of signature we want to use by activating the right Spring profile. It has 2 profiles: `symmetrical` and `asymmetrical`, being the former the default one.
The key is configured in `application.yml#jwt.key` property. If the key is symmetrical, all applications including the `token-service` must use the same key. If the key is asymmetrical, the `token-service` has the private key (found in the file `private.key` and the applications have the public key (found in the file `public.key`).

In the following sections we are going to generate tokens (via the `token-service`) to test various scenarios. If the applications are running with the `symmetrical` profile remember to take the symmetrical key from one of the `application.yml#jwt.key` property. In the contrary, if you are testing with the `asymmetrical` profile use the `private.key` file.


### Non-authenticated request should get back a `401` status code.
Our first scenario attempts to access our `gateway` application without any tokens.

Launch the `gateway` application and try this request.

```
curl localhost:8080
```
Produces:
```
{"timestamp":1481534933500,"status":401,"error":"Unauthorized","message":"Authentication Failed: JWT token not found","path":"/"}
```

### Authenticated request with both, `aud` and `sub` claims, and signed with the same key as configured in the `application.yml` should get back a `200` status code and a greeting message.
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

### Invalid signatures should get back `401` status code.
Let's try an invalid signature. Simply change the last character of the token.
```
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJnYXRld2F5Iiwic3ViIjoiYm9iIn0.-UDM8eThnUL_0rDZcGbmjMTjHUOIJx9R1q" localhost:8080
```
Produces:
```
{"timestamp":1481535342194,"status":401,"error":"Unauthorized","message":"Authentication Failed: JWT signature does not match locally computed signature. JWT validity cannot be asserted and should not be trusted.","path":"/"}
```

### JWT without a matching audience should get back `401` status code.
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

### Role-based Authorization
So far we have reached endpoints which does not require further checks besides the `aud`. But the `gateway` exposes another endpoint, `/bye` which requires the user to have the `ADMIN` role. This role is contained in an agreed JWT claim. The name of this claim is configurable (`application.yml#jwt.claimName`) by default the name is `roles` (comma-separated values).

Let's try first sending a request which has no roles (`$token` variable created in the previous scenario).
```
curl -H "Authorization: Bearer $token" localhost:8080/bye
```
Produces:
```
{"timestamp":1481537257128,"status":403,"error":"Forbidden","exception":"org.springframework.security.access.AccessDeniedException","message":"Access is denied","path":"/bye"}
```

Now, let's request a token with `roles: ADMIN`.
```
symkey=trdFmDVIKGhC8wR7be36Jyve3lqQRLTI
token=`curl -X POST -H "Content-Type: multipart/form-data" -F claims='{ "aud":"gateway", "sub":"bob", "roles": "ADMIN" }' -F symkey=$symkey localhost:8081/token`
```

Send `/bye` request to the `gateway` app:
```
curl -H "Authorization: Bearer $token" localhost:8080/bye
```

### Securing downstream client resource (e.g. a service that deals with user's information)
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

### Securing Service-to-Service calls where the downstream service is a not a client resource (e.g. an infrastructure service)
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

So far we have been running the applications locally. Now we are going to deploy them, including the `token-service` to Pivotal Cloud Foundry. We have provided a script, `deploy.sh`, that generates a symmetrical key, configures the applications to use the key and push the apps all in one go.

To facilitate testing, we have provided a script, `generateTokens.sh`, that generates different tokens that we can use to test the  authorization scenarios described earlier. Once you execute the script, you can use them like this:

```
. ./generateTokens.sh
gateway=`cf app gateway | grep urls | awk '{print $2}'`
curl -H "Authorization: Bearer $symGatewayBob" http://$gateway
```
