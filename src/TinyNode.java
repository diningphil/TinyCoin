import java.util.ArrayList;

import peersim.cdsim.CDProtocol;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;
import peersim.vector.SingleValueHolder;

public class TinyNode extends SingleValueHolder implements CDProtocol, EDProtocol, Linkable {

	private ArrayList<Node> neighbours;
	private int nodeType;
	private CachedBlockchain localBlockchain;

	public TinyNode(String prefix) { 
		super(prefix);
	}

	/**
	 * This is the default mechanism of peersim to create 
	 * copies of the objects. To generate a new average protocol,
	 * peersim will call this clone method.
	 */
	public Object clone() // Questo metodo è più importante di una brillante carriera in CS...
	{
		TinyNode af = null;
		af = (TinyNode) super.clone();
		af.neighbours = new ArrayList<Node>();
		af.localBlockchain = new CachedBlockchain(); 
		return af;
	}

	@Override
	public void nextCycle(Node node, int pid) {	
		int val = (int) (SharedInfo.random.nextFloat()*100);
		if (val < SharedInfo.transGenerationThreshold) {
			broadcastNewTransaction(node, pid);
		}		
	}
	
	private void broadcastNewTransaction(Node node, int pid) {
		long nodeID = node.getID();
		
		// Pick a random amount of bitcoins (from UTXO) > 0
		// Create a transaction ( choose a dest node at random )
		Transaction t = localBlockchain.buildTransaction(nodeID);
	
		if(t != null) { // Node has > 0 BTCs
			// update your UTXO (use receivedTransaction)
			localBlockchain.receiveTransaction(t);
			
			// Broadcast the block
			broadcastMessage(node, pid, new TinyCoinMessage(TinyCoinMessage.TRANSACTION, t, node.getID()));			
		}
//		else {
//			System.out.println("Node " + nodeID + " has 0 bitcoins");
//		}
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		
		TinyCoinMessage msg = (TinyCoinMessage) event;
	
		//System.out.println("Node " + node.getID() + " received msg id: " + msg.type + " at time " + CommonState.getTime());
		
		
		switch(nodeType) {
		case SharedInfo.NORMAL:
			normalHandle(node, pid, msg);
			break;
		case SharedInfo.CPU_MINER:
			minerHandle(node, pid, msg);
			break;
		case SharedInfo.GPU_MINER:
			minerHandle(node, pid, msg);
			break;
		case SharedInfo.FPGA_MINER:
			minerHandle(node, pid, msg);
			break;
		case SharedInfo.ASIC_MINER:
			minerHandle(node, pid, msg);
			break;
		default:
			System.err.println("Invalid node type, fix your program");
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

	private void minerHandle(Node node, int pid, TinyCoinMessage msg) {
		long nodeID = node.getID();
		
		// If it is MINED message
		if(msg.type == TinyCoinMessage.MINED) {
			System.out.println("Node " + node.getID() + " received MINED at time " + CommonState.getTime());
			
			Block block = localBlockchain.mineBlock(nodeID);
		
			if (block != null) { // There were transactions to mine	
				// Broadcast the block
				System.out.println("Broadcasting MINED block " + block.blockID);
				broadcastMessage(node, pid, new TinyCoinMessage(TinyCoinMessage.BLOCK, block, node.getID()));
			} else {
				System.out.println("NULL BLOCK");
			}
		} else {
			normalHandle(node, pid, msg);
		}
	}

	private void normalHandle(Node node, int pid, TinyCoinMessage msg) {
		
		// If it is a Transaction, broadcast AND not received yet 
		// (receivedTransaction returns false if it must be discarded)

		
		if(msg.type == TinyCoinMessage.TRANSACTION) {
			Transaction t = (Transaction) msg.message;
			
			//System.err.println("Node " + node.getID() + " received TRANSACTION " + t.transID + "at time " + CommonState.getTime() + " from " + msg.sender);
			
			if(localBlockchain.receiveTransaction(t))
				broadcastMessage(node, pid, msg);
		}
		// If it is a Block, append it to the blockchain (execute algorithm) AND not received yet
		// (Notice: if its father has not been received, keep it in a local cache) TODO TO BE IMPLEMENTED
		else if (msg.type == TinyCoinMessage.BLOCK) {
			
			if(addBlock((Block)msg.message)) {
				//System.out.println("Node " + node.getID() + " received BLOCK at time " + CommonState.getTime());
				broadcastMessage(node, pid, msg);
			}
		}
		
	}

	private void broadcastMessage(Node node, int pid, TinyCoinMessage msg) {
		
		Transport tr = (Transport) node.getProtocol(FastConfig.getTransport(pid));

		for (Node n: neighbours) { // TODO With WireKOut I have a fixed number of neighbours. Better insert a DROP prob
			tr.send(node, n, msg, pid);
		}
		
	}


	public boolean addBlock(Block genesisBlock) {
		return localBlockchain.addBlock(genesisBlock);	
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