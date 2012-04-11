
Reef AMQP to HTTP Bridge
=========================

This bridge provides access to the the standard reef operations and client functions through an HTTP interface. This
mapping is relativity straightforward because the underlying reef requests are modeled on HTTP operations. This bridge
uses the "protobuf-java-format" project to convert to and from different representations of the canonical protobuf
representations.

Data Formats
-------------

All service requests accept and return data in formats declared by the HTTP Headers "Content-Type" and "Accept".
Uploaded data needs to be formatted to match the "Content-Type" header. Returned data is formatted to match the "Accept"
header. If an Accept header is not sent we use the format declared in Content-Type. If neither Content-Type or Accept
are included we default to JSON.

Available formats:

- application/json => Represents the protobuf as json. Field names are named using the proto definitions underscore names:
  (getInstanceName => "instance_name")
- application/protobuf => Uses the built in protobuf serialization/deserialization to byte streams.

Authentication
---------------

The rest and api services that send data to the live reef server, like all requests in reef, require an authentication
token. This token is normally provided by providing the auth token in the HTTP Headers or in the query string. The header
or request name is REEF_AUTH_TOKEN and the value is the string auth token obtained from login or loaded in during
configuration.

The bridge can also be configured to obtain an auth token using a preconfigured user that will be used by all requests
that didn't contain an auth token. It is vitally important that this user be strictly limited in what they can do on
the system. In almost every situation that user should be read-only.

That user is configured in the org.totalgrid.reef.httpbridge.cfg file. If those fields are commented out or
left blank then no default auth token will be provided.

Requests
===========

Convert
------------
Since one of the main jobs of this bridge is to accept protos in one representation and convert them to another it
was easy to create a service that does only that. The data can be converted between formats which should be useful for
debugging purposes and possibly other uses in the future. The convert service also provides a way to see exactly what
protobuf types we know how to parse and will provide the descriptor of those objects.

### GET /convert ###
Returns a list of all protos we support on the /rest interface. Their names are the "short name" we use for
the exchange binding.

```
GET /convert =>
    {"types":["user_command_request",...,"measurement_batch"]}
```

### GET /convert/"short name" ###
Returns the protobuf descriptor for the particular proto.

```
GET /convert/application_config =>
    {"name":"StatusSnapshot","field":[{"name":"process_id","number":1,"label":"LABEL_OPTIONAL","type":"TYPE_STRING"},
    {"name":"instance_name","number":2,"label":"LABEL_OPTIONAL","type":"TYPE_STRING"},...]}
```

### POST /convert/"short name" ###
Takes a proto representation posted with format "Content-Type" and returns that proto represented as the format in the
"Accept" header.

```
POST /convert/entity
{
  Content-Type: application/protobuf,
  Accept: application/json,
  data: {(byte array)[0xff,0x23,0x45,...]}
} =>
    {"name":"EntityName", "types":["Entity", "Point"]}

POST /convert/entity
{
  Content-Type: application/json,
  Accept: application/protobuf,
  data: {"name":"EntityName", "types":["Entity", "Point"]}
} =>
    (byte array)[0xff,0x23,0x45,...]
```

Login
-----------------

If we do not have a default user configured, or need more than write only permissions, we need a way to acquire a new
auth token. We use a separate namespace and servlet for that to avoid the confusion that comes from mixing login at the
same level as the REST or API functions.

Note that is important to only use the login and auth tokens with an HTTPS bridge, other wise the auth token and
passwords will be sent in clear text and it becomes trivial to hijack the credentials.

### GET /login ###

Logging in requires that the user passes the user_name and password for a valid
agent on the reef server. If the user/password is valid the server will return the auth token both inside the response
data and as a REEF_AUTH_TOKEN response header. Requests can be done with either GET or POST.

```
GET /login?name=UserName&password=Password =>
    auth-token-string

POST /login
{
  Content-Type: application/url-encoded
  Accept: application/json
  data : {name=UserName&password=Password}
} =>
    {"token":"auth-token-string"}

POST /login
{
  Content-Type: application/json
  Accept: application/json
  data : {"name":"UserName","password"="Password"}
} =>
    {"token":"auth-token-string"}
```

Rest
------------------

We provide low-level access to the reef client that allows the user to perform any action possible with the java
client by using the low-level ClientOperations. The user must POST a protobuf representation of a supported object (list
is available using GET /convert) and include a verb header: REEF_VERB. Allowable values are GET, POST, PUT, DELETE.

Like the reef client, all requests take a singular proto as the request and return a list of results contained in a
json array with name "results".

This interface is provided primarily as a "release valve" if we have an urgent need to implement a function that is not
exposed via the api service and is not expected to be used by most clients.

```
POST /rest/entity
{
  Content-Type: application/json
  Accept: application/json
  REEF_VERB: GET
  data: {"name":"EntityName"}
} =>
    {"results":[{"name":"EntityName","uuid":"162378-1231231","types":["Type1"]...]}
```


Api
-------------------

This is the most useful part of the bridge, it provides a semantic layer on the objects to provide much of the same
functionality as the java service-client apis. We currently support most of the service-client calls where the
parameters are all simple to represent as strings (and therefore useable in a query string).

