Release Notes
==============

Releases are published to the maven repository at https://repo.totalgrid.org/artifactory/webapp/home.html

Version Numbers are of the format {Major}.{Minor}.{Patch}.

* Major version updates are reserved for major features releases
* Minor version updates imply a significant api or datatype change
* Patch version updates should have little to no api or datatype changes

Version 0.4.8 - May 9, 2012
=============

This was primarily a refactoring and cleanup release. Prior to this version there were two seperate Client interfaces,
one for scala, one for java. We have merged all of the functionality into the java client and removed the scala client.
Java developers and applications now have full acces to features that before were only available to scala applications.
There were also some important performance enhancements on the service implementation side.

### Major Features:

* Promoted java client to "first class" citizen and ported all functionality that was previously available only to scala
  into java client. This allowed removing all external uses of the scala client class.
* Added point_uuid to Measurements and added getXxxxxByUuid calls for snapshots, history and statistics functions
* Added --force option to reef:unload
* All requests use TTL (time to live) to stop server from handling old requests.
* Client applications that handle command requests are bound to the command queue by server only, only the main service
  nodes need to have "bind" level access to the broker.
* Endpoints can be "externally-managed" by ProtocolAdapters, (previous behavior of "autoAssign" is still default)
* ProtocolAdapters change endpoint state to ERROR if there is an issue during configuration of the protocol

### Reef Internals:

* Added batched loading of related properties to speed up slow getXxxxx queries that were making far too many database
  requests to populate objects.
* Filtering of invisibile entries are done during read rather than as a post filtering step (so response limit works)
* Requests that include ReefUUID or ReefID will "short-circuit" other ways of searching for faster lookups.

### Bug fixes

* Default JAVA_MAX_MEM and JAVA_MAX_PERM_MEM for server builds are now 1024M and 256M respectivley
* If calculator protocol has a misconfiguration just the bad points are marked #ERROR, not the whole endpoint.
* Fixed issue where measurement processor would get unassigned and never reattached

Version 0.4.7 - April 11, 2012
==============

Security focused release, all core applications now use different agents who have been limited to access only necessary
resources. Resource level security is partially implemented but should not be considered production ready in
this release. See services-authz/README.md for details on current authz features and limitations.

### Major Features:

* Client identifies version of code during login and logs mismatchs with server version
* Added shell commands login:list and login:revoke to see who is logged into the system
* "Core applications" load multiple .cfg files to allow overriding of properties (especially of user name)
* Added a suite of standard users and roles for the core applications
* All applications run as minimally privileged agents
* Liquibase database generation switched to default on. reef:resetdb is no longer destructive
* agent-permissions:filter shell command allows viewing which resources a role will have access to.
* Reworked display of agent:* and agent-permissions:* shell commands to be readable.
* Distributions have better licensing and include READMEs and RELEASE_NOTES
* Java compatible ProtocolManager interface for writing ProtocolAdapters

### Service/API Updates:

* CommandLocks are never deleted, just marked as "inactive", provides audit log
* Added client_version and server_version fields to SimpleAuth
* Added revoked and issue_time to AuthToken objects
* Applications can register their application version number and belong to multiple networks
* Dnp3 slave can configure output types and scale outputs and control inputs for fixed point systems

### Reef Internals:

* Changed handling of test .cfg files (see cfg-templates/README.md)
* Split up integration tests into function oriented files.
* Config file loading expects many files and silently ignores missing files
* Subscription ids don't include double quotes in the strings
* Services bundle won't start if the database schema is not uptodate

Version 0.4.6 - March 8, 2012
==============

Release is focused on the new calculator protocol and some minor bug fixes.

### Major Features:

* Initial implementation of "calculator" protocol (see /calculator/README.markdown)
* jQuery client files are now served at /jquery-libs/reef.client.js and /jquery-libs/reef.client.core-services.js

### Service/API Updates:

* EntityEdges are available through EntityService (and edges got distance field).

### Bug Fixes:

* sim:config command can select individual endpoints again
* fixed accidental logouts with multiple protocols sharing an FEP

Version 0.4.5 - February 21, 2012
==============

This focus is primarily related to the http-bridge and jQuery client.

### Major Features:

* Implemented jQuery based client that handles most of the AllScadaService calls including limited
  subscription support. (74 of 179)
