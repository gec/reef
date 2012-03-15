DNP3 Sample Model
============

This sample model aims to show how to configure dnp3 inputs and endpoints using reef. It also provides a working
system that shows how reef behaves with "real" dnp3 connections (even if an external dnp3 device is unavailable).

## Model

The demo model is a very simple equipment model. We have 2 very simplified Substations:

 * **RoundtripSubstation** is the "real data" in this system, all of the measurements it receives are pulled in
   over the dnp3 link. Normally the dnp3 link would be getting real data from the field but for this example we
   need some fake data.
 * **OriginalSubstation** is our fake "field device" that generates measurements that we are going to pretend come
   from the field. We are going to publish this data using a dnp3 slave.

```
karaf@root> entity:tree -name Substations owns 10 EquipmentGroup Equipment Point Command
  +- Substations (Root, Substation)
    +- RoundtripSubstation (EquipmentGroup, Substation)
      +- RoundtripSubstation.Line01 (Equipment, Line)
        |- RoundtripSubstation.Line01.ScaledCurrent (Analog, Point)
        |- RoundtripSubstation.Line01.Current (Analog, Point)
        |- RoundtripSubstation.Line01.ScaledSetpoint (Command, Setpoint)
        |- RoundtripSubstation.Line01.VoltageSetpoint (Command, Setpoint)
      +- RoundtripSubstation.Breaker01 (Breaker, Equipment)
        |- RoundtripSubstation.Breaker01.Tripped (Point, Status)
        |- RoundtripSubstation.Breaker01.Bkr (Point, Status, bkrStatus)
        |- RoundtripSubstation.Breaker01.Close (Command, Control, bkrClose)
        |- RoundtripSubstation.Breaker01.Trip (Command, Control, bkrTrip)
    +- OriginalSubstation (EquipmentGroup, Substation)
      +- OriginalSubstation.Line01 (Equipment, Line)
        |- OriginalSubstation.Line01.ScaledCurrent (Analog, Point)
        |- OriginalSubstation.Line01.Current (Analog, Point)
        |- OriginalSubstation.Line01.ScaledSetpoint (Command, Setpoint)
        |- OriginalSubstation.Line01.VoltageSetpoint (Command, Setpoint)
      +- OriginalSubstation.Breaker01 (Breaker, Equipment)
        |- OriginalSubstation.Breaker01.Tripped (Point, Status)
        |- OriginalSubstation.Breaker01.Bkr (Point, Status, bkrStatus)
        |- OriginalSubstation.Breaker01.Close (Command, Control, bkrClose)
        |- OriginalSubstation.Breaker01.Trip (Command, Control, bkrTrip)
```

## Endpoints

```
karaf@root> endpoint:list
Found: 3
Endpoint             Protocol       State        Enabled         Port                    Port State
----------------------------------------------------------------------------------------------------
DNPInput          |  dnp3        |  COMMS_UP  |  true     |  tcp://127.0.0.1:9999@any  |  OPEN
DNPOutput         |  dnp3-slave  |  COMMS_UP  |  true     |  tcp://0.0.0.0:9999@any    |  OPEN
OriginalEndpoint  |  benchmark   |  COMMS_UP  |  true     |  unknown                   |  unknown
```

We have 3 endpoints in this model, a standard dnp3 master endpoint to read data from the outside world (DNPInput), a
dnp3-slave that is publishing data to the world (DNPOutput) and a simulator to produce random measurements so we have
something interesting to see (OriginalEndpoint).

The port for DNPInput is "tcp://0.0.0.0:9999@any" which means we are listening on port 9999 on all attached network
adapters. If we want to listen on only a particular network that can be done by putting the network address as host in
the model file. The DNPOutput port is "tcp://127.0.0.1:9999@any" which means we are trying to connect to the local port
9999 and therefore the input is hooked up to the output.

This endpoint:list was collected while the system was online so you will see that that both ports are OPEN. If we were
not connected we would see that the ports would have different states. A very common case is when a dnp3-slave is
configured but no master is attached, in that case our slave will report that it is "OPENING" the port until a connection
is made. This is a very useful when debugging networking issues (along with netstat -atpn).

