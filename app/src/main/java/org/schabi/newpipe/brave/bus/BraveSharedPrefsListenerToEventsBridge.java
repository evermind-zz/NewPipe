package org.schabi.newpipe.brave.bus;

import android.content.Context;
import android.content.SharedPreferences;

import org.schabi.newpipe.R;
import org.schabi.newpipe.brave.bus.events.BraveEvents;
import org.schabi.newpipe.brave.feature.challenge.BraveCfChallengeConfig;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;

/**
 * generate Events from some BravePipe specific config options.
 *
 * Not limited to EventBus Events
 */
public class BraveSharedPrefsListenerToEventsBridge
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final Context context;

    public BraveSharedPrefsListenerToEventsBridge(
            final Context context) {
        this.context = context;
        initSharedPrefs();
    }

    @Override
    public void onSharedPreferenceChanged(
            final SharedPreferences prefs,
            @Nullable final String key) {

        if (hasPrefChanged(R.string.brave_settings_scroll_only_below_player_key, key)) {
            postPrefEventScrollOnlyBelowPlayer(prefs);
        } else if (hasPrefChanged(R.string.brave_settings_comment_replies_same_window_key, key)) {
            postPrefEventSameWindowCommentReplies(prefs);
        } else if (hasPrefChanged(
                R.string.brave_settings_handle_cloudflare_challenge_interactive_enable_key, key)) {
            postPrefCloudflareChallengeInteractive(prefs);
        }
    }

    private void postPrefCloudflareChallengeInteractive(final SharedPreferences prefs) {
        final boolean isInteractive = prefs.getBoolean(context.getString(
                        R.string.brave_settings_handle_cloudflare_challenge_interactive_enable_key),
                false);
        BraveCfChallengeConfig.INSTANCE.updateFloatingVisible(isInteractive);
    }
    private void initPrefCloudflareChallengeInteractive(final SharedPreferences prefs) {
        final boolean isInteractive = prefs.getBoolean(context.getString(
                        R.string.brave_settings_handle_cloudflare_challenge_interactive_enable_key),
                false);
        BraveCfChallengeConfig.INSTANCE.init(isInteractive);
    }

    private void initSharedPrefs() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        postPrefEventScrollOnlyBelowPlayer(prefs);
        postPrefEventSameWindowCommentReplies(prefs);
        initPrefCloudflareChallengeInteractive(prefs);
    }

    private void postPrefEventSameWindowCommentReplies(
            final SharedPreferences prefs) {
        final boolean doSameWindowCommentReplies = prefs.getBoolean(context.getString(
                R.string.brave_settings_comment_replies_same_window_key), false);

        BraveBus.getBus().postSticky(new BraveEvents
                .PrefEventSameWindowCommentReplies(doSameWindowCommentReplies));
    }

    private void postPrefEventScrollOnlyBelowPlayer(
            final SharedPreferences prefs) {
        final boolean doScrollOnlyInViewPager = prefs.getBoolean(context.getString(
                R.string.brave_settings_scroll_only_below_player_key), false);

        BraveBus.getBus().postSticky(new BraveEvents
                .PrefEventScrollOnlyBelowPlayer(doScrollOnlyInViewPager));
    }

    private boolean hasPrefChanged(
            @StringRes final int resId,
            @Nullable final String key) {
        return context.getString(resId).equals(key);
    }
}