* It is now possible to implement a service in java (before it required scala)

Version 0.4.4 - February 10, 2012
==============

This release was focused on bug fixes, stability and most importantly performance. This release contains some
large changes to measurement throughput and application startup but may possibly have introduced new issues.
Applications are encouraged to take advantage of the ConnectedApplication and ApplicationConnectionManager classes
to make their applications more error tolerant.

(Requires resetdb on server after upgrade.)

### Major Features:

* Added support for liquibase for schema generation and migration (not enabled by default)
* Metrics service now exposes service for remotely retrieving metrics (now supported in CLI)
* Reworked how applications register and maintain connection to server. If heartbeats fail they will automatically restart.
* Implemented an ApplicationConnectionManager that provides an easy way to write applications that want to know if reef goes down.

### Service/API Updates:

* Added batch gets commands to Command, Point, Endpoint and Entity services (getXbyNames)
* Better error messages on login/logout, timeout and qpid errors.
* reef:resetdb uses password from o.t.r.user.cfg file by default
* added get/findApplicationByName/Uuid functions to ApplicationService

### Bug Fixes:

* Fixed accidental synchronization in measurement processor that was effectively making it single threaded.
* MixedMeasurementStores write to the multiple stores in parallel.
* Heartbeat service ignores time sent to it by clients (clock mismatch caused apps to be marked offline)
* reef:unload uses batchsize of 1 by default (avoids timeouts while deleting points with many measurements)
* When an endpoint goes offline a measurement message is published
* Mitigated "spammed auth" issues by indexing table, moving event generation to another thread,

### Reef Internals:

* Entities Atttributes are now are correctly eventted (and subscribable)
* Added more benchmark tests and made benchmark tests externally configurable.
* Bundles all publish correct version number in OSGi
* Database connections are now explict (no more SessionFactory singleton)
* MeasurementStores are now published to OSGi using factory class MeasurementStoreProvider

### Upstream project versions

* Liquibase : 2.0.4-OSGI - Fork of main liquibase project: https://github.com/liquibase/liquibase/pull/26
* Executor4s : 0.1.10 - upgraded to get default exception logging

Version 0.4.3 - January 24, 2012
==============

### Major Features:

* Added measurement stream filtering to deadband out changes and remove duplicates
* Added reef-http-bridge feature, started javascript bindings
* Added Measurement Statistics service to report count and oldest measurement
* Entities and EntityEdges are now are correctly evented (and subscribable)

### Service/API Updates:

* Added getMeasurementHistoryByName overloads
* Deprecated PointService.getPointsOwnedByEntity(Entity) in favor of passing in ReefUUID, will be removed in 0.5.x

### Bug Fixes:

* Entity types are sorted by string now (order was undefined but usually insertion order before)
* Current Measurement value requests are returned in order they are requested (order was undefined)
* Database connection pools size is now configurable
* Measurement processor and services use separate database connection pools
* Various fixes to memory broker and in-memory-measurement-store to better simulate production implementations

### Reef Internals:

* BatchServiceRequests now have a batchedFlush command to send
* Dependencies bundle publishes all packages with correct version numbers
* Factored version numbers into parent pom file
* Implemented StandaloneNode for use in standalone tests
* Removed "recorded proto documentation" from integration tests

### Upstream project versions

* Squeryl : 0.9.5-RC1 - Back on mainline project and used manual session management

Version 0.4.2  - January 4, 2012
==============

Bugfix and stability release. (Requires resetdb on server after upgrade.)

### Service/API Updates:

* Added EntityService.getEntityRelationsForParents command to a "wide request" in a single request

### Breaking Changes:

* CommandService.bindCommandHandler returns SubscriptionBinding instead of Cancelable

### Bug Fixes:

* reef:unload and reef:load got default timeout set to 30 seconds
* Long command execution error strings caused response timeouts
* FrontEnd gives all protocol implementations channel if endpoint has channel
* ServiceBindings (from bindCommandHandler) are correctly passed to SubscriptionCreationListeners
* Fixed memory leak in DefaultSimulator (benchmark protocol)
* Fixed measproc.ignoredMeasurements metrics counter

### Reef Internals:

* Can be compiled using FSC in intellij
* MeasurementStore implementation looks up configuration from etc/org.totalgrid.reef.mstore.cfg
* Benchmark program collects many more details of the system-under-test.
* Split Qpid broker implementation out of reef-broker package

