package org.cloudbus.cloudsim;

import java.util.concurrent.ThreadLocalRandom;

import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

public class Wifi extends SimEntity {
	
	/** The wifiStrength of the broker. */
	protected int wifiStrength;

	private Wifi(String name) {
		super(name);
		
		int randomWifiStrength = ThreadLocalRandom.current().nextInt(0, 4);
		setWifiStrength(randomWifiStrength);
	}
	
	private static Wifi instance = null;
	
	public static Wifi getInstance(String name) {
	      if(instance == null) {
	         instance = new Wifi(name);
	      }
	      return instance;
	   }
	
	
	/**
	 * Function for Wifi Strength
	 * 
	 *  BY RIDHI SHARMA
	 */
	
	public void decWifiStrength() {
		this.wifiStrength--;
	}
	
	public void incWifiStrength() {
		this.wifiStrength++;
	}
	
	public int getWifiStrength() {
		return wifiStrength;
	}
	
	public void setWifiStrength(int _wifiStrength) {
		this.wifiStrength = _wifiStrength;
	}

	@Override
	public void startEntity() {
		Log.printLine(getName() + " is intilizing...");
		
	}

	@Override
	public void processEvent(SimEvent ev) {
		switch(ev.getTag()) {
		case CloudSimTags.Wifi_DEC:
			decWifiStrength();
			break;
		case CloudSimTags.Wifi_INC:
			incWifiStrength();
			break;
		case CloudSimTags.END_OF_SIMULATION:
			shutdownEntity();
			break;
		}
		
	}

	@Override
	public void shutdownEntity() {
		Log.printLine(getName() + " is shutting down...");
		
	}

}
