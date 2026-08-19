package com.example.callsentry;

import android.app.*;import android.app.role.RoleManager;import android.content.*;import android.os.*;import android.provider.Settings;import android.view.*;import android.widget.*;

public class MainActivity extends Activity {
 TextView status,stats; ScamDb db;
 @Override public void onCreate(Bundle b){super.onCreate(b);db=new ScamDb(this);build(); SyncJob.schedule(this); new Thread(()->{int n=FtcSync.sync(this,14);runOnUiThread(()->refresh("Updated FTC data: "+n+" reports imported"));}).start();}
 void build(){ScrollView sv=new ScrollView(this);LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(36,40,36,40);sv.addView(l);
  TextView title=t("CallSentry",30);l.addView(title);l.addView(t("Scam call screening using FTC complaint data + carrier caller verification. Numbers stay on this phone.",17));
  status=t("",18);l.addView(status); Button enable=new Button(this);enable.setText("Enable call screening");enable.setOnClickListener(v->enableRole());l.addView(enable);
  stats=t("",16);l.addView(stats); Button sync=new Button(this);sync.setText("Refresh scam database now");sync.setOnClickListener(v->new Thread(()->{int n=FtcSync.sync(this,30);runOnUiThread(()->refresh("Refresh complete: "+n+" new reports"));}).start());l.addView(sync);
  l.addView(t("Blocking policy",20));l.addView(t("Calls are blocked when a number has 3+ FTC complaints in the last 30 days, or when a failed carrier verification is corroborated by an FTC complaint. A single report is treated as suspicious but allowed. This reduces false positives.",16));
  Button settings=new Button(this);settings.setText("Android caller ID / spam settings");settings.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));}catch(Exception ignored){}});l.addView(settings);
  setContentView(sv);refresh("");}
 TextView t(String s,int z){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setPadding(0,12,0,12);return v;}
 void enableRole(){if(Build.VERSION.SDK_INT>=29){RoleManager rm=getSystemService(RoleManager.class);if(rm!=null&&rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)){startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING),42);}}}
 @Override protected void onResume(){super.onResume();refresh("");}
 void refresh(String msg){boolean held=false;if(Build.VERSION.SDK_INT>=29){RoleManager rm=getSystemService(RoleManager.class);held=rm!=null&&rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);}status.setText((held?"✓ CallSentry is ACTIVE":"CallSentry is not yet the active screening app")+(msg.isEmpty()?"":"\n"+msg));stats.setText("Local reputation database: "+db.totalReports()+" FTC complaint records\nLast refresh: "+db.lastSyncText());}
}
