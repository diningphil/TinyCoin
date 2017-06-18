import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.dynamics.NodeInitializer;

public class TinyCoinInitialiser implements NodeInitializer, Control {

	/*
	 * The protocol to reference.
	 * "PAR_PROTID" contains the parameter name to be used in the config file.
	 * "pid" will contains the value of the protocol referenced.
	 */
	private static final String PAR_PROT = "protocol";
	int pid = 0;
	
	public TinyCoinInitialiser(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);	
	}
	
	@Override
	public void initialize(Node node) {
		TinyNode tinyProtocol = (TinyNode) node.getProtocol(pid);
		long nodeID = node.getID();
			
		int normal_or_miner = (int) (SharedInfo.random.nextFloat()*100);
		SharedInfo sI = SharedInfo.getSharedInfo();
	
		sI.idToNode.put(nodeID, node);
		
		if (normal_or_miner < SharedInfo.normal) {
			sI.normals.add(nodeID);
			tinyProtocol.setType(SharedInfo.NORMAL);
		} else {
			int value = (int) (SharedInfo.random.nextFloat()*100);
			
			if (value <= SharedInfo.cpu) {
				// create a cpu miner
				sI.cpus.add(nodeID);
				sI.miners.put(nodeID, SharedInfo.CPU_MINER);
				tinyProtocol.setType(SharedInfo.CPU_MINER);
				
			} else if (value <= SharedInfo.gpu) {
				// create a gpu miner
				sI.gpus.add(nodeID);
				sI.miners.put(nodeID, SharedInfo.GPU_MINER);
				tinyProtocol.setType(SharedInfo.GPU_MINER);
			
			} else if (value <= SharedInfo.fpga) {
				// create a fpga miner
				sI.fpgas.add(nodeID);
				sI.miners.put(nodeID, SharedInfo.FPGA_MINER);
				tinyProtocol.setType(SharedInfo.FPGA_MINER);
			
			} else {
				// create an asic miner
				sI.asics.add(nodeID);
				sI.miners.put(nodeID, SharedInfo.ASIC_MINER);
				tinyProtocol.setType(SharedInfo.ASIC_MINER);
			}
		}	
	}

	@Override
	public boolean execute() {
		for(int i = 0; i < Network.size(); i++)
			initialize(Network.get(i));
		
		
		return false;
	}
}
