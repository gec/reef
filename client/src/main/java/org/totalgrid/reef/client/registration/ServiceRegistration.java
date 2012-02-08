package org.totalgrid.reef.client.registration;

import org.totalgrid.reef.client.Routable;
import org.totalgrid.reef.client.SubscriptionBinding;

public interface ServiceRegistration {

    EventPublisher getEventPublisher();

    SubscriptionBinding bindService(Service service, Routable destination, boolean competing);
}
