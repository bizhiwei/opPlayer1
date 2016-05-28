package com.lenovo.vrplayer;

import android.app.Activity;
//import android.content.res.Resources;
//import android.media.MediaPlayer;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.media.MediaPlayer;

public class MediaPlayActivity extends Activity  implements SensorEventListener {

	private static final String TAG = "MediaPlaybActivity";

	private GLSurfaceView mVideoView = null;
	private GLVideoRender mRender = null;
	private MediaPlayer mMediaPlayer = null;
	
	private SeekBar mSeekBarVideo;
	private ImageButton mButtonPlay;

	SensorManager mSensorManager = null;
	Sensor mRotationVectorSensor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		
		mMediaPlayer = new MediaPlayer();

		try {
			String filePath = getIntent().getStringExtra("filePath");
			mMediaPlayer.setDataSource(filePath);

		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
		}

		setContentView(R.layout.acivity_media_play);
		
		mVideoView = (GLSurfaceView)findViewById(R.id.surfaceMediaPlay);		
		mVideoView.setEGLContextClientVersion(2);
        mRender = new GLVideoRender(this, mMediaPlayer);        
        mVideoView.setRenderer(mRender);        
        
        mButtonPlay = (ImageButton)findViewById(R.id.buttonPlay);
        mSeekBarVideo = (SeekBar)findViewById(R.id.seekBarVideo);
        mSeekBarVideo.setMax(1000);
        
        mButtonPlay.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {				
				if (mMediaPlayer.isPlaying())
				{
					mMediaPlayer.pause();
				}
				else
				{
					mMediaPlayer.start();
				}				
			}        	
        });
        
        mSeekBarVideo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				mMediaPlayer.seekTo(mMediaPlayer.getDuration()*progress/1000);
			}
		});

		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		mRotationVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
		mSensorManager.registerListener(this, mRotationVectorSensor, SensorManager.SENSOR_DELAY_FASTEST);
	}

	
	@Override
	protected void onResume() {
		super.onResume();
		mVideoView.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mMediaPlayer.pause();
		mVideoView.onPause();		
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		mMediaPlayer.stop();	
	}

	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR)
		{
			float[] rotationMatrix = new float[16];
			float[] rotationVector = new float[4];
			rotationVector[0] = event.values[0];
			rotationVector[1] = event.values[1];
			rotationVector[2] = event.values[2];
			rotationVector[3] = event.values[3];
			SensorManager.getRotationMatrixFromVector(rotationMatrix,rotationVector);

			if (mRender != null)
				mRender.updateRotationMatrix(rotationMatrix);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int i) {

	}
}
