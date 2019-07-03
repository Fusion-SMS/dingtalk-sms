package com.qwe7002.dingtalk_sms;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;


public class main_activity extends AppCompatActivity {
    Context context = null;


    @SuppressLint("BatteryLife")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        final EditText bot_token = findViewById(R.id.bot_token);
        final EditText trusted_phone_number = findViewById(R.id.trusted_phone_number);
        final Switch fallback_sms = findViewById(R.id.fallback_sms);
        final Switch battery_monitoring_switch = findViewById(R.id.battery_monitoring);
        final Switch display_dual_sim_display_name = findViewById(R.id.display_dual_sim);
        final SharedPreferences sharedPreferences = getSharedPreferences("data", MODE_PRIVATE);
        String bot_token_save = sharedPreferences.getString("bot_token", "");
        if (sharedPreferences.getBoolean("initialized", false)) {
            public_func.start_service(context, sharedPreferences.getBoolean("battery_monitoring_switch", false));
        }
        boolean display_dual_sim_display_name_config = sharedPreferences.getBoolean("display_dual_sim_display_name", false);
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            if (public_func.get_active_card(context) < 2) {
                display_dual_sim_display_name.setEnabled(false);
                display_dual_sim_display_name_config = false;
            }
            display_dual_sim_display_name.setChecked(display_dual_sim_display_name_config);
        }
        Button save_button = findViewById(R.id.save);
        Button logcat = findViewById(R.id.logcat_button);

        bot_token.setText(bot_token_save);

        trusted_phone_number.setText(sharedPreferences.getString("trusted_phone_number", ""));
        battery_monitoring_switch.setChecked(sharedPreferences.getBoolean("battery_monitoring_switch", false));
        fallback_sms.setChecked(sharedPreferences.getBoolean("fallback_sms", false));

        logcat.setOnClickListener(v -> {
            Intent logcat_intent = new Intent(main_activity.this, logcat_activity.class);
            startActivity(logcat_intent);
        });

        save_button.setOnClickListener(v -> {

            if (bot_token.getText().toString().isEmpty()) {
                Snackbar.make(v, R.string.token_not_configure, Snackbar.LENGTH_LONG).show();
                return;
            }
            if (fallback_sms.isChecked() && trusted_phone_number.getText().toString().isEmpty()) {
                Snackbar.make(v, R.string.trusted_phone_number_empty, Snackbar.LENGTH_LONG).show();
                return;
            }
            ActivityCompat.requestPermissions(main_activity.this, new String[]{Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS}, 1);

            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                assert powerManager != null;
                boolean has_ignored = powerManager.isIgnoringBatteryOptimizations(getPackageName());
                if (!has_ignored) {
                    Intent intent = new Intent(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    if (intent.resolveActivityInfo(getPackageManager(), PackageManager.MATCH_DEFAULT_ONLY) != null) {
                        startActivity(intent);
                    }
                }
            }
            final ProgressDialog progress_dialog = new ProgressDialog(main_activity.this);
            progress_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress_dialog.setTitle(getString(R.string.connect_wait_title));
            progress_dialog.setMessage(getString(R.string.connect_wait_message));
            progress_dialog.setIndeterminate(false);
            progress_dialog.setCancelable(false);
            progress_dialog.show();
            String request_uri = bot_token.getText().toString().trim();
            request_json request_body = new request_json();
            String Content= getString(R.string.system_message_head) + "\n" + getString(R.string.success_connect);
            JsonObject object = new JsonObject();
            object.addProperty("content",Content);
            request_body.text =object;
            Gson gson = new Gson();
            String request_body_raw = gson.toJson(request_body);
            RequestBody body = RequestBody.create(request_body_raw,public_func.JSON);
            OkHttpClient okhttp_client = public_func.get_okhttp_obj();
            Request request;
            try {
                request = new Request.Builder().url(request_uri).method("POST", body).build();
            }catch (java.lang.IllegalArgumentException e){
                progress_dialog.cancel();
                Snackbar.make(v, Objects.requireNonNull(e.getMessage()), Snackbar.LENGTH_LONG)
                        .show();
                return;
            }
            Call call = okhttp_client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Looper.prepare();
                    progress_dialog.cancel();
                    String error_message = "Send Message Network Error:" + e.getMessage();
                    public_func.write_log(context, error_message);
                    Snackbar.make(v, error_message, Snackbar.LENGTH_LONG)
                            .show();
                    Looper.loop();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    Looper.prepare();
                    progress_dialog.cancel();
                    if (response.code() != 200) {
                        assert response.body() != null;
                        String result = Objects.requireNonNull(response.body()).string();
                        JsonObject result_obj = new JsonParser().parse(result).getAsJsonObject();
                        String error_message = "Send Message API Error:" + result_obj.get("description");
                        public_func.write_log(context, error_message);
                        Snackbar.make(v, error_message, Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    if (!bot_token.getText().toString().trim().equals(bot_token_save)) {
                        public_func.write_file(context, "message.json", "{}");
                    }
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("bot_token", bot_token.getText().toString().trim());
                    editor.putString("trusted_phone_number", trusted_phone_number.getText().toString().trim());
                    editor.putBoolean("fallback_sms", fallback_sms.isChecked());
                    editor.putBoolean("battery_monitoring_switch", battery_monitoring_switch.isChecked());
                    editor.putBoolean("display_dual_sim_display_name", display_dual_sim_display_name.isChecked());
                    editor.putBoolean("initialized", true);
                    editor.apply();
                    Snackbar.make(v, R.string.success, Snackbar.LENGTH_LONG)
                            .show();
                    public_func.stop_all_service(context);
                    public_func.start_service(context, battery_monitoring_switch.isChecked());
                    Looper.loop();


                }
            });
        });
    }

}

