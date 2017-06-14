import java.util.List;
import java.util.Random;

import peersim.config.*;
import peersim.core.*;
import peersim.edsim.EDSimulator;

public class Oracle implements Control{

	private static final String PAR_PROT = "protocol";
	private final int pid;

	public Oracle(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
	}
	
	private static int sendCountdown = -1;
	// TODO VEDI SCHEDULING PER CONTROLLI EVENT DRIVEN (intro in tutorial Mark Jelasity)
	
	/*
	 * This needs to be executed every cycle, keeping
	 * an internal counter to decide if it is time to
	 * send a "mined" message
	 */
	@Override
	public boolean execute() {
	
		if (sendCountdown == -1)
			sendCountdown = setCountdown();
		
		else if(sendCountdown > 0)
			sendCountdown--;
		
		else
			sendMined();
		
		return false; // continue execution
	}
	
	
	private void sendMined() {
		Random r = new Random();	
		int power = (int) r.nextFloat()*100;
		SharedInfo sI = SharedInfo.getSharedInfo();
		
		int chosenNode = -1;
		
		if (power <= SharedInfo.cpuPower)
			// pick a cpu miner
			chosenNode = pickAtRandom(sI.cpus);
		else if (power <= SharedInfo.gpuPower)
			// pick a gpu miner
			chosenNode = pickAtRandom(sI.gpus);
		else if (power <= SharedInfo.fpgaPower)
			// pick a fpga miner
			chosenNode = pickAtRandom(sI.fpgas);
		else 
			// pick an asic miner
			chosenNode = pickAtRandom(sI.asics);
			
		  // TODO SEND MESSAGE to chosenNode
		  EDSimulator.add(0, message, sender, pid);
		
		
		
		
	}

	private int pickAtRandom(List<Integer> set) {
		Random r = new Random();
		return set.get(r.nextInt(set.size()));
	}
	
	private int setCountdown() {
		Random r = new Random();
		return (int) r.nextGaussian()*SharedInfo.oracleVariance + SharedInfo.oracleMean;
	}

}
