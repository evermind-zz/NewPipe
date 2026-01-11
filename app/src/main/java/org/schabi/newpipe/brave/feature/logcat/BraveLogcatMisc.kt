package org.schabi.newpipe.brave.feature.logcat

import com.github.logviewer.LogFileFormat
import com.github.logviewer.LogFilePrefix
import com.github.logviewer.LogItem
import com.github.logviewer.Settings
import java.io.BufferedWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.schabi.newpipe.BuildConfig

object BraveLogcatMisc {

    fun setLogcatOutputFileFormat() {
        /**
         * have a more github friendly logcat format
         */
        val braveLogfileFormat = object : LogFileFormat {
            override suspend fun writeLogs(
                logFileName: String,
                logs: Array<LogItem>,
                writer: BufferedWriter
            ) {
                if (logs.isNotEmpty()) {
                    writer.write("<details><summary><b>Logcat: $logFileName")
                    writer.write("</b>")
                    writer.write("</summary><p>\n")
                    writer.write("\n```\n")
                    for (log in logs) {
                        writer.write(log.origin + "\n")
                    }
                    writer.write("\n```\n")
                    writer.write("</details>\n")
                    writer.write("<hr>\n")
                }
            }
        }

        val braveLogFilePrefix: LogFilePrefix = object : LogFilePrefix {
            override suspend fun getPrefix(): String {
                val dateFormat =
                    SimpleDateFormat("'${BuildConfig.FLAVOR}_'yyyy-MM-dd_HH:mm:ss.SSS", Locale.ROOT)
                return dateFormat.format(Date())
            }
        }

        Settings.update { current ->
            current.copy(
                logfileFormat = braveLogfileFormat,
                logFilePrefix = braveLogFilePrefix
            )
        }
    }
}
