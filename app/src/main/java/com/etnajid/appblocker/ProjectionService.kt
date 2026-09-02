package com.etnajid.appblocker

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*

/** Consent-driven screen sampler. Frames never leave the device. Sampling backs off when clean. */
class ProjectionService : Service() {
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var worker: HandlerThread? = null
    private var interval = 1000L
    private val handler by lazy { Handler(worker!!.looper) }
    override fun onStartCommand(intent: Intent?, flags: Int, id: Int): Int {
        val data = intent?.getParcelableExtra<Intent>("projection_data") ?: return START_NOT_STICKY
        createChannel(); startForeground(12, Notification.Builder(this,"blocker").setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("App Blocker").setContentText("On-device video protection active").build())
        worker=HandlerThread("frame-detector", Process.THREAD_PRIORITY_BACKGROUND).also { it.start() }
        val pm=getSystemService(MediaProjectionManager::class.java); projection=pm.getMediaProjection(intent.getIntExtra("projection_result", -1),data)
        val dm=resources.displayMetrics; reader=ImageReader.newInstance(dm.widthPixels,dm.heightPixels,PixelFormat.RGBA_8888,2)
        projection!!.createVirtualDisplay("AppBlockerCapture",dm.widthPixels,dm.heightPixels,dm.densityDpi,0,reader!!.surface,null,handler)
        schedule(); return START_NOT_STICKY
    }
    private fun schedule(){ handler.postDelayed({
        val image=reader?.acquireLatestImage(); if(image!=null){
            val flagged=NativeGuard.classifyFrame(image.planes[0].buffer,image.width,image.height)
            image.close(); if(flagged){ AppState.log(this,"Reels frame blocked"); interval=1000L } else interval=(interval+500L).coerceAtMost(5000L)
        }; schedule()
    },interval) }
    private fun createChannel(){getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("blocker","Protection",NotificationManager.IMPORTANCE_LOW))}
    override fun onBind(i:Intent?)=null
    override fun onDestroy(){ reader?.close();projection?.stop();worker?.quitSafely();super.onDestroy() }
}
