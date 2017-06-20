import java.util.List;
import peersim.config.*;
import peersim.core.*;
import peersim.edsim.EDSimulator;

public class Oracle implements Control{

	private static final String PAR_PROT = "protocol";
	private int pid;
	private Block genesisBlock; 
	
	public Oracle(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
			
		genesisBlock = new Block(-1, -1); // minerID, prevBlockID
		//genesisBlock.confirmed = true;
		
		for(int i = 0; i < Network.size(); i++) { // for each node
			Transaction t = new Transaction(SharedInfo.getNextTransactionID(), SharedInfo.random.nextInt(SharedInfo.maxInitialAmount), -1, i);
			genesisBlock.addTransaction(t);
		}
	
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
		
		if(CommonState.getTime() == 0) { // GENESIS
			SharedInfo sI = SharedInfo.getSharedInfo();
			TinyCoinMessage message = new TinyCoinMessage(TinyCoinMessage.BLOCK, genesisBlock, -1);
			long delay = 0;
			for(long i = 0; i < Network.size(); i++) {
				Node dest = sI.idToNode.get(i);
				EDSimulator.add(delay, message, dest, pid); // 0 DELAY HERE
			}
		}
			
		
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
		SharedInfo sI = SharedInfo.getSharedInfo();
		
		long chosenNode = -1;

		// TODO MAY REQUIRE MUCH TIME WITH FEW NODES!!
		while(chosenNode == -1) {
			int power = (int) (SharedInfo.random.nextFloat()*100);
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
		}
		
		// Send the message
		//System.out.println("Sending \"mined\" msg through protocol " + Configuration.lookupPid(pid) + " to node " + chosenNode);
		//System.out.println("Which is a " + sI.miners.get(chosenNode));
		
		TinyCoinMessage message = new TinyCoinMessage(TinyCoinMessage.MINED, 0, -1);
		long delay = 0;
		Node dest = sI.idToNode.get(chosenNode);

		EDSimulator.add(delay, message, dest, pid); // 0 DELAY HERE!
				
	}

	private long pickAtRandom(List<Long> set) {
		if(set.isEmpty()) return -1;
		return set.get(SharedInfo.random.nextInt(set.size()));
	}
	
	private int setCountdown() {
		return (int) SharedInfo.random.nextGaussian()*SharedInfo.oracleVariance + SharedInfo.oracleMean;
	}

}
