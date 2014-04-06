package com.cdgnet.openmobs;

import java.util.TreeMap;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.annotation.BusProperty;

import android.net.TrafficStats;
import android.util.Log;

class SharingInformation{
	private Double price1;
	private Double price2;
	private Long remainingTraffic;
	public SharingInformation(Double price1, Double price2, Long remainingTraffic){
		this.price1 = price1;
		this.price2 = price2;
		this.remainingTraffic = remainingTraffic;
	}
	public Double getPrice1() {
		return price1;
	}
	public void setPrice1(Double price1) {
		this.price1 = price1;
	}
	public Double getPrice2() {
		return price2;
	}
	public void setPrice2(Double price2) {
		this.price2 = price2;
	}
	public Long getRemainingTraffic() {
		return remainingTraffic;
	}
	public void setRemainingTraffic(Long remainingTraffic) {
		this.remainingTraffic = remainingTraffic;
	}
	
	public String toString() {
		return "[ price1 = "+getPrice1() + " price2 = "+getPrice2() + " remainingTraffic = "+getRemainingTraffic()+" ]";
	}
}

public class NetSharer implements NetSharerInterface, BusObject {
	private String deviceId;
	private Double price1;
	private Double price2;
	private long dataPlanTraffic;
	private long startStats;
	
	private TreeMap<String, SharingInformation> sharingDevices;
	
	public NetSharer(String deviceId){
		this.deviceId = deviceId;
		//to do: load config from DB
		this.price1 = 0.008;
		this.price2 = 0.01;
		this.dataPlanTraffic = 1024*1024*1024;
		sharingDevices = new TreeMap<String, SharingInformation>();
		startStats = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes();
	}
	
	public TreeMap<String, SharingInformation> getSharingDevices(){
		return sharingDevices;
	}
	
	@Override
	public String GetDeviceId() throws BusException {
		return deviceId;
	}
	@Override
	public double GetPrice1() throws BusException {
		return price1;
	}
	@Override
	public double GetPrice2() throws BusException {
		return price2;
	}
	
	public void setPrice1(Double price){
		price1 = price;
	}
	
	public void setPrice2(Double price){
		price1 = price;
	}
	
	public void UpdateSharingInformation(NetSharerInterface ns) throws BusException{
		String id = ns.GetDeviceId();
		Log.i("NetSharer", "id=" + id);
		if (sharingDevices.containsKey(id)){
			SharingInformation si = sharingDevices.get(id);
			si.setPrice1(ns.GetPrice1());
			si.setPrice2(ns.GetPrice2());
		}
		else {
			SharingInformation si = new SharingInformation(ns.GetPrice1(), ns.GetPrice2(), ns.GetRemainingTraffic());
			sharingDevices.put(id, si);
		}
	}

	@Override
	public long GetRemainingTraffic() throws BusException {
		long currentStats = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes();
		return getDataPlanTraffic() - (currentStats - startStats); 
	}

	public long getDataPlanTraffic() {
		return dataPlanTraffic;
	}

	public void setDataPlanTraffic(long dataPlanTraffic) {
		this.dataPlanTraffic = dataPlanTraffic;
	}
}
