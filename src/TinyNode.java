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

	public CachedBlockchain publicBlockchain;
	public CachedBlockchain privateBlockchain;
	public int privateBranchLen = 0;
	public ArrayList<Block> blocksToKeep;

	public TinyNode(String prefix) { 
		super(prefix);
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
		af.publicBlockchain = new CachedBlockchain();

		/** SELFISH MINING FIELDS **/
		af.privateBlockchain = new CachedBlockchain();
		af.isSelfish = false;
		af.privateBranchLen = 0;
		af.blocksToKeep = new ArrayList<>();

		return af;
	}

	@Override
	public void nextCycle(Node node, int pid) {	
		
		if(publicBlockchain.nodeID == -1) {
			publicBlockchain.nodeID = node.getID();
			privateBlockchain.nodeID = node.getID();
		}

		
		int val = (int) (SharedInfo.random.nextFloat()*100);
		if (val < SharedInfo.transGenerationThreshold) {
			broadcastNewTransaction(node, pid);
		}		
	}
	
	private void broadcastNewTransaction(Node node, int pid) {
		long nodeID = node.getID();

		if(!isSelfish) {
			// Pick a random amount of bitcoins (from UTXO) > 0
			// Create a transaction ( choose a dest node at random )
			Transaction t = publicBlockchain.buildTransaction(nodeID);

			if (t != null) { // Node has > 0 BTCs
				// update your UTXO (use receivedTransaction)
				publicBlockchain.receiveTransaction(t);

				//TODO CONTROLLA se va fatto
				privateBlockchain.receiveTransaction(t);

				// Broadcast the block
				broadcastMessage(node, pid, new TinyCoinMessage(TinyCoinMessage.TRANSACTION, t, node.getID()));

			}
		}
		else {

			// TODO cosa fare se sono selfish e ricevo transazioni? Costruisco una che mi interessa!
			// TODO CONTROLLA PER BENE cosa devo fare se ricevo una transazione. E' importante definire un comportamento
			// Per ora facciamo così

			Transaction t = privateBlockchain.buildTransaction(nodeID);

			if (t != null) { // Node has > 0 BTCs
				// update your UTXO (use receivedTransaction)
				privateBlockchain.receiveTransaction(t);

				// TODO I'm not sure it will be accepted! CONSEQUENTLY SOME BLOCKS MAY BE REFUSED BY PUBLIC BLOCKCHAINS!
				publicBlockchain.receiveTransaction(t);

				// Broadcast the block
				broadcastMessage(node, pid, new TinyCoinMessage(TinyCoinMessage.TRANSACTION, t, node.getID()));
			}
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

			/** SELFISH MINING **/
			if(isSelfish) {

				int deltaPrev = privateBlockchain.head.height - publicBlockchain.head.height;
				Block block = privateBlockchain.mineBlock(nodeID);

				if(block != null) {
					blocksToKeep.add(block);
					privateBranchLen++;
					if (deltaPrev == 0 && privateBranchLen == 2) {
						privateBranchLen = 0;
						// BROADCASTS ALL PRIVATE NODES

						System.err.println("Selfish miner " + nodeID + " broadcasting all private blocks ");

						for (Block b : blocksToKeep)
							broadcastMessage(node, pid, new TinyCoinMessage(TinyCoinMessage.BLOCK, b, node.getID()));
						blocksToKeep.clear();
					}
					else {
						System.err.println("Selfish miner " + nodeID + " hiding block " + block.blockID);
					}
				}
			/** HONEST MINING **/
			} else {
				Block block = publicBlockchain.mineBlock(nodeID);

				if (block != null) { // There were transactions to mine ( In the case of SELFISH mining, I have decided to broadcast )
					// Broadcast the block
					System.out.println("Broadcasting MINED block " + block.blockID + " isSelfish = " + isSelfish);
					broadcastMessage(node, pid, new TinyCoinMessage(TinyCoinMessage.BLOCK, block, node.getID()));
				}
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

			boolean broadcast = false;
			if(publicBlockchain.receiveTransaction(t))
				broadcast = true;
			if(privateBlockchain.receiveTransaction(t))
				broadcast = true;

			if(broadcast)
				broadcastMessage(node, pid, msg);
		}
		// If it is a Block, append it to the blockchain (execute algorithm) AND not received yet
		// (Notice: if its father has not been received, keep it in a local cache)
		else if (msg.type == TinyCoinMessage.BLOCK) {

			Block b = (Block) msg.message;

			/** SELFISH MINING **/
			if(isSelfish) {


				if(b.blockID == -1) { // GENESIS BLOCK
					publicBlockchain.receiveBlock(b);
					privateBlockchain.receiveBlock(b);
					return;
				}

				int deltaPrev = privateBlockchain.head.height - publicBlockchain.head.height;

				if(receiveBlock(b, publicBlockchain)) {
					if(deltaPrev == 0) {
						privateBlockchain = new CachedBlockchain(publicBlockchain);
						privateBranchLen = 0;
					}
					else if (deltaPrev == 1) {
						// Publish last block of private chain
						assert blocksToKeep.size() == 1;

						System.err.println("Selfish miner publishing last block of private chain");

						for (Block privateBlock : blocksToKeep)
							broadcastMessage(node, pid, new TinyCoinMessage(TinyCoinMessage.BLOCK, privateBlock, node.getID()));
					}
					else if (deltaPrev == 2) {
						// Publish all of the private chain

						assert blocksToKeep.size() == 2;

						System.err.println("Selfish miner publishing all the private blocks (2)");
						for (Block privateBlock : blocksToKeep)
							broadcastMessage(node, pid, new TinyCoinMessage(TinyCoinMessage.BLOCK, privateBlock, node.getID()));
					}
					else {
						assert blocksToKeep.size() > 2;
						// Publish first unpublished block
						System.err.println("Selfish miner publishing first unpublished block");
						broadcastMessage(node, pid, new TinyCoinMessage(TinyCoinMessage.BLOCK, blocksToKeep.remove(0), node.getID()));
					}
				}

			/** HONEST MINING **/
			} else {
				if(receiveBlock(b, publicBlockchain)) {
					//System.out.println("Node " + node.getID() + " added new BLOCK "+ ((Block)msg.message).blockID  +"at time " + CommonState.getTime());
					broadcastMessage(node, pid, msg);
				}
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


	public boolean receiveBlock(Block block, CachedBlockchain blockchain) {
		return blockchain.receiveBlock(block);
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