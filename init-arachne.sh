#!/bin/bash

curl -v \
    --header "Content-Type: application/json" \
    --request POST http://localhost:8080/setup \
    --data '
        {
            "adminUsername":"admin",
            "adminPassword":"bla",
            "adminEmail":"admin@localhost",
            "caCertSpecs": {
                "keyAlgo": "RSA",
                "keySize": 2048,
                "certLifeTimeDays": 3650,
                "subject": "cn=Arachne CA",
                "signatureAlgo": "SHA256withRSA"
            },
            "serverCertSpecs": {
                "keyAlgo": "RSA",
                "keySize": 2048,
                "certLifeTimeDays": 365,
                "subject": "cn='$HOSTNAME'",
                "signatureAlgo": "SHA256withRSA"
            },
            "userCertSpecs": {
                "keyAlgo": "RSA",
                "keySize": 2048,
                "certLifeTimeDays": 90,
                "subject": "cn={username}",
                "signatureAlgo": "SHA256withRSA"
            }
        }
        '
