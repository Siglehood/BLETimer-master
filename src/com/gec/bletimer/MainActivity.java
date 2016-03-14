package com.gec.bletimer;

import java.util.UUID;

import com.gec.bletimer.util.SharedPrefs;
import com.gec.bletimer.util.Toaster;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
	public static final String EXTRA_BLUETOOTH_DEVICE = "bluetooth_device";

	public static final int REQUEST_BLE_ENABLE = 0;
	public static final int WHAT_BLE_INFO = 1;
	public static final int WHAT_DISCOVER_SERVICES = 2;
	public static final int WHAT_CHARACTERISTIC_WRITE_SUCCEED = 3;
	public static final int WHAT_CHARACTERISTIC_WRITE_FAIL = 4;

	private static final String TAG = MainActivity.class.getSimpleName();
	private static final boolean D = true;

	private static final String SIMPLE_PROFILE_SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb";
	private static final String SIMPLE_PROFILE_CHARACTERISTIC_UUID = "0000fff5-0000-1000-8000-00805f9b34fb";

	private BluetoothDevice mBluetoothDevice = null;
	private BluetoothGatt mBluetoothGatt = null;
	private BluetoothGattService mBluetoothGattService = null;
	private BluetoothGattCharacteristic mBluetoothGattCharacteristic = null;

	private TextView mBleNameTv = null;
	private TextView mBleAddrTv = null;
	private EditText mTimeCloseEt = null;
	private EditText mTimeOpenEt = null;
	private Button mSettingBtn = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initUI();
		connBLE();
	}

	@Override
	protected void onStop() {
		if (mBluetoothGatt != null) {
			mBluetoothGatt.disconnect();
			mBluetoothGatt.close();
		}
		if (D)
			Log.d(TAG, "Disconnected.");
		super.onStop();
	}

	/**
	 * Handler处理消息
	 */
	private Handler mHandler = new Handler(new Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case WHAT_BLE_INFO:
				showAndConn();
				break;
			case WHAT_DISCOVER_SERVICES:
				connStateChange(msg.arg1);
				break;
			case WHAT_CHARACTERISTIC_WRITE_SUCCEED:
				Toaster.shortToastShow(getApplicationContext(), R.string.toast_setting_succeed);
				break;
			case WHAT_CHARACTERISTIC_WRITE_FAIL:
				Toaster.shortToastShow(getApplicationContext(), R.string.toast_setting_fail);
				break;
			default:
				break;
			}
			return false;
		}
	});

	/**
	 * BLE GATT服务连接回调
	 */
	private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			mHandler.sendMessage(Message.obtain(mHandler, WHAT_DISCOVER_SERVICES, newState, 0));
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			servicesDiscovered(status);
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			characteristicWriteSuccess(status);
		};
	};

	/**
	 * UI初始化
	 */
	private void initUI() {
		mBleNameTv = (TextView) this.findViewById(R.id.tv_ble_name);
		mBleAddrTv = (TextView) this.findViewById(R.id.tv_ble_address);
		mTimeCloseEt = (EditText) this.findViewById(R.id.et_time_close);
		mTimeOpenEt = (EditText) this.findViewById(R.id.et_time_open);
		mSettingBtn = (Button) this.findViewById(R.id.btn_setting);
		hideSoftInputMethod();
		getStrFromPrefs();
	}

	/**
	 * 从SharedPreferences获取保存的时间
	 */
	private void getStrFromPrefs() {
		String timeCloseStr = SharedPrefs.getSharedPrefsStr(getApplicationContext(), "time_close");
		if (!TextUtils.isEmpty(timeCloseStr))
			mTimeCloseEt.setText(timeCloseStr);
		String timeOpenStr = SharedPrefs.getSharedPrefsStr(getApplicationContext(), "time_open");
		if (!TextUtils.isEmpty(timeOpenStr))
			mTimeOpenEt.setText(timeOpenStr);
	}

	/**
	 * 点击小键盘回车符隐藏输入法
	 */
	private void hideSoftInputMethod() {
		mTimeCloseEt.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					InputMethodManager imm = (InputMethodManager) v.getContext()
							.getSystemService(Context.INPUT_METHOD_SERVICE);
					if (imm.isActive())
						imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
					return true;
				}
				return false;
			}
		});

		mTimeOpenEt.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					InputMethodManager imm = (InputMethodManager) v.getContext()
							.getSystemService(Context.INPUT_METHOD_SERVICE);
					if (imm.isActive())
						imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
					return true;
				}
				return false;
			}
		});
	}

	/**
	 * 连接BLE
	 */
	private void connBLE() {
		Bundle bundle = getIntent().getExtras();
		if (bundle != null)
			mBluetoothDevice = bundle.getParcelable(EXTRA_BLUETOOTH_DEVICE);
		if (mBluetoothDevice != null) {
			if (D)
				Log.d(TAG, mBluetoothDevice.getName() + "——" + mBluetoothDevice.getAddress());
			mHandler.sendEmptyMessage(WHAT_BLE_INFO);
		}
	}

	private void showAndConn() {
		mBleNameTv.setText(getText(R.string.ble_name) + mBluetoothDevice.getName());
		mBleAddrTv.setText(getText(R.string.ble_address) + mBluetoothDevice.getAddress());
		gattServiceConn();
	}

	/**
	 * 连接GATT服务器，会连接该BLE设备并回调
	 */
	private void gattServiceConn() {
		if (mBluetoothDevice != null)
			mBluetoothGatt = mBluetoothDevice.connectGatt(this, false, mBluetoothGattCallback);
	}

	private void connStateChange(int newState) {
		if (D)
			Log.d(TAG, "Connected");
		timeIsValid(newState);
	}

	/**
	 * 判断输入是否有效
	 * 
	 * @param newState
	 */
	private void timeIsValid(final int newState) {
		mSettingBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (TextUtils.isEmpty(mTimeCloseEt.getText().toString())) {
					Toaster.shortToastShow(getApplicationContext(), R.string.toast_empty);
					return;
				}
				if (Integer.valueOf(mTimeCloseEt.getText().toString()) > 127) {
					Toaster.shortToastShow(getApplicationContext(), R.string.toast_input_most);
					mTimeCloseEt.setText("");
					return;
				}
				if (TextUtils.isEmpty(mTimeOpenEt.getText().toString())) {
					Toaster.shortToastShow(getApplicationContext(), R.string.toast_empty);
					return;
				}
				if (Integer.valueOf(mTimeOpenEt.getText().toString()) > 127) {
					Toaster.shortToastShow(getApplicationContext(), R.string.toast_input_most);
					mTimeOpenEt.setText("");
					return;
				}
				putStr2Prefs();
				discoverServices(newState);
			}
		});
	}

	/**
	 * 搜索远程设备的服务
	 * 
	 * @param newState
	 */
	private void discoverServices(int newState) {
		if (mBluetoothGatt != null && newState == BluetoothProfile.STATE_CONNECTED)
			mBluetoothGatt.discoverServices();
		else
			Toaster.shortToastShow(getApplicationContext(), R.string.toast_disconn);
	}

	/**
	 * 保存时间到SharedPreferences
	 */
	private void putStr2Prefs() {
		SharedPrefs.putSharedPrefs(getApplicationContext(), "time_close", mTimeCloseEt.getText().toString());
		SharedPrefs.putSharedPrefs(getApplicationContext(), "time_open", mTimeOpenEt.getText().toString());
	}

	/**
	 * 发现远程设备的服务
	 * 
	 * @param status
	 */
	private void servicesDiscovered(int status) {
		if (mBluetoothGatt != null && status == BluetoothGatt.GATT_SUCCESS) {
			// 获取BLE GATT服务
			mBluetoothGattService = mBluetoothGatt.getService(UUID.fromString(SIMPLE_PROFILE_SERVICE_UUID));
			if (mBluetoothGattService != null) {
				// 获取BLE GATT特征值
				mBluetoothGattCharacteristic = mBluetoothGattService
						.getCharacteristic(UUID.fromString(SIMPLE_PROFILE_CHARACTERISTIC_UUID));
				if (mBluetoothGattCharacteristic != null) {
					byte closeTimeByte = Byte.valueOf(mTimeCloseEt.getText().toString());
					byte openTimeByte = Byte.valueOf(mTimeOpenEt.getText().toString());
					byte[] timeByteArr = { 0, closeTimeByte, openTimeByte };
					mBluetoothGattCharacteristic.setValue(timeByteArr);
					mBluetoothGatt.writeCharacteristic(mBluetoothGattCharacteristic);
				}
			}
		}
	}

	/**
	 * 判断写入特征值是否成功
	 * 
	 * @param status
	 */
	private void characteristicWriteSuccess(int status) {
		if (D)
			Log.d(TAG, "status=" + status);
		if (status == BluetoothGatt.GATT_SUCCESS)
			mHandler.sendEmptyMessage(WHAT_CHARACTERISTIC_WRITE_SUCCEED);
		else
			mHandler.sendEmptyMessage(WHAT_CHARACTERISTIC_WRITE_FAIL);
	}
}
