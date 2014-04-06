package com.cdgnet.openmobs;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusMethod;
import org.alljoyn.bus.annotation.BusProperty;
import org.alljoyn.bus.annotation.BusSignal;

@BusInterface (name = "com.cdgnet.openmobs.NetSharer")
public interface NetSharerInterface {
	 @BusMethod
	 public String GetDeviceId() throws  BusException;
	 
	 @BusMethod 
	 public double GetPrice1() throws BusException;
	 
	 @BusMethod 
	 public double GetPrice2() throws BusException;
	 
	 @BusMethod
	 public long GetRemainingTraffic() throws BusException;
}
