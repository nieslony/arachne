openssl x509 -req -days 360 -in signing-ca-csr.pem -CA root-ca-cert.pem -CAkey root-ca-key.pem -out signing-ca-cert.pem -CAcreateserial
