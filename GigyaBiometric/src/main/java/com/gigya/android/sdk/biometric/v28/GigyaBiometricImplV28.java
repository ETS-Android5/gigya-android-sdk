package com.gigya.android.sdk.biometric.v28;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;
import android.support.annotation.NonNull;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.biometric.GigyaBiometric;
import com.gigya.android.sdk.biometric.GigyaBiometricImpl;
import com.gigya.android.sdk.biometric.GigyaPromptInfo;
import com.gigya.android.sdk.biometric.IGigyaBiometricCallback;
import com.gigya.android.sdk.biometric.R;
import com.gigya.android.sdk.services.SessionService;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

@TargetApi(Build.VERSION_CODES.P)
public class GigyaBiometricImplV28 extends GigyaBiometricImpl {

    private static final String LOG_TAG = "GigyaBiometricImplV28";

    public GigyaBiometricImplV28(SessionService sessionService) {
        super(sessionService);
    }

    @Override
    synchronized public void showPrompt(Context context, final GigyaBiometric.Action action, @NonNull GigyaPromptInfo gigyaPromptInfo,
                                        int encryptionMode, final @NonNull IGigyaBiometricCallback callback) {
        final SecretKey key = getKey();
        if (key == null) {
            GigyaLogger.error(LOG_TAG, "Unable to generate secret key from KeyStore API");
            return;
        }
        final Cipher cipher = createCipherFor(key, encryptionMode);
        if (cipher != null) {
            BiometricPrompt prompt = new BiometricPrompt.Builder(context)
                    .setTitle(gigyaPromptInfo.getTitle() != null ? gigyaPromptInfo.getTitle() : context.getString(R.string.prompt_default_title))
                    .setSubtitle(gigyaPromptInfo.getSubtitle() != null ? gigyaPromptInfo.getSubtitle() : context.getString(R.string.prompt_default_subtitle))
                    .setDescription(gigyaPromptInfo.getDescription() != null ? gigyaPromptInfo.getDescription() : context.getString(R.string.prompt_default_description))
                    .setNegativeButton("Cancel", context.getMainExecutor(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            callback.onBiometricOperationCanceled();
                        }
                    })
                    .build();
            final BiometricPrompt.CryptoObject cryptoObject = new BiometricPrompt.CryptoObject(cipher);
            prompt.authenticate(cryptoObject, new CancellationSignal(), context.getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    callback.onBiometricOperationFailed(errString.toString());
                }

                @Override
                public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                    super.onAuthenticationHelp(helpCode, helpString);
                }

                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    onSuccessfulAuthentication(cipher, action, callback);
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    callback.onBiometricOperationCanceled();
                }
            });
        } else {
            //Error.
            GigyaLogger.error(LOG_TAG, "Failed to initialize cipher");
        }
    }
}