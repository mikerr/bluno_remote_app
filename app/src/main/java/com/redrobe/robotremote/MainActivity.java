package com.redrobe.robotremote;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity  extends com.redrobe.robotremote.BlunoLibrary implements SensorEventListener {

	private Button buttonScan;
	private Button buttonSerialSend;
	private EditText serialSendText;
	private TextView serialReceivedText;

	private float mLastX, mLastY, mLastZ;
	private boolean mInitialized;
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private final float NOISE = (float) 0.5;

	private boolean mGyro = false;
	private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.1F); // buttonclicks

	@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
// can be safely ignored for this demo
		}
	private static final String[] BLE_PERMISSIONS = new String[]{
			Manifest.permission.ACCESS_COARSE_LOCATION,
			Manifest.permission.ACCESS_FINE_LOCATION,
	};

	private static final String[] ANDROID_12_BLE_PERMISSIONS = new String[]{
			Manifest.permission.BLUETOOTH_SCAN,
			Manifest.permission.BLUETOOTH_CONNECT,
			Manifest.permission.ACCESS_FINE_LOCATION,
	};

	public static void requestBlePermissions(Activity activity, int requestCode) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
			ActivityCompat.requestPermissions(activity, ANDROID_12_BLE_PERMISSIONS, requestCode);
		else
			ActivityCompat.requestPermissions(activity, BLE_PERMISSIONS, requestCode);
	}
	public void checkpermissions(){
		// need coarse_location for BLE device scanning
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (!(this.checkSelfPermission
					(Manifest.permission.ACCESS_COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED))
			{
				//Toast.makeText(this.getBaseContext(),"Bluetooth scanning needs location access", Toast.).show();
				AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
				dlgAlert.setMessage("Location access required for bluetooth scanning");
				dlgAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						//dismiss the dialog
						//requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
						requestBlePermissions(MainActivity.this,0);
					}
				});
				dlgAlert.show();
			}
		}
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        onCreateProcess();														//onCreate Process by BlunoLibrary
        
        serialBegin(115200);													//set the Uart Baudrate on BLE chip to 115200
		
        serialReceivedText=(TextView) findViewById(R.id.serialReveicedText);	//initial the EditText of the received data
        serialSendText=(EditText) findViewById(R.id.serialSendText);			//initial the EditText of the sending data
        
        buttonSerialSend = (Button) findViewById(R.id.buttonSerialSend);		//initial the button for sending the data
        buttonSerialSend.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				serialSend(serialSendText.getText().toString());				//send the data to the BLUNO
			}
		});
        
        buttonScan = (Button) findViewById(R.id.buttonScan);					//initial the button for scanning the BLE device
        buttonScan.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				buttonScanOnClickProcess();										//Alert Dialog for selecting the BLE device
			}
		});

		mInitialized = false;
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		//if gyro on
		//mSensorManager.registerListener(this, mAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);

		checkpermissions();
	}

	protected void onResume(){
		super.onResume();
		//System.out.println("BlUNOActivity onResume");
		if (mGyro) mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

		onResumeProcess();														//onResume Process by BlunoLibrary
	}

	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		onActivityResultProcess(requestCode, resultCode, data);					//onActivityResult Process by BlunoLibrary
		super.onActivityResult(requestCode, resultCode, data);
	}
	
    @Override
	public void onSensorChanged(SensorEvent event) {
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];
		if (!mInitialized) {
			mLastX = x;
			mLastY = y;
			mLastZ = z;
			mInitialized = true;
		} else {
			float deltaX = Math.abs(mLastX - x);
			float deltaY = Math.abs(mLastY - y);
			float deltaZ = Math.abs(mLastZ - z);
			if (deltaX < NOISE) deltaX = (float) 0.0;
			if (deltaY < NOISE) deltaY = (float) 0.0;
			if (deltaZ < NOISE) deltaZ = (float) 0.0;
			mLastX = x;
			mLastY = y;
			mLastZ = z;

			String  direction="";
			int orientation = getResources().getConfiguration().orientation;
			if (orientation == Configuration.ORIENTATION_PORTRAIT) {
				//portrait
				if (x > 1.5) direction = "L";
				if (x < -1.5) direction = "R";

				if (y > 1.5) direction = "U";
				if (y < -1.5) direction = "D";
			} else {
				//landscape
				if (x > 1.5) direction = "U";
				if (x < -1.5) direction = "D";

				if (y > 1.5) direction = "R";
				if (y < -1.5) direction = "L";
			}
			if (direction != "") {
				serialSendText.setText(direction);
				serialSend(serialSendText.getText().toString());
			}
		}
	}
	@Override

    protected void onPause() {
        super.onPause();
		if (mGyro) mSensorManager.unregisterListener(this);
        onPauseProcess();														//onPause Process by BlunoLibrary
    }
	
	protected void onStop() {
		super.onStop();
		onStopProcess();														//onStop Process by BlunoLibrary
	}
    
	@Override
    protected void onDestroy() {
        super.onDestroy();	
        onDestroyProcess();														//onDestroy Process by BlunoLibrary
    }

	@Override
	public void onConectionStateChange(connectionStateEnum theConnectionState) {//Once connection state changes, this function will be called
		switch (theConnectionState) {											//Four connection state
		case isConnected:
			buttonScan.setText("Connected");
			break;
		case isConnecting:
			buttonScan.setText("Connecting");
			break;
		case isToScan:
			buttonScan.setText("Scan");
			break;
		case isScanning:
			buttonScan.setText("Scanning");
			break;
		case isDisconnecting:
			buttonScan.setText("isDisconnecting");
			break;
		default:
			break;
		}
	}


	public void upClicked(View view) {
		serialSendText=(EditText) findViewById(R.id.serialSendText);
		serialSendText.setText("U");
		serialSend(serialSendText.getText().toString());
		view.startAnimation(buttonClick);
	}
	public void downClicked(View view) {
		serialSendText=(EditText) findViewById(R.id.serialSendText);
		serialSendText.setText("D");
		serialSend(serialSendText.getText().toString());
		view.startAnimation(buttonClick);
	}
	public void leftClicked(View view) {
		serialSendText=(EditText) findViewById(R.id.serialSendText);
		serialSendText.setText("L");
		serialSend(serialSendText.getText().toString());
		view.startAnimation(buttonClick);
	}
	public void rightClicked(View view) {
		serialSendText=(EditText) findViewById(R.id.serialSendText);
		serialSendText.setText("R");
		serialSend(serialSendText.getText().toString());
		view.startAnimation(buttonClick);
	}
	public void centerClicked(View view) {
		serialSendText=(EditText) findViewById(R.id.serialSendText);
		serialSendText.setText("C");
		serialSend(serialSendText.getText().toString());
		view.startAnimation(buttonClick);
	}

	public void AClicked(View view) {
		serialSendText=(EditText) findViewById(R.id.serialSendText);
		serialSendText.setText("A");
		serialSend(serialSendText.getText().toString());
		view.startAnimation(buttonClick);
	}
	public void BClicked(View view) {
		serialSendText=(EditText) findViewById(R.id.serialSendText);
		serialSendText.setText("B");
		serialSend(serialSendText.getText().toString());
		view.startAnimation(buttonClick);
	}
	public void XClicked(View view) {
		serialSendText=(EditText) findViewById(R.id.serialSendText);
		serialSendText.setText("X");
		serialSend(serialSendText.getText().toString());
		view.startAnimation(buttonClick);
	}
	public void YClicked(View view) {
		serialSendText=(EditText) findViewById(R.id.serialSendText);
		serialSendText.setText("Y");
		serialSend(serialSendText.getText().toString());
		view.startAnimation(buttonClick);
	}

	public void gyroClicked(View view) {
		serialSendText=(EditText) findViewById(R.id.serialSendText);

		CheckBox gyro = (CheckBox) findViewById(R.id.gyro);
		if (gyro.isChecked())	{
			mGyro = true;
			mSensorManager.registerListener(this, mAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
		} else {
			mGyro=false;
			mSensorManager.unregisterListener(this);
		}
	}
	@Override
	public void onSerialReceived(String theString) {							//Once connection data received, this function will be called
		// TODO Auto-generated method stub
		serialReceivedText.append(theString);							//append the text into the EditText
		//The Serial data from the BLUNO may be sub-packaged, so using a buffer to hold the String is a good choice.
					
	}

}