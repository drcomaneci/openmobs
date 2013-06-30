package com.cdgnet.openmobs;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.annotation.BusProperty;

public class NetSharer implements NetSharerInterface, BusObject {
	String deviceId;
	public NetSharer(String imei){
		this.deviceId = imei;
	}
	public String GetSharingParameters() throws  BusException{
		return "test from "+deviceId; 
	}
}
