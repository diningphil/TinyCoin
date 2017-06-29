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
		TinyNode af;
		af = (TinyNode) super.clone();
		af.neighbours = new ArrayList<>();
		af.publicBlockchain = new CachedBlockchain();

		/* SELFISH MINING FIELDS */
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

		
		double val = SharedInfo.random.nextDouble();
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

				privateBlockchain.receiveTransaction(t);

				// Broadcast the block
				broadcastMessage(node, pid, new TinyCoinMessage(TinyCoinMessage.TRANSACTION, t, node.getID()));

			}
		}
		else {

			// Cosa fare se sono selfish e ricevo transazioni? Costruisco una che mi interessa secondo la private blockchain!!

			Transaction t = privateBlockchain.buildTransaction(nodeID);

			if (t != null) { // Node has > 0 BTCs
				// update your UTXO (use receivedTransaction)
				privateBlockchain.receiveTransaction(t);

				// I'm not sure it will be accepted! CONSEQUENTLY SOME TRANSACTIONS/BLOCKS MAY BE REFUSED BY PUBLIC BLOCKCHAINS!
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

			/* SELFISH MINING */
			if(isSelfish) {

				int deltaPrev = privateBlockchain.head.height - publicBlockchain.head.height;
				Block block = privateBlockchain.mineBlock(nodeID);

				if(block != null) {

					/* I MAY HAVE PUT THE BLOCK INTO THE WAITING QUEUE */
					blocksToKeep.add(block);
					privateBranchLen++;


					/* NOTICE: deltaPrev >= 10 to use only when trying 100% selfish. It disturbs other simulations */
					if (deltaPrev == 0 && privateBranchLen == 2 /*|| deltaPrev >= 40*/) { // Was tie with branch of 1 || after a while publish in any case

						System.out.println("Selfish miner " + nodeID + " broadcasting all private blocks ");

						// broadcast the private chain
						for (Block b : blocksToKeep) { // Pool wins due to the lead of 1
							broadcastMessage(node, pid, new TinyCoinMessage(TinyCoinMessage.BLOCK, b, node.getID()));

							// I will receive it from my neighbours in any case, because of flooding
							receiveBlock(b, publicBlockchain);
						}

						blocksToKeep.clear();
						privateBranchLen = 0;
					} else {
						System.out.println("Selfish miner " + nodeID + " hiding block " + block.blockID + " prevID is " + block.prevBlockID + " private head is " + privateBlockchain.head.blockID);
					}

				} else { // block is null
					System.err.println("Node " + nodeID + ": mining has not produced a block!");
				}

				/* HONEST MINING */
			} else {
				Block block = publicBlockchain.mineBlock(nodeID);

				if (block != null) { // There were transactions to mine ( In the case of SELFISH mining, I have decided to broadcast )
					// Broadcast the block
					System.out.println("Honest miner broadcasting MINED block " + block.blockID + " with prev ID " + block.prevBlockID);
					broadcastMessage(node, pid, new TinyCoinMessage(TinyCoinMessage.BLOCK, block, node.getID()));
				} else {
					System.out.println("No transactions to mine");
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
			if(isSelfish && privateBlockchain.receiveTransaction(t))
				broadcast = true;

			if(broadcast)
				broadcastMessage(node, pid, msg);
		}
		// If it is a Block, append it to the blockchain (execute algorithm) AND not received yet
		// (Notice: if its father has not been received, keep it in a local cache)
		else if (msg.type == TinyCoinMessage.BLOCK) {

			Block b = (Block) msg.message;

			/* SELFISH MINING */
			if(isSelfish) {

				if(b.blockID == -1) { // GENESIS BLOCK
					publicBlockchain.receiveBlock(b);
					privateBlockchain.receiveBlock(b);
					return;
				}

				// It may happen that another selfish miners publishes a block. If I have already published my blocks, I must do nothing

				int deltaPrev = privateBlockchain.head.height - publicBlockchain.head.height;

				if(receiveBlock(b, publicBlockchain)) {

					if(deltaPrev == 0) {
						// They win

						privateBlockchain = new CachedBlockchain(publicBlockchain);
						privateBranchLen = 0;
						blocksToKeep.clear();
					}
					else if (deltaPrev == 1) { /* SAME LENGHT, try our luck, OR ANOTHER FORK HAS HAPPENED; AND DIFFERENCE IS STILL 1 */
						// Publish last block of private chain

						if(blocksToKeep.size() > 0) {
							System.out.println("Selfish miner " + node.getID() + " received  " + b.blockID + " publishing last block of private chain " + blocksToKeep.get(0).blockID);
							Block last = blocksToKeep.remove(0);

							broadcastMessage(node, pid, new TinyCoinMessage(TinyCoinMessage.BLOCK, last, node.getID()));

							receiveBlock(last, publicBlockchain);

						}
					}
					else if (deltaPrev == 2) {
						// Publish the entire private chain

						if(blocksToKeep.size() > 0) {
							System.out.println("Selfish miner "+ node.getID() +" received  " + b.blockID + " publishing all the private blocks");
							for (Block privateBlock : blocksToKeep) {

								broadcastMessage(node, pid, new TinyCoinMessage(TinyCoinMessage.BLOCK, privateBlock, node.getID()));

								receiveBlock(privateBlock, publicBlockchain);
							}
							blocksToKeep.clear();
						}

						privateBranchLen = 0;
					}
					else if(deltaPrev > 2){
						// Publish first unpublished block
						if (blocksToKeep.size() > 0) {
							System.out.println("Selfish miner "+ node.getID() +" received  " + b.blockID +  " publishing first unpublished block " + blocksToKeep.get(0).blockID);

							Block first = blocksToKeep.remove(0);

							broadcastMessage(node, pid, new TinyCoinMessage(TinyCoinMessage.BLOCK, first, node.getID()));

							receiveBlock(first, publicBlockchain);
						}
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