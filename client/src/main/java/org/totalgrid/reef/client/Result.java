package org.totalgrid.reef.client;

public interface Result<T> {
    boolean isSuccess();
    T get();
}
