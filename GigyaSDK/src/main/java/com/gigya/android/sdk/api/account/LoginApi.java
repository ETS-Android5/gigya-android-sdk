package com.gigya.android.sdk.api.account;


import com.gigya.android.sdk.AccountManager;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.InterruptionEnabledApi;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaApiRequestBuilder;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;
import com.gigya.android.sdk.utils.ObjectUtils;

import java.util.Map;

@SuppressWarnings("unchecked")
public class LoginApi<T extends GigyaAccount> extends InterruptionEnabledApi<T> {

    private static final String API = "accounts.login";
    private final Class<T> clazz;

    public LoginApi(NetworkAdapter networkAdapter, SessionManager sessionManager, AccountManager accountManager) {
        super(networkAdapter, sessionManager, accountManager);
        this.clazz = accountManager.getAccountClazz();
    }

    public void call(final Map<String, Object> params, final GigyaLoginCallback callback) {
        GigyaApiRequest gigyaApiRequest = new GigyaApiRequestBuilder(sessionManager)
                .api(API)
                .params(params)
                .httpMethod(NetworkAdapter.Method.GET)
                .build();
        sendRequest(gigyaApiRequest, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaApiResponse response, GigyaCallback<T> callback) {
        if (evaluateSuccessError(response, (GigyaLoginCallback) callback)) {
            /* Response success with error that should be handled. */
            return;
        }
        /* Update session info */
        if (response.contains("sessionInfo") && sessionManager != null) {
            SessionInfo session = response.getField("sessionInfo", SessionInfo.class);
            sessionManager.setSession(session);
        }
        final String json = response.asJson();
        // To avoid writing a clone constructor.
        T parsed = response.getGson().fromJson(json, clazz);
        // Update account.
        accountManager.setAccount(ObjectUtils.deepCopy(response.getGson(), parsed, clazz));
        callback.onSuccess(parsed);
    }

    @Override
    protected void onRequestError(String api, GigyaApiResponse response, GigyaCallback<T> callback) {
        /* Error may contain specific interruption. */
        handleInterruptionError(response, (GigyaLoginCallback) callback);
    }
}