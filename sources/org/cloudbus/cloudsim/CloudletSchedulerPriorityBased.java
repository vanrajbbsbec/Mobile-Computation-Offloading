/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.cloudbus.cloudsim.core.CloudSim;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.Collections;

/**
 * CloudletSchedulerSpaceShared implements a policy of scheduling performed by a virtual machine. It
 * consider that there will be only one cloudlet per VM. Other cloudlets will be in a waiting list.
 * We consider that file transfer from cloudlets waiting happens before cloudlet execution. I.e.,
 * even though cloudlets must wait for CPU, data transfer happens as soon as cloudlets are
 * submitted.
 * 
 * @author RIDHI SHARMA
 * @since CloudSim Toolkit 3.03
 */
@SuppressWarnings("unused")
public class CloudletSchedulerPriorityBased extends CloudletScheduler {
	
	/** The cloudlet waiting list. */
	private List<? extends ResCloudlet> cloudletWaitingList;

	/** The cloudlet exec list. */
	private List<? extends ResCloudlet> cloudletExecList;

	/** The cloudlet paused list. */
	private List<? extends ResCloudlet> cloudletPausedList;

	/** The cloudlet finished list. */
	private List<? extends ResCloudlet> cloudletFinishedList;

	/** The current CPUs. */
	protected int currentCpus;

	/** The used PEs. */
	protected int usedPes;
	
	final protected int maxPriority = 5;
	final protected int minPriority = 1;

	private double batteryThreshold = 50.0;
	@SuppressWarnings("unused")
	private double batteryCriticalThreshold = 20.0;
	
	
	public CloudletSchedulerPriorityBased() {
		super();
		cloudletWaitingList = new ArrayList<ResCloudlet>();
		cloudletExecList = new ArrayList<ResCloudlet>();
		cloudletPausedList = new ArrayList<ResCloudlet>();
		cloudletFinishedList = new ArrayList<ResCloudlet>();
		usedPes = 0;
		currentCpus = 0;
	}
	
	
	

	@Override
	public double updateVmProcessing(double currentTime, List<Double> mipsShare) {
		Battery objBat = intitBattery();
		setCurrentMipsShare(mipsShare);
		double timeSpam = currentTime - getPreviousTime(); // time since last update
		double capacity = 0.0;
		int cpus = 0;

		for (Double mips : mipsShare) { // count the CPUs available to the VMM
			capacity += mips;
			if (mips > 0) {
				cpus++;
			}
		}
		currentCpus = cpus;
		capacity /= cpus; // average capacity of each cpu

		// each machine in the exec list has the same amount of cpu
		for (ResCloudlet rcl : getCloudletExecList()) {
			rcl.updateCloudletFinishedSoFar((long) (capacity * timeSpam * rcl.getNumberOfPes() * Consts.MILLION));
		}

		// no more cloudlets in this scheduler
		if (getCloudletExecList().size() == 0 && getCloudletWaitingList().size() == 0) {
			setPreviousTime(currentTime);
			return 0.0;
		}

		// update each cloudlet
		int finished = 0;
		List<ResCloudlet> toRemove = new ArrayList<ResCloudlet>();
		for (ResCloudlet rcl : getCloudletExecList()) {
			// finished anyway, rounding issue...
			if (rcl.getRemainingCloudletLength() == 0) {
				toRemove.add(rcl);
				cloudletFinish(rcl);
				finished++;
			}
		}
		getCloudletExecList().removeAll(toRemove);

		// for each finished cloudlet, add a new one from the waiting list
		if (!getCloudletWaitingList().isEmpty()) {
			java.util.Collections.sort(getCloudletWaitingList(), new SortByPriority()); //RIDHI SHARMA
			List<ResCloudlet> clw = getCloudletWaitingList();
			for (int i = 0; i < finished; i++) {
				toRemove.clear();
				for (ResCloudlet rcl : getCloudletWaitingList()) {
					if ((currentCpus - usedPes) >= rcl.getNumberOfPes() && rcl.getCloudlet().getAfterBatteryLife() > batteryCriticalThreshold ) {
						rcl.setCloudletStatus(Cloudlet.INEXEC);
						for (int k = 0; k < rcl.getNumberOfPes(); k++) {
							rcl.setMachineAndPeId(0, i);
						}
						getCloudletExecList().add(rcl);
						usedPes += rcl.getNumberOfPes();
						toRemove.add(rcl);
						break;
					}
					else
					{
						cloudletCancel(rcl.getCloudletId());
					}
				}
				getCloudletWaitingList().removeAll(toRemove);
			}
		}

		// estimate finish time of cloudlets in the execution queue
		double nextEvent = Double.MAX_VALUE;
		for (ResCloudlet rcl : getCloudletExecList()) {
			double remainingLength = rcl.getRemainingCloudletLength();
			double estimatedFinishTime = currentTime + (remainingLength / (capacity * rcl.getNumberOfPes()));
			if (estimatedFinishTime - currentTime < CloudSim.getMinTimeBetweenEvents()) {
				estimatedFinishTime = currentTime + CloudSim.getMinTimeBetweenEvents();
			}
			if (estimatedFinishTime < nextEvent) {
				nextEvent = estimatedFinishTime;
			}
		}
		setPreviousTime(currentTime);
		return nextEvent;
	}

