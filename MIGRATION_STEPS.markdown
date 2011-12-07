# Migration Steps

## 0.3.x -> 0.4.x

This upgrade should be relativley straightforward and mechanical, 90% of the changes
should consist of the changing of import statements. We apologize for the inconvenience
this may cause but we felt it was important to try to get these names right for future
developers. We hope that future upgrades will not require this level of migration by the
user. We have laid out the general steps for migrating an application.

* Many packages were changed, below is a list of sample import changes that provide nearly
  one-to-one replacements with the 0.3.x classes.

```diff
-import org.totalgrid.reef.japi.ReefServiceException;
+import org.totalgrid.reef.client.exception.ReefServiceException;

-import org.totalgrid.reef.japi.client.AMQPConnectionSettings;
+import org.totalgrid.reef.client.settings.AmqpSettings;

-import org.totalgrid.reef.japi.client.Connection;
+import org.totalgrid.reef.client.ReconnectingConnectionFactory;

-import org.totalgrid.reef.messaging.javaclient.AMQPConnection;
+import org.totalgrid.reef.client.factory.ReefReconnectingFactory;

-import org.totalgrid.reef.japi.Envelope;
+import org.totalgrid.reef.client.proto.Envelope;

-import org.totalgrid.reef.japi.client.SessionExecutionPool;
+import org.totalgrid.reef.client.Connection;

-import org.totalgrid.reef.japi.client.ConnectionListener;
+import org.totalgrid.reef.client.ConnectionWatcher;

-import org.totalgrid.reef.japi.request.AllScadaService;
+import org.totalgrid.reef.client.service.AllScadaService;

-import org.totalgrid.reef.proto.ReefServicesList;
+import org.totalgrid.reef.client.service.ReefServices;

-import org.totalgrid.reef.proto.*;
+import org.totalgrid.reef.client.service.proto.*;
```

* A few protobuf objects were renamed for better clarity.

  -  FEP.CommEndpointConfig -> FEP.Endpoint
  -  FEP.CommEndpointConnection -> FEP.EndpointConnection
  -  Commands.CommandAccess -> Commands.CommandLock
  -  Envelope.Event -> Envelope.SubscriptionEventType

* A few fields in the protobufs were renamed or changed type, notables are:

  -  ReefUUID.getUuid -> ReefUUID.getValue
  -  *.getUid (string) -> *.getId (ReefID)

* Service APIs that change names

  -  EndpointManagementService -> EndpointService
  -  EventCreationService -> EventPublishingService
  -  AuthTokenService -> (removed, use login and logout on Connection)

* Connecting to reef has gotten much simpler, the ReefConnection factory is much easier
  to use and the Connection class now has login() and logout() functions to login a user.
  Notice also that the new settings objects, AmqpSettings and UserSettings, have helpers
  to load their properties from .cfg files so it mean application code to load settings can
  be deleted.

* Getting an AllScadaService is much simpler and doesn't require any SessionPools or
  constructing of "Wrapper" classes from an impl package:

```diff
-SessionExecutionPool pool = connection.newSessionPool();
-String authToken = new AuthTokenServicePooledWrapper(pool).createNewAuthorizationToken("core","core");
-AllScadaService services = new AllScadaServicePooledWrapper(pool, authToken);
+Client client = connection.login(new UserSettings("core", "core"));
+AllScadaService services = client.getService(AllScadaService.class);
```
