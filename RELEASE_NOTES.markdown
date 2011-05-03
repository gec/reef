Release Notes
==============

Releases are published to the maven repository at https://repo.totalgrid.org/artifactory/webapp/home.html

Version Numbers are of the format {Major}.{Minor}.{Patch}.

* Major version updates are reserved for major features releases
* Minor version updates imply a significant api or datatype change
* Patch version updates should have little to no api or datatype changes

Version 0.2.3-RC3
==============

Primarily a stability and usability release, very limited new functionality.

### API Updates:

* IConnection got connect/disconnect functions
* IConnection start and stop are idempotent
* Added Mid-Level-APIs for Agents, Entities, MeasurementOverrides and Points
* Calling ISession.close is thread safe and will terminate any active requests, all future requests will fail instantly
* Mid-Level-APIs now have createSubscription(EventAcceptor<T>) functions so we don't need to pass sessions to mid-level client consumers
* Duplicate getMsg() function removed from ReefServiceException, use getMessage()

### Service Updates:

* Can subscribe to Events and Alarms through EventList and AlarmList services
* When issuing a Command we block until the status code is returned from the field, doesn't return executing
* Configuration files can now include custom types for a point or command. REEF-39
* Added setpoint support to karaf shell and xml loader

### Shell Commands:

* endpoint:list and channel:list to inspect communication path
* Added suite of Agent related commands to create/remove agents and change passwords
* "reef:login" no longer has password as argument, prompts for password
* added -dryRun option to "reef:load" to quickly check file for correctness
* invalid system models will not by load by default (added -ignoreWarnings option)
* "meas:download" will download measurement history to a Comma Separated File (CSV) for offline processing
* Added suite of Alarm related commands including silence, acknowledge and remove.
* Added remote-login command to support using karaf HMI on remote reef node
* Added metrics:throughput command to quickly measure measurement rates. use: "metrics:throughput *.measProcessed"

### Breaking Changes:

* FrontEndPort Protobuf names changed to CommChannel
* Post verb is now off by default, only specific services use it now

### Reef Internals:

* Protocol Adapters now inform system when channels (ports) and endpoints change online state
* All shell commands use Mid-Level-APIs
* ClientSession interface includes SubscriptionManagement trait and close function
* Postgres measurement store implementation was refactored to use multiple tables to decouple current value
  and history requests so more historical measurements can be stored without slowing down current value queries.

### Bug Fixes:

* "reef:resetdb" clears login token to avoid confusing error messages REEF-33
* Reef server no longer has 10 second delay on stopping REEF-29
* DNP Connections are correctly dropped when shutting down front ends REEF-17
* Event Service (not EventList Service) returns most recent 100, not oldest REEF-34
* Support for PULSE_TRIP control code for DNP endpoints REEF-31
* DNP protocol adapter sets units correctly REEF-24
* Other bugs mentioned in previous sections: REEF-43, REEF-10
* Alarm retrievals and updates by UID work as expected now
* Better loader warnings and behavior when config files are missing (includes REEF-46)

Version 0.2.3-dev
==============
### API Updates:

* Added more instrumentation and calculations to get measurement rates
* Created mid-level APIs for config files, commands, measurements and events
* Added new Service "EntityAttributes" that allows adding key-value data to entities
* Added display name to Commands
* Added "ValueMap" boolean -> string conversions to measurement pipeline
* Added synchronous start/stop functions to IConnection

### Breaking Changes:

* MeasurementHistory no longer has "ascending" flag, always oldest to newest. REEF-27
* Command UID is now Entity UUID, not name
* Units now more strictly enforced in XML loading
* IConnection Start/Stop now take millisecond timeouts

### Reef Internals:

* created wrapper to capture live request/responses for documentation
* Service providers are now asynchronous by nature
* Updated to scaliform 0.0.9
* Fixed mixed source compilation issues: REEF-19

### Bug Fixes:

* Indexes not needed for benchmark protocol communication endpoints
* Timers use "constant delay" rather than "constant offset" semantics
* Measurements are flushed when overridden (rather than on next field measurement update)
* Services requests for "all" of a resource are truncated to 100 entries: REEF-20
* Measurements from DNP3 endpoints have correct units: REEF-24
* Fixed corruption of password salts that was causing test failures.


Version 0.0.3
=============
### Major changes:
* Summary points (counts for alarms and 'abnormal' measurements)
* Events have a rendered text message and the arguments are now name+value rather than positional
* Replaced measurement and command streaming with "addressable services" so fep can't flood broker with messages
* Refactored MeasProc + FEP assignment into services and removed problematic standalone coordinator (faster and less prone to failure)
* Changed threading model for all entry points so actor starvation wont cause spurious application timeouts


Version 0.0.2
=============
### Major changes:
* Renamed/Moved proto definitions to match proto style guide and be more consistent
* Events + Alarms are now being generated and can be subscribe to by substation, device or point
* Rails HMI is now 100% on the bus, including the user authorization
* demo system has configuration to talk to demo hardware in the lab
* simulator produces 'random walks' to make the test data more interesting, usable
* # of 'abnormal' points is counted as a POC of the summary mechanisms


Version 0.0.1
=============
### Major changes:
* Entity queries (list of substations, list of equipment in substations, points in substations, commands under points etc)
* Points and Measurements (including subscriptions)


RoadMap
=============

* Add UUID subproto to all resources

#### Formatting

This file uses github flavored markdown, a good test renderer is available at github:
http://github.github.com/github-flavored-markdown/preview.html