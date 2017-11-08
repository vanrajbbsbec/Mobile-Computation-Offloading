/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */


package org.cloudbus.cloudsim.examples;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerPriorityBased;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.File;
import org.cloudbus.cloudsim.HarddriveStorage;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.ParameterException;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.SanStorage;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

/**
 * An example showing how to create
 * scalable simulations.
 */
@SuppressWarnings("unused")
public class CloudSimExample6 {

	/** The cloudlet list. */
	private static List<Cloudlet> cloudletList;

	/** The vmlist. */
	private static List<Vm> vmlist;

	private static List<Vm> createVM(int userId, int vms) {

		//Creates a container to store VMs. This list is passed to the broker later
		LinkedList<Vm> list = new LinkedList<Vm>();

		//VM Parameters
		long size = 10000; //image size (MB)
		int ram = 512; //vm memory (MB)
		int mips = 1000;
		long bw = 1000;
		int pesNumber = 1; //number of cpus
		String vmm = "Xen"; //VMM name

		//create VMs
		Vm[] vm = new Vm[vms];

		for(int i=0;i<vms;i++){
			//vm[i] = new Vm(i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
			//for creating a VM with a space shared scheduling policy for cloudlets:
			vm[i] = new Vm(i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerPriorityBased());

			list.add(vm[i]);
		}

		return list;
	}


	private static List<Cloudlet> createCloudlet(int userId, int cloudlets){
		// Creates a container to store Cloudlets
		LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

		//cloudlet parameters
		//long length = 1000;
		//long fileSize = 300;
		//long outputSize = 300;
		
		long length = 50000;
		long fileSize = 30000;
		long outputSize = 30000;
		
		int priority = 0;
		
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet[] cloudlet = new Cloudlet[cloudlets];

		for(int i=0;i<cloudlets;i++){
			priority = ThreadLocalRandom.current().nextInt(1, 5 + 1);
			cloudlet[i] = new Cloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel, priority);
			// setting the owner of these Cloudlets
			cloudlet[i].setUserId(userId);
			list.add(cloudlet[i]);
			cloudlet[i].addRequiredFile("File" + i);
		}

