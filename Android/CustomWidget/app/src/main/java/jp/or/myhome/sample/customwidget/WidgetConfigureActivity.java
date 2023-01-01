package jp.or.myhome.sample.customwidget;

import androidx.appcompat.app.AppCompatActivity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;

public class WidgetConfigureActivity extends AppCompatActivity implements View.OnClickListener, ColorPickerDialogListener, SeekBar.OnSeekBarChangeListener {
    public static final String TAG = MainActivity.TAG;
    JSONObject json;
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_configure);

        setResult(RESULT_CANCELED);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null)
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        try{
            json = loadPreference(this, mAppWidgetId);
            EditText edit;
            edit = (EditText)findViewById(R.id.edit_config_text);
            edit.setText(json.getString("title_text"));
            TextView text;
            int color;
            text = (TextView) findViewById(R.id.txt_config_text_color);
            color = json.getInt("title_color");
            text.setText("#" + String.format("%08X", color));
            text.setTextColor(color);
            text = (TextView) findViewById(R.id.txt_config_background_color);
            color = json.getInt("background_color");
            text.setText("#" + String.format("%08X", color));
            text.setTextColor(color);
            int size = json.getInt("title_size");
            SeekBar seek;
            seek = (SeekBar)findViewById(R.id.seek_config_text_size);
            seek.setProgress(size);
            updateFontImage();
        }catch(Exception ex){
            Log.d(TAG, ex.getMessage());
        }

        Button btn;
        btn = (Button)findViewById(R.id.btn_config_add);
        btn.setOnClickListener(this);
        btn = (Button)findViewById(R.id.btn_config_text_color);
        btn.setOnClickListener(this);
        btn = (Button)findViewById(R.id.btn_config_background_color);
        btn.setOnClickListener(this);
        SeekBar seek;
        seek = (SeekBar)findViewById(R.id.seek_config_text_size);
        seek.setOnSeekBarChangeListener(this);
    }

    public static JSONObject loadPreference(Context context, int appWidgetId) throws Exception{
        SharedPreferences pref = context.getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE);
        String jsonString = pref.getString(MainActivity.PREF_ID_PREFIX + appWidgetId, null);
        if (jsonString == null) {
            JSONObject json = new JSONObject();
            json.put("title_text", "");
            json.put("title_color", Color.WHITE);
            json.put("background_color", Color.GREEN);
            json.put("title_size", 24);
            return json;
        } else {
            return new JSONObject(jsonString);
        }
    }

    public static void savePreference(Context context, int mAppWidgetId, JSONObject json){
        SharedPreferences pref = context.getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(MainActivity.PREF_ID_PREFIX + mAppWidgetId, json.toString());
        editor.apply();
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.btn_config_add:{
                try {
                    EditText edit;
                    edit = (EditText) findViewById(R.id.edit_config_text);
                    String title = edit.getText().toString();

                    json.put("title_text", title);
                    savePreference(this, mAppWidgetId, json);

                    new ProgressAsyncTaskManager.Callback( this, "通信中です。", null )
                    {
                        @Override
                        public Object doInBackground(Object obj) throws Exception {
                            SharedPreferences pref = getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE);
                            String uuid = pref.getString(MainActivity.PREF_UUID, "");
                            String base_url = pref.getString(MainActivity.PREF_BASEURL, "");
                            JSONObject request = new JSONObject();
                            request.put("uuid", uuid);
                            request.put("widget_id", mAppWidgetId);
                            request.put("title", title);
                            request.put("model", Build.MODEL);
                            JSONObject response = HttpPostJson.doPost(base_url + "/widget-add", request, MainActivity.DEFAULT_TIMEOUT);
                            Log.d(TAG, "HttpPostJson OK");
                            return response;
                        }

                        @Override
                        public void doPostExecute(Object obj) {
                            if (obj instanceof Exception) {
                                Toast.makeText(getApplicationContext(), ((Exception)obj).getMessage(), Toast.LENGTH_LONG).show();
                                return;
                            }

                            JSONObject response = (JSONObject)obj;
                            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
                            CustomAppWidget.updateAppWidget(getApplicationContext(), appWidgetManager, mAppWidgetId);

                            Intent resultValue = new Intent();
                            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                            setResult(RESULT_OK, resultValue);
                            finish();
                        }
                    };
                }catch(Exception ex){
                    Log.d(TAG, ex.getMessage());
                }
                break;
            }
            case R.id.btn_config_text_color:{
                try {
                    ColorPickerDialog.newBuilder()
                            .setDialogTitle(R.string.color_select_description)
                            .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                            .setAllowPresets(false)
                            .setDialogId(R.id.btn_config_text_color)
                            .setColor(json.getInt("title_color"))
                            .setShowAlphaSlider(false)
                            .show(this);
                }catch(Exception ex){
                    Log.d(TAG, ex.getMessage());
                }
                break;
            }
            case R.id.btn_config_background_color:{
                try {
                    ColorPickerDialog.newBuilder()
                            .setDialogTitle(R.string.color_select_description)
                            .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                            .setAllowPresets(false)
                            .setDialogId(R.id.btn_config_background_color)
                            .setColor(json.getInt("background_color"))
                            .setShowAlphaSlider(true)
                            .show(this);
                }catch(Exception ex){
                    Log.d(TAG, ex.getMessage());
                }
                break;
            }
        }
    }

    @Override
    public void onColorSelected(int dialogId, int color) {
        Log.d(TAG, "onColorSelected() called with: dialogId = [" + dialogId + "], color = [" + color + "]");
        switch (dialogId) {
            case R.id.btn_config_text_color: {
                try {
                    json.put("title_color", color);
                    TextView text;
                    text = (TextView) findViewById(R.id.txt_config_text_color);
                    text.setText("#" + String.format("%08X", color));
                    text.setTextColor(color);
                    updateFontImage();
                }catch(Exception ex){
                    Log.d(TAG, ex.getMessage());
                }
                break;
            }
            case R.id.btn_config_background_color: {
                try {
                    json.put("background_color", color);
                    TextView text;
                    text = (TextView) findViewById(R.id.txt_config_background_color);
                    text.setText("#" + String.format("%08X", color));
                    text.setTextColor(color);
                    updateFontImage();
                }catch(Exception ex){
                    Log.d(TAG, ex.getMessage());
                }
                break;
            }
        }
    }

    private void updateFontImage(){
        try{
            int progress = json.getInt("title_size");
            int foreColor = json.getInt("title_color");
            int backColor = json.getInt("background_color");
            TextView text;
            text = (TextView)findViewById(R.id.txt_config_text_size);
            text.setText(String.valueOf(progress) + "sp");
            text.setTextSize(progress);
            text.setTextColor(foreColor);
            text.setBackgroundColor(backColor);
        }catch(Exception ex){
            Log.d(TAG, ex.getMessage());
        }
    }

    @Override
    public void onDialogDismissed(int dialogId) {
        Log.d(TAG, "onDialogDismissed() called with: dialogId = [" + dialogId + "]");
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        try {
            json.put("title_size", i);
            updateFontImage();
        }catch(Exception ex){
            Log.d(TAG, ex.getMessage());
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}