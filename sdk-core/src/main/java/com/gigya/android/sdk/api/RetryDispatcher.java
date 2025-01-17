package com.gigya.android.sdk.api;

import androidx.annotation.NonNull;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.IRestAdapter;
import com.gigya.android.sdk.network.adapter.IRestAdapterCallback;

import java.security.SecureRandom;

public class RetryDispatcher {

    private static final String LOG_TAG = "RetryDispatcher";

    private GigyaApiRequest request;
    private IRestAdapter adapter;
    private IApiRequestFactory factory;
    private IRetryHandler handler;
    private int errorCode;
    private int tries;

    private final SecureRandom random = new SecureRandom();

    public interface IRetryHandler {

        void onCompleteWithResponse(GigyaApiResponse retryResponse);

        void onCompleteWithError(GigyaError error);

        void onUpdateDate(String date);
    }

    private boolean decrement() {
        this.tries--;
        return this.tries > 0;
    }

    public void dispatch() {
        // Recreate the request to avoid duplicate nonce & additional errors.
        final GigyaApiRequest newRequest = factory.create(
                request.getApi(),
                request.getParams(),
                request.getMethod());

        adapter.send(newRequest, false, new IRestAdapterCallback() {

            @Override
            public void onResponse(String jsonResponse, String responseDateHeader) {

                // Make sure to update the offset date.
                handler.onUpdateDate(responseDateHeader);

                final GigyaApiResponse apiResponse = new GigyaApiResponse(jsonResponse);
                final boolean retry = decrement();
                if (retry && apiResponse.getErrorCode() == errorCode) {

                    GigyaLogger.debug(LOG_TAG, "Retry error for code: " + errorCode + ". number of tries remaining = " + tries);

                    dispatch();
                    return;
                }

                GigyaLogger.debug(LOG_TAG, "Retry success.");

                handler.onCompleteWithResponse(apiResponse);
            }

            @Override
            public void onError(GigyaError gigyaError) {
                final boolean retry = decrement();
                if (retry && gigyaError.getErrorCode() == errorCode) {

                    GigyaLogger.debug(LOG_TAG, "Retry error for code: " + errorCode + ". number of tries remaining = " + tries);

                    dispatch();
                    return;
                }

                GigyaLogger.debug(LOG_TAG, "Retry Error completion. Parent error flow will continue");

                handler.onCompleteWithError(gigyaError);
            }
        });
    }

    public static class Builder {

        private RetryDispatcher dispatcher;

        public Builder(IRestAdapter adapter, IApiRequestFactory factory) {
            dispatcher = new RetryDispatcher();
            dispatcher.adapter = adapter;
            dispatcher.factory = factory;
        }

        public RetryDispatcher.Builder request(@NonNull GigyaApiRequest request) {
            dispatcher.request = request;
            return this;
        }

        public RetryDispatcher.Builder errorCode(int errorCode) {
            dispatcher.errorCode = errorCode;
            return this;
        }

        public RetryDispatcher.Builder tries(int tries) {
            dispatcher.tries = tries;
            return this;
        }

        public RetryDispatcher handler(@NonNull IRetryHandler handler) {
            dispatcher.handler = handler;
            return dispatcher;
        }
    }

}
