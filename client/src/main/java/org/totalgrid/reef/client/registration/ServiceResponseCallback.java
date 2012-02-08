package org.totalgrid.reef.client.registration;

import org.totalgrid.reef.client.proto.Envelope;

public interface ServiceResponseCallback {
    
    void onResponse(Envelope.ServiceResponse response);
}