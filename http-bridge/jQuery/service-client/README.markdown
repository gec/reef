
Reef Javascript Support
=========================

If the server has the http-bridge installed it means we can use any Http client to pull data from the server. In
particular it makes it very simple to access a small view into the data using javascript.

jQuery Client
=============

We have created a "native", jQuery based, client to the reef services that provides most of the same functionality as
the java client. We provide an auto-generated API layer to most of the functions in ReefServicesList. The client layer
is implemented in a similar fashion to the java client; the largest fundamental difference is there isn't the idea of
a "connection" to the server that can fail, each request is an independent event.

We chose to write the client using jQuery because it is the most popular framework and has a native promise based ajax
function. This allows us to implement the client in javascript in nearly the identical way as in the java
client. This should mean that transitioning between the two languages is relatively easy. I believe this library
could be ported to another framework or straight javascript easily given a promise implementation and a few utility
functions (like $.each and $.extend).

The principal author of this client is not a javascript expert so the code in the client and examples should not
be used as an example of best practices, improvement suggestions are welcome.

This guide is written assuming that the reader is already familiar with javascript. Below are some helpful links
for the most important jQuery pieces we make use of.

- jQuery ajax: http://api.jquery.com/jQuery.ajax/ (see "Callback function queues" section).
- Deferred Object (Promise): http://api.jquery.com/category/deferred-object/
- jQuery plugin guide: http://docs.jquery.com/Plugins/Authoring

> This library should be considered a beta release, expect the functionality to evolve quickly for the next few releases.

### Code Organization

The client is split into two major of components:

- src/main/web/reef.client.js : Core client functionality. Includes 'login' and the base function used by api services
  to make requests to the server.
- src/main/web/reef.client.core-services.js : Auto-generated functions for all of the compatible functions in
  ReefServices. This code should never be altered by hand, instead the changes should be made to HttpServiceCallBindings.scala.

Both of these files are necessary to import and should be includes in your application or served by your server for
inclusion into your web page. (Do not hotlink against the github resource.)

```html
<script language="javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js" ></script>
<script language="javascript" src="./reef.client.js"></script>
<script language="javascript" src="./reef.client.core-services.js"></script>
```

### Api Requests

The api request definitions in reef.client.core-services.js are auto-generated from the java Api interface files. This
means that the reef services javadocs should be used as the primary source of documentation for the functions provided
by the javascript client. We include the javadoc for each interface and method for convenience but we recommend that
developers refer to the javadoc api guides when planning out their applications.

Below is an example of an api request definition. The definition includes two important pieces of meta-data about the
function: the type of the result and how many results we should expect.

```javascript
/**
 * Gets list of all currently registered applications
 */
calls.getApplications = function() {
    return client.apiRequest({
        request: "getApplications",
        style: "MULTI",
        resultType: "application_config"
    });
};
```

"style" will be one of:

- MULTI - an array of 0 or more results
- SINGLE - a single result (or a failure will be thrown)
- OPTIONAL - a single result or undefined if there is no data

"resultType" is a form of the name of the proto (as understood by the /convert/ servlet). This name can be used to get
a description of the fields available on the result object by calling /convert/{resultType}. (See describeResultType section)

Some api functions are not ported to the javascript client. If they are not available there will be a comment in the
services definition file describing why. Usually it is because the types of the arguments are too complex to URL encode
but in most cases there is a related function that has the same effect with simpler arguments. If a needed function is
not available please include this comment in any feature requests:

```javascript
// Can't encode executeCommandAsControl : Can't encode type: org.t.reef.client.service.proto.Model.Command
```

### Promises

Each api request to the server returns a jQuery Deferred object (Promise) representing the eventual success or
failure of the request. We then attach functions to those promises that should be called with the result using the
.done() method of Deferred.

```javascript
var promise = client.getPoints();
promise.done(function(points){
    // do some work with the list of points here
});
```

The Deferred object nearly exactly matches the Promise class we have in the java client except that it lacks the "await"
method. This is expected because of the effectively single threaded nature of browser javascript runtimes, all work based
on ajax calls needs to be defined using callbacks (closures). Using promises makes writing callback heavy code easier
because we can making of a request and the handling of the data into separate sections, something that is hard to do
using naive callbacks.

### Error Handling

We can also attach a failure handler to be notified if the request fails. For each of these handlers we can attach more
than one callback, they will all be called in the order they were added.

```javascript
var promise = client.getPoints();
promise.fail(function(errorMessage){
    // print or display the error message here.
});
```

We can also include a generic error handler that is notified of all failures (before any custom error handlers).

```javascript
var client = $.reefClient({
    'server'     : 'http://127.0.0.1:8886',
    'error_function'  : function(errorString){
        console.log("Unexpected error: " + errorString);
        return errorString;
    }
});
```

### Authorization

The reef http-bridge can be configured to allow generic "read-only" access to the system. If so then the client application
will not need to do anything special to get access to the resources on the server. If no read-only user is configured or
the client application needs "write" or "execute" access we can login as a regular user on the system using the reef
username/password combination. This should only be done inside a secure network or when using HTTPS to avoid sending
passwords in plaintext.

There is an explicit .login() function on the client that we can call to get an AuthToken for that particular user. Most
users will find it easier to use the 'auto_login' to start logging in as soon as the client is created. Downside is we
lose the ability to throw a specific error for login failure.

```javascript
var client = $.reefClient({
    'server'     : 'http://127.0.0.1:8886',
    'auto_login' : {
        'name' : 'userName',
        'password' : 'userPassword'
    }
});
// is equivalent to
var client = $.reefClient({'server' : 'http://127.0.0.1:8886'});
client.login('userName', 'userPassword').done(function(authToken){
    console.log("Logged in with token: " + authToken);
});
```

