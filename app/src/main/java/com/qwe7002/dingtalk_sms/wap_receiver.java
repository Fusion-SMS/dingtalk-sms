package com.qwe7002.dingtalk_sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class wap_receiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        public_func.write_log(context, "Received wap message.");
    }
}
