#Json Web Token demonstration

Demonstrate how to secure applications using Json Web Token.

# Json Web Token service (jwt-token-service)

Rest service that issues JWT tokens to facilitate testing.

To request a symmetrically signed JWT token we submit the following request where we specify as a request parameter (`symmetricalKey`) the key and claims -in json format- in the request's body.

```
curl -X POST -H "Content-Type: application/json" -d '{"aud":"gateway","sub":"bob"}' localhost:8081/symetricalToken?symetricalKey=trdFmDVIKGhC8wR7be36Jyve3lqQRLTI
```
