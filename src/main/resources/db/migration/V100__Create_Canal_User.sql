-- Create canal user for binlog replication
-- Using IF NOT EXISTS or ignoring errors for the user creation
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';
FLUSH PRIVILEGES;
