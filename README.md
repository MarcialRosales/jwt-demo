#Json Web Token demonstration

Demonstrate how to secure applications using Json Web Token.

There are 2 authorization scenarios. One scenario where the client (`AnyRestClient`) sends a request whose purpose is to access/manipulate a client's resource, e.g. its account, its messages, etc. The client's resources are served by a downstream service called `resource-service`.
```
    <AnyRestClient> ----http(with client_JWT)---> Gateway --http(with client_JWT)------> resource-service
```
In this scenario, the `resource-service` authorizes based on the client's JWT because it has the roles used by the `resource-service` to decide whether it accepts the request. e.g. `resource.read` or `resource.write`.

A second scenario is where the client sends a request but the `gateway` application in this case needs to call a downstream infrastructure service, e.g. a notification service, a cache service, etc.
```
    <AnyRestClient> ---http(with client_JWT)---> Gateway ---http(with gateway_JWT)----> backend-service
```
In this scenario, the `backend-service` does not serve any client's resource hence it does not really need a client's JWT. Instead, it expects a JWT token which has `aud` = `backend-service`.



## Gateway application (`gateway-app`)

Sample application secured via a JWT which must have 2 claims: `sub` the Subject and `aud` the Audience which must match the name of our gateway application (i.e. `gateway`).

The Gateway app presents just a root end point, `/`. It returns us a greeting message `hello Bob`, if the request carries a `Bearer` JWT token in the `Authorization` header and the token contains the claims `sub: Bob` and `aud: gateway`.

Note: The Gateway is initialized with a symmetrical key under `application.yml` and property `jwt.secret`.

Let's try various requests. But first, launch it (it runs by default on port 8080).

### Non-authenticated request should get back a `401` status code.

```
curl localhost:8080
```
Produces:
```
{"timestamp":1481534933500,"status":401,"error":"Unauthorized","message":"Authentication Failed: JWT token not found","path":"/"}
```

### Authenticated request with both, `aud` and `sub` claims, and signed with the same key as configured in the `application.yml` should get back a `200` status code and a greeting message.

First of all, we are going to copy into the clipboard the symmetrical key configured in our gateway app (`application.yml#jwt.secret`).

```
trdFmDVIKGhC8wR7be36Jyve3lqQRLTI
```
Next we are going to submit the following request to `jwt-token-service`. First of all, launch it (it runs on port 8081).
```
curl -X POST -H "Content-Type: application/json" -d '{"aud":"gateway","sub":"bob"}' localhost:8081/symmetricalToken?symmetricalKey=trdFmDVIKGhC8wR7be36Jyve3lqQRLTI
```
Produces:
```
eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJnYXRld2F5Iiwic3ViIjoiYm9iIn0.-UDM8eThnUL_0rDZcGbmjMSQsE5aqpRTjHUOIJx9R1k
```

And finally, we are going to submit a request to the `gateway` app using the token we just obtained.

```
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJnYXRld2F5Iiwic3ViIjoiYm9iIn0.-UDM8eThnUL_0rDZcGbmjMSQsE5aqpRTjHUOIJx9R1k" localhost:8080
```
Produces:
```
hello bob
```

### Invalid signatures should get back `401` status code.
Let's simply change the last character of the token.
```
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJnYXRld2F5Iiwic3ViIjoiYm9iIn0.-UDM8eThnUL_0rDZcGbmjMTjHUOIJx9R1q" localhost:8080
```
Produces:
```
{"timestamp":1481535342194,"status":401,"error":"Unauthorized","message":"Authentication Failed: JWT signature does not match locally computed signature. JWT validity cannot be asserted and should not be trusted.","path":"/"}
```

### JWT without a matching audience should get back `401` status code.

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
`resource-service` is our downstream service that the `gateway` will call. It runs by default on port 8083.

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

If we send a `GET` request:
```
curl  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJnYXRld2F5Iiwic3ViIjoiYm9iIiwicm9sZXMiOiJBRE1JTixiYWNrZW5kLnJlYWQifQ.986Yzi0m5zTyncyLmnL_fxec8R5bw2msOUKXtR9x_9Q" localhost:8080/backend
```
It should succeed (it returns nothing).

But if we send a `POST` request, it should fail:
```
curl -X POST -d '' -H  "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJnYXRld2F5Iiwic3ViIjoiYm9iIiwicm9sZXMiOiJBRE1JTixiYWNrZW5kLnJlYWQifQ.986Yzi0m5zTyncyLmnL_fxec8R5bw2msOUKXtR9x_9Q" localhost:8080/backend
```
Produces:
```
{"timestamp":1481543136869,"status":403,"error":"Forbidden","exception":"org.springframework.security.access.AccessDeniedException","message":"No message available","path":"/backend"}
```

### Securing Service-to-Service calls where the downstream service is a not a client resource (e.g. an infrastructure service)
`backend-service` is our downstream service that the `gateway` will call. It runs by default on port 8082. This service requires the caller to pass a JWT with the `aud` claim equal to `backend`. In other words, a token that authorizes the `subject` to access the `audience`.

The flow is as follows:
```
 <RestClient> ---/backend(with client_JWT)---> Gateway ---/(with gateway_JWT)----> backend-service
```
See how the `Gateway` does not send the client's token to the `backend-service`. Instead it uses the token assigned to itself to talk to the `backend-service`.

`gateway` is already configured with a valid token. Check out `application.yml#backend.secret` property. It was obtained using this request:
```
curl -X POST -H "Content-Type: application/json" -d '{"aud":"backend", "sub": "gateway" }' localhost:8081/symmetricalToken?symmetricalKey=trdFmDVIKGhC8wR7be36Jyve3lqQRLTI
```

Let's send a `/backend` request to the `gateway`. See that we can use any of the valid JWT we used in the previous sections.
```
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJnYXRld2F5Iiwic3ViIjoiYm9iIiwicm9sZXMiOiJBRE1JTixiYWNrZW5kLnJlYWQifQ.986Yzi0m5zTyncyLmnL_fxec8R5bw2msOUKXtR9x_9Q" localhost:8080/backend
```



## Json Web Token service (`jwt-token-service`)

Rest service that issues JWT tokens to facilitate testing.

To request a symmetrically signed JWT token we submit the following request where we specify as a request parameter (`symmetricalKey`) the key and claims -in json format- in the request's body.

```
curl -X POST -H "Content-Type: application/json" -d '{"aud":"gateway","sub":"bob"}' localhost:8081/symmetricalToken?symmetricalKey=trdFmDVIKGhC8wR7be36Jyve3lqQRLTI
```
