package com.webviewtemplate.webviewtemplate

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.window.OnBackInvokedDispatcher
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.webviewtemplate.webviewtemplate.databinding.ActivityMainBinding
import com.onesignal.OneSignal

class MainActivity : Activity() {

    // رابط موقعك
    private val applicationUrl = "https://driver.zoonasd.com/"

    // OneSignal App ID
    private val ONESIGNAL_APP_ID = "e542557c-fbed-4ca6-96fa-0b37e0d21490"

    // Notification Channel ID
    private val CHANNEL_ID = "5aa08429-7147-4953-80e7-af2e23b115b2"

    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        webView = binding.webView

        // زر الرجوع
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        }

        // إعداد WebView
        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptEnabled = true

        // إنشاء Notification Channel
        createRideNotificationChannel()

        // تهيئة OneSignal
        initOneSignal()

        // تحقق إذا جاءنا Intent لفتح accept-ride.html
        val openAccept = intent.getBooleanExtra("openAcceptRide", false)
        if (openAccept) {
            webView.loadUrl("https://driver.zoonasd.com/accept-ride.html")
        } else {
            webView.loadUrl(applicationUrl)
        }
    }

    // إنشاء Notification Channel مع صوت واهتزاز
    private fun createRideNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Ride Requests"
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )

            val soundUri = Uri.parse("android.resource://${packageName}/raw/ride_request_sound")
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            channel.setSound(soundUri, audioAttributes)
            channel.enableVibration(true)
            channel.lockscreenVisibility = NotificationManager.VISIBILITY_PUBLIC

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    // تهيئة OneSignal مع Listener
    private fun initOneSignal() {
        OneSignal.initWithContext(this)
        OneSignal.setAppId(ONESIGNAL_APP_ID)

        OneSignal.setNotificationWillShowInForegroundHandler { notificationReceivedEvent ->
            val notification = notificationReceivedEvent.notification
            // عند استقبال طلب، نظهر إشعار Full Screen
            showIncomingRideNotification()
            notificationReceivedEvent.complete(null) // نمنع الإشعار الافتراضي
        }
    }

    // عرض إشعار Full Screen عند طلب جديد
    private fun showIncomingRideNotification() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("openAcceptRide", true) // عند الضغط على قبول يفتح accept-ride.html

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Incoming Ride")
            .setContentText("You have a new ride request")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 500, 500)) // اهتزاز متكرر
            .setSound(Uri.parse("android.resource://${packageName}/raw/ride_request_sound"))

        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(123, builder.build())
    }
}