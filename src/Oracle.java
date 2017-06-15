import java.util.List;
import peersim.config.*;
import peersim.core.*;
import peersim.edsim.EDSimulator;

public class Oracle implements Control{

	private static final String PAR_PROT = "protocol";
	private int pid;

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
		
		else {
			sendMined();
			sendCountdown = setCountdown(); // reset
		}
		
		return false; // continue execution
	}
	
	
	private void sendMined() {
		int power = (int) (SharedInfo.random.nextFloat()*100);
		SharedInfo sI = SharedInfo.getSharedInfo();
		
		long chosenNode = -1;
		
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
			
		// Send the message
		System.out.println("Sending \"mined\" msg through protocol " + Configuration.lookupPid(pid) + " to node " + chosenNode);
		//System.out.println("Which is a " + sI.miners.get(chosenNode));
		
		TinyCoinMessage message = new TinyCoinMessage(TinyCoinMessage.MINED, 0);
		long delay = 0;
		Node dest = sI.idToNode.get(chosenNode);

		EDSimulator.add(delay, message, dest, pid); // 0 DELAY HERE!
				
	}

	private long pickAtRandom(List<Long> set) {
		return set.get(SharedInfo.random.nextInt(set.size()));
	}
	
	private int setCountdown() {
		return (int) SharedInfo.random.nextGaussian()*SharedInfo.oracleVariance + SharedInfo.oracleMean;
	}

}
