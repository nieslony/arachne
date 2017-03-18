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
    name text NOT NULL UNIQUE,
    email text NOT NULL,
    password text NOT NULL
);;
