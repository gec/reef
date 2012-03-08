
Calculation Engine
==================

## Goals

Reef provides a simple calculator that uses field measurements and creates measurements to provide more meaningful
data to operators and applications. Calculations can be used to "fill in" missing measurements that are not
telemetered in the field or generate entirely new points that provide business value.

#### Common Calculations

* Derived values - We may be reading one or more physical measurements but what we are actually interested in is
  a derived value based on those inputs. For example we may be reading current and voltage on a power line but the
  measurement we really care about is power (current * voltage). Another common example is when there is input power
  and output power to a device and we want to measure the efficiency of that device (output/input). It is also
  common to be measuring an instantaneous value such as power but we are actually interested in the time integrated
  value like energy.
* Implied State - There are often devices in the field that are not telemetered but we can determine what the current
  state of the device is using other data. For example we can determine if a manual cut-out switch around a circuit
  breaker is open if the circuit breaker is measuring 0 input voltage.
* Summary Points - There are often cases where we would like to know how a number of similar systems are performing or
  track overall numbers. We can calculate summary points such as "total power across system" or "average voltage on
  lines" that will often have important business implications. These sorts of summaries could be performed "offline"
  but seeing a point like "total power made/saved today" increasing throughout the day provides valuable visibility
  into the system as a whole.

> NOTE: the calculation system should *not* be used when serious precision is needed. In particular the time based
> functions like INTEGRATE should not be used to calculate "billable" quantities. Dedicated hardware should be used
> for all billable metering points.

## Configuring a new Calculation

Each calculation is composed of 4 main parts:

* OutputPoint : The name of the point we will publish calculated measurements to. The points' unit should be checked
  manually against the calculation to make sure it is the right unit. Ex: (volts * amps => watts)
* Inputs : A listing of the fully qualified input point names and the shorter variable names we will use in the
  calculations. We also can configure what how much data for the input point we want:
   * When operating across points we will normally want the single most recent value for each input point
   * For certain operations like "rolling averages" or "recent max values" we may want a the last few minutes or hours
     of data for a single point.
* Triggering conditions : It is often the case that we will want to recalculate and publish our value every time one
  our inputs changes and that is the default. That may be very noisy or misleading in some situations so we can also
  choose to recalculate on a schedule (irregardless of whether a new input measurement was received). We can also choose
  to only recalculate when a specific input changes.
* Formula : Most importantly we need to define the actual calculation we want to do on the data. This formula needs to
  be defined in terms of the variable (not point names) used in the inputs. This is necessary so we do not have to limit
  what you may name your points to keep them "calculable". Variables must be simple text and may not contain spaces or
  special characters (other than '_')


## Calculation Process

There are a number of strategies and tweakable parameters when configuring a calculation and it is helpful to understand
how a calculation is actually done when choosing the correct strategy.

Calculation Process:

* Calculation is triggered either by a new measurement being received or the timer going off. <triggering>
* A snapshot of the current measurements is taken. <inputs>
* The snapshot is evaluated to determine if we have enough data to make the calculation. This determination is made
  based on how the inputs are configured <single> or <multi>
* We also analyze the input measurement data based on its quality and may strip out bad values or fail the calculation
  if there are bad inputs. <inputQualityStrategy>
* We then evaluate the formula with the, possibly adjusted, input measurement snapshot.  <formula>
* Assuming the calculation was successful we calculate the output quality based on the inputs <outputQuality>
* We also calculate the time to attach to the new measurement (usually the most recent input time) <outputTime>
* Assuming no errors, we publish the calculated measurement.

If there is an error during configuration or calculation we will publish a measurement with the string value "#ERROR",
a bad quality and unit set to "#ERROR". If this occurs check the log for a more detailed error message.

```
Name                         Value     Type       Unit       Q     Time                             Off
--------------------------------------------------------------------------------------------------------
Microgrid2.Output.Energy  |  #ERROR |  String  |  #ERROR  |  A  |  Thu Mar 08 11:13:08 EST 2012  |  8
```

### Formulas

Formulas are generally specified using the format "OPERATION(VARIABLE)". The goal is to use as much of the same names
and conventions as microsoft excel so a formula can be developed and tested using excel and then copy-pasted into reef.

Each operation in reef takes one or more input values and produces a single output value. This is important to
understand, operations do not return a set of values, they return a single scalar value.

### Operation List

Basic Operations that have ("operators") letting us write simple expressions.

