package com.qwe7002.dingtalk_sms;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;

import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;

import static android.content.Context.MODE_PRIVATE;
import static androidx.core.content.PermissionChecker.checkSelfPermission;

class public_func {
    static final String log_tag = "DingTalk-sms";
    static final String boardcast_stop_service = "com.qwe7002.dingtalk_sms.stop_all";
    static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    static String get_send_phone_number(String phone_number) {
        return phone_number.trim()
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");
    }

    static OkHttpClient get_okhttp_obj() {
        return new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    static boolean is_numeric(String str) {
        for (int i = str.length(); --i >= 0; ) {
            char c = str.charAt(i);
            if (c == '+') {
                continue; //Allowed characters +
            }
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    static void send_sms(Context context, String send_to, String content, int sub_id) {
        android.telephony.SmsManager sms_manager;
        String sim_card = "1";
        if (sub_id == -1) {
            sms_manager = SmsManager.getDefault();
        } else {
            sms_manager = SmsManager.getSmsManagerForSubscriptionId(sub_id);
            sim_card = "2";
        }
        ArrayList<String> divideContents = sms_manager.divideMessage(content);
        ArrayList<PendingIntent> send_receiver_list = new ArrayList<>();
        IntentFilter filter = new IntentFilter("send_sms");
        BroadcastReceiver receiver = new sms_send_receiver();
        context.getApplicationContext().registerReceiver(receiver, filter);
        Intent sent_intent = new Intent("send_sms");
        sent_intent.putExtra("sim_card", sim_card);
        sent_intent.putExtra("send_to", send_to);
        sent_intent.putExtra("content", content);
        PendingIntent sentIntent = PendingIntent.getBroadcast(context, 0, sent_intent, PendingIntent.FLAG_CANCEL_CURRENT);
        send_receiver_list.add(sentIntent);
        sms_manager.sendMultipartTextMessage(send_to, null, divideContents, send_receiver_list, null);
    }
    static void send_fallback_sms(String send_to, String content, int sub_id) {
        android.telephony.SmsManager sms_manager;
        if (sub_id == -1) {
            sms_manager = SmsManager.getDefault();
        } else {
            sms_manager = SmsManager.getSmsManagerForSubscriptionId(sub_id);
        }
        ArrayList<String> divideContents = sms_manager.divideMessage(content);
        sms_manager.sendMultipartTextMessage(send_to, null, divideContents, null, null);
    }


    static Notification get_notification_obj(Context context, String notification_name) {
        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(notification_name, public_func.log_tag,
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(channel);

            notification = new Notification.Builder(context, notification_name)
                    .setAutoCancel(false)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOngoing(true)
                    .setTicker(context.getString(R.string.app_name))
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(notification_name + context.getString(R.string.service_is_running))
                    .build();
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notification = new Notification.Builder(context)
                    .setAutoCancel(false)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOngoing(true)
                    .setTicker(context.getString(R.string.app_name))
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(notification_name + context.getString(R.string.service_is_running))
                    .setPriority(Notification.PRIORITY_MIN)
                    .build();
        }
        return notification;
    }

    static void stop_all_service(Context context) {
        Intent intent = new Intent(boardcast_stop_service);
        context.sendBroadcast(intent);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static void start_service(Context context, Boolean battery_switch) {
        Intent battery_service = new Intent(context, battery_monitoring_service.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (battery_switch) {
                context.startForegroundService(battery_service);
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (battery_switch) {
                context.startService(battery_service);
            }
        }
    }


    static int get_active_card(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return -1;
        }
        return SubscriptionManager.from(context).getActiveSubscriptionInfoCount();
    }

    static String get_sim_name_title(Context context, SharedPreferences sharedPreferences, int slot) {
        String result = "";
        if (sharedPreferences.getBoolean("display_dual_sim_display_name", false)) {
            result = "(" + get_sim_display_name(context, slot) + ")";
        }
        return result;
    }

    private static String get_sim_display_name(Context context, int slot) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return "Unknown";
        }
        SubscriptionInfo info = SubscriptionManager.from(context).getActiveSubscriptionInfoForSimSlotIndex(slot);
        if (info == null) {
            if (get_active_card(context) == 1 && slot == 0) {
                info = SubscriptionManager.from(context).getActiveSubscriptionInfoForSimSlotIndex(1);
            }
        }
        assert info != null;
        String result = info.getDisplayName().toString();
        if (info.getDisplayName().toString().contains("CARD") || info.getDisplayName().toString().contains("SUB")) {
            result = info.getCarrierName().toString();
        }
        return result;
    }

    static String get_contact_name(Context context, String phone_number) {
        String contact_name = null;
        if (checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PermissionChecker.PERMISSION_GRANTED) {
            try {
                Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone_number));
                String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};
                Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        String cursor_name = cursor.getString(0);
                        if (!cursor_name.isEmpty())
                            contact_name = cursor_name;
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return contact_name;
    }

    static void write_log(Context context, String log) {
        Log.i(public_func.log_tag, log);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date ts = new Date(System.currentTimeMillis());
        String error_log = read_file(context) + "\n" + simpleDateFormat.format(ts) + " " + log;
        write_file(context, "error.log", error_log);
    }

    static String read_log(Context context) {
        return read_file(context);
    }

    static void write_file(Context context, String file_name, String write_string) {
        try {
            FileOutputStream file_stream = context.openFileOutput(file_name, MODE_PRIVATE);
            byte[] bytes = write_string.getBytes();
            file_stream.write(bytes);
            file_stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String read_file(Context context) {
        String result = "";
        try {
            FileInputStream file_stream = context.openFileInput("error.log");
            int length = file_stream.available();
            byte[] buffer = new byte[length];
            file_stream.read(buffer);
            result = new String(buffer, StandardCharsets.UTF_8);
            file_stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;

    }
}
