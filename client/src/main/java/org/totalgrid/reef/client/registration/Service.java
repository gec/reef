package org.totalgrid.reef.client.registration;

import org.totalgrid.reef.client.proto.Envelope;

import java.util.List;
import java.util.Map;

public interface Service {
    void respond(Envelope.ServiceRequest request, Map<String, List<String>> headers, ServiceResponseCallback callback);
}