### Upstream project versions

* Executor4s : 0.1.9 - upgraded to get exceptions on deadlocks

Version 0.4.1 - December 20, 2011
==============

Update focused on stability improvements and command service updates. It is recommend that all applications
update to get important threading related bug fixes. This upgrade is considered very-low risk, the main change
that may affect applications is the order of returned results has been made explicit which might affect tests
that assume a particular result will be first/last. Before this version the result order was loosely based on
insertion order.

### Major Features:

* Command requests can carry a String value REEF-199
* Command results can include an error string REEF-200
* New RestOperations service level API to implement custom REST queries
* Results to most service queries are sorted by name (entity isn't sorted yet) REEF-203
* Added *ServiceAsync APIs that return Promise wrapped values for event driven applications.

### Shell Commands:

* reef:headers that displays and allows updating of the client request headers
* reef:unload deletes all event-configs to reset them to defaults

### Service/API Updates:

* Added CommandService.executeCommandAsSetpoint(command, String)
* Added CommandResultCallback.setCommandResult overload that takes an error string

### Breaking Changes:

* MeasurementService.publishMeasurements functions return Boolean object, instead of boolean primitive.
* CommandService.executeCommand* functions return CommandResult instead of CommandStatus. CommandResult includes
  command status (.getStatus) and error string (.getErrorMessage) for non-successful commands.
* Added missing "throws ReefServiceException" on EventService.getRecentEvents
* Database schema has changed requiring a "reef:resetdb" (Server side)

### Bug Fixes:

* Fixed await() deadlock inside subscription callback
* Removed compile-time slf4j-simple dependency.
* Remote CLI correctly handles broker disconnect events. REEF-195

### Reef Internals:

* Updated to executor4s 0.1.9-RC5

Version 0.4.0 - December 7, 2011
==============

Major refactoring of communication client and threading structure. See MIGRATION_STEPS.markdown
for help in updating an application from 0.3.x.

### Major Features:

* Reef client API is much simpler and easier to use, no reaching into any implementation packages
* Connecting to a reef broker is now by default a synchronous "single-shot" operation
* Service Specific Java interfaces are now used to auto-generate wrappers around scala implementations
* BatchServiceRequest is used by the loader-xml project to load models up to 70% faster than in 0.3.3. reef-175
* BatchServiceRequest is used to unload model, up to 90% faster than in 0.3.3
* Now setting qpid heartbeat timeout, requires new setting in config files: org.totalgrid.reef.amqp.heartbeatTimeSeconds=30. Fixes reef-183

### Shell Commands:

* Shell will attempt to auto-login using credentials in the org.totalgrid.reef.user.cfg file
* reef:unload will automatically disable and wait for endpoints to go to COMMS_DOWN before deleting. reef-173
* Better error handling when commands fail

### Service/API Updates:

* Entity service only returns BadRequestException about unknown types if no results are returned
* Added getEntityRelations queries to EntityService to make complex queries simpler to execute.
* Added SimpleAuthRequest proto and service to make logging in and out not dependent on complex reef types.
* Command requests are checked to verify right command type is used (control vs. setpoint)

### Reef Internals:

* Protocol interface includes reef Client
* Removed all usage of scala actors, replaced with Java Executors
* Added many more integration tests for dnp3 and model loading and upload, fixed many endpoint related bugs.
* Upgraded to karaf 2.2.4 from 2.2.2

Version 0.3.3  - November 9, 2011
==============

Minor bug fix and feature release.

### Bug Fixes:

* Consistent error handling and error messages for MeasurementBatchService REEF-178, REEF-179
* loader-xml doesn't require indexes for protocols not explicitly an indexed protocol

### Client Updates:

* Added alterEndpointConnectionState calls to EndpointManagerService
* Added bindCommandHandler() implementation to CommandService

Version 0.3.2  - October 10, 2011
==============

Minor bug fix release.

### Bug Fixes:

* Loader timeouts increased to handle removing points with large measurement counts
* Measurement removal happens during point removal
* reef:resetdb command asks for system password twice
* Minor fixes to entity and entity attributes services


Version 0.3.1  - September 3, 2011
==============

### Major Features:

* DNP3 Slave Protocol Implemented (updated dnp3 library to 1.0.0)
* Support for AMQP over SSL
* Separate CLI distribution for inspecting remote systems

### Client Updates:

* RequestSpy logging for client API calls
* Client supports SSL connection broker using standard java trust-store files
* Maximum number of returned objects is client configurable (was default of 100)

### Service/API Updates:

* Added more requests for entities and their children
* Points and Commands can searched for by Endpoint
* Entity service only fails requests with unknown types if nothing is found
* Endpoints can be modeled as either data “sink” or “source”
* Reef objects can be created with externally defined UUIDs
* Added getCommandHistory for a single command
* Fixed response codes for Entity and EntityAttribute services
* When an Entity is deleted, all edges and events pointing to it are removed 

### Shell Commands:

* New configfile:download and configfile:upload commands to make adjusting configFiles easier
* New entity:tree command to view relationships between objects
* New command:hist to look at global and individual command history
* New event:view and event:publish commands
* Commands for managing event-configurations: event-config:list,view,delete,create
* Added meas:override,block,unblock for managing overriding measurements

### Xml Loader:

* Enabled Xml validation, loaded files now must be well formed before attempt processing
* Added support for attaching ConfigFiles and Attributes to any point/command/equipment
* Progress output wraps every 50 characters
* Config Files are loaded in a binary safe fashion
* Numerous minor bug fixes and enhanced error reporting

### Breaking Changes:

* AMQPConnectionInfo has more parameters
* Measurements subscriptions are no longer “raw” events and are not binary compatible with 0.3.0 client
* Removed “core” user and changed system user password to be user defined

### Reef Internals:

* Added Mockito, JMock test dependencies
* Created proto-shell entry-point to run outside of OSGi
* Reef etc files split up by functional area (amqp, user, node, sql)
* Refactored services to move all state into a RequestContext object and make services stateless.
* Broke up ‘core’ project into ‘application-framework’, ‘fep’, ‘measproc’ and ‘services’
* Scala library moved to 2.9.0-1

### Bug Fixes:

* Measproc/FEP correctly ignore connections received during shutdown
* Event services gracefully handles missing attributes when rendering events

Version 0.3.0 - April 29, 2011
==============

Primarily Service and API refinements and refactorings.

### API Updates:

* Java facing APIs are 100% java
* XxxxService interfaces are now implemented using a SessionExecutionPool wrapper that
  uses a pool of sessions and handles the connection going up and down.
* Subscriptions don't automatically start flowing messages, a start() function was added
* Replaced getOne, getMany, getAsyncOne with unified functions that return Promises that
  have the one() or many() expectation functions.
* Javadoc and sources jars are now published into maven repo

### Service Updates:

* All protos for "long lived" and "static" resources now have ReefUUID field
* "long lived" and "static" resources use UUID instead of integers
* Distribtion renamed to totalgrid-reef-0.3.0.
* Loader now attaches Analog, Status, Counter types to Points on Load
* Authorization is now CRUD rather than verb based (can distinguish between a create and update)

### Shell Commands:

* Added configfile:list command to view config files

### Breaking Changes:

* APIs renamed and moved packages, "I" prefix removed, all java apis moved to 
  org.totalgrid.reef.japi.*.
* Most protos are no longer binary comptabile with 0.2.x versions.

### Reef Internals:

* Updated karaf to version 2.2.1
* Updated squeryl to 0.9.4-RC7-uuid for UUID support
* Updated to qpid 0.10 java-client-api
* Logback used in local tests
* Logging output now has correct line numbers from scala code
* Updated pax-logging to 1.6.3-LOCATION with line number patches until 1.6.3 is released
* DNP3 logging has better error messages


### Bug Fixes:

Version 0.2.3  - March 2, 2011
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
* Added "point:commands" command to display points with their feedback commands.
 
### Breaking Changes:

* FrontEndPort Protobuf names changed to CommChannel
* Post verb is now off by default, only specific services use it now
* Commands now have correct "owns" relationship to parent equipment, only "feedback" to points

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
* Fixed "Content not allowed in prolog" XML loading issue REEF-61
* Multiple Controls with same index but different dnp3Options can be loaded by loader REEF-65
* Fixed measurement history trimming bug that caused slow down over time REEF-67

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

#### Formatting

This file uses github flavored markdown, a good test renderer is available at github:
http://github.github.com/github-flavored-markdown/preview.html