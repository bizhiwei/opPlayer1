package com.lenovo.vrplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends Activity {
	
	ListView lvFiles;
	ArrayList<HashMap<String, Object>> listItem;
	SimpleAdapter listItemAdapter;
	File curFile;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		lvFiles = (ListView)findViewById(R.id.listViewFiles);
		
		listItem = new ArrayList<HashMap<String, Object>>();
		listItemAdapter = new SimpleAdapter(this, 
											listItem,
											R.layout.listview_item,
											new String[]{"ItemImage", "ItemText", "ItemInfo"},
											new int[]{R.id.ItemImage, R.id.ItemText, R.id.ItemInfo});
		
		lvFiles.setAdapter(listItemAdapter);		
		
		ShowFolder(Environment.getExternalStorageDirectory());
		
		lvFiles.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
									long arg3) {

				String path = (String) listItem.get(arg2).get("ItemText");
				setTitle(path);
				ShowFolder(new File(curFile.getAbsolutePath() + "/" + path));
			}

		});


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
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private boolean isVideoFile(String fileName)
	{
		int lastDotIndex = fileName.lastIndexOf(".");
		if (lastDotIndex > -1)
		{
			String[] VideoTypeArray = {"MKV", "MP4", "FLV", "AVI", "MOV"};
			
			String fileExtName = fileName.substring(lastDotIndex+1);
			
			for (int i=0; i<VideoTypeArray.length; i++)
			{
				if (fileExtName.equalsIgnoreCase(VideoTypeArray[i]))
					return true;
			}
		}

		return false;
	}
	
	private void ShowFolder(File path)
	{
		if (!path.isDirectory())
		{
			Intent intent = new Intent();
			intent.setClass(MainActivity.this, MediaPlayActivity.class);
			intent.putExtra("filePath", path.getAbsolutePath());
			startActivity(intent);
			
			return;
		}
		
		File[] files = path.listFiles();
		
		listItem.clear();
		for (File file:files)
		{
			if (file.isDirectory())
			{
				HashMap<String, Object> map = new HashMap<String, Object>();
				map.put("ItemImage", R.drawable.ic_folder);
				map.put("ItemText", file.getName());
				Date date = new Date(file.lastModified());
				map.put("ItemInfo", date.toString());
				listItem.add(map);
			}
			else if (isVideoFile(file.getName()))
			{
				HashMap<String, Object> map = new HashMap<String, Object>();
				map.put("ItemImage", android.R.drawable.ic_media_play);
				map.put("ItemText", file.getName());
				Date date = new Date(file.lastModified());
				map.put("ItemInfo", date.toString());
				listItem.add(map);
			}
		}
		
		Collections.sort(listItem, new Comparator<HashMap<String, Object>>() {

			@Override
			public int compare(HashMap<String, Object> lhs, HashMap<String, Object> rhs) {
				return ((String) lhs.get("ItemText")).compareTo((String) rhs.get("ItemText"));
			}
		});
		
		listItemAdapter.notifyDataSetChanged();
		curFile = path;
	}
}
