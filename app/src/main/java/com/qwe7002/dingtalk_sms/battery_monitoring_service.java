package com.qwe7002.dingtalk_sms;

import android.Manifest;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.PermissionChecker;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.Context.BATTERY_SERVICE;
import static androidx.core.content.PermissionChecker.checkSelfPermission;

public class battery_monitoring_service extends Service {
    private battery_receiver battery_receiver = null;
    private stop_broadcast_receiver stop_broadcast_receiver = null;
    static String bot_token;
    static Boolean fallback;
    static String trusted_phone_number;
    Context context;
    static SharedPreferences sharedPreferences;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sharedPreferences = context.getSharedPreferences("data", MODE_PRIVATE);
        Notification notification = public_func.get_notification_obj(context, getString(R.string.Battery_monitoring));
        startForeground(1, notification);
        if (!sharedPreferences.getBoolean("battery_monitoring_switch", false)) {
            Log.d("stop", "onStartCommand: ");
            stopSelf();
        }
        bot_token = sharedPreferences.getString("bot_token", "");
        fallback = sharedPreferences.getBoolean("fallback_sms", false);
        trusted_phone_number = sharedPreferences.getString("trusted_phone_number", null);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        IntentFilter intentFilter = new IntentFilter(public_func.boardcast_stop_service);
        stop_broadcast_receiver = new stop_broadcast_receiver();

        battery_receiver = new battery_receiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_OKAY);
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);

        registerReceiver(battery_receiver, filter);
        registerReceiver(stop_broadcast_receiver, intentFilter);

    }

    @Override
    public void onDestroy() {
        unregisterReceiver(battery_receiver);
        unregisterReceiver(stop_broadcast_receiver);
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class stop_broadcast_receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopSelf();
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

}

class battery_receiver extends BroadcastReceiver {
    OkHttpClient okhttp_client = public_func.get_okhttp_obj();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final request_json request_body = new request_json();
        StringBuilder prebody = new StringBuilder(context.getString(R.string.system_message_head) + "\n");
        final String action = intent.getAction();
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(BATTERY_SERVICE);
        switch (Objects.requireNonNull(action)) {
            case Intent.ACTION_BATTERY_OKAY:
                prebody = prebody.append(context.getString(R.string.low_battery_status_end));
                break;
            case Intent.ACTION_BATTERY_LOW:
                prebody = prebody.append(context.getString(R.string.battery_low));
                break;
            case Intent.ACTION_POWER_CONNECTED:
                prebody = prebody.append(context.getString(R.string.ac_connect));
                break;
            case Intent.ACTION_POWER_DISCONNECTED:
                prebody = prebody.append(context.getString(R.string.ac_disconnect));
                break;
        }
        assert batteryManager != null;
        String Content = prebody.append("\n").append(context.getString(R.string.current_battery_level)).append(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)).append("%").toString();
        JsonObject object = new JsonObject();
        object.addProperty("content",Content);
        request_body.text =object;
        Gson gson = new Gson();
        String request_body_raw = gson.toJson(request_body);
        RequestBody body = RequestBody.create(request_body_raw,public_func.JSON);
        Request request = new Request.Builder().url(battery_monitoring_service.bot_token).method("POST", body).build();
        Call call = okhttp_client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                String error_message = "Send battery info error:" + e.getMessage();
                public_func.write_log(context, error_message);
                if (action.equals(Intent.ACTION_BATTERY_LOW)) {
                    if (checkSelfPermission(context, Manifest.permission.SEND_SMS) == PermissionChecker.PERMISSION_GRANTED) {
                        if (battery_monitoring_service.fallback) {
                            String msg_send_to = battery_monitoring_service.trusted_phone_number;
                            if (msg_send_to != null) {
                                public_func.send_fallback_sms(msg_send_to, Content, -1);
                            }
                        }
                    }
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.code() != 200) {
                    assert response.body() != null;
                    String error_message = "Send battery info error:" + Objects.requireNonNull(response.body()).string();
                    public_func.write_log(context, error_message);
                }
            }
        });


    }
}
