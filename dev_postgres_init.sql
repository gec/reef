CREATE USER core WITH PASSWORD 'core'; 
CREATE DATABASE reef_d; 
CREATE DATABASE reef_t; 
GRANT ALL PRIVILEGES ON DATABASE reef_d TO core; 
GRANT ALL PRIVILEGES ON DATABASE reef_t TO core;

CREATE DATABASE reef2_t; 
GRANT ALL PRIVILEGES ON DATABASE reef2_t TO core;
