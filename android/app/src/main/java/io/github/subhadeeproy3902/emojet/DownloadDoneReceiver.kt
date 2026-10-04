package io.github.subhadeeproy3902.emojet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Android's download manager says a download finished: check the model (or install the app update)
 *  right away, even with the app closed. goAsync keeps the app alive for the few-second checksum. */
class DownloadDoneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try {
                ModelManager(context).poll(blocking = true)
                UpdateManager(context).pollApk()
            } finally {
                pending.finish()
            }
        }.start()
    }
}
