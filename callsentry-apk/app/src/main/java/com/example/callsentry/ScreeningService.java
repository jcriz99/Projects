package com.example.callsentry;

import android.os.*;import android.telecom.*;

public class ScreeningService extends CallScreeningService {
 @Override public void onScreenCall(Call.Details d){
  String raw=d.getHandle()==null?"":d.getHandle().getSchemeSpecificPart();String n=ScamDb.norm(raw);int verify=0;if(Build.VERSION.SDK_INT>=30)verify=d.getCallerNumberVerificationStatus();
  ScamDb db=new ScamDb(this);int reports=db.recentCount(n,30);boolean failed=Build.VERSION.SDK_INT>=30&&verify==Connection.VERIFICATION_STATUS_FAILED;boolean block=reports>=3||(failed&&reports>=1);
  CallResponse.Builder b=new CallResponse.Builder().setSkipCallLog(false).setSkipNotification(false);
  if(block)b.setDisallowCall(true).setRejectCall(true).setSilenceCall(false);else b.setDisallowCall(false).setRejectCall(false).setSilenceCall(false);
  respondToCall(d,b.build());db.log(n,block?"BLOCK":"ALLOW",reports,failed);
 }
}
