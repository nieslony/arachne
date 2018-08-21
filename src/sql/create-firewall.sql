/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * Author:  claas
 * Created: 10.02.2018
 */

CREATE TABLE firewallEntries (
    id SERIAL NOT NULL PRIMARY KEY,
    label text,
    description text,
    isActive boolean
);;

CREATE TABLE firewallEntryWho (
    firewallEntry_id SERIAL references firewallEntries(id),
    ruleType text NOT NULL,
    param text
);;

CREATE TYPE Protocol AS ENUM('TCP', 'UDP');;
CREATE TABLE firewallEntryWhat (
    firewallEntry_id SERIAL references firewallEntries(id),
    service text,
    whatType text,
    port integer,
    ports integer ARRAY,
    portFrom integer,
    portTo integer,
    protocol Protocol
);;

CREATE TABLE firewallEntryWhere (
    firewallEntry_id SERIAL references firewallEntries(id),
    whereType text NOT NULL,
    hostname text,
    network cidr
);;