Api calls that return exactly one result (getXByName, getXByUuid) will return just that object. Optional returns or lists
will return a list of results with the name "results".

Api calls can be made with GET and a query string, or POSTED with application/url-encoded data. Results are returned in
the format specified in the Accept header. Parameter names exactly match the parameter names in the javadoc.

Url Encodable Parameter Types:

- String
- Int
- Long
- Boolean
- ReefUUID (just the value portion, not the whole proto)
- ReefId (just the value portion, not the whole proto)
- EntityRelation (as a string)

```
GET /api/getEntityByName?name=EntityName =>
    {"name":"EntityName","uuid":"162378-1231231","types":["Type1"]}

GET /api/getEntityByUuid?uuid=162378-1231231 =>
    {"name":"EntityName","uuid":"162378-1231231","types":["Type1"]}

GET /api/getMeasurementByName?name=Point1 =>
    {"name":"EntityName","uuid":"162378-1231231","types":["Type1"]}

POST /api/getMeasurementsByNames
{
  Content-Type: application/url-encoded
  Accept: application/json
  data: pointNames=SimulatedSubstation.Breaker01.Bkr&pointNames=SimulatedSubstation.Line01.Current
} =>
    {"results":[
        {"name":"SimulatedSubstation.Breaker01.Bkr","type":"STRING","bool_val":false,"string_val":"TRIPPED","quality":{},"unit":"status","time":1327344297745},
        {"name":"SimulatedSubstation.Line01.Current","type":"DOUBLE","double_val":15.163,"quality":{},"unit":"A","time":1327344309352}
    ]}

```

Subscribe
-----------------

When a subscription call is made using the api service we will start a listener for that data and provide that data to
the client using the best available "transport" mechanism. In the near future we hope to implement support for both
websocket and "comet" based approaches to pushing data to the client. We also would like to merge all subscriptions for
a single client into one stream in the future.

Currently we only support a "short polling" based transport. We pass the client back a "subscription token" using the
"Pragma" response header. (We would like to be using a custom header but we have to tunnel our response through one of
the CORS "safe headers" because the Access-Control-Expose-Headers support is unreliable in most browsers).

The client is responsible for polling the subscription services with a GET and that "subscription-token" as the address.
Up-to 100 of the events received will be returned to the client for each request.

```
POST /api/subscribeToMeasurementsByNames
{
  Content-Type: application/url-encoded
  Accept: application/json
  data: pointNames=SimulatedSubstation.Breaker01.Bkr&pointNames=SimulatedSubstation.Line01.Current
} => {
  Pragma: sub-id-123456-1241234324-23432432,
  data: {"results":[
    {"name":"SimulatedSubstation.Breaker01.Bkr","type":"STRING","bool_val":false,"string_val":"TRIPPED","quality":{},"unit":"status","time":1327344297745},
    {"name":"SimulatedSubstation.Line01.Current","type":"DOUBLE","double_val":15.163,"quality":{},"unit":"A","time":1327344309352}
  ]}
}

GET /subscribe/sub-id-123456-1241234324-23432432
{
  Accept: application/json
} => {
  "results":[
    {"name":"SimulatedSubstation.Line01.Current","type":"DOUBLE","double_val":15.163,"quality":{},"unit":"A","time":1327344500000}
  ]
}

GET /subscribe/sub-id-123456-1241234324-23432432
{
  Accept: application/json
} => {
  "results":[]
}
```

When a client is done with its subscription it should cancel the subscription by making the same request but changing the
verb from GET to DELETE. This will cancel the subscription and free up the server side resources.

Installation and Configuration
===============================

This application is deployed as an OSGI module and uses the "whiteboard" Http service described here:
http://felix.apache.org/site/apache-felix-http-service.html#ApacheFelixHTTPService-UsingtheWhiteboard

This means that the configuration of the "web server" (in our case Jetty) is done separately from our bundle and only
needs to be done once for all applications that want to expose an HTTP or HTTPS service. The configuration of the web
server is done using the org.ops4j.pax.web.cfg config file and is well documented here:
http://team.ops4j.org/wiki/display/paxweb/Basic+Configuration

A template configuration file is included with the distribution that includes all of the fields necessary to fill in to
configure an HTTPS only bridge (the recommended way of using it).

The other piece of configuration necessary is the default user to use if no auth token is included in a request. The
field names are org.totalgrid.reef.httpbridge.defaultUser.name and
org.totalgrid.reef.httpbridge.defaultUser.password.


Implementation Details
=======================

Same Origin Policy
----------------------

Since one of the main consumers of the bridge information is javascript widgets on web pages that are generally not
served by reef it is important to include the correct headers to workaround the "same origin policy" enforced by most
browsers. We use the technique called CORS that basically requires a few "Access-Control-Allow-*" headers with the
responses and responding to OPTIONS requests on all servlets with appropriate headers.

http://enable-cors.org/

Caching
----------------------

Since the data we are returning is by its nature transitory it should not be cached so we include cache control
headers on all of the requests. The exact headers we return were originally included to support Internet Explorer and
were based on the hints described here:

http://www.codecouch.com/2009/01/how-to-stop-internet-explorer-from-caching-ajax-requests/