package org.schabi.newpipe.brave.feature.challenge

import com.github.evermindzz.challengefloatsaway.ChallengeSettings
import org.schabi.newpipe.DownloaderImpl

object BraveCfChallengeConfig {

    fun updateFloatingVisible(isInteractive: Boolean) = ChallengeSettings.update { it.copy(isInteractive = isInteractive) }

    fun init(isInteractive: Boolean) {
        ChallengeSettings.update { current ->
            current.copy(
                userAgent = DownloaderImpl.USER_AGENT,
                cookieDomains = arrayOf<String>(
                    "https://rumble.com",
                    "rumble.com",
                    ".rumble.com",
                    "https://www.rumble.com",
                    "www.rumble.com"
                ),
                isInteractive = isInteractive
            )
        }
    }
}