* SUM (+)
* SUBTRACT (-)
* PRODUCT (*)
* DIVIDE (/)
* POWER (^)

The formula may be written using the operators and it is effectively translated to the 'long hand' functional version:

> A * B => PRODUCT(A,B)  ::  A + B + C => SUM(A,B,C)  ::  A - B - C => SUBTRACT(A,B,C)

Extended mathematical operations.

* SQRT     - SquareRoot
* AVERAGE

Boolean operations

* AND
* OR
* COUNT - is equal to the number of input booleans that are true

Time based operations.

* INTEGRATE - integrates the value over the input range returning units/millisecond

## CLI commands

The calculation protocol comes with some useful CLI commands to verify that the calculations are configured correctly
and performing as desired. See the Remote CLI guide or run the commands with --help for detailed usage on these commands.

```
karaf@root> calculation:list

Found: 4
OutputPoint                       Formula                                     Unit
------------------------------------------------------------------------------------
Microgrid1.ChargeLoad          |  A - B                                    |  W
Microgrid1.Input.Energy        |  INTEGRATE(PINT) * 0.001 * (1/3600000.0)  |  kWH
Microgrid1.Input.Power         |  A * B                                    |  W
Microgrid1.LoadRatio           |  A / B                                    |  %
```

The calculation:list command will show all of the configured commands in the system along with their formula and
output unit. This is most useful for getting a quick look at which operations and functions are being used.

```
karaf@root> calculation:view Microgrid1.Input.Power

OutputPoint | Microgrid1.Input.Power   | W
Endpoint    | Microgrid1Calculator     |
Triggering  | AnyUpdate                |
Var         | Point                    | Type
A           | Microgrid1.Input.Current | SINGLE(MOST_RECENT)
B           | Microgrid1.Input.Voltage | SINGLE(MOST_RECENT)
Formula     | A * B                    |

Found: 3
Dist     Name                         Value       Type       Unit     Q     Time                             Off
----------------------------------------------------------------------------------------------------------------
-     |  Microgrid1.Input.Current  |  2.752    |  Analog  |  A     |     |  Wed Mar 07 11:11:50 EST 2012  |  0
-     |  Microgrid1.Input.Voltage  |  121.897  |  Analog  |  V     |     |  Wed Mar 07 11:11:49 EST 2012  |  1
!     |  Microgrid1.Input.Power    |  335.478  |  Analog  |  W     |     |  Wed Mar 07 11:11:50 EST 2012  |  7
```

The calculation:view command is useful for checking that a calculation is working as desired (and producing the correct
values.) It shows all of the details of the calculation configuration such as when the calculation is triggered, what
input measurements it uses and their configurations.

Most usefully it shows the current value of each of the inputs and the calculation output. If a calculation depends on
other calculations we will show those intermediate calculations and their inputs as well. We will also show the values
of any calculations that depend on this calc. (both behaviors are controllable with command flags).

Importantly the "-w" option (watch) will subscribe to all of the measurements and continuously display any new updates
until control-c is pressed.


## XML Configuration

Calculations are configured like other points in the modeling file.

```xml
<equip:analog name="Power" unit="W">
    <calc:calculation>
        <calc:inputs>
            <calc:single pointName="Current" variable="A"/>
            <calc:single pointName="Voltage" variable="B"/>
        </calc:inputs>
        <calc:formula>A * B</calc:formula>
    </calc:calculation>
</equip:analog>
```

Each calculation section must be contained by a point which is the output point where we will publish the result of
the calculation. At minimum a calculation must declare one or more inputs and a formula, default configurations for all
"strategies" will be applied.

Calculations can be configured anywhere a normal point could be used, including in point or equipment profiles. The
important point to note is that the names of variables use the same "prefixing" rules as the point itself. If in the
above example the Power point was defined in a profile included by a circuit called "CircuitA" the output point would
be "CircuitA.Power" and it would depend on "CircuitA.Current" and "CircuitB.Current". If you do not want the inputs to
use "relative" naming you can add the attribute addParentNames="false".

Calculated points will also need to be attached to an endpoint with the protocol "calculator".

> In most cases only one calculator endpoint is needed in a system; the example configuration includes a number of
> endpoints to make testing easier.

For more detailed documentation on the XML configuration it is best to look at the [sample configuration][SAMPLE] and the
[xsd documentation][XSD].

[SAMPLE]: ../assemblies/assembly-common/filtered-resources/samples/calculations/config.xml
[XSD]:    ../loader-xml/src/main/resources/calculations.xsd
