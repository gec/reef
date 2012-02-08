package org.totalgrid.reef.client.registration;

import org.totalgrid.reef.client.proto.Envelope;

public interface EventPublisher {

    <T> void publishEvent(Envelope.SubscriptionEventType eventType, T eventMessage, String routingKey);
}
