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

rm -f /tmp/postgres.sql
cat > /tmp/postgres.sql <<-DELIM

CREATE USER core WITH PASSWORD 'core';
CREATE DATABASE reef_d;
CREATE DATABASE reef_t;
GRANT ALL PRIVILEGES ON DATABASE reef_d TO core;
GRANT ALL PRIVILEGES ON DATABASE reef_t TO core;

DELIM
sudo su postgres -c "psql < /tmp/postgres.sql" && rm /tmp/postgres.sql


Qpid 0.8
==============================

The Qpid project has 2 implementations of the broker, java and c++. We have done our testing primarily with the c++
broker running as a daemon on Ubuntu 10.10 and it is our recommendation for production installations. Unfortunately the
c++ broker is not as easy to install as it could be, especially on non-fedora linux. We are planning on releasing the
"sprinkle" scripts we use to install qpid 0.8 c++ on lucid linux. Using the c++ broker on windows appears to require
modifying windows level users, not necessarily bad but not necessary for development or testing. In general we suggest
that people use the java broker until it proves inadequate.

Java Broker
------------------------------

The java broker is very easy to use on all platforms, just download and extract qpid-java-broker-0.8.tar.gz file. Then
run the appropriate script in the bin directory. qpid-server or qpid-server.bat

C++ broker
------------------------------

Ubuntu:

You'll have to build from source, we will be publishing some ruby scripts to streamline that process in the near future.

Windows:

http://www.riverace.com/qpid/downloads.htm


If running on windows you will need to update the "org.totalgrid.reef.amqp.cfg" file to set the
"org.totalgrid.reef.amqp.user" and "org.totalgrid.reef.amqp.password" fields to a valid windows
user. It's recommended that you create a "qpid" windows user with minimal permissions for logging into qpid.

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


IntelliJ IDEA
===============================

Install the Scala plugin from File -> Settings -> Plugins.

Create new project, import from Maven format.

To run tests, Edit Run/Debug Configuration -> Defaults -> JUnit and set Working Directory to $MODULE_DIR$.