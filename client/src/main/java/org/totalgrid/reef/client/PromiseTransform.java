package org.totalgrid.reef.client;

public interface PromiseTransform <T, U> {
    U transform(T value) throws Exception;
}
