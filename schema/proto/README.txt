
====================================================
Reef Protobuf Style/Naming Guide 


====================================================

First, refer to the Google Protobuf style guide:
http://code.google.com/apis/protocolbuffers/docs/style.html

1) Package name:

  package reef.api.proto.PackageName;

The package name serves at the protobuf namespace as well as the Java outer class all proto messages are contained in. The package name should be the same as the filename. Package names must NOT be the same as any object type contained in the file. Names should be capitalized and camel-cased and follow one of two rules:

  a) The general concept/subsystem the package is responsible for (e.g. Model, ApplicationManagement, FEP).
  b) The most important contained object type pluralized (e.g. Alarms, Measurements). Related object types may still be included in this package.


2) Java package: 

  option java_package = "reef.api.proto";

This is necessary to produce correct Java source, and should always be included exactly like this.


3) Object type:

  message ProtoObject {

Object names should be capitalized/camel-cased and describe their function in a typical object-oriented fashion (e.g. Event, ConfigFile). Avoid ad-hoc namespacing (e.g. ScadaEvent, ScadaAlarm, ScadaTag) and overspecification (e.g. ScadaEvent vs. ServiceEvent). Instead, rely on the package system to specify context or resolve term overloading.


4) Field names should be all lower-case, with words separated by underscores, as specified by the protobuf style guide.


5) Uid field:

  optional string uid = 1;
  
All proto objects which refer to persistent resources should specify the per-resource-type unique identifier in string form and named "uid". The format of the uid field is opaque; it is just a variable used to refer back to a unique object.
  

6) Repeated fields:
 
  repeated string types = 2;

Repeated fields should be pluralized.


7) Optional vs. required: Because Reef uses the same proto objects in many different contexts, using the protobuf "required" field for runtime validity checks is impractical. The required field also potentially hampers backwards compatibility in the event of deprecation. Routine use of required is not recommended, though some cases may argue strongly enough to justify it.
  

8) Boolean fields which represent "state" should not use the prefix "is" merely to identify the field as boolean (e.g. "open" not "is_open"). However, fields which represent "essence" may use it (e.g. "is_switch").


9) Enums: 

  enum BreakerState {
    TRIPPED   = 1;
    CLOSED    = 2;
  }
  
As per the protobuf style guide, the enum name is capitalized/camel-cased and the values are all-caps and separated by underscores. Enum value names should not try to include information about their role (e.g. TRIPPED_BREAKER_STATE).