		return list;
	}


	////////////////////////// STATIC METHODS ///////////////////////

	/**
	 * Creates main() to run this example
	 */
	public static void main(String[] args) {
		Log.printLine("Starting CloudSimExample6...");

		try {
			// First step: Initialize the CloudSim package. It should be called
			// before creating any entities.
			int num_user = 1;   // number of grid users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;  // mean trace events
			int CloudletsToBeCreated = 200;

			// Initialize the CloudSim library
			CloudSim.init(num_user, calendar, trace_flag);

			// Second step: Create Datacenters
			//Datacenters are the resource providers in CloudSim. We need at list one of them to run a CloudSim simulation
			@SuppressWarnings("unused")
			Datacenter datacenter0 = createDatacenter("Datacenter_0");
			
			int randomeFileSize = 0;
			for(int i=0; i<CloudletsToBeCreated; i++)
			{
				randomeFileSize = ThreadLocalRandom.current().nextInt(100, 5000 + 1);
				datacenter0.addFile(new File("File" + i, randomeFileSize));
			}
			
			@SuppressWarnings("unused")
			Datacenter datacenter1 = createDatacenter("Datacenter_1");
			for(int i=0; i<CloudletsToBeCreated; i++)
			{
				randomeFileSize = ThreadLocalRandom.current().nextInt(100, 5000 + 1);
				datacenter1.addFile(new File("File" + i, randomeFileSize));
			}
			
			@SuppressWarnings("unused")
			Datacenter datacenter2 = createDatacenter("Datacenter_2");
			for(int i=0; i<CloudletsToBeCreated; i++)
			{
				randomeFileSize = ThreadLocalRandom.current().nextInt(100, 5000 + 1);
				datacenter2.addFile(new File("File" + i, randomeFileSize));
			}

			//Third step: Create Broker
			DatacenterBroker broker = createBroker();
			int brokerId = broker.getId();
			

			//Fourth step: Create VMs and Cloudlets and send them to broker
			vmlist = createVM(brokerId,20); //creating 20 vms
			cloudletList = createCloudlet(brokerId,CloudletsToBeCreated); // creating 100 cloudlets

			broker.submitVmList(vmlist);
			broker.submitCloudletList(cloudletList);

			// Fifth step: Starts the simulation
			CloudSim.startSimulation();

			// Final step: Print results when simulation is over
			//List<Cloudlet> newList = broker.getCloudletReceivedList();
			List<Cloudlet> newList = broker.getCloudletSubmittedList();

			CloudSim.stopSimulation();

			printCloudletList(newList);
			
			saveCloudletListCSV(newList);

			Log.printLine("CloudSimExample6 finished!");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
		}
	}

	private static Datacenter createDatacenter(String name) throws ParameterException{

		// Here are the steps needed to create a PowerDatacenter:
		// 1. We need to create a list to store one or more
		//    Machines
		List<Host> hostList = new ArrayList<Host>();

		// 2. A Machine contains one or more PEs or CPUs/Cores. Therefore, should
		//    create a list to store these PEs before creating
		//    a Machine.
		List<Pe> peList1 = new ArrayList<Pe>();

		int mips = 100000;

		// 3. Create PEs and add these into the list.
		//for a quad-core machine, a list of 4 PEs is required:
		peList1.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating
		peList1.add(new Pe(1, new PeProvisionerSimple(mips)));
		peList1.add(new Pe(2, new PeProvisionerSimple(mips)));
		peList1.add(new Pe(3, new PeProvisionerSimple(mips)));

		//Another list, for a dual-core machine
		List<Pe> peList2 = new ArrayList<Pe>();

		peList2.add(new Pe(0, new PeProvisionerSimple(mips)));
		peList2.add(new Pe(1, new PeProvisionerSimple(mips)));

		//4. Create Hosts with its id and list of PEs and add them to the list of machines
		int hostId=0;
		int ram = 4096; //host memory (MB)
		long storage = 1000000; //host storage
		int bw = 10000;

		hostList.add(
    			new Host(
    				hostId,
    				new RamProvisionerSimple(ram),
    				new BwProvisionerSimple(bw),
    				storage,
    				peList1,
    				new VmSchedulerTimeShared(peList1)
    			)
    		); // This is our first machine

		hostId++;

		hostList.add(
    			new Host(
    				hostId,
    				new RamProvisionerSimple(ram),
    				new BwProvisionerSimple(bw),
    				storage,
    				peList2,
    				new VmSchedulerTimeShared(peList2)
    			)
    		); // Second machine


		//To create a host with a space-shared allocation policy for PEs to VMs:
		//hostList.add(
    	//		new Host(
    	//			hostId,
    	//			new CpuProvisionerSimple(peList1),
    	//			new RamProvisionerSimple(ram),
    	//			new BwProvisionerSimple(bw),
    	//			storage,
    	//			new VmSchedulerSpaceShared(peList1)
    	//		)
    	//	);

		//To create a host with a oportunistic space-shared allocation policy for PEs to VMs:
		//hostList.add(
    	//		new Host(
    	//			hostId,
    	//			new CpuProvisionerSimple(peList1),
    	//			new RamProvisionerSimple(ram),
    	//			new BwProvisionerSimple(bw),
    	//			storage,
    	//			new VmSchedulerOportunisticSpaceShared(peList1)
    	//		)
    	//	);


		// 5. Create a DatacenterCharacteristics object that stores the
		//    properties of a data center: architecture, OS, list of
		//    Machines, allocation policy: time- or space-shared, time zone
		//    and its price (G$/Pe time unit).
		String arch = "x86";      // system architecture
		String os = "Linux";          // operating system
		String vmm = "Xen";
		double time_zone = 10.0;         // time zone this resource located
		double cost = 3.0;              // the cost of using processing in this resource
		double costPerMem = 0.05;		// the cost of using memory in this resource
		double costPerStorage = 0.1;	// the cost of using storage in this resource
		double costPerBw = 0.1;			// the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>();	//we are not adding SAN devices by now

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);
		
		storageList.add(new SanStorage("hdd1", 10000000, 1000, 10));


		// 6. Finally, we need to create a PowerDatacenter object.
		Datacenter datacenter = null;
		try {
			datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;
	}

	//We strongly encourage users to develop their own broker policies, to submit vms and cloudlets according
	//to the specific rules of the simulated scenario
	private static DatacenterBroker createBroker(){

		DatacenterBroker broker = null;
		try {
			broker = new DatacenterBroker("Broker");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return broker;
	}

	/**
	 * Prints the Cloudlet objects
	 * @param list  list of Cloudlets
	 */
	private static void printCloudletList(List<Cloudlet> list) {
		int size = list.size();
		Cloudlet cloudlet;

		String indent = "\t";
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID" + indent +  indent + "STATUS" + indent +
				"Data center ID" + indent + indent + indent + "VM ID" + indent + indent + "Time" + indent + indent + "Start Time" + indent + indent + "Finish Time" + indent + indent +
				"Input File Size" + indent + indent + "Output File Size" + indent + "Battery Life");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);

			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
				Log.print("SUCCESS");

				Log.printLine( indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId() +
						 indent + indent + dft.format(cloudlet.getActualCPUTime()) +
						indent + indent + dft.format(cloudlet.getExecStartTime())+ indent + indent + indent + dft.format(cloudlet.getFinishTime()) + indent + indent + indent + dft.format(cloudlet.getCloudletFileSize())
								+ indent + indent + indent + dft.format(cloudlet.getCloudletOutputSize()) + indent + indent + indent + dft.format(cloudlet.getAfterBatteryLife()) + indent + indent + dft.format(cloudlet.getBeforeBatteryLife()));
			}
		}

	}
	
	
	private static void saveCloudletListCSV(List<Cloudlet> list) throws IOException {
		java.util.Date d = new java.util.Date();
		String file_name = "";
		String separator = ",";
		if (file_name == "") {
			file_name = "c:/csv/cloudlets_csv" + d.getTime() + ".csv";
		}
		FileWriter writer = new FileWriter(file_name);
		int size = list.size();
		Cloudlet cloudlet;
		
		DecimalFormat dft = new DecimalFormat("###.##");
		
		StringBuilder sb = new StringBuilder();
		sb.append("Cloudlet ID");
		sb.append(separator);
		
		sb.append("STATUS");
		sb.append(separator);
		
		sb.append("PRIORITY");
		sb.append(separator);
		
		sb.append("Data center ID");
		sb.append(separator);
		
		sb.append("VM ID");
		sb.append(separator);
		
		sb.append("Time");
		sb.append(separator);
		
		sb.append("Start Time");
		sb.append(separator);
		
		sb.append("Finish Time");
		sb.append(separator);
		
		sb.append("Input File Size");
		sb.append(separator);
		
		sb.append("Output File Size");
		sb.append(separator);
		
		sb.append("Battery Before Exec");
		sb.append(separator);
		
		sb.append("Battery After Exec");
		sb.append(separator);
		
		sb.append("Required Files");
		//sb.append(separator);
		
		sb.append(System.getProperty("line.separator"));
		
		
		
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			sb.append(cloudlet.getCloudletId());
			sb.append(separator);
			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
				sb.append("SUCCESS");
				sb.append(separator);
			}
			else if(cloudlet.getCloudletStatus() == Cloudlet.CANCELED)
			{
				sb.append("CANCELED");
				sb.append(separator);
			}
			else if(cloudlet.getCloudletStatus() == Cloudlet.QUEUED)
			{
				sb.append("QUEUED");
				sb.append(separator);
			}
			
			sb.append(cloudlet.getPriority());
			sb.append(separator);
			
			sb.append(cloudlet.getResourceId());
			sb.append(separator);
			sb.append(cloudlet.getVmId());
			sb.append(separator);
			sb.append(dft.format(cloudlet.getActualCPUTime()));
			sb.append(separator);
			sb.append(dft.format(cloudlet.getExecStartTime()));
			sb.append(separator);
			sb.append(dft.format(cloudlet.getFinishTime()));
			sb.append(separator);
			sb.append(dft.format(cloudlet.getCloudletFileSize()));
			sb.append(separator);
			sb.append(dft.format(cloudlet.getCloudletOutputSize()));
			sb.append(separator);
			sb.append(dft.format(cloudlet.getBeforeBatteryLife()));
			sb.append(separator);
			sb.append(dft.format(cloudlet.getAfterBatteryLife()));
			sb.append(separator);
			sb.append(cloudlet.requiredFileSize);
			
			
			sb.append(System.getProperty("line.separator"));
		}
		
		writer.write(sb.toString());
	    writer.close();

		
	}


//	private static double getRequiredFileNames(List<String> requiredFiles) {
//		// TODO Auto-generated method stub
//		double time = 0.0;
//		String fileNames = "";
//		Iterator<String> Iterator = requiredFiles.iterator();
//		while (Iterator.hasNext()) {
//			String fileName = Iterator.next();
//			for (int i = 0; i < getStorageList().size(); i++) {
//				Storage tempStorage = getStorageList().get(i);
//				File tempFile = tempStorage.getFile(fileName);
//				if (tempFile != null) {
//					time += tempFile.getSize() / tempStorage.getMaxTransferRate();
//					break;
//				}
//			}
//		}
//		return time;
//	}
	
	
}
