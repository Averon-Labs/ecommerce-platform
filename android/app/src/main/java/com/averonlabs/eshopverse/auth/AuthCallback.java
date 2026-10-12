package com.averonlabs.eshopverse.auth;

/**
 * Generic callback for asynchronous authentication operations.
 *
 * @param <T> Type of the successful result.
 */
public interface AuthCallback<T> {
    void onSuccess(T data);
    void onError(Throwable throwable);
}
