package jp.or.myhome.sample.customwidget;

import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "LogTag";
    public static final String PREF_NAME = "Private";
    public static final String PREF_BASEURL = "customwidget_baseurl";
    public static final String PREF_UUID = "customwidget_uuid";
    public static final String PREF_ID_PREFIX = "customwidget_id_";
    public static final String CHANNEL_ID_PROCESS = "widget_processing";
    public static final String CHANNEL_ID_FINISH = "widget_finished";
    public static final String ACTION_NAME = "customwidget_doAction";
    public static final String DEFAULT_BASE_URL = "https://【Node.jsサーバのURL】";
    public static final int DEFAULT_TIMEOUT = 10000;
    SharedPreferences pref;
    String uuid;
    String base_url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pref = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        base_url = pref.getString(PREF_BASEURL, null);
        if( base_url == null ) {
            base_url = DEFAULT_BASE_URL;
            SharedPreferences.Editor editor = pref.edit();
            editor.putString(PREF_BASEURL, base_url);
            editor.apply();
        }
        uuid = pref.getString(PREF_UUID, null);
        if( uuid == null ) {
            uuid = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = pref.edit();
            editor.putString(PREF_UUID, uuid);
            editor.apply();
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID_PROCESS,"処理中チャネル", NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(channel);
        NotificationChannel channel2 = new NotificationChannel(CHANNEL_ID_FINISH,"処理完了チャネル", NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(channel2);

        TextView text;
        text = (TextView)findViewById(R.id.txt_config_uuid);
        text.setText(uuid);
        text = (TextView)findViewById(R.id.txt_config_model);
        text.setText(Build.MODEL);
        EditText edit;
        edit = (EditText)findViewById(R.id.edit_config_urlbase);
        edit.setText(base_url);
        Button btn;
        btn = (Button)findViewById(R.id.btn_config_update);
        btn.setOnClickListener(this);
        ImageButton imgbtn;
        imgbtn = (ImageButton)findViewById(R.id.imgbtn_view_console);
        imgbtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.btn_config_update:{
                EditText edit;
                edit = (EditText)findViewById(R.id.edit_config_urlbase);
                String base_url = edit.getText().toString();
                SharedPreferences.Editor editor = pref.edit();
                editor.putString(PREF_BASEURL, base_url);
                editor.apply();

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("設定しました。");
                builder.setPositiveButton("閉じる", null);
                builder.create().show();
                break;
            }
            case R.id.btn_config_reset:{
                EditText edit;
                edit = (EditText)findViewById(R.id.edit_config_urlbase);
                edit.setText(base_url);
                break;
            }
            case R.id.imgbtn_view_console:{
                Uri uri = Uri.parse(base_url + "/widget_console");
                Intent intent = new Intent(Intent.ACTION_VIEW,uri);
                startActivity(intent);
                break;
            }
        }
    }
}