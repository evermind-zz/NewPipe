package org.schabi.newpipe;

import android.content.Context;
import android.content.SharedPreferences;

import org.schabi.newpipe.brave.feature.challenge.BraveCfChallenge403Interceptor;

import com.github.evermindzz.challengefloatsaway.manager.ChallengeManagerInterface;
import com.github.evermindzz.challengefloatsaway.manager.ChallengeServiceManager;

import org.schabi.newpipe.extractor.downloader.BraveCookieManager;
import org.schabi.newpipe.util.image.PicassoHelper;

import java.io.IOException;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import okhttp3.Interceptor;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;

import static org.schabi.newpipe.DownloaderImpl.USER_AGENT;

/**
 * Used for code that only exists in BravePipe and is used
 * within the {@link DownloaderImpl}.
 */
public final class BraveDownloaderImplUtils {
    public static final Config CONFIG = new Config();

    private BraveDownloaderImplUtils() {
    }

    // some servers eg rumble do not allow HEAD requests anymore (discovered 202300203)
    public static long getContentLengthViaGet(final String url) throws IOException {
        final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", "*/*");
        conn.setRequestProperty("Accept-Encoding", "*");
        final String contentSize = conn.getHeaderField("Content-Length");
        conn.disconnect();
        return Long.parseLong(contentSize);
    }

    public static void addOrRemoveInterceptors(final OkHttpClient.Builder builder) {
        final Context context = App.getApp().getApplicationContext();
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        addOrRemoveHostInterceptor(builder, context, settings);
        addOrRemoveRumbleCloudflareInterceptor(builder, context, settings);
        addOrRemoveTimeoutInterceptor(builder, context, settings);
    }

    private static void addOrRemoveRumbleCloudflareInterceptor(
            final OkHttpClient.Builder builder,
            final Context context,
            final SharedPreferences settings) {

        final Optional<Interceptor> rumbleInterceptor =
                BraveCfChallenge403Interceptor.getInterceptor(builder);
        final boolean isHandleCloudflareChallengeEnabled = settings.getBoolean(context.getString(
                R.string.brave_settings_handle_cloudflare_challenge_enable_key), false);

        if (isHandleCloudflareChallengeEnabled) {
            if (rumbleInterceptor.isEmpty()) {
                final ChallengeManagerInterface manager = new ChallengeServiceManager(
                        App.getApp().getApplicationContext());
                builder.addInterceptor(new BraveCfChallenge403Interceptor(manager));
            }
        } else {
            rumbleInterceptor.ifPresent(interceptor -> {
                if (interceptor instanceof BraveCfChallenge403Interceptor) {
                    ((BraveCfChallenge403Interceptor) interceptor).cleanupBeforeDestroy();
                }
                builder.interceptors().remove(interceptor);

            });

        }
    }

    public static void addOrRemoveHostInterceptor(
            final OkHttpClient.Builder builder,
            final Context context,
            final SharedPreferences settings) {

        final Set<String> selectedHosts = settings.getStringSet(
                context.getString(R.string.brave_settings_host_replace_key),
                Collections.emptySet());

        final Optional<Interceptor> hostInterceptor = BraveHostInterceptor.getInterceptor(builder);
        if (selectedHosts.isEmpty()) {
            hostInterceptor.ifPresent(interceptor -> builder.interceptors().remove(interceptor));
        } else {
            final Map<String, String> replaceHosts = new HashMap<>();
            for (final String oldAndNewHost : selectedHosts) {
                final String[] result = oldAndNewHost.split(":");
                replaceHosts.put(result[0], result[1]);
            }

            if (hostInterceptor.isPresent()) {
                ((BraveHostInterceptor) hostInterceptor.get()).setHosts(replaceHosts);
            } else {
                builder.addInterceptor(new BraveHostInterceptor(replaceHosts));
            }
        }
    }

    private static void addOrRemoveTimeoutInterceptor(
            final OkHttpClient.Builder builder,
            final Context context,
            final SharedPreferences settings) {

        final boolean isClientForSponsorblockingOrReturnDislikesEnabled = (settings.getBoolean(
                context.getString(R.string.sponsor_block_enable_key), false)
                || settings.getBoolean(
                context.getString(R.string.enable_return_youtube_dislike_key), false));

        final Optional<Interceptor> timeoutInterceptor =
                BraveTimeoutInterceptor.getInterceptor(builder);
        if (isClientForSponsorblockingOrReturnDislikesEnabled) {
            if (timeoutInterceptor.isEmpty()) {
                builder.addInterceptor(new BraveTimeoutInterceptor());
            }
        } else {
            timeoutInterceptor.ifPresent(interceptor -> builder.interceptors().remove(interceptor));
        }
    }

    /**
     * Rumble needs to handle cookies to correctly redirect.
     *
     * It was reported in
     * <a href="https://github.com/bravepipeproject/BravePipeExtractor/issues/123">issue#123</a>
     * even though it seems it was only temporary Rumble glitch this functionality is added here.
     *
     * @param theBuilder the builder
     */
    public static void addCookieManager(final OkHttpClient.Builder theBuilder) {
        final BraveCookieManager cookieManager = new BraveCookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        theBuilder.cookieJar(new JavaNetCookieJar(cookieManager));
    }

    /**
     * Listen to the SharedPreferences and handle if replacing hosts or sponsorblock are enabled.
     */
    public static class Config implements SharedPreferences.OnSharedPreferenceChangeListener {

        public void registerOnChanged(@NonNull final Context context) {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(
                final SharedPreferences settings, final String configOption) {

            if (configOption == null) {
                return;
            }

            final Context context = App.getApp().getApplicationContext();
            if (configOption.equals(
                    context.getString(R.string.brave_settings_host_replace_key))
                    || configOption.equals(context.getString(
                    R.string.brave_settings_handle_cloudflare_challenge_enable_key))
                    || configOption.equals(
                    context.getString(R.string.sponsor_block_enable_key))
                    || configOption.equals(
                    context.getString(R.string.enable_return_youtube_dislike_key))) {

                DownloaderImpl.getInstance().reInitInterceptors();
            }

            if (configOption.equals(
                    context.getString(R.string.brave_settings_host_replace_key))) {
                PicassoHelper.reInit(context);
            }
        }

        public void unRegisterOnChanged(@NonNull final Context context) {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .registerOnSharedPreferenceChangeListener(this);
        }
    }
}
