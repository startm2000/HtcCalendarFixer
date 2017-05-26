package com.example.test;

import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.CalendarContract;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

	private String dstFilePath = "/sdcard/DCIM/";
    private static final String TAG = "MainActivity";
    private static final String PKG_NAME = "com.example.test";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button bt1 = (Button) findViewById(R.id.bt1);
        bt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            	String message = CalendarFixerUtil.detectGoogleAccountCalendarEventMissing(MainActivity.this);
            	((Button)view).setText(message);
            }
        });
        
        Button bt2 = (Button) findViewById(R.id.bt2);
        bt2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            	makeIssueHappen();
            }
        });
        
        Button bt3 = (Button) findViewById(R.id.bt3);
        bt3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            	String message = CalendarFixerUtil.fixGoogleAccountCalendarEventMissing(MainActivity.this);
            	((Button)view).setText(message);
            }
        });
        
    }
    
    private void makeIssueHappen() {
		ArrayList<ContentProviderOperation> calendarOperationList = new ArrayList<ContentProviderOperation>();
		ContentProviderOperation.Builder builder = null;
		
		HashMap<Long, String> idNamePair = CalendarFixerUtil.getExistenceGoogleCalendarAccountIDAndNames(getContentResolver());
		if (idNamePair == null || idNamePair.size() <= 0) {
			Log.w(TAG, "No Google account's calendar found");
			Toast.makeText(this, "Failed because there is no Google calendar account.", Toast.LENGTH_SHORT).show();
			return;
		}

		Set<Long> names = idNamePair.keySet();
		String accName = "";
		for (Long id : names) {
			accName = idNamePair.get(id);
			break;
		}
        Uri.Builder builderu = CalendarContract.Calendars.CONTENT_URI.buildUpon();
        builderu.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true");
        builderu.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accName);
        builderu.appendQueryParameter(CalendarContract.EventsEntity.ACCOUNT_TYPE, "com.google");

        builder = ContentProviderOperation.newUpdate(builderu.build());
        builder.withValue(CalendarContract.Calendars.CAL_SYNC1, "0");
        calendarOperationList.add(builder.build());

        try {
			getContentResolver().applyBatch(CalendarContract.AUTHORITY, calendarOperationList);
		} catch (RemoteException | OperationApplicationException e) {
			e.printStackTrace();
		}
        
        Bundle extras = new Bundle();
		extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
		for (Long id : names) {
			android.accounts.Account account = new android.accounts.Account(idNamePair.get(id), "com.google");
			//ContentResolver.addStatusChangeListener(mask, callback)
			ContentResolver.requestSync(account, CalendarContract.AUTHORITY, extras);
		}
		Toast.makeText(this, "Issue happen and sync triggered", Toast.LENGTH_SHORT).show();
    }

}