import java.util.ArrayList;
import peersim.cdsim.CDProtocol;
import peersim.config.FastConfig;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;
import peersim.transport.Transport;
import peersim.vector.SingleValueHolder;

public class TinyNode extends SingleValueHolder implements CDProtocol, EDProtocol, Linkable {

	private ArrayList<Node> neighbours;
	private int nodeType;
	boolean isSelfish;

	public CachedBlockchain localBlockchain;

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
		
		if(localBlockchain.nodeID == -1)
			localBlockchain.nodeID = node.getID();
		
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
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		
		TinyCoinMessage msg = (TinyCoinMessage) event;
	
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

	}

	private void minerHandle(Node node, int pid, TinyCoinMessage msg) {
		long nodeID = node.getID();
		
		// If it is MINED message
		if(msg.type == TinyCoinMessage.MINED) {
			
			Block block = localBlockchain.mineBlock(nodeID);
		
			if (block != null) { // There were transactions to mine	
				// Broadcast the block
				System.out.println("Broadcasting MINED block " + block.blockID);
				broadcastMessage(node, pid, new TinyCoinMessage(TinyCoinMessage.BLOCK, block, node.getID()));
			}// else {
				//System.out.println("NULL BLOCK => no transactions to mine. It may happen when I have received a block very few time ago, check if data structure is empty");
			//}
		} else {
			normalHandle(node, pid, msg);
		}
	}

	private void normalHandle(Node node, int pid, TinyCoinMessage msg) {
		
		// If it is a Transaction, broadcast AND not received yet 
		// (receivedTransaction returns false if it must be discarded)

		
		if(msg.type == TinyCoinMessage.TRANSACTION) {
			Transaction t = (Transaction) msg.message;
			
			if(localBlockchain.receiveTransaction(t))
				broadcastMessage(node, pid, msg);
		}
		// If it is a Block, append it to the blockchain (execute algorithm) AND not received yet
		// (Notice: if its father has not been received, keep it in a local cache)
		else if (msg.type == TinyCoinMessage.BLOCK) {
			if(receiveBlock((Block)msg.message)) {
				//System.out.println("Node " + node.getID() + " added new BLOCK "+ ((Block)msg.message).blockID  +"at time " + CommonState.getTime());
				broadcastMessage(node, pid, msg);
			}
		}
		
	}

	private void broadcastMessage(Node node, int pid, TinyCoinMessage msg) {
		
		Transport tr = (Transport) node.getProtocol(FastConfig.getTransport(pid));

		for (Node n: neighbours) {
			if(msg.type == TinyCoinMessage.BLOCK)
				EDSimulator.add(SharedInfo.latency + ((Block) msg.message).extraLatency, msg, n, pid);
			else
				tr.send(node, n, msg, pid);
		}
		
	}


	public boolean receiveBlock(Block block) {
		return localBlockchain.receiveBlock(block);	
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

	public void setType(int type) { this.nodeType = type; }
	public void setSelfish(boolean isSelfish) { this.isSelfish = isSelfish; }
}