	@Override
	public double cloudletSubmit(Cloudlet cloudlet, double fileTransferTime) {
		Battery objBat = intitBattery();
		if ((currentCpus - usedPes) >= cloudlet.getNumberOfPes() && cloudlet.getAfterBatteryLife() > batteryThreshold ) {
			ResCloudlet rcl = new ResCloudlet(cloudlet);
			rcl.setCloudletStatus(Cloudlet.INEXEC);
			for (int i = 0; i < cloudlet.getNumberOfPes(); i++) {
				rcl.setMachineAndPeId(0, i);
			}
			getCloudletExecList().add(rcl);
			usedPes += cloudlet.getNumberOfPes();
		}
		else if((currentCpus - usedPes) >= cloudlet.getNumberOfPes() && cloudlet.getAfterBatteryLife() < batteryThreshold)
		{
			if(cloudlet.getPriority() == maxPriority) // After Battery Threshold is reached, cloudlet with max priority will be executed
			{
				ResCloudlet rcl = new ResCloudlet(cloudlet);
				rcl.setCloudletStatus(Cloudlet.INEXEC);
				for (int i = 0; i < cloudlet.getNumberOfPes(); i++) {
					rcl.setMachineAndPeId(0, i);
				}
				getCloudletExecList().add(rcl);
				usedPes += cloudlet.getNumberOfPes();
			}
			else // Cloudlet with priority less than 5 will be queued and processed later on in UpdateVMProcessing 
			{
				ResCloudlet rcl = new ResCloudlet(cloudlet);
				rcl.setCloudletStatus(Cloudlet.QUEUED);
				getCloudletWaitingList().add(rcl);
				return 0.0;
			}
		}
		else // not enough free PEs: go to the waiting queue
		{
			ResCloudlet rcl = new ResCloudlet(cloudlet);
			rcl.setCloudletStatus(Cloudlet.QUEUED);
			getCloudletWaitingList().add(rcl);
			return 0.0;
		}

		// calculate the expected time for cloudlet completion
				double capacity = 0.0;
				int cpus = 0;
				for (Double mips : getCurrentMipsShare()) {
					capacity += mips;
					if (mips > 0) {
						cpus++;
					}
				}

				currentCpus = cpus;
				capacity /= cpus;
				

				// use the current capacity to estimate the extra amount of
				// time to file transferring. It must be added to the cloudlet length
				double extraSize = capacity * fileTransferTime;
				long length = cloudlet.getCloudletLength();
				length += extraSize;
				cloudlet.setCloudletLength(length);
				return cloudlet.getCloudletLength() / capacity;
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




	@Override
	public double cloudletSubmit(Cloudlet cloudlet) {
		return cloudletSubmit(cloudlet, 0.0);
	}

	@Override
	public Cloudlet cloudletCancel(int cloudletId) {
		
		// First, looks in the finished queue
//				for (ResCloudlet rcl : getCloudletFinishedList()) {
//					if (rcl.getCloudletId() == cloudletId) {
//						Iterator<ResCloudlet> itr = getCloudletFinishedList().iterator();
//						getCloudletFinishedList().remove(rcl);
//						return rcl.getCloudlet();
//					}
//				}
				
				Iterator<ResCloudlet> itr = getCloudletFinishedList().iterator();
				while(itr.hasNext())
				{
					ResCloudlet rcl = itr.next();
					if(rcl.getCloudletId() == cloudletId)
					{
						itr.remove();
					}
					return rcl.getCloudlet();
				}
				
				

				// Then searches in the exec list
//				for (ResCloudlet rcl : getCloudletExecList()) {
//					if (rcl.getCloudletId() == cloudletId) {
//						getCloudletExecList().remove(rcl);
//						if (rcl.getRemainingCloudletLength() == 0) {
//							cloudletFinish(rcl);
//						} else {
//							rcl.setCloudletStatus(Cloudlet.CANCELED);
//						}
//						return rcl.getCloudlet();
//					}
//				}
				
				Iterator<ResCloudlet> itrExe = getCloudletExecList().iterator();
				while(itrExe.hasNext())
				{
					ResCloudlet rcl = itrExe.next();
					if(rcl.getCloudletId() == cloudletId)
					{
						itrExe.remove();
					}
					return rcl.getCloudlet();
				}

				// Now, looks in the paused queue
//				for (ResCloudlet rcl : getCloudletPausedList()) {
//					if (rcl.getCloudletId() == cloudletId) {
//						getCloudletPausedList().remove(rcl);
//						return rcl.getCloudlet();
//					}
//				}
				
				Iterator<ResCloudlet> itrPau = getCloudletPausedList().iterator();
				while(itrPau.hasNext())
				{
					ResCloudlet rcl = itrPau.next();
					if(rcl.getCloudletId() == cloudletId)
					{
						itrPau.remove();
					}
					return rcl.getCloudlet();
				}

				// Finally, looks in the waiting list
//				for (ResCloudlet rcl : getCloudletWaitingList()) {
//					if (rcl.getCloudletId() == cloudletId) {
//						rcl.setCloudletStatus(Cloudlet.CANCELED);
//						getCloudletWaitingList().remove(rcl);
//						return rcl.getCloudlet();
//					}
//				}
				
				Iterator<ResCloudlet> itrWait = getCloudletWaitingList().iterator();
				while(itrWait.hasNext())
				{
					ResCloudlet rcl = itrWait.next();
					if(rcl.getCloudletId() == cloudletId)
					{
						itrWait.remove();
					}
					return rcl.getCloudlet();
				}
				

				return null;
	}

	@Override
	public boolean cloudletPause(int cloudletId) {
		boolean found = false;
		int position = 0;

		// first, looks for the cloudlet in the exec list
		for (ResCloudlet rcl : getCloudletExecList()) {
			if (rcl.getCloudletId() == cloudletId) {
				found = true;
				break;
			}
			position++;
		}

		if (found) {
			// moves to the paused list
			ResCloudlet rgl = getCloudletExecList().remove(position);
			if (rgl.getRemainingCloudletLength() == 0) {
				cloudletFinish(rgl);
			} else {
				rgl.setCloudletStatus(Cloudlet.PAUSED);
				getCloudletPausedList().add(rgl);
			}
			return true;

		}

		// now, look for the cloudlet in the waiting list
		position = 0;
		found = false;
		for (ResCloudlet rcl : getCloudletWaitingList()) {
			if (rcl.getCloudletId() == cloudletId) {
				found = true;
				break;
			}
			position++;
		}

		if (found) {
			// moves to the paused list
			ResCloudlet rgl = getCloudletWaitingList().remove(position);
			if (rgl.getRemainingCloudletLength() == 0) {
				cloudletFinish(rgl);
			} else {
				rgl.setCloudletStatus(Cloudlet.PAUSED);
				getCloudletPausedList().add(rgl);
			}
			return true;

		}

		return false;
	}

	@Override
	public double cloudletResume(int cloudletId) {
		boolean found = false;
		int position = 0;

		// look for the cloudlet in the paused list
		for (ResCloudlet rcl : getCloudletPausedList()) {
			if (rcl.getCloudletId() == cloudletId) {
				found = true;
				break;
			}
			position++;
		}

		if (found) {
			ResCloudlet rcl = getCloudletPausedList().remove(position);

			// it can go to the exec list
			if ((currentCpus - usedPes) >= rcl.getNumberOfPes()) {
				rcl.setCloudletStatus(Cloudlet.INEXEC);
				for (int i = 0; i < rcl.getNumberOfPes(); i++) {
					rcl.setMachineAndPeId(0, i);
				}

				long size = rcl.getRemainingCloudletLength();
				size *= rcl.getNumberOfPes();
				rcl.getCloudlet().setCloudletLength(size);

				getCloudletExecList().add(rcl);
				usedPes += rcl.getNumberOfPes();

				// calculate the expected time for cloudlet completion
				double capacity = 0.0;
				int cpus = 0;
				for (Double mips : getCurrentMipsShare()) {
					capacity += mips;
					if (mips > 0) {
						cpus++;
					}
				}
				currentCpus = cpus;
				capacity /= cpus;

				long remainingLength = rcl.getRemainingCloudletLength();
				double estimatedFinishTime = CloudSim.clock()
						+ (remainingLength / (capacity * rcl.getNumberOfPes()));

				return estimatedFinishTime;
			} else {// no enough free PEs: go to the waiting queue
				rcl.setCloudletStatus(Cloudlet.QUEUED);

				long size = rcl.getRemainingCloudletLength();
				size *= rcl.getNumberOfPes();
				rcl.getCloudlet().setCloudletLength(size);

				getCloudletWaitingList().add(rcl);
				return 0.0;
			}

		}

		// not found in the paused list: either it is in in the queue, executing or not exist
		return 0.0;
	}

	@Override
	public void cloudletFinish(ResCloudlet rcl) {
		rcl.setCloudletStatus(Cloudlet.SUCCESS);
		rcl.finalizeCloudlet();
		getCloudletFinishedList().add(rcl);
		usedPes -= rcl.getNumberOfPes();
		
	}

	@Override
	public int getCloudletStatus(int cloudletId) {
		for (ResCloudlet rcl : getCloudletExecList()) {
			if (rcl.getCloudletId() == cloudletId) {
				return rcl.getCloudletStatus();
			}
		}

		for (ResCloudlet rcl : getCloudletPausedList()) {
			if (rcl.getCloudletId() == cloudletId) {
				return rcl.getCloudletStatus();
			}
		}

		for (ResCloudlet rcl : getCloudletWaitingList()) {
			if (rcl.getCloudletId() == cloudletId) {
				return rcl.getCloudletStatus();
			}
		}

		return -1;
	}

	@Override
	public boolean isFinishedCloudlets() {
		return getCloudletFinishedList().size() > 0;
	}

	@Override
	public Cloudlet getNextFinishedCloudlet() {
		if (getCloudletFinishedList().size() > 0) {
			return getCloudletFinishedList().remove(0).getCloudlet();
		}
		return null;
	}

	@Override
	public int runningCloudlets() {
		return getCloudletExecList().size();
	}

	@Override
	public Cloudlet migrateCloudlet() {
		ResCloudlet rcl = getCloudletExecList().remove(0);
		rcl.finalizeCloudlet();
		Cloudlet cl = rcl.getCloudlet();
		usedPes -= cl.getNumberOfPes();
		return cl;
	}

	@Override
	public double getTotalUtilizationOfCpu(double time) {
		double totalUtilization = 0;
		for (ResCloudlet gl : getCloudletExecList()) {
			totalUtilization += gl.getCloudlet().getUtilizationOfCpu(time);
		}
		return totalUtilization;
	}

	@Override
	public List<Double> getCurrentRequestedMips() {
		List<Double> mipsShare = new ArrayList<Double>();
		if (getCurrentMipsShare() != null) {
			for (Double mips : getCurrentMipsShare()) {
				mipsShare.add(mips);
			}
		}
		return mipsShare;
	}

	@Override
	public double getTotalCurrentAvailableMipsForCloudlet(ResCloudlet rcl, List<Double> mipsShare) {
		double capacity = 0.0;
		int cpus = 0;
		for (Double mips : mipsShare) { // count the cpus available to the vmm
			capacity += mips;
			if (mips > 0) {
				cpus++;
			}
		}
		currentCpus = cpus;
		capacity /= cpus; // average capacity of each cpu
		return capacity;
	}

	@Override
	public double getTotalCurrentRequestedMipsForCloudlet(ResCloudlet rcl, double time) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getTotalCurrentAllocatedMipsForCloudlet(ResCloudlet rcl, double time) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getCurrentRequestedUtilizationOfRam() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getCurrentRequestedUtilizationOfBw() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/**
	 * Gets the cloudlet exec list.
	 * 
	 * @param <T> the generic type
	 * @return the cloudlet exec list
	 */
	@SuppressWarnings("unchecked")
	protected <T extends ResCloudlet> List<T> getCloudletExecList() {
		return (List<T>) cloudletExecList;
	}
	
	
	/**
	 * Gets the cloudlet waiting list.
	 * 
	 * @param <T> the generic type
	 * @return the cloudlet waiting list
	 */
	@SuppressWarnings("unchecked")
	protected <T extends ResCloudlet> List<T> getCloudletWaitingList() {
		return (List<T>) cloudletWaitingList;
	}
	
	
	/**
	 * Gets the cloudlet finished list.
	 * 
	 * @param <T> the generic type
	 * @return the cloudlet finished list
	 */
	@SuppressWarnings("unchecked")
	protected <T extends ResCloudlet> List<T> getCloudletFinishedList() {
		return (List<T>) cloudletFinishedList;
	}
	
	
	/**
	 * Gets the cloudlet paused list.
	 * 
	 * @param <T> the generic type
	 * @return the cloudlet paused list
	 */
	@SuppressWarnings("unchecked")
	protected <T extends ResCloudlet> List<T> getCloudletPausedList() {
		return (List<T>) cloudletPausedList;
	}
}

class SortByPriority implements Comparator<ResCloudlet>
{
	public int compare(ResCloudlet a, ResCloudlet b)
	{
		return a.getPriority() - b.getPriority();
	}
}