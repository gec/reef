

## Contents ##

This package provides all of the components necessary to run a Reef server node.

## Instructions ##

Follow the guide for instructions of how to install database and broker packages and run the server using bin/start
and bin/client or bin/karaf. To start a new server we need to prepare the database using the following commands.

```
# prepare database
start-level 90
features:install reef
reef:resetdb

# startup system and load a sample equipment configuration
start-level 100
reef:load samples/mainstreet/config.xml

# if using dnp3 protocol
features:install reef-dnp3
```

## Documentation ##

See the documentation at http://code.google.com/p/reef/ for instructions on how to install and configure the server.