package com.qwe7002.dingtalk_sms;

import android.content.Context;
import android.os.Bundle;
import android.os.FileObserver;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

public class logcat_activity extends AppCompatActivity {

    Context context;
    file_observer observer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logcat);
        TextView logcat = findViewById(R.id.logcat_view);
        this.setTitle(R.string.logcat);
        context = getApplicationContext();
        logcat.setText(public_func.read_log(context));
        observer = new file_observer(context, logcat);

    }
    @Override
    public void onPause() {
        super.onPause();
        observer.stopWatching();
    }

    @Override
    public void onResume() {
        super.onResume();
        observer.startWatching();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.logcat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        public_func.write_file(context, "error.log", "");
        return true;
    }

    class file_observer extends FileObserver {
        private Context context;
        private TextView logcat;

        file_observer(Context context, TextView logcat) {
            super(context.getFilesDir().getAbsolutePath());
            this.context = context;
            this.logcat = logcat;
        }

        @Override
        public void onEvent(int event, String path) {
            if (event == FileObserver.MODIFY) {
                runOnUiThread(() -> logcat.setText(public_func.read_log(context)));
            }

        }
    }

}


