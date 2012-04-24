package org.totalgrid.reef.client;

public interface ResultTransformation<T, U> {
    Result<U> transform(T value);
}