```
karaf@root> entity:tree CommunicationEndpoint source 10 EquipmentGroup Equipment Point Command
  +- DNPInput (CommunicationEndpoint, LogicalNode)
    |- RoundtripSubstation.Line01.ScaledCurrent (Analog, Point)
    |- RoundtripSubstation.Line01.Current (Analog, Point)
    |- RoundtripSubstation.Line01.ScaledSetpoint (Command, Setpoint)
    |- RoundtripSubstation.Line01.VoltageSetpoint (Command, Setpoint)
    |- RoundtripSubstation.Breaker01.Tripped (Point, Status)
    |- RoundtripSubstation.Breaker01.Bkr (Point, Status, bkrStatus)
    |- RoundtripSubstation.Breaker01.Close (Command, Control, bkrClose)
    |- RoundtripSubstation.Breaker01.Trip (Command, Control, bkrTrip)
  +- DNPOutput (CommunicationEndpoint, LogicalNode)
  +- OriginalEndpoint (CommunicationEndpoint, LogicalNode)
    |- OriginalSubstation.Line01.ScaledCurrent (Analog, Point)
    |- OriginalSubstation.Line01.Current (Analog, Point)
    |- OriginalSubstation.Line01.ScaledSetpoint (Command, Setpoint)
    |- OriginalSubstation.Line01.VoltageSetpoint (Command, Setpoint)
    |- OriginalSubstation.Breaker01.Tripped (Point, Status)
    |- OriginalSubstation.Breaker01.Bkr (Point, Status, bkrStatus)
    |- OriginalSubstation.Breaker01.Close (Command, Control, bkrClose)
    |- OriginalSubstation.Breaker01.Trip (Command, Control, bkrTrip)
```

This tree shows which endpoints are responsible for creating measurements and handling command requests. This
responsibility relationship is represented in the reef model using the name "source".

Notice that the DNPOutput endpoint doesn't have any children. This means its not the source for any data (which makes
sense because its job is publish data). We use the "sink" relationship to represent an endpoint that is consuming
measurements.

```
karaf@root> entity:tree CommunicationEndpoint sink 10 EquipmentGroup Equipment Point Command
  +- DNPInput (CommunicationEndpoint, LogicalNode)
  +- DNPOutput (CommunicationEndpoint, LogicalNode)
    |- OriginalSubstation.Breaker01.Close (Command, Control, bkrClose)
    |- OriginalSubstation.Line01.ScaledSetpoint (Command, Setpoint)
    |- OriginalSubstation.Breaker01.Trip (Command, Control, bkrTrip)
    |- OriginalSubstation.Line01.VoltageSetpoint (Command, Setpoint)
    |- OriginalSubstation.Line01.Current (Analog, Point)
    |- OriginalSubstation.Breaker01.Bkr (Point, Status, bkrStatus)
    |- OriginalSubstation.Line01.ScaledCurrent (Analog, Point)
    |- OriginalSubstation.Breaker01.Tripped (Point, Status)
  +- OriginalEndpoint (CommunicationEndpoint, LogicalNode)
```

Here we see that the DNPOutput endpoint has relationships to all of the points of OriginalSubstation. Notice that
OriginalEndpoint and DNPInput don't have any "sink" related Points or Commands.

## Measurements

Its helpful to understand the data flow that a single measurement goes through to roundtrip a value:

```
(OriginalSubstation generate measurement) --> (publish measurement to broker) --> (measurement subscription) -->
    DNPOutput --> (c++ dnp3 master) --> (ip networking) -->
    (c++ dnp3 slave) --> DNPInput --> (publish measurement to broker)
```

In the below snapshot of measurements in the system (taken while the system was online) we see that the values from
OriginalSubstation have been correctly round-tripped out of the system and back in using dnp3.

```
karaf@root> meas:list
Found: 8
Name                                         Value        Type       Unit       Q     Time                         Off
-----------------------------------------------------------------------------------------------------------------------
OriginalSubstation.Breaker01.Bkr          |  CLOSED    |  String  |  status  |  A  |  Mar 14 19:37:01 EDT 2012   |  1
OriginalSubstation.Breaker01.Tripped      |  TRIPPED   |  String  |  raw     |  A  |  Mar 14 19:37:01 EDT 2012   |  1
OriginalSubstation.Line01.Current         |  1421.094  |  Analog  |  A       |     |  Mar 14 19:37:10 EDT 2012   |  1
OriginalSubstation.Line01.ScaledCurrent   |  1.491     |  Analog  |  kA      |     |  Mar 14 19:37:10 EDT 2012   |  1
RoundtripSubstation.Breaker01.Bkr         |  CLOSED    |  String  |  status  |  A  |  Mar 14 19:37:01 EDT 2012   |  5
RoundtripSubstation.Breaker01.Tripped     |  TRIPPED   |  String  |  raw     |  A  |  Mar 14 19:37:01 EDT 2012   |  5
RoundtripSubstation.Line01.Current        |  1421.094  |  Analog  |  A       |     |  ~Mar 14 19:37:10 EDT 2012  |  160
RoundtripSubstation.Line01.ScaledCurrent  |  1490.665  |  Analog  |  kA      |     |  ~Mar 14 19:37:10 EDT 2012  |  160
```

There are a few interesting things to note with these results:

 - Notice that the statuses (*.Bkr, *.Tripped and *.Current) have had their values correctly push through.
 - *.ScaledCurrent has been multiplied by 1000 before publishing by the dnp3 slave (to support legacy systems
   that use 'fixed point' scaling to workaround the historic lack of double/floating point types in dnp3). This
   configured using the outputScaling attribute in the xml.
 - The roundtripped Current and ScaledCurrent have a "~" before their time, this indicates that this is this time stamp
   has been recorded in the field (rather than assigned as the measurement is first received by reef). This also
   explains the larger "Off" value which is just the difference of the "field time" when the measurement occurred and the
   "system time" when the measurement was received by reef. In this case the entire roundtrip took 160 milliseconds.
 - The roundtripped Bkr and Tripped values do not a set "device time" because of limitations in the dnp3 protocol and
   timestamping binary events.

