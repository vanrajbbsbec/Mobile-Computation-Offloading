package org.cloudbus.cloudsim;

import java.util.concurrent.ThreadLocalRandom;

import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

public class Battery extends SimEntity {
	
	private Battery(String name) {
		super(name);
		
		int randomBattery = ThreadLocalRandom.current().nextInt(80, 100 + 1);
		setBatteryLife(randomBattery);	

	}
	
	private static Battery instance = null;

	/**  The battery life for broker. */
	protected double batteryLife;
	
	private Cloudlet cloudlet;
	
	
	public static Battery getInstance(String name) {
	      if(instance == null) {
	         instance = new Battery(name);
	      }
	      return instance;
	   }
	
	
	/**
	 * Function for BatteryLife
	 * 
	 *  BY RIDHI SHARMA
	 */
	
	public  void setBatteryLife(int _batteryLife) {
		this.batteryLife = _batteryLife;
	}
	
	public void decBatteryLife(SimEvent ev) {
		Object[] batteryData = (Object[]) ev.getData();
		cloudlet =  (Cloudlet) batteryData[1];
		cloudlet.setBeforeBatteryLife(this.batteryLife);
		this.batteryLife = this.batteryLife - Double.parseDouble(batteryData[0].toString());
		cloudlet.setAfterBatteryLife(this.batteryLife);
		Log.printLine(getName() + " @ " + batteryLife);
	}
	
	public void incBatteryLife(double i) {
		this.batteryLife = this.batteryLife + i;
		Log.printLine(getName() + " @ " + batteryLife);
	}
	
	public double getBatteryLife() {
		return batteryLife;
	}
	
	
	
	

	@Override
	public void startEntity() {
		Log.printLine(getName() + " is intilizing...");
		Log.printLine(getName() + " @ " + batteryLife);
		
	}

	@Override
	public void processEvent(SimEvent ev) {
		switch(ev.getTag()) {
		case CloudSimTags.Battery_DEC:
			decBatteryLife(ev);
			break;
		case CloudSimTags.Battery_INC:
			incBatteryLife(1);
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
	
	
	public static Battery intitBattery() {
		Battery objBattery = null;
		try
		{
			objBattery = Battery.getInstance("Mobile_Battery");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			return null;
		}
		return objBattery;
	}

}
