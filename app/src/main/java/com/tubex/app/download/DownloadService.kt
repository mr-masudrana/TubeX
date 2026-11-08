package com.tubex.app.download

import com.tubex.app.MainActivity
import com.tubex.app.R
import com.tubex.app.util.L
import com.tubex.app.notification.Notif

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** ডাউনলোড চলার সময় প্রসেস জীবিত রাখে এবং প্রগ্রেস নোটিফিকেশন দেখায়। */
class DownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var collecting = false
    private var lastStartId = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        Notif.ensureChannels(this)
        ServiceCompat.startForeground(
            this, NOTIF_ID, build(L.t("ডাউনলোড চলছে"), -1), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
        if (!collecting) {
            collecting = true
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            scope.launch {
                DownloadEngine.status.collect { s ->
                    if (s != null) nm.notify(NOTIF_ID, build(s.first, s.second))
                }
            }
            scope.launch {
                DownloadEngine.active.collect { on ->
                    if (!on) {
                        ServiceCompat.stopForeground(this@DownloadService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf(lastStartId)   // এর মধ্যে নতুন স্টার্ট এলে সার্ভিস বন্ধ হবে না
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE
    )

    /**
     * Android 15 (API 35)+: dataSync ফোরগ্রাউন্ড সার্ভিস ২৪ ঘণ্টায় সর্বোচ্চ প্রায় ৬ ঘণ্টা চলে (targetSdk ৩৫ হলে কার্যকর)।
     * সময় শেষে এখানে ডাক আসে; কয়েক সেকেন্ডের মধ্যে stopSelf() না করলে সিস্টেম অ্যাপকে ক্র্যাশ করায়।
     */
    override fun onTimeout(startId: Int, fgsType: Int) {
        DownloadEngine.pauseForTimeout(applicationContext)
        val n = NotificationCompat.Builder(this, Notif.CH_DL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(L.t("ডাউনলোড"))
            .setContentText(L.t("সিস্টেমের সময়সীমা শেষ — ডাউনলোড থামানো হয়েছে। অ্যাপ খুলে আবার চালু করুন।"))
            .setAutoCancel(true).setContentIntent(openAppIntent())
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_ID_TIMEOUT, n)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun build(text: String, pct: Int): Notification {
        val pi = openAppIntent()
        val b = NotificationCompat.Builder(this, Notif.CH_DL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(L.t("ডাউনলোড"))
            .setContentText(text)
            .setOngoing(true).setOnlyAlertOnce(true)
            .setContentIntent(pi)
        if (pct >= 0) b.setProgress(100, pct, false) else b.setProgress(0, 0, true)
        return b.build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 4001
        private const val NOTIF_ID_TIMEOUT = 4002
    }
}
