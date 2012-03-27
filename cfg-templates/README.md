# Config Files #

The configuration files in reef are organized by function but in most cases could be merged into a single large file
without changing the resultant configuration.

> Note: All of the .cfg files will have the form org.totalgrid.reef.{something}.cfg, we will shorten this to
> o.t.r.{something}

* o.t.r.amqp : configuration for broker we are trying to connect to, by default points to a local node.
* o.t.r.user : base username/password, on a server node this will be system level user. In a remote-cli this can be
  the remote-cli or operator user.
* o.t.r.node : "Network", "Location" and "NodeName" of this particular instance of the server. This should be changed for
  every node in a multi node system.
* o.t.r.sql  : Configuration for the persistence layer. Defines database driver and server location.
* o.t.r.services : service provider settings like how to publish metrics and how many measurements to allow in measurement
  store.
* o.t.r.mstore : (stands for measurementstore) and describes which measurement store implementations we should use
* o.t.r.benchmarks : Configuration file for running the benchmark routine on a running node using reef:benchmark. It is
  intended that that file be copied and edited to test specific cases.

## Loading and Defaults ##

Config files are loaded in order from "most generic" to "most specific", overwriting any duplicate values with the most
recently read value. This allows us to specify a default configuration in the most generic file and then override only
a few variables in specific files for specific purposes. The loader ignores files that do not exist, but will throw
an error if a needed parameter is not specified in any of the loaded configuration files.

Each of the "core applications" loads a set of common configuration files then one or two specific config files. In most
cases this is the name of the particular type of application. Usually the app specific files just contain the name of
the user we want to run the application with.

* common : o.t.r.amqp, o.t.r.user, o.t.r.node
* services : common, o.t.r.sql, o.t.r.services
* measproc : common, o.t.r.services
* fep : common, o.t.r.fep
* protocols : common, o.t.r.fep, o.t.r.protocol.{protocol name}
* metrics : common, o.t.r.metrics
* remote-cli : common, o.t.r.cli
* measurement store : o.t.r.mstore

## Testing Only Files ##

There are 3 .cfg files that are only used during development and are not included in any distribution.

* org.totalgrid.reef.test.cfg - This config file is used for all the tests and needs to point to a real and active
  qpid and postgres server for the tests to pass. This is also the configuration used by the integration tests when
  run with the "-Dremote-test" flag.
* standalone-node.cfg - This is the configuration for the "integrated node" that is run inside our integration tests
  when they are run without "-Dremote-test". By default it uses the same database and broker settings as the test file
  but it can be configured to use in-memory implementations if desired.
* target.cfg - This file is used by the "standalone app" versions of some of the operations that are normally done using
  the remote-cli. The most commonly used example is the StandaloneLoader to run reef:load or reef:unload. This file
  should be configured to point at the node we are trying to work against (default configuration points to local system).