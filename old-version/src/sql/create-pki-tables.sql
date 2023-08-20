/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * Author:  claas
 * Created: 30.03.2017
 */

CREATE TYPE certtype AS ENUM ('CA', 'SERVER', 'CLIENT');;

CREATE TABLE certificates(
    serial text NOT NULL UNIQUE,
    commonName text NOT NULL,
    validFrom timestamp NOT NULL,
    validTo timestamp NOT NULL,
    isRevoked boolean DEFAULT false,
    certificate bytea NOT NULL,
    privateKey bytea NOT NULL,
    certtype certtype NOT NULL
);;

CREATE INDEX ON certificates(commonName);;
