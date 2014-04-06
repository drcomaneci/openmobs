package com.cdgnet.openmobs;

import java.util.Map.Entry;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.Status;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Pair;

public class AllJoynCommunicationService extends Service {

	static {
	    System.loadLibrary("alljoyn_java");
	}
	
	private static final String TAG = "PeerService";
	private static final short CONTACT_PORT = 42;
	private static final String SERVICE_NAME = "com.cdgnet.openmobs.NetSharer";
	private static boolean isRunning = false;
	private static BusAttachment mServiceBus;
	private static BusAttachment mClientBus;
	private static BusHandler mBusHandler;
	final Messenger mMessenger = new Messenger(new IncomingHandler()); // Target we publish for clients to send messages to IncomingHandler.
	private NetSharer mNetSharer;
	private String phoneImei = null;
	private String serviceName;
	
	class IncomingHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            default:
                super.handleMessage(msg);
            }
        }
    }
	
	public String getPhoneImei(){
		if ( phoneImei == null){
			TelephonyManager mngr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE); 
			phoneImei = mngr.getDeviceId();
		}
		return phoneImei;
	}
	
	public String getDeviceId(){
		return Secure.getString(getBaseContext().getContentResolver(), Secure.ANDROID_ID); 
	}
	
	class BusHandler extends Handler {
		public static final int CONNECT = 1;
		public static final int JOIN_SESSION = 2;
		
		public BusHandler(Looper looper) {
            super(looper);
        }
		
		@Override
        public void handleMessage(Message msg){
			switch (msg.what){
			case CONNECT:
				org.alljoyn.bus.alljoyn.DaemonInit.PrepareDaemon(getApplicationContext());
				mServiceBus = new BusAttachment("OpenMobsService", BusAttachment.RemoteMessage.Receive);
				mClientBus = new BusAttachment("OpenMobsClient", BusAttachment.RemoteMessage.Receive);
				serviceName = SERVICE_NAME + getDeviceId();
				mNetSharer = new NetSharer(getDeviceId());
				
				mClientBus.registerBusListener(new BusListener(){
						@Override
						public void foundAdvertisedName(String name, short transport, String namePrefix) {
							Log.i(TAG, String.format("MyBusListener.foundAdvertisedName(%s, 0x%04x, %s)", name, transport, namePrefix));
							Message msg = obtainMessage(BusHandler.JOIN_SESSION);
				    	    msg.arg1 = transport;
				    	    msg.obj = name;
				    	    sendMessage(msg);
						}
				});
		        Status status = mServiceBus.registerBusObject(mNetSharer, "/NetSharer");
		        logStatus("BusAttachment.registerBusObject()", status);
		        if (status != Status.OK) {
		            return;
		        }
		        
		        status = mServiceBus.connect();
		        logStatus("BusAttachment.connect()", status);
		        if (status != Status.OK) {
		            return;
		        }
		        
		        status = mClientBus.connect();
		        logStatus("BusAttachment.connect()", status);
		        if (status != Status.OK) {
		            return;
		        }
		        
		        Mutable.ShortValue contactPort = new Mutable.ShortValue(CONTACT_PORT);
		        
		        SessionOpts sessionOpts = new SessionOpts();
		        sessionOpts.traffic = SessionOpts.TRAFFIC_MESSAGES;
		        sessionOpts.isMultipoint = false;
		        sessionOpts.proximity = SessionOpts.PROXIMITY_ANY;
		       
		        
		        sessionOpts.transports = SessionOpts.TRANSPORT_ANY + SessionOpts.TRANSPORT_WFD;

		        status = mServiceBus.bindSessionPort(contactPort, sessionOpts, new SessionPortListener() {
		            @Override
		            public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
		                if (sessionPort == CONTACT_PORT) {
		                    return true;
		                } else {
		                    return false;
		                }
		            }
		        });
		        
		        logStatus(String.format("BusAttachment.bindSessionPort(%d, %s)",
		                contactPort.value, sessionOpts.toString()), status);
		        
		        if (status != Status.OK) {
		            return;
		        }
		        
		        /*
		         * request a well-known name from the bus
		         */
		        int flag = BusAttachment.ALLJOYN_REQUESTNAME_FLAG_REPLACE_EXISTING | BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE;
		        
		        status = mServiceBus.requestName(serviceName, flag);
		        logStatus(String.format("BusAttachment.requestName(%s, 0x%08x)", serviceName, flag), status);
		        if (status == Status.OK) {
		        	/*
		        	 * If we successfully obtain a well-known name from the bus 
		        	 * advertise the same well-known name
		        	 */
		        	status = mServiceBus.advertiseName(serviceName, sessionOpts.transports);
		            logStatus(String.format("BusAttachement.advertiseName(%s)", serviceName), status);
		            if (status != Status.OK) {
		            	/*
		                 * If we are unable to advertise the name, release
		                 * the well-known name from the local bus.
		                 */
		                status = mServiceBus.releaseName(serviceName);
		                logStatus(String.format("BusAttachment.releaseName(%s)", serviceName), status);
		            	return;
		            }
		        }
		        
		        status = mClientBus.findAdvertisedName(SERVICE_NAME);
		        logStatus(String.format("BusAttachement.findAdvertisedName(%s)", SERVICE_NAME), status);
		        if (Status.OK != status) {
		        	return;
		        }
				break;
			case JOIN_SESSION:
                SessionOpts sesOpts = new SessionOpts();
                sesOpts.transports = (short)msg.arg1;
                Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
                
                Status s = mClientBus.joinSession((String) msg.obj, CONTACT_PORT, sessionId, sesOpts, new SessionListener() {
                    @Override
                    public void sessionLost(int sessionId) {
                        Log.i(TAG, String.format("MyBusListener.sessionLost(%d)", sessionId));
                    }
                });
                logStatus("BusAttachment.joinSession() - sessionId: " + sessionId.value, s);
                    
                if (s == Status.OK) {
                	/*
                     * To communicate with an AllJoyn object, we create a ProxyBusObject.  
                     * A ProxyBusObject is composed of a name, path, sessionID and interfaces.
                     * 
                     * This ProxyBusObject is located at the well-known SERVICE_NAME, under path
                     * "/SimpleService", uses sessionID of CONTACT_PORT, and implements the SimpleInterface.
                     */
                	ProxyBusObject proxyObj =  mClientBus.getProxyBusObject((String) msg.obj, 
                										"/NetSharer",
                										sessionId.value,
                										new Class<?>[] { NetSharerInterface.class });

                	NetSharerInterface nsproxy = proxyObj.getInterface(NetSharerInterface.class);
                	try {
						Log.i(TAG, "Call to update SharingInformation for "+ nsproxy.GetDeviceId());
						mNetSharer.UpdateSharingInformation(nsproxy);
						for(Entry<String, SharingInformation> e : mNetSharer.getSharingDevices().entrySet()){
							Log.i(TAG, e.getKey() + " = " + e.getValue());
						}
					} catch (BusException e) {
						Log.e(TAG, e.getMessage());
						e.printStackTrace();
					} finally{
						mClientBus.leaveSession(sessionId.value);
					}
                	
                	
                }
                	
                break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
	}
	

	@Override
	public IBinder onBind(Intent arg0) {
		return mMessenger.getBinder();
	}
	
	private void unregisterBusAttachment() {
		mServiceBus.unregisterBusObject(mNetSharer);
        mServiceBus.disconnect();
        mClientBus.disconnect();
	}
	
	@Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service Started.");
        
        /* Make all AllJoyn calls through a separate handler thread */
        HandlerThread busThread = new HandlerThread("BusHandler");
        busThread.start();
        mBusHandler = new BusHandler(busThread.getLooper());

        /* Connect to an AllJoyn object. */
        mBusHandler.sendEmptyMessage(BusHandler.CONNECT);
        isRunning = true;
    }
	
	public static boolean isRunning()
    {
        return isRunning;
    }
	
	@Override
    public void onDestroy() {
        super.onDestroy();        
        unregisterBusAttachment();
        Log.i("MyService", "Service Stopped.");
        isRunning = false;
    }
	
	private void logStatus(String msg, Status status) {
        String log = String.format("%s: %s", msg, status);
        if (status == Status.OK) {
            Log.i(TAG, log);
        } else {
            Log.e(TAG, log);
        }
    }

}
