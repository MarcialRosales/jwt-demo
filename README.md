#Json Web Token demonstration

Demonstrate how to secure applications using Json Web Token.

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
```
The principal intended to process the JWT MUST be identified with the value of the audience claim. If the principal processing the claim does not identify itself with the identifier in the aud claim value then the JWT MUST be rejected.
```


## Authorization scenarios

Our rest client (curl, Postman, or whatever you prefer) talks to the `Gateway` which runs on port 8080. This application expects all requests to provide, at least, a signed JWT token (a.k.a. JWS) with at least these 2 claims: `sub` the Subject and `aud` the Audience which must match the name of our gateway application (i.e. `gateway`).

This demonstration project uses -at least for now- Symmetrical keys to sign the token. In particular, it uses HS256. In order to verify a signature, we need the symmetrical key that produced it. The `Gateway` is initialized with a symmetrical key on the `application.yml#jwt.secret` property.

We will need to generate tokens for our client. We have provided an application called `token-service` that issues tokens signed with the same symmetrical key configured in the `gateway` (and also in other apps).


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
In order to access the gateway, we need to have a valid token. Let's launch `token-service`. This service exposes one simple rest endpoint which takes one request parameter which is the key we want to use to sign the token. We are going to copy into the clipboard the symmetrical key configured in our gateway app (`application.yml#jwt.secret`).

```
trdFmDVIKGhC8wR7be36Jyve3lqQRLTI
```
Next we are going to submit the following request to `jwt-token-service`. See that we are declaring 2 claims, `aud` and `sub`.
```
curl -X POST -H "Content-Type: application/json" -d '{"aud":"gateway","sub":"bob"}' localhost:8081/symmetricalToken?symmetricalKey=trdFmDVIKGhC8wR7be36Jyve3lqQRLTI
```
Produces:
```
eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJnYXRld2F5Iiwic3ViIjoiYm9iIn0.-UDM8eThnUL_0rDZcGbmjMSQsE5aqpRTjHUOIJx9R1k
```

And finally, we are going to submit a request to the `gateway` using the token we just obtained.

```
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJnYXRld2F5Iiwic3ViIjoiYm9iIn0.-UDM8eThnUL_0rDZcGbmjMSQsE5aqpRTjHUOIJx9R1k" localhost:8080
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
curl -X POST -H "Content-Type: application/json" -d '{"aud":"other","sub":"bob"}' localhost:8081/symmetricalToken?symmetricalKey=trdFmDVIKGhC8wR7be36Jyve3lqQRLTI
```
Produces:
```
eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJvdGhlciIsInN1YiI6ImJvYiJ9.Z7Q_SveBQpMaVtTNslfsht1yLF04AMd7IDBGzldVrEc
```
Send request to gateway:
```
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJvdGhlciIsInN1YiI6ImJvYiJ9.Z7Q_SveBQpMaVtTNslfsht1yLF04AMd7IDBGzldVrEc" localhost:8080
```
Produces:
```
{"timestamp":1481535529491,"status":401,"error":"Unauthorized","message":"Authentication Failed: Expected aud claim to be: gateway, but was: other.","path":"/"}
```

### Role-based Authorization
So far we have reached endpoints which does not require further checks besides the `aud`. But the `gateway` exposes another endpoint, `/bye` which requires the user to have the `ADMIN` role. This role is contained in an agreed JWT claim. The name of this claim is configurable (`application.yml#jwt.claimName`) by default the name is `roles` (comma-separated values).

Let's try first sending a request which has no roles.
```
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJnYXRld2F5Iiwic3ViIjoiYm9iIn0.-UDM8eThnUL_0rDZcGbmjMSQsE5aqpRTjHUOIJx9R1k" localhost:8080/bye
```
Produces:
```
{"timestamp":1481537257128,"status":403,"error":"Forbidden","exception":"org.springframework.security.access.AccessDeniedException","message":"Access is denied","path":"/bye"}
```

Now, let's request a token with `roles: ADMIN`.

```
curl -X POST -H "Content-Type: application/json" -d '{"aud":"gateway","sub":"bob", "roles": "ADMIN" }' localhost:8081/symmetricalToken?symmetricalKey=trdFmDVIKGhC8wR7be36Jyve3lqQRLTI
```
Produces:
```
eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJnYXRld2F5Iiwic3ViIjoiYm9iIiwicm9sZXMiOiJBRE1JTiJ9._YXXN3uYlnwHQoQ05k_5uG-TNhuGJZ5QefWxpPNQM4k
```
Send `/bye` request to the `gateway` app:
```
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJnYXRld2F5Iiwic3ViIjoiYm9iIiwicm9sZXMiOiJBRE1JTiJ9._YXXN3uYlnwHQoQ05k_5uG-TNhuGJZ5QefWxpPNQM4k" localhost:8080/bye
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
curl -X POST -H "Content-Type: application/json" -d '{"aud":"gateway","sub":"bob", "roles": "ADMIN,backend.read" }' localhost:8081/symmetricalToken?symmetricalKey=trdFmDVIKGhC8wR7be36Jyve3lqQRLTI
```
Produces:
```
eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJnYXRld2F5Iiwic3ViIjoiYm9iIiwicm9sZXMiOiJBRE1JTixiYWNrZW5kLnJlYWQifQ.986Yzi0m5zTyncyLmnL_fxec8R5bw2msOUKXtR9x_9Q
```

Let's launch the resource-service first. Now we send a `GET` request:
```
curl  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJnYXRld2F5Iiwic3ViIjoiYm9iIiwicm9sZXMiOiJBRE1JTixiYWNrZW5kLnJlYWQifQ.986Yzi0m5zTyncyLmnL_fxec8R5bw2msOUKXtR9x_9Q" localhost:8080/backend
```
It should succeed (it returns nothing).

But if we send a `POST` request, it should fail because our user hasn't got yet the `resource.write` role.
```
curl -X POST -d '' -H  "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJnYXRld2F5Iiwic3ViIjoiYm9iIiwicm9sZXMiOiJBRE1JTixiYWNrZW5kLnJlYWQifQ.986Yzi0m5zTyncyLmnL_fxec8R5bw2msOUKXtR9x_9Q" localhost:8080/backend
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

`gateway` is already configured with a valid token. Check out `application.yml#backend.secret` property. It was obtained using this request:
```
curl -X POST -H "Content-Type: application/json" -d '{"aud":"backend", "sub": "gateway" }' localhost:8081/symmetricalToken?symmetricalKey=trdFmDVIKGhC8wR7be36Jyve3lqQRLTI
```

Lets launch first the backend-service and then send a `/backend` request to the `gateway`. See that we can use any of the valid JWT we used in the previous sections.
```
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJnYXRld2F5Iiwic3ViIjoiYm9iIiwicm9sZXMiOiJBRE1JTixiYWNrZW5kLnJlYWQifQ.986Yzi0m5zTyncyLmnL_fxec8R5bw2msOUKXtR9x_9Q" localhost:8080/backend
```

## Json Web Token service (`jwt-token-service`)

Rest service that issues JWT tokens to facilitate testing.

To request a symmetrically signed JWT token we submit the following request where we specify as a request parameter (`symmetricalKey`) the key and claims -in json format- in the request's body.

```
curl -X POST -H "Content-Type: application/json" -d '{"aud":"gateway","sub":"bob"}' localhost:8081/symmetricalToken?symmetricalKey=trdFmDVIKGhC8wR7be36Jyve3lqQRLTI
```
