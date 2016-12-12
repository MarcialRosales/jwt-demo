#Json Web Token demonstration

Demonstrate how to secure applications using Json Web Token.

## Gateway application (`gateway-app`)

Sample application secured via a JWT which must have 2 claims: `sub` the Subject and `aud` the Audience which must match the name of our gateway application (i.e. `gateway`).

The Gateway app presents just a root end point, `/`. It returns us a greeting message `hello Bob`, if the request carries a `Bearer` JWT token in the `Authorization` header and the token contains the claims `sub: Bob` and `aud: gateway`.

Note: The Gateway is initialized with a symmetrical key under `application.yml` and property `jwt.secret`.

Let's try various requests.

### Non-authenticated request should get back a `401` status code.

```
curl localhost:8080
```
Produces:
```
{"timestamp":1481534933500,"status":401,"error":"Unauthorized","message":"Authentication Failed: JWT token not found","path":"/"}
```

### Authenticated request with both, `aud` and `gateway` claims, and signed with the same key as configured in the `application.yml` should get back a `200` status code and a greeting message.

First of all, we are going to copy into the clipboard the symmetrical key configured in our gateway app.
```
trdFmDVIKGhC8wR7be36Jyve3lqQRLTI
```
Next we are going to submit the following request to `jwt-token-service`:
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
curl -X POST -H "Content-Type: application/json" -d '{"aud":"other","sub":"bob"}' localhost:8081/symetricalToken?symetricalKey=trdFmDVIKGhC8wR7be36Jyve3lqQRLTI
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


## Json Web Token service (`jwt-token-service`)

Rest service that issues JWT tokens to facilitate testing.

To request a symmetrically signed JWT token we submit the following request where we specify as a request parameter (`symmetricalKey`) the key and claims -in json format- in the request's body.

```
curl -X POST -H "Content-Type: application/json" -d '{"aud":"gateway","sub":"bob"}' localhost:8081/symmetricalToken?symmetricalKey=trdFmDVIKGhC8wR7be36Jyve3lqQRLTI
```
