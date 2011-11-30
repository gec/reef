## Migration Steps

### 0.3.x -> 0.4.x

* Many packages were changed, below is a list of sample import changes that provide nearly

``diff
-import org.totalgrid.reef.japi.ReefServiceException;
+import org.totalgrid.reef.clientapi.exceptions.ReefServiceException;

-import org.totalgrid.reef.japi.client.AMQPConnectionSettings;
+import org.totalgrid.reef.clientapi.settings.AmqpSettings;

-import org.totalgrid.reef.japi.client.Connection;
+import org.totalgrid.reef.clientapi.ReconnectingConnectionFactory;

-import org.totalgrid.reef.japi.client.Connection;
+import org.totalgrid.reef.clientapi.ReconnectingConnectionFactory;

-import org.totalgrid.reef.messaging.javaclient.AMQPConnection;
+import org.totalgrid.reef.client.factory.ReefReconnectingFactory;

-import org.totalgrid.reef.japi.Envelope;
+import org.totalgrid.reef.clientapi.proto.Envelope;

-import org.totalgrid.reef.japi.client.SessionExecutionPool;
+import org.totalgrid.reef.clientapi.Connection;

-import org.totalgrid.reef.japi.client.ConnectionListener;
+import org.totalgrid.reef.clientapi.ConnectionWatcher;

-import org.totalgrid.reef.japi.request.AllScadaService;
+import org.totalgrid.reef.client.service.AllScadaService;
``