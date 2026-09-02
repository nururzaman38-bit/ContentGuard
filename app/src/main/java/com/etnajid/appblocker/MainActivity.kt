package com.etnajid.appblocker

import android.app.*
import android.app.admin.DevicePolicyManager
import android.content.*
import android.media.projection.MediaProjectionManager
import android.graphics.Color
import android.net.VpnService
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.*
import android.widget.*
import java.text.DateFormat
import java.util.*

class MainActivity : Activity() {
    private lateinit var body: LinearLayout
    private val teal = Color.rgb(128,203,196)
    override fun onCreate(b: Bundle?) { super.onCreate(b); showDashboard() }
    override fun onActivityResult(requestCode:Int, resultCode:Int, data:Intent?) { super.onActivityResult(requestCode,resultCode,data); if(requestCode==42 && resultCode==RESULT_OK && data!=null) startForegroundService(Intent(this,ProjectionService::class.java).putExtra("projection_data",data).putExtra("projection_result",resultCode)); if(requestCode==9 && resultCode==RESULT_OK) startService(Intent(this,BlockVpnService::class.java)) }
    private fun root(): LinearLayout { val l=LinearLayout(this); l.orientation=LinearLayout.VERTICAL; l.setPadding(24,24,24,8); l.setBackgroundColor(Color.rgb(16,18,20)); return l }
    private fun title(t:String) = TextView(this).apply { text=t; textSize=25f; setTextColor(Color.WHITE); setPadding(0,0,0,18) }
    private fun button(t:String, action:()->Unit) = Button(this).apply { text=t; setOnClickListener { action() } }
    private fun frame(page:String) { val r=root(); r.addView(title("App Blocker  •  $page")); body=LinearLayout(this); body.orientation=LinearLayout.VERTICAL; r.addView(body, LinearLayout.LayoutParams(-1,0,1f)); val nav=LinearLayout(this); listOf("Dashboard" to ::showDashboard,"Blocklist" to ::showBlocklist,"Activity Log" to ::showLog,"Settings" to ::showSettings).forEach { (n,a)-> nav.addView(button(n){a()},LinearLayout.LayoutParams(0,60,1f)) }; r.addView(nav); setContentView(r) }
    private fun showDashboard() { frame("Dashboard"); body.addView(TextView(this).apply { text="PROTECTION STATUS\n${if(AppState.enabled(this@MainActivity,"adult_sites")) "ACTIVE" else "SETUP REQUIRED"}\n\nToday's blocked events: ${AppState.count(this@MainActivity)}\nClean streak: ${AppState.streak(this@MainActivity)} days"; textSize=18f; setTextColor(teal); setPadding(0,8,0,24) }); body.addView(button("PANIC LOCK — block browsing now") { AppState.set(this,"panic",true); AppState.log(this,"Panic lock enabled"); toast("Panic lock active") }) }
    private fun showSettings() { frame("Settings"); body.addView(TextView(this).apply{text="Individual protections\nA feature locks permanently after activation. Uninstall Protection is intentionally reversible during testing.";setTextColor(Color.LTGRAY);textSize=15f}); AppState.features.forEachIndexed { i,n -> toggle(n, "feature_$i", i==0) }; toggle("Uninstall Protection (testing)","uninstall_protection",false); body.addView(button("Focus schedule: configure time window") { timePicker() }); body.addView(button("Permissions & onboarding") { onboarding() }); body.addView(button("Battery optimization reminder") { startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, android.net.Uri.parse("package:$packageName"))) }) }
    private fun toggle(label:String,key:String,locked:Boolean) { val s=Switch(this); s.text=label; s.textSize=16f; s.setTextColor(Color.WHITE); s.isChecked=AppState.enabled(this,key); if(s.isChecked && locked) s.isEnabled=false; s.setOnCheckedChangeListener { _,on -> if(on) { AppState.set(this,key,true); if(locked) s.isEnabled=false; if(key=="feature_0") startVpn()
            if(key=="feature_3") { val pm=getSystemService(MediaProjectionManager::class.java); startActivityForResult(pm.createScreenCaptureIntent(), 42) }
            if(key=="uninstall_protection") requestDeviceAdmin() } else if(key=="uninstall_protection") { /* TODO: PRODUCTION — lock permanently once ON; remove this reversible path before release. */ AppState.set(this,key,false) } }; body.addView(s) }
    private fun showBlocklist() { frame("Blocklist"); body.addView(TextView(this).apply{text="Bundled domains and your custom keywords\nChanges use a delay confirmation.";setTextColor(Color.LTGRAY);textSize=16f}); val input=EditText(this); input.hint="domain or keyword"; body.addView(input); body.addView(button("Add after 10-minute delay") { if(input.text.isNotBlank()) { AppState.log(this,"Added blocklist item"); toast("Change scheduled in 10 minutes") } }); body.addView(button("Watched apps: choose installed apps") { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,android.net.Uri.parse("package:$packageName"))) }) }
    private fun showLog() { frame("Activity Log"); val events=getSharedPreferences("activity_log",0).getStringSet("events",emptySet())!!.sortedDescending(); body.addView(TextView(this).apply{text=if(events.isEmpty()) "No block events yet." else events.joinToString("\n\n") { val p=it.split('|'); "${DateFormat.getDateTimeInstance().format(Date(p[0].toLong()))}\n${p.getOrElse(1){"system"}} — ${p.getOrElse(2){"blocked"}}" };setTextColor(Color.WHITE);textSize=15f}) }
    private fun requestDeviceAdmin(){ val d=getSystemService(DevicePolicyManager::class.java); val r=ComponentName(this,AdminReceiver::class.java); if(!d.isAdminActive(r)) startActivityForResult(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,r).putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,"Adds uninstall friction while protection is enabled."),43) }
    private fun startVpn(){ val i=VpnService.prepare(this); if(i!=null) startActivityForResult(i,9) else startService(Intent(this,BlockVpnService::class.java)) }
    private fun onboarding(){ AlertDialog.Builder(this).setTitle("Permission setup").setMessage("Accessibility scans watched apps. VPN filters domains. Device Admin discourages uninstall. Overlay catches blocked taps. Notifications keep protection visible. MediaProjection enables reels frame analysis. Android will show each system prompt when you choose the corresponding feature.").setPositiveButton("Accessibility") {_,_->startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}.setNegativeButton("Close",null).show() }
    private fun timePicker(){ TimePickerDialog(this,{_,h,m->AppState.set(this,"schedule",true);toast("Focus starts at %02d:%02d".format(h,m))},22,0,true).show() }
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}

