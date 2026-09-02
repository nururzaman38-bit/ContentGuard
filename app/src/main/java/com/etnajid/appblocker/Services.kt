package com.etnajid.appblocker

import android.accessibilityservice.AccessibilityService
import android.app.*
import android.app.admin.DeviceAdminReceiver
import android.content.*
import android.graphics.Color
import android.net.VpnService
import android.os.*
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import java.io.BufferedReader
import java.io.InputStreamReader

class AdminReceiver : DeviceAdminReceiver() {
    override fun onDisabled(c: Context, i: Intent) { AppState.log(c,"Device Admin disabled") }
}

class BlockVpnService : VpnService() {
    private var thread: Thread?=null
    override fun onCreate() { super.onCreate(); loadAssets() }
    private fun loadAssets() {
        try { assets.open("blocklist.txt").bufferedReader().useLines { lines -> lines.map { it.trim().lowercase() }.filter { it.isNotEmpty() && !it.startsWith("#") }.forEach { runCatching { NativeGuard.addPattern(it) } } } }
        catch (e: Exception) { android.util.Log.w("ContentGuard", "blocklist.txt missing; add it under app/src/main/assets", e) }
        try { assets.open("nsfw_model.tflite").close() } catch (e: Exception) { android.util.Log.w("ContentGuard", "nsfw_model.tflite missing; frame classification is unavailable until supplied", e) }
    }
    override fun onStartCommand(i:Intent?, flags:Int, id:Int):Int { getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("blocker","Protection",NotificationManager.IMPORTANCE_LOW)); startForeground(7, notification("DNS filtering active")); thread=Thread { runFilter() }.also{it.start()}; return START_STICKY }
    private fun runFilter() { try { val p=Builder().setSession("App Blocker DNS").addAddress("10.8.0.2",32).addRoute("0.0.0.0",0).addDnsServer("1.1.1.1").establish(); p?.use { while(!Thread.currentThread().isInterrupted) Thread.sleep(1000) } } catch(e:Exception) { android.util.Log.e("AppBlocker","VPN unavailable",e) } }
    override fun onDestroy(){thread?.interrupt();super.onDestroy()}
    private fun notification(t:String)=Notification.Builder(this,"blocker").setContentTitle("App Blocker").setContentText(t).setSmallIcon(android.R.drawable.ic_lock_lock).build()
}

class ContentAccessibilityService : AccessibilityService() {
    private var last=0L
    override fun onServiceConnected(){ super.onServiceConnected(); val nm=getSystemService(NotificationManager::class.java); nm.createNotificationChannel(NotificationChannel("blocker","Protection",NotificationManager.IMPORTANCE_LOW)) }
    override fun onAccessibilityEvent(e:AccessibilityEvent?) { if(e==null || SystemClock.elapsedRealtime()-last<300)return; last=SystemClock.elapsedRealtime(); val text=e.text?.joinToString(" ") ?: ""; val root=rootInActiveWindow ?: return; val all=text+" "+root.text; if(NativeGuard.matches(all.lowercase()) || AppState.panic(this)) { AppState.log(this,"Blocked content",e.packageName?.toString() ?: "unknown"); performGlobalAction(GLOBAL_ACTION_HOME); showWarning() } }
    override fun onInterrupt() {}
    private fun showWarning(){ val w=TextView(this);w.text="This content isn't allowed — skip or scroll to another video";w.textSize=20f;w.setTextColor(Color.WHITE);w.setBackgroundColor(Color.rgb(30,35,38));w.setPadding(40,40,40,40); val wm=getSystemService(WINDOW_SERVICE) as android.view.WindowManager; val lp=android.view.WindowManager.LayoutParams(-1,-2,android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, -3); wm.addView(w,lp); Handler(Looper.getMainLooper()).postDelayed({runCatching{wm.removeView(w)}},3000) }
}

object NativeGuard {
    init { runCatching { System.loadLibrary("contentguard") }.onFailure { android.util.Log.e("ContentGuard", "Native protection engine unavailable", it) } }
    external fun matches(text:String):Boolean
    external fun addPattern(text:String)
    external fun classifyFrame(buffer: java.nio.ByteBuffer, width: Int, height: Int): Boolean
}
