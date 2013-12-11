package com.aastle.fittimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.joda.time.DateTime;


public class MainActivity extends Activity {
    StopWatch stopWatch;
    Button buttonStopWatch;
    Button resetButton;
    Button statsButton;
    Drawable shapePaused;
    Drawable shapeStart;
    Drawable shapeStats;
    private static final String TAG = "SQL";
    private static final String DATABASE_NAME = "trimtimer.s3db";
    private static final String TABLE_NAME = "times";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stopWatch = (StopWatch) findViewById(R.id.stopwatch);
        shapePaused = getResources().getDrawable(R.drawable.shape_circle_stop_start_paused);
        shapeStart = getResources().getDrawable(R.drawable.shape_circle_stop_start);
        shapeStats = getResources().getDrawable(R.drawable.shape_stats_circle);

        buttonStopWatch = (Button) findViewById(R.id.buttonStartStop);
        buttonStopWatch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!stopWatch.running()) {
                    stopWatch.startClock();
                    buttonStopWatch.setText("PAUSE");
                    buttonStopWatch.setTextSize(50);
                    buttonStopWatch.setBackground(shapeStart);
                    long id = saveTime(TABLE_NAME,getDate(),getTime(),"running");
                    //Log.e(TAG,"!stopWatch.running, INSERT id = "+ id);
                } else if (stopWatch.running()) {
                    stopWatch.pauseClock();
                    buttonStopWatch.setText("RESUME");
                    buttonStopWatch.setTextSize(40);
                    buttonStopWatch.setBackground(shapePaused);
                    // TODO record time elapsed and date to sqlite db
                    long id = saveTime(TABLE_NAME,getDate(),getTime(),"paused");
                    //Log.e(TAG,"stopWatch.running, INSERT id: "+id);

                }
            }
        });
        resetButton = (Button) findViewById(R.id.buttonReset);
        resetButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(stopWatch.running()){
                    stopWatch.resetClock();
                    buttonStopWatch.setText("PAUSE");
                    buttonStopWatch.setTextSize(50);
                    buttonStopWatch.setBackground(shapeStart);
                    stopWatch.startClock();
                    //Log.e(TAG,getTimeFromSqlite());
                }else if(!stopWatch.running()){
                    stopWatch.resetClock();
                    buttonStopWatch.setText("START");
                    buttonStopWatch.setTextSize(50);
                    buttonStopWatch.setBackground(shapeStart);
                }

            }
        });
        statsButton = (Button)findViewById(R.id.buttonStats);
        statsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogBox(getStats());
            }
        });
    }
    private String getDate(){
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        return dateFormat.format(date);
    }
    private String getTime(){
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this,SettingsActivity.class));
                return true;
            case R.id.about:
                showDialogBox(buildAboutInfo());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private long saveTime(String table, String date, String time, String appState){
        DatabaseHelper dbHelper = new DatabaseHelper(getBaseContext(),DATABASE_NAME,null,1);
        long id = dbHelper.insertTime(table,date,time,appState);
        return id;
    }

    private String getStats(){
        Cursor statsCursor = getTimeFromSqlite();
        return buildStats(statsCursor);
    }

    private Cursor getTimeFromSqlite(){

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT _id,appstate,date,time FROM ");
        sqlBuilder.append(TABLE_NAME);
        sqlBuilder.append(" WHERE date >= date()");
        DatabaseHelper databaseHelper = new DatabaseHelper(getBaseContext(),DATABASE_NAME,null,1);
        //Log.e(TAG,"Sqlite table times.rowCount = "+databaseHelper.getCountOfTableRows(TABLE_NAME));
        databaseHelper.setTableName(TABLE_NAME);
        Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(sqlBuilder.toString(), null);
        return cursor;
    }
    private String buildStats(Cursor cursor){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat simpleHumanDateFormat = new SimpleDateFormat("MMMM d',' yyyy");
        SimpleDateFormat simpleTimeFormatInput = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat simpleHumanTimeFormat = new SimpleDateFormat("H:mm:ss");
        StringBuilder stringBuilder = new StringBuilder();

        cursor.moveToFirst();
        while (cursor.isAfterLast() == false)
        {
            Date date = null;
            Date time = null;
            try{
                date = simpleDateFormat.parse(cursor.getString(cursor.getColumnIndex("date")));
                time = simpleTimeFormatInput.parse(cursor.getString(cursor.getColumnIndex("time")));
            }catch (ParseException pe){
                Log.e(TAG,pe.getMessage());
            }
            stringBuilder.append(simpleHumanDateFormat.format(date));
            stringBuilder.append(" \n");
            stringBuilder.append(simpleHumanTimeFormat.format(time));
            stringBuilder.append(" \n");
            stringBuilder.append(cursor.getString(cursor.getColumnIndex("appstate")));
            stringBuilder.append(" \n");
            cursor.moveToNext();
        }
        //Log.d("SQL",sqlBuilder.toString());
        return stringBuilder.toString();
    }

    private String buildAboutInfo(){
        String packageName = "";
        String versionName = "";

        Long time = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        int year = calendar.get(Calendar.YEAR);

        int versionCode = 0;
        try{
            packageName = getPackageName();
            versionCode = getPackageManager().getPackageInfo(packageName, 0).versionCode;
            versionName = getPackageManager().getPackageInfo(packageName, 0).versionName;
        }catch (Exception e){
            Log.e(TAG,"Package Name not Found :(");
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Trim Timer\n");
        stringBuilder.append("Version: ");
        stringBuilder.append(versionCode);
        stringBuilder.append("\nBuild: ");
        stringBuilder.append(versionName);
        stringBuilder.append("\nCopyright ");
        stringBuilder.append(year);
        stringBuilder.append(" by Alan W. Astle");
        return stringBuilder.toString();

    }
    private void showDialogBox(CharSequence about){
        new AlertDialog.Builder(this)
                .setMessage(about)
                .setPositiveButton("OK",null)
                .show();

    }


}