Once we have logged in using either technique the authToken will be maintained by the client and sent with all future
requests. To dispose of the authToken call .logout() on the client.

Since requests are handled serially we will not attempt the next requested function until the previous one completes.
This means that we don't need to wait for the login() or auto_login to complete before making a "real" request. If the
login fails, the next request will fail as not authorized anyways.

### Subscriptions

We have support for subscriptions using the jQuery client. We have not implemented any of the more advanced
transport mechanisms such as comet or websockets yet but we believe the subscription api, from the application
perspective, is stable. When making a subscription request that would return a SubscriptionResult in java we change
the callback type to be an object that contains both the results and a subscription object.

```javascript
var subscriptionObject = {
    // takes a callback expecting (eventCode, payloadData) and returns a promise
    // representing subscription status
    start : sub.start,
    // cancels the subscription. If successfully canceled on server side the
    // promise.done() method will be called
    cancel : sub.cancel
};

// shape of data returned from subscribeTo* api functions
subscriptionResult = {
   result : [data1,data2,data3],
   subscription : subscriptionObject
}
```

The function passed to the subscription start() method will be called for each subscription event along with its
eventCode: 'ADDED', 'MODIFIED', 'REMOVED' ('NOTE:' Currently the eventCode will always be 'MODIFIED'). The return
value of the start() call is a promise that can be used to listen for failures during the subscriptions.

```javascript
val promise = client.subscribeToMeasurementsByNames(['SimulatedSubstation.Line01.Current'])
promise.done(function(subscriptionResult){
    // handle initial results
    var startingMeasurements = subscriptionResult.result;
    prepareTable(startingMeasurements);

    // attach to subscription
    var sub = subscriptionResult.subscription;
    sub.start( function(eventCode, measurement){
        if(eventCode === 'MODIFIED') updateTable(measurement);
    }).done(function(reason){
        // called when the subscription is successfully canceled (during orderly shutdown)
    }).fail(function(errString){
        // called if there is a failure maintaining the subscription,
        // subscription should be considered dead
    };
});
```

When a subscription is no longer needed (leaving page or changing display) it should be canceled. Subscriptions will
eventually be canceled and discarded by the server but clients cleaning up subscriptions will help maintain good
performance.

#### Subscription Transports

Currently the "transport" for the subscription data is a naive server side cache which is polled by the client. Each
subscription is managed independently and no long polling is being done. This is fully functional and all of the data
is being delivered, it is just not very efficient. Pages with many subscriptions may see poor performance.

In the near future we hope to implement both comet and websocket subscription transports and "multiplex" the
subscription data onto a single transport stream.

### Examples

Retrieve the list of all Points in the system and print their names. Notice we use the done() to get the results of
request passed into our "payload function" which prints all the point names. We are also using the jQuery helper $.each
to iterate through the list cleanly.

```javascript
client.getPoints().done(function(points){
    $.each(points, function(point){
        console.log(point.name);
    });
}).fail(function(errString){
    // we can catch the error for the individual request here.
    console.log("Error getting points: " + errString);
};
```

### Client Functions

There are only a few base functions on the client. All other calls are added by the ServicesLists.

```javascript
// base function for making apiRequests, used by the generated api bindings, returns promise with result
'apiRequest': apiRequest,
// login a user, two parameters userName and userPassword, returns promise containing authToken
'login': login,
// logout the user and delete the authToken, returns promise containing status flag
'logout' : logout,
// get the protobuf description of the type, takes object id, returns promise containing JSON formatted decriptor
'describe' : describe,
// returns string describing user and server we are using
'toString' : toString
```

### Limitations

- Can't use functions that require passing in complex types (protobuf objects or enums).

#### describeResultType()

All api request promises define the convenience function describeResultType() that makes api calls somewhat reflective.
The describe call asks the server to describe the data so the describeResultType return value is another promise we
need a done() handler to get the payload data. The payload data is the protobuf "self-descriptor" format serialized to JSON.

In most cases it is easier to read the original proto definition files (since they are designed to be human readable).
Those files are embedded in the javadocs in the *.protodoc packages. This interface is primarily for making "smart"
displays that don't know what sort of data they will be displaying.

```javascript
client.getApplications().describeResultType().done(function(descriptor){
  console.log(JSON.stringify(descriptor, undefined, 2));
});
```

```json
{
  "name": "ApplicationConfig",
  "field": [
    {
      "name": "uuid",
      "number": 1,
      "label": "LABEL_OPTIONAL",
      "type": "TYPE_MESSAGE",
      "type_name": ".org.totalgrid.reef.client.service.proto.Model.ReefUUID"
    },
    {
      "name": "user_name",
      "number": 2,
      "label": "LABEL_OPTIONAL",
      "type": "TYPE_STRING"
    },
    {
      "name": "online",
      "number": 10,
      "label": "LABEL_OPTIONAL",
      "type": "TYPE_BOOL"
    },
    {
      "name": "times_out_at",
      "number": 11,
      "label": "LABEL_OPTIONAL",
      "type": "TYPE_UINT64"
    }
  ]
}
```

### Compatibility

This library has been developed primarily against chrome v17.0 but passes its unit tests against:

- Chrome 17+
- Internet Explorer 8
- Firefox 10.0

We are using jQuery 1.7.1 because of its advanced CORS (Cross-Site-Resource-Sharing) support but most versions
since 1.5.1 should work if serving the code from the same domain as the bridge.