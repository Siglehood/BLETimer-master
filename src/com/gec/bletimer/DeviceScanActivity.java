package com.gec.bletimer;

import com.gec.bletimer.adapter.LeDeviceListAdapter;
import com.gec.bletimer.util.Toaster;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.os.Process;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

public class DeviceScanActivity extends ListActivity {
	public static final int REQUEST_BLE_ENABLE = 0;
	public static final int WHAT_COMPONENT_UPDATE = 1;
	public static final int WHAT_ADAPTER_UPDATE = 2;

	private static final long SCAN_PERIOD = 30 * 1000;

	private LeDeviceListAdapter mLeDeviceListAdapter = null;
	private BluetoothAdapter mBluetoothAdapter = null;
	private ProgressBar mProgressBar = null;

	private boolean mScanning = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_scan);
		initUI();
		BLESupported();
		initBLE();
		BLEEnabled();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mLeDeviceListAdapter = new LeDeviceListAdapter(DeviceScanActivity.this);
		setListAdapter(mLeDeviceListAdapter);
		scanLeDevice(true);
	}

	@Override
	protected void onPause() {
		if (mScanning)
			scanLeDevice(false);
		mLeDeviceListAdapter.clear();
		super.onPause();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_BLE_ENABLE && resultCode == Activity.RESULT_CANCELED) {
			finish();
			Process.killProcess(Process.myPid());
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		listItemClick(position);
	}

	private void listItemClick(int position) {
		BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
		if (device == null)
			return;
		Intent intent = new Intent(this, MainActivity.class);
		Bundle bundle = new Bundle();
		bundle.putParcelable(MainActivity.EXTRA_BLUETOOTH_DEVICE, device);
		intent.putExtras(bundle);
		if (mScanning)
			scanLeDevice(false);
		startActivity(intent);
		finish();

	}

	private Handler mHandler = new Handler(new Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case WHAT_COMPONENT_UPDATE:
				mProgressBar.setVisibility(View.GONE);
				break;
			case WHAT_ADAPTER_UPDATE:
				mLeDeviceListAdapter.addDevice((BluetoothDevice) msg.obj);
				mLeDeviceListAdapter.notifyDataSetChanged();
				break;
			default:
				break;
			}
			return false;
		}
	});

	private void initUI() {
		mProgressBar = (ProgressBar) this.findViewById(R.id.progress_bar);
	}

	/**
	 * 是否支持BLE特性
	 */
	private void BLESupported() {
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toaster.shortToastShow(getApplicationContext(), R.string.ble_not_supported);
			finish();
			Process.killProcess(Process.myPid());
		}
	}

	/**
	 * 初始化本地蓝牙设备
	 */
	private void initBLE() {
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
	}

	/**
	 * 检测蓝牙设备是否开启，如果未开启，发起Intent并回调
	 */
	private void BLEEnabled() {
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_BLE_ENABLE);
		}
	}

	@SuppressWarnings("deprecation")
	private void scanLeDevice(boolean enable) {
		if (enable) {
			mHandler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					mScanning = false;
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
					mHandler.sendEmptyMessage(WHAT_COMPONENT_UPDATE);
				}
			}, SCAN_PERIOD);
			mScanning = true;
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			mScanning = false;
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
	}

	private LeScanCallback mLeScanCallback = new LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
			if (device != null)
				mHandler.sendMessage(mHandler.obtainMessage(WHAT_ADAPTER_UPDATE, device));
		}
	};
}
