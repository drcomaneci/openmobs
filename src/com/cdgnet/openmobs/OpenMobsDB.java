package com.cdgnet.openmobs;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class OpenMobsDB extends SQLiteOpenHelper {

	  private static final String DATABASE_NAME = "openmobs.db";
	  private static final int DATABASE_VERSION = 1;

	  // Database creation sql statement
	  private static final String DATABASE_CREATE = 
			  "create table config(_id integer primary key autoincrement, price1 float, price2 float);";
	  private static final String DROP_DATABASE =
			  "DROP TABLE IF EXISTS config;";

	  public OpenMobsDB(Context context) {
	    super(context, DATABASE_NAME, null, DATABASE_VERSION);
	  }

	  @Override
	  public void onCreate(SQLiteDatabase database) {
	    database.execSQL(DATABASE_CREATE);
	  }

	  @Override
	  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	    Log.w(OpenMobsDB.class.getName(),
	        "Upgrading database from version " + oldVersion + " to "
	            + newVersion + ", which will destroy all old data");
	    db.execSQL(DROP_DATABASE);
	    onCreate(db);
	  }

} 
