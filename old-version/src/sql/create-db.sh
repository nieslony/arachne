DB_PWD=Moin123
DB_USER=openvpnadmin
DB_NAME=openvpnadmin

#HASH=$( echo -n md5 ; echo -n ${DB_PWD}${DB_USER} | md5sum | cut -d' ' -f1 )
HASH=$( echo -n sha512 ; echo -n ${DB_PWD}${DB_USER} | sha512sum | cut -d' ' -f1 )

cat setup-database.sql |
    sed -e s/DB_NAME/${DB_NAME}/g |
    sed -e s/DB_USER/${DB_USER}/g |
    sed -e s/DB_PASSWORD/$HASH/g
    
