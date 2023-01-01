package jp.or.myhome.sample.customwidget;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;

public class ProgressAsyncTaskManager extends AsyncTask<Object, Void, Object> implements OnClickListener
{
    Callback callback;
    Activity activity;
    boolean isCancel = false;
    ProgressDialog dialog = null;
    static final String default_message = "カードをかざしてください。";
    String message = null;

    public static abstract class Callback
    {
        ProgressAsyncTaskManager manager = null;

        public Callback( Activity activity, String message, Object inputObj )
        {
            manager = new ProgressAsyncTaskManager( this, activity, message );
            manager.execute( inputObj );
        }

        public Callback( Activity activity, Object inputObj )
        {
            manager = new ProgressAsyncTaskManager( this, activity, default_message );
            manager.execute( inputObj );
        }

        public void doCancelExecute(){
        };

        public abstract Object doInBackground( Object inputObj ) throws Exception;
        public abstract void doPostExecute( Object outputObj );
    }

    public ProgressAsyncTaskManager( Callback callback, Activity activity, String message )
    {
        this.callback = callback;
        this.activity = activity;
        this.message = message;
    }

    @Override
    protected void onPreExecute()
    {
        if( message != null )
        {
            dialog = new ProgressDialog( activity );
            dialog.setMessage( message );
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setButton( ProgressDialog.BUTTON_NEGATIVE, "キャンセル", this );
            dialog.setCancelable( false );
            dialog.show();
        }
    }

    @Override
    protected Object doInBackground( Object... objs )
    {
        try{
            return callback.doInBackground( objs[0] );
        }catch( Exception ex )
        {
            return ex;
        }
    }

    @Override
    protected void onCancelled( Object obj )
    {
        if( dialog != null )
            dialog.dismiss();
        callback.doPostExecute( new Exception("キャンセルされました。") );
    }

    @Override
    protected void onPostExecute( Object obj )
    {
        if( dialog != null )
            dialog.dismiss();
        callback.doPostExecute( obj );
    }

    @Override
    public void onClick(DialogInterface dialog, int which )
    {
        isCancel = true;
        try{
            callback.doCancelExecute();
        }catch(Exception ex){}
        cancel( true );
    }
}
