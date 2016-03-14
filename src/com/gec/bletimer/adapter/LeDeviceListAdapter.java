package com.gec.bletimer.adapter;

import java.util.ArrayList;

import com.gec.bletimer.R;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class LeDeviceListAdapter extends BaseAdapter {
	private ArrayList<BluetoothDevice> mLeDevices = null;
	private LayoutInflater mLayoutInflater = null;

	public LeDeviceListAdapter(Context context) {
		mLeDevices = new ArrayList<BluetoothDevice>();
		mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public void addDevice(BluetoothDevice device) {
		if (!mLeDevices.contains(device)) {
			mLeDevices.add(device);
		}
	}

	public BluetoothDevice getDevice(int position) {
		return mLeDevices.get(position);
	}

	public void clear() {
		mLeDevices.clear();
	}

	@Override
	public int getCount() {
		return mLeDevices.size();
	}

	@Override
	public Object getItem(int i) {
		return mLeDevices.get(i);
	}

	@Override
	public long getItemId(int i) {
		return i;
	}

	@Override
	public View getView(int i, View view, ViewGroup viewGroup) {
		ViewHolder viewHolder = null;
		if (view == null) {
			view = mLayoutInflater.inflate(R.layout.list_item, viewGroup, false);
			viewHolder = new ViewHolder();
			viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
			viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
			view.setTag(viewHolder);
		} else
			viewHolder = (ViewHolder) view.getTag();
		BluetoothDevice device = mLeDevices.get(i);
		String deviceName = device.getName();
		if (!TextUtils.isEmpty(deviceName))
			viewHolder.deviceName.setText(deviceName);
		else
			viewHolder.deviceName.setText(R.string.unknown_device);
		viewHolder.deviceAddress.setText(device.getAddress());
		return view;
	}

	private static class ViewHolder {
		private TextView deviceName = null;
		private TextView deviceAddress = null;
	}
}
