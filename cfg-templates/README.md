# Testing Only Config Files #

See assemblies/assembly-common/filtered-resources/etc/README.md for details on config files.

There are 3 .cfg files that are only used during development and are not included in any distribution. Default configuration
files are kept here and copied to the main directory by maven so every developer can edit the configurations for
deployment and testing without accidentally committing changes to the source tree. If these files change upstream they
may be overwritten by maven (depending on timestamps). To force a "clean checkout" of these files from the template
directory use the maven profile "-P overwrite_defaults".

* org.totalgrid.reef.test.cfg - This config file is used for all the tests and needs to point to a real and active
  qpid and postgres server for the tests to pass. This is also the configuration used by the integration tests when
  run with the "-Dremote-test" flag.
* standalone-node.cfg - This is the configuration for the "integrated node" that is run inside our integration tests
  when they are run without "-Dremote-test". By default it uses the same database and broker settings as the test file
  but it can be configured to use in-memory implementations if desired.
* target.cfg - This file is used by the "standalone app" versions of some of the operations that are normally done using
  the remote-cli. The most commonly used example is the StandaloneLoader to run reef:load or reef:unload. This file
  should be configured to point at the node we are trying to work against (default configuration points to local system).