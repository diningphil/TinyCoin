import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import peersim.core.Network;

public class CachedBlockchain {
	/** 
	 * This class maintains:
	 * - memory pool of received transactions (which are not in confirmed blocks)
	 * - blockchain
	 * - UTXO
	 * - ?
	 * And it provides utilities to
	 * - update blockchain and handle forks
	 * - confirm blocks and assign rewards
	 * - select a group of transactions to put into a block
	 * - discard inconsistencies (if I'm mining a block whose transactions are already in the blockchain etc.)
	 */
	
	private Block head;
	
	private HashMap<Long, Block> blockchain;
	private ArrayList<Integer> UTXO; // bitcoin address --> amount of bitcoins
	private HashMap<Long, Transaction> memPoolOfTransactions;

	public CachedBlockchain() {
		blockchain = new HashMap<>();
		UTXO = new ArrayList<>(Network.size());
			for(int i = 0; i < Network.size(); i++)
				UTXO.add(i, 0);;
		
		memPoolOfTransactions = new HashMap<>();
	}

	public Transaction buildTransaction(long nodeID) {

		// TODO: DANGEROUS CAST TO INT! SORT THINGS OUT!
		int nodeBTCs = UTXO.get((int) nodeID);
		
		if (nodeBTCs <= 0) // TODO Metti == 0 quando hai risolto problema
			return null;
		
		int transBTCs = SharedInfo.random.nextInt(nodeBTCs) + 1; // avoid 0 BTC transactions
		int destNode = SharedInfo.random.nextInt(Network.size());

		// TODO: DANGEROUS CAST TO INT! SORT THINGS OUT!
		return new Transaction(SharedInfo.getNextTransactionID(), transBTCs, (int) nodeID, destNode);
		
	}
	
	public Block buildBlock(long nodeID) {
	
		int counter = 0;
		ArrayList<Long> keys = new ArrayList<>(memPoolOfTransactions.keySet());
		
		// TODO: DANGEROUS CAST TO INT! SORT THINGS OUT!
		Block b = new Block((int) nodeID, head.blockID);
	
		while(counter < memPoolOfTransactions.size() && counter < SharedInfo.maxTransPerBlock) {
			// Remove transactions at random (best would be the oldest)
			int index = SharedInfo.random.nextInt(keys.size());
			Transaction t = memPoolOfTransactions.get(keys.get(index));
			keys.remove(index);			
			b.addTransaction(t.transID, t.bitcoins, t.srcAddress, t.destAddress);
			
			counter++;
		}
		
		if (counter > 0) return b;
		
		return null;
	}
	
	public boolean addBlock(Block block) {
		
		if(block.prevBlockID == -1 && head == null) { // Check if I'm adding the genesis block
			
			head = block;
			
			blockchain.put(block.blockID, block);
			
			computeUTXO(head);
			cleanMemoryPool(head);
			
		} else if(blockchain.containsKey(block.prevBlockID)) {
								
			block.height = blockchain.get(block.prevBlockID).height + 1;	
			blockchain.put(block.blockID, block);
				
			if(block.height > head.height) {
				head = block;
			}
			
			computeUTXO(head);
			cleanMemoryPool(head);
	
		} else {
			// TODO see P2P Q&A on Moodle
			System.out.println("NOT YET IMPLEMENTED, prevBlockID requested is " + block.prevBlockID);
			return false;
		}
		return true;
	}
	
