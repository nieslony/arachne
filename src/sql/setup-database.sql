DROP DATABASE IF EXISTS openvpnadmin;
DROP USER IF EXISTS openvpnadmin;

CREATE USER openvpnadmin WITH UNENCRYPTED PASSWORD 'Moin123';
CREATE DATABASE openvpnadmin OWNER openvpnadmin;

