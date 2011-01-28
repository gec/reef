PostgreSQL
==============================

Ubuntu:

> sudo apt-get install postgresql-8.4


Windows:

Download and install from postgresql.org

You will need to configure a user and database table for reef to use. The schema is initialized
later with an OSGI command, all we need to do externally is create the user and database. The following
script will setup the user/databases for running and testing with default settings. If on windows copy
the block starting with "CREATE USER" into the "SQL Shell (psql)" application (after logging in). If on
linux you should be able to copy/paste the whole script into a command shell.

rm -f postgres.sql
cat > postgres.sql <<-DELIM

CREATE USER core WITH PASSWORD 'core';
CREATE DATABASE reef_d;
CREATE DATABASE reef_t;
GRANT ALL PRIVILEGES ON DATABASE reef_d TO core;
GRANT ALL PRIVILEGES ON DATABASE reef_t TO core;

DELIM
sudo su postgres -c "psql < postgres.sql"


Qpid 0.8
==============================

Ubuntu:

You'll have to build from source.

Windows:

http://www.riverace.com/qpid/downloads.htm

If running on windows you will need to update the "org.totalgrid.reef.cfg" file to set the
"org.totalgrid.reef.amqp.user" and "org.totalgrid.reef.amqp.password" fields to a valid windows
user.

DNP3
==============================

If you plan to use Reef w/ DNP3 support you'll have to install a separate library before adding
the dnp3 protocol from the Karaf shell.

http://code.google.com/p/dnp3/downloads/list

Download the shared library for your platform.

Ubuntu:

Copy the library to /usr/lib.

> sudo cp <path to library> /usr/lib/libdnp3java.so.<version>

Make a symbolic link to the current version:

> sudo ln -s /usr/lib/libdnp3java.so.<version> /usr/lib/libdnp3java.so

Windows:

Copy the file to your windows system 32 directory. 64bit users copy the 64bit library to systemWow64.