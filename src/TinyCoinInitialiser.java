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
	private int pid = 0;

	private int selfishAssigned = 0;
	private int asicAssigned = 0;

	public TinyCoinInitialiser(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);	
	}
	
	@Override
	public void initialize(Node node) {
		TinyNode tinyProtocol = (TinyNode) node.getProtocol(pid);
		long nodeID = node.getID();
			
		double normal_or_miner = SharedInfo.random.nextDouble();

		SharedInfo sI = SharedInfo.getSharedInfo();
	
		sI.idToNode.put(nodeID, node);
		
		if (normal_or_miner < SharedInfo.normal) {
			sI.normals.add(nodeID);
			tinyProtocol.setType(SharedInfo.NORMAL);
		} else {

			boolean ok = false;
			while(!ok) {
				double value = SharedInfo.random.nextDouble();

				if (value <= SharedInfo.cpu) {
					// create a cpu miner
					sI.cpus.add(nodeID);
					sI.miners.put(nodeID, SharedInfo.CPU_MINER);
					tinyProtocol.setType(SharedInfo.CPU_MINER);

					double selfish = SharedInfo.random.nextDouble();
					if(selfish <= SharedInfo.prob_cpu_selfish && selfishAssigned <= SharedInfo.max_selfishMiners) {
						tinyProtocol.setSelfish(true);
						sI.selfish.add((int) nodeID);
						selfishAssigned++;
					}
					ok = true;

				} else {

					if (value <= SharedInfo.gpu) {
						// create a gpu miner
						sI.gpus.add(nodeID);
						sI.miners.put(nodeID, SharedInfo.GPU_MINER);
						tinyProtocol.setType(SharedInfo.GPU_MINER);

						double selfish = SharedInfo.random.nextDouble();
						if (selfish <= SharedInfo.prob_gpu_selfish && selfishAssigned <= SharedInfo.max_selfishMiners) {
							tinyProtocol.setSelfish(true);
							sI.selfish.add((int) nodeID);
							selfishAssigned++;
						}
						ok = true;

					} else if (value <= SharedInfo.fpga) {
						// create a fpga miner
						sI.fpgas.add(nodeID);
						sI.miners.put(nodeID, SharedInfo.FPGA_MINER);
						tinyProtocol.setType(SharedInfo.FPGA_MINER);

						double selfish = SharedInfo.random.nextDouble();
						if (selfish <= SharedInfo.prob_fpga_selfish && selfishAssigned <= SharedInfo.max_selfishMiners) {
							tinyProtocol.setSelfish(true);
							sI.selfish.add((int) nodeID);
							selfishAssigned++;
						}
						ok = true;

					} else if(asicAssigned < SharedInfo.maxAsic) { // otherwise choose another type of miner (useful to simulate pools with just 1 node)
						// create an asic miner
						sI.asics.add(nodeID);
						sI.miners.put(nodeID, SharedInfo.ASIC_MINER);
						tinyProtocol.setType(SharedInfo.ASIC_MINER);
						asicAssigned++;

						double selfish = SharedInfo.random.nextDouble();
						if (selfish <= SharedInfo.prob_asic_selfish && selfishAssigned <= SharedInfo.max_selfishMiners) {
							tinyProtocol.setSelfish(true);
							sI.selfish.add((int) nodeID);
							selfishAssigned++;
						}
						ok = true;
					}
				}
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
