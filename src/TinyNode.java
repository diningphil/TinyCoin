import java.util.ArrayList;

import peersim.cdsim.CDProtocol;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.vector.SingleValueHolder;

public class TinyNode extends SingleValueHolder implements CDProtocol, EDProtocol, Linkable {

	private ArrayList<Node> neighbours;
	private int nodeType;
	private CachedBlockchain localBlockchain;

	public TinyNode(String prefix) { 
		super(prefix);
		localBlockchain = new CachedBlockchain();
	}


	@Override
	public void nextCycle(Node node, int protocolID) {
		
		int val = (int) (SharedInfo.random.nextFloat()*100);
		if (val < SharedInfo.transGenerationThreshold)
			broadcastNewTransaction();
		
	}
	
	private void broadcastNewTransaction() {
		
		// Pick a random amount of bitcoins (from UTXO) > 0
		
		// Create a transaction ( choose a dest node at random )
		
		// Broadcast the block
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		
		TinyCoinMessage msg = (TinyCoinMessage) event;
	
		switch(nodeType) {
		case SharedInfo.NORMAL:
			normalHandle(msg);
			break;
		case SharedInfo.CPU_MINER:
			minerHandle(msg);
			break;
		case SharedInfo.GPU_MINER:
			minerHandle(msg);
			break;
		case SharedInfo.FPGA_MINER:
			minerHandle(msg);
			break;
		case SharedInfo.ASIC_MINER:
			minerHandle(msg);
			break;
		default:
			System.err.println("Invalid node type in TinyCoinMessage, fix your program");
		}
		/** DEBUG **
		 * System.out.println("Node " + node.getID() + " of type " + nodeType + ": received message");
		System.out.println("My neighbours are:");

		for(Node n: neighbours){
			System.out.print(n.getID() + " ");
		}
		System.out.println();
		************/
	}

	private void minerHandle(TinyCoinMessage msg) {
		// If it is MINED message AND not received yet
		
			// Pick rand no of "free" trans (not in prev blocks) up to a Max number
		
			// Build a block 
		
			// Compute algo di ricezione blocco
		
			// Broadcast
		
		// Else
		// normal handle
	}

	private void normalHandle(TinyCoinMessage msg) {
		// If it is a Transaction, broadcast AND not received yet
		
		// If it is a Block, append it to the blockchain (execute algorithm) AND not received yet
		// (Notice: if its father has not been received, keep it in a local cache)
		
	}

	public void addBlock(Block genesisBlock) {
		localBlockchain.addBlock(genesisBlock);	
	}

	/**
	 * This is the default mechanism of peersim to create 
	 * copies of the objects. To generate a new average protocol,
	 * peersim will call this clone method.
	 */
	public Object clone()
	{
		TinyNode af = null;
		af = (TinyNode) super.clone();
		af.neighbours = new ArrayList<Node>();
		return af;
	}

	/** ---- OVERRIDE from Linkable
	 * Basic management of the neighbours.
	 * These methods are called by the WireKOut initializer
	 * to create the initial (and final) topology
	 */

	@Override
	public void onKill() {
		neighbours = null;
	}

	@Override
	public int degree()
	{
		return neighbours.size();
	}

	@Override
	public Node getNeighbor(int i) 
	{
		return neighbours.get(i);
	}

	/**
	 * Add a node only if the node is not already present
	 */
	@Override
	public boolean addNeighbor(Node neighbour) 
	{
		if (neighbours.contains(neighbour))
			return false;
		
		neighbours.add(neighbour);
		return true;
	}

	@Override
	public boolean contains(Node neighbor) 
	{
		return neighbours.contains(neighbor);
	}

	@Override
	public void pack() {
		System.out.println("Average.pack() -- not implemented");
	}

	public void setType(int type) {
		this.nodeType = type;
		
	}
}