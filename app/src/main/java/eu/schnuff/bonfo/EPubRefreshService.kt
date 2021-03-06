package eu.schnuff.bonfo

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import eu.schnuff.bonfo.dummy.EPubContent


class EPubRefreshService : Service() {
    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val notification = createNotification()
                startForegroundService(notification)
                EPubContent.readItems(this,
                        onProgress = {max, now -> startForegroundService(notification, now, max)},
                        onComplete = {stopForegroundService()}
                )
            }
            ACTION_ABORT -> stopForegroundService()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /* Used to build and start foreground service. */
    private fun startForegroundService(notification: NotificationCompat.Builder, progress:Int? = null, maxProgress:Int? = null) {
        // Start foreground service.
        notification.apply {
            if (progress != null && maxProgress != null) {
                setProgress(maxProgress, progress, false)
                setContentText("$progress/$maxProgress")
            } else {
                setProgress(1, 0, true)
            }
        }
        startForeground(1, notification.build())
    }

    private fun createNotification() : NotificationCompat.Builder {
        createNotificationChannel()
        // Create notification default intent.
        val intent = Intent(this, BookListActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val abortIntent = PendingIntent.getService(this, 0, Intent(this, EPubRefreshService::class.java)
                .setAction(ACTION_ABORT), 0)

        // Create notification builder.
        return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.EpubRefreshTitle))
                .setContentText(getString(R.string.EPubRefreshInitializing))
                .setSmallIcon(R.drawable.ic_sync_24dp)
                //.setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_sync_24dp))
                .setCategory(Notification.CATEGORY_PROGRESS)
                .addAction(R.drawable.ic_warning_24dp, getString(R.string.EPubRefreshActionAbort), abortIntent)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .setWhen(System.currentTimeMillis())
    }

    private fun stopForegroundService() {
        // Stop foreground service and remove the notification.
        stopForeground(true)
        // Stop the foreground service.
        stopSelf()
    }

    private fun createNotificationChannel() {
    // Create the NotificationChannel, but only on API 26+ because
    // the NotificationChannel class is new and not in the support library
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_description)
            setShowBadge(false)
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}


    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_ABORT = "ACTION_ABORT"
        const val CHANNEL_ID = "Epub Refresh"
    }
}