## Commands

```
karaf@root> command:list
Found: 8
Name                                           DisplayName         Type                Endpoint
-------------------------------------------------------------------------------------------------------
OriginalSubstation.Breaker01.Close          |  Close            |  CONTROL          |  OriginalEndpoint
OriginalSubstation.Breaker01.Trip           |  Trip             |  CONTROL          |  OriginalEndpoint
OriginalSubstation.Line01.ScaledSetpoint    |  ScaledSetpoint   |  SETPOINT_DOUBLE  |  OriginalEndpoint
OriginalSubstation.Line01.VoltageSetpoint   |  VoltageSetpoint  |  SETPOINT_DOUBLE  |  OriginalEndpoint
RoundtripSubstation.Breaker01.Close         |  Close            |  CONTROL          |  DNPInput
RoundtripSubstation.Breaker01.Trip          |  Trip             |  CONTROL          |  DNPInput
RoundtripSubstation.Line01.ScaledSetpoint   |  ScaledSetpoint   |  SETPOINT_INT     |  DNPInput
RoundtripSubstation.Line01.VoltageSetpoint  |  VoltageSetpoint  |  SETPOINT_DOUBLE  |  DNPInput
```

The commands are setup similarly to the measurements, they are nearly identical configurations. The one important
difference is that the RoundtripSubstation.Line01.ScaledSetpoint is configured to expect only an integer setpoint value
to show how legacy systems can be support (see below).

The command data flow is basically the exact opposite of the measurement flow but a response code is sent all the way
back through the entire flow.

```
(Operator issues command request) <--> (command service validates request) <--> ('addressable' command subscription) <-->
    DNPInput <--> (c++ dnp3 slave) <--> (ip networking) <-->
    (c++ dnp3 master) <--> DNPOutput <--> (command service validates request) <--> Simulator
```

### Command examples

```
karaf@root> command:issue RoundtripSubstation.Breaker01.Close
Status: SUCCESS

karaf@root> command:hist
Found: 2
Id     Command                                 Status      Message     User       Type     Value
------------------------------------------------------------------------------------------------
2   |  OriginalSubstation.Breaker01.Close   |  SUCCESS  |           |  system  |  NONE  |
1   |  RoundtripSubstation.Breaker01.Close  |  SUCCESS  |           |  system  |  NONE  |
```

Notice that when we issue a request for the roundtrip control we get a SUCCESS response indicating that the command
request was correctly handled by the simulator. If we check the history we see two requests, our original request to
the roundtripped point and a second request to the original point which was generated by the dnp3-slave endpoint.

```
karaf@root> command:issue  RoundtripSubstation.Line01.ScaledSetpoint 12345
Status: SUCCESS

karaf@root> command:hist
Id     Command                                        Status      Message     User       Type       Value
----------------------------------------------------------------------------------------------------------
6   |  OriginalSubstation.Line01.ScaledSetpoint    |  SUCCESS  |           |  system  |  DOUBLE  |  12.345
5   |  RoundtripSubstation.Line01.ScaledSetpoint   |  SUCCESS  |           |  system  |  INT     |  12345
```

If we are supporting a legacy system that can't support floating point we can use the inputScaling attribute on the xml
to multiply the external value (12345 * 0.001) to get the "real" setpoint value.

## Working with legacy systems

In recent years the dnp3 specification has been updated to include support for floating point/double values for analogs
and commands. Before this was available the standard practice was to divide the values by the desired precision before
publishing. Ex: if field value was 1.988 watts, the integer value 1988 would be publishied and the scaling would be 1000
(sometimes notated as #.####). This conversion would have to be done before publishing and before consumption by both
sides which leads to many mis-configurations and errors. Unfortunately there is a large base of older dnp3 masters/slaves
deployed in the field so we need to be able to support those sorts of devices.

You have already seen the scaling we can do before publishing measurements or commands but there is another setting that
may need to be changed for legacy masters. The dnp3 protocol allows the master to specify exactly what format it would
like its requested data returned. However most masters allow the slave side to use its default formatting types and if
it can't handle the returned data the master will usually panic and disconnect from the slave.

By default we have configured the reef dnp3 slave to use floating point analogs to avoid all of the scaling headaches but
if you encounter a master that can't handle Object30Var6 or Object32Var6 you can override those defaults by adding the
following snippet to the slave configuration file.

```xml
<Slave>
    <Stack .../>
    <SlaveConfig ...>
        <StaticRsp>
            <AnalogGrpVar Grp="30" Var="2" />
        </StaticRsp>
        <EventRsp>
            <AnalogGrpVar Grp="32" Var="2" />
        </EventRsp>
    </SlaveConfig>
</Slave>
```