	public void computeUTXO(Block start) { // go backwards till the genesis block (do not worry about forks)	
		boolean genesisBlockFound = false;
		
		Block tmpBlock = start;
		int sixConfirmRule = 0;
		
		//il problema è che finche un blocco non viene accettato i bitcoin non si modificano! non posso usare roba a caso
		
		do {
			if(tmpBlock.prevBlockID == -1) genesisBlockFound = true;
			
			if(sixConfirmRule >= 6) {
				// Confirm the block and assign rewards and bitcoins to those who performed a transaction
				tmpBlock.confirmed = true;
				
				Iterator<Long> it = tmpBlock.transactions.keySet().iterator();
				while(it.hasNext()) {
					Transaction t = tmpBlock.transactions.get(it.next());
					
					if(!genesisBlockFound) {
						int srcAmount = UTXO.get(t.srcAddress);
						UTXO.set(t.srcAddress, srcAmount - t.bitcoins);
					}
					
					int destAmount = UTXO.get(t.destAddress);
					UTXO.set(t.destAddress, destAmount + t.bitcoins);
				}
				
				// Assign reward to the miner if I'm not on the genesis block
				if(!genesisBlockFound)
					UTXO.set(tmpBlock.minerID, UTXO.get(tmpBlock.minerID) + SharedInfo.blockReward + tmpBlock.extraReward);
			} else
				assert tmpBlock.confirmed == false;
			
			sixConfirmRule++;
			
			// Assign the parent of the current block to tmpBlock
			if(blockchain.containsKey(tmpBlock.prevBlockID))
					tmpBlock = blockchain.get(tmpBlock.prevBlockID);
			
		} while(!genesisBlockFound);
		
		/** CHECK THAT ALL NODES HAVE AN AMOUNT OF BITCOIN >= 0, otherwise you have done smth wrong */
		for(int i = 0; i < UTXO.size(); i++) {
			if(UTXO.get(i) < 0) System.err.println("UTXO BROKEN! value = " + UTXO.get(i) + " for node " + i);
			
			/** DEBUG */
			  if(head.height == 2) {
			 		System.out.println(blockchainToJSON());
			 		System.exit(0);
			   }
			 /**/
		}
		
	}

	private void cleanMemoryPool(Block block) {
		Iterator<Long> it = block.transactions.keySet().iterator();
		while(it.hasNext()) {
			memPoolOfTransactions.remove(it.next());
		}
	}
	
	public boolean receiveTransaction(Transaction t) {
		// Algorithm taken from slide "Receiving a transaction"
		/** there is only one input
		for each input (h, i) in t do
		*/
		
		/** It does not make sense to check an output if each transaction is a simple transfer of money
		 * from a to b. It is unspecified in the project whether we must find a set of input transactions that
		 * matches the output or not, and it would imply the creation of new transactions to reflect the "change".
		 * For the moment assume this version
		 
		if output (h, i) is not in local UTXO // (or signature invalid NOT FOR THIS PRJ) \\	
			then
				Drop t and stop
			end if
		end for
		*/
		int ownedAmount = UTXO.get(t.srcAddress); 
		if (ownedAmount < t.bitcoins) {
			// It may be possible that I have not received other transactions that make the amount of bitcoins owned by an account increase!
			//System.out.println(t.srcAddress + " does not have enough money in my local state! "+ownedAmount + " vs " + t.bitcoins + ". Discarding"); return false; // discard
		}
		
		/** This check is unnecessary, only one input, one output with same bitcoin value
		//if sum of values of inputs < sum of values of new outputs then
			Drop t and stop	
		end if
		*/
		
		/** for each input (h, i) in t do	
	 	  *   Remove (h, i) from local UTXO
		  * end for */
		UTXO.set(t.srcAddress, UTXO.get(t.srcAddress) - t.bitcoins);
		UTXO.set(t.destAddress, UTXO.get(t.destAddress) + t.bitcoins);
		
		// Append t to local memory pool	
		memPoolOfTransactions.put(t.transID, t);
				
		// todo in protocol:
		// Forward t to neighbors in the Bitcoin network
		
		return true;
	}
	
	private String blockchainToJSON() {
		String res = "{ blockchain: [";
		Block curr = null;
		if(head != null) {
			do {
				if(curr == null)
					curr = head;
				else
					curr = blockchain.get(curr.prevBlockID);
				
				res += curr.toJSON();			
				if(curr.prevBlockID != -1) {
					res += ",";
				}
			} while(curr.prevBlockID != -1);
			
		} else {
			return "";
		}
		res += "]}";
		return res;
	}
}
