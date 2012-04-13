package org.totalgrid.reef.client;

import net.agileautomata.executor4s.Executor;
import org.totalgrid.reef.client.sapi.client.BasicRequestHeaders;
import org.totalgrid.reef.client.sapi.client.RequestSpyHook;
import org.totalgrid.reef.client.sapi.client.rest.RestOperations;
import org.totalgrid.reef.client.sapi.client.rest.ServiceRegistry;

public interface ClientInternal {

    Executor getExecutor();

    RestOperations getOperations();

    RequestSpyHook getRequestSpyHook();

    ServiceRegistry getServiceRegistry();

    BasicRequestHeaders getHeaders();
    void setHeaders(BasicRequestHeaders headers);
}
