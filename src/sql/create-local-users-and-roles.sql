/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * Author:  claas
 * Created: 09.03.2017
 */

CREATE TABLE users(
    id SERIAL NOT NULL PRIMARY KEY,
    username text NOT NULL UNIQUE,
    fullName text,
    givenName text,
    surName text,
    email text,
    password text
);;
CREATE INDEX ON propertygroups (name);;

CREATE TABLE roles(
    id SERIAL NOT NULL PRIMARY KEY,
    rolename text NOT NULL
);;
INSERT INTO roles (rolename) VALUES
    ('admin'),
    ('user');;

CREATE TABLE role_rules(
    role_id SERIAL references roles(id),
    roleRuleName text NOT NULL,    
    param text NOT NULL,
    UNIQUE(role_id, roleRuleName, param)
);;
