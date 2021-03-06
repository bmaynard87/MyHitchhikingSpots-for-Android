package com.myhitchhikingspots.utilities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.myhitchhikingspots.Constants;
import com.myhitchhikingspots.MyHitchhikingSpotsApplication;
import com.myhitchhikingspots.R;
import com.myhitchhikingspots.interfaces.AsyncTaskListener;
import com.myhitchhikingspots.model.SpotDao;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DatabaseExporter extends AsyncTask<Void, Void, Boolean> {
    private ProgressDialog dialog;
    private Context context;
    private AsyncTaskListener<String> onFinished;
    private final String TAG = "database-exporter";
    private String result = "";

    public DatabaseExporter(Context context) {
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        //Add flag that requests the screen to stay awake so that we prevent it from sleeping and the app doesn't go to background until we release it
        ((Activity) context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Set up progress dialog
        dialog = new ProgressDialog(context);
        this.dialog.setIndeterminate(true);
        this.dialog.setCancelable(false);
        this.dialog.setTitle(context.getString(R.string.settings_exportdb_button_label));
        this.dialog.setMessage(context.getString(R.string.settings_exporting_db_message));
        this.dialog.show();
    }

    protected Boolean doInBackground(Void... params) {
        Crashlytics.log(Log.INFO, TAG, "DatabaseExporter started executing..");
        try {
            MyHitchhikingSpotsApplication appContext = ((MyHitchhikingSpotsApplication) context.getApplicationContext());

            File exportDir = new File(Constants.EXPORTED_DB_STORAGE_PATH);

            if (!exportDir.exists()) {
                Crashlytics.log(Log.INFO, TAG, "Directory created. " + exportDir.getPath());
                exportDir.mkdirs();
            }

            String fileName = Utils.getExportFileName(new Date());

            File destinationFile = new File(exportDir, fileName);
            destinationFile.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(destinationFile));
            Cursor curCSV = appContext.rawQuery("select * from " + SpotDao.TABLENAME, null);
            csvWrite.writeNext(curCSV.getColumnNames());
            while (curCSV.moveToNext()) {
                String[] mySecondStringArray = new String[curCSV.getColumnNames().length];
                for (int i = 0; i < curCSV.getColumnNames().length; i++) {
                    mySecondStringArray[i] = curCSV.getString(i);
                }
                csvWrite.writeNext(mySecondStringArray);
            }
            csvWrite.close();
            curCSV.close();

            //Build "Copied to" string so that the user knows where the exported database file was copied to
            result = String.format(context.getString(R.string.settings_exportdb_finish_successfull_message), destinationFile.getPath());

            return true;
        } catch (IOException e) {
            Crashlytics.logException(e);
        }
        return false;
    }

    protected void onPostExecute(final Boolean success) {

        //Remove the flag that keeps the screen from sleeping
        ((Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (this.dialog.isShowing()) {
            this.dialog.dismiss();
        }

        Crashlytics.setString("doInBackground result", result);

        if (onFinished != null)
            onFinished.notifyTaskFinished(success, result);
    }

    public void addListener(AsyncTaskListener<String> doWhenFinished) {
        onFinished = doWhenFinished;
    }
}