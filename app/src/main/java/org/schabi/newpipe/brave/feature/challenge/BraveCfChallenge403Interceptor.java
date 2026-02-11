package org.schabi.newpipe.brave.feature.challenge;

import android.util.Log;

import com.github.evermindzz.challengefloatsaway.ChallengeResult;
import com.github.evermindzz.challengefloatsaway.manager.ChallengeManagerInterface;

import java.io.IOException;
import java.util.Optional;

import androidx.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Handle 403 in case cloudflare questions a human using the app.
 */
public class BraveCfChallenge403Interceptor implements Interceptor {

    private static final MediaType HTML = MediaType.get("text/html; charset=utf-8");
    private static final String TAG = BraveCfChallenge403Interceptor.class.getSimpleName();
    private final ChallengeManagerInterface bypassManager;

    public BraveCfChallenge403Interceptor(final ChallengeManagerInterface manager) {
        this.bypassManager = manager;
    }

    public static Optional<Interceptor> getInterceptor(
            final OkHttpClient.Builder builder) {
        return builder.interceptors().stream().filter(
                BraveCfChallenge403Interceptor.class::isInstance).findFirst();
    }

    @NonNull
    @Override
    public Response intercept(final Chain chain) throws IOException {
        final Request request = chain.request();

        if (!request.url().host().contains("rumble.com")) {
            return chain.proceed(request);
        }

        // TODO reusing cookies from webview in okhttp might be useless as there it uses a different
        // TODO network stack. maybe use a config option to let the user decide.
        //USELESS->DISABLED // reuse previously retrieved cookies from the webView
        final String cookies = ""; //USELESS->DISABLED bypassManager.getCurrentCookies();
        //USELESS->DISABLED final Request.Builder builder = request.newBuilder();
        //USELESS->DISABLED if (!cookies.isEmpty()) {
        //USELESS->DISABLED     builder.header("Cookie", cookies);
        //USELESS->DISABLED }

        //USELESS->DISABLED final Response response = chain.proceed(builder.build());
        final Response response = chain.proceed(request);

        //DBG if (!request.url().toString().contains("https://rumble.com/v")
        //DBG    && response.code() == 200) {
        if (response.code() == 200) {
            debugMessage("CF_DBG Ic 1.0", response.code(), cookies, request.url().toString());
            return response;
        }

        //DBG if ((response.code() == 403 || response.code() == 200) && request.url().toString()
        //DBG         .contains("comment.list")) {
        if (response.code() == 403) {
            final ChallengeResult bypassResult =
                    bypassManager.fetchContentViaWebView(request.url().toString(), 30000);

            debugMessage("CF_DBG Ic 2.0", response.code(),
                    bypassResult.cookies, request.url().toString());

            if (bypassResult.success && bypassResult.content != null) {

                debugMessage("CF_DBG Ic 2.1 webview success", 200,
                        bypassResult.cookies, request.url().toString());

                // reuse the webView's content as a proper okHttp response.
                final Request newRequest = request.newBuilder().build();
                return new Response.Builder()
                        .request(newRequest)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(ResponseBody.create(HTML, bypassResult.content))
                        .build();
            }
        }

        debugMessage("CF_DBG Ic 3.0", response.code(), cookies, request.url().toString());

        return response;
    }

    private void debugMessage(
            final String prefix,
            final int code,
            final String cookies,
            final String url
    ) {
        Log.d(TAG, prefix + " code " + code + " cookies " + cookies + " url " + url);
    }

    public void cleanupBeforeDestroy() {
        bypassManager.destroy();
    }
}
