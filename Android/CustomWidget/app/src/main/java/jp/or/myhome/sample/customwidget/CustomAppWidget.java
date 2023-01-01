package jp.or.myhome.sample.customwidget;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;
import androidx.core.app.NotificationCompat;
import org.json.JSONObject;

public class CustomAppWidget extends AppWidgetProvider {
    public static final String TAG = MainActivity.TAG;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Log.d(TAG, "updateAppWidget called");

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.custom_app_widget);
        try{
            JSONObject json = WidgetConfigureActivity.loadPreference(context, appWidgetId);

            Intent clickIntent = new Intent(context, CustomAppWidget.class);
            clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            clickIntent.putExtra("title_text", json.getString("title_text"));
            clickIntent.setAction(MainActivity.ACTION_NAME);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.appwidget_container, pendingIntent);

            views.setTextViewText(R.id.appwidget_text, json.getString("title_text"));
            views.setTextColor(R.id.appwidget_text, json.getInt("title_color"));
            views.setTextViewTextSize(R.id.appwidget_text, TypedValue.COMPLEX_UNIT_SP, json.getInt("title_size"));
            views.setInt(R.id.appwidget_container, "setBackgroundColor", json.getInt("background_color"));
        }catch(Exception ex){
            Log.d(TAG, ex.getMessage());
        }
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "update called");

        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.d(TAG, "onDeleted called");

        // When the user deletes the widget, delete the preference associated with it.
        SharedPreferences pref = context.getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        for (int appWidgetId : appWidgetIds) {
            editor.remove(MainActivity.PREF_ID_PREFIX + appWidgetId);
        }
        editor.apply();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SharedPreferences pref = context.getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE);
                    String uuid = pref.getString(MainActivity.PREF_UUID, null);
                    String base_url = pref.getString(MainActivity.PREF_BASEURL, null);
                    for (int appWidgetId : appWidgetIds) {
                        JSONObject request = new JSONObject();
                        request.put("uuid", uuid);
                        request.put("widget_id", appWidgetId);
                        JSONObject response = HttpPostJson.doPost(base_url + "/widget-delete", request, MainActivity.DEFAULT_TIMEOUT);
                        Log.d(TAG, "HttpPostJson.doPost OK");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive() Action: " + intent.getAction());

        String action = intent.getAction();
        Bundle extras = intent.getExtras();
        if(extras != null) {
            int widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            Log.d(TAG, "onReceive(), widgetId=" + widgetId);

            if (MainActivity.ACTION_NAME.equals(action) && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String title = intent.getStringExtra("title_text");
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID_PROCESS);
                            builder.setContentTitle("カスタムウィジェット");
                            builder.setContentText("「" + title + "」処理中");
                            builder.setSmallIcon(android.R.drawable.ic_popup_reminder);
                            builder.setAutoCancel(false);
                            notificationManager.notify(widgetId, builder.build());

                            SharedPreferences pref = context.getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE);
                            String uuid = pref.getString(MainActivity.PREF_UUID, null);
                            String base_url = pref.getString(MainActivity.PREF_BASEURL, null);
                            JSONObject request = new JSONObject();
                            request.put("uuid", uuid);
                            request.put("widget_id", widgetId);
                            JSONObject response = HttpPostJson.doPost(base_url + "/widget-call", request, MainActivity.DEFAULT_TIMEOUT);
                            Log.d(TAG, "HttpPostJson.doPost OK");

                            NotificationCompat.Builder builder2 = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID_FINISH);
                            builder2.setContentTitle("カスタムウィジェット");
                            builder2.setContentText("「" + title + "」の処理完了");
                            builder2.setSmallIcon(android.R.drawable.ic_popup_reminder);
                            builder2.setAutoCancel(true);
                            notificationManager.notify(0, builder2.build());
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            notificationManager.cancel(widgetId);
                        }
                    }
                });
                thread.start();
            }
        }

        super.onReceive(context, intent);
    }

    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "onEnabled called");

        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        Log.d(TAG, "onDisabled called");

        // Enter relevant functionality for when the last widget is disabled
    }
}