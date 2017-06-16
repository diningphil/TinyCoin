import java.util.HashMap;

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
	private HashMap<Long, Transaction> UTXO, memPoolOfTransactions;
	
	public void addBlock(Block block) {
		
		if(head == null)
			head = block;
		else {
			if(blockchain.containsKey(block.prevBlockID)) {
				block.height = blockchain.get(block.prevBlockID).height + 1;
				
				if(block.height > head.height) {
					head = block;
				}
			}
			computeUTXO(block);
			cleanMemoryPool(block);
		}
	}

	public void computeUTXO(Block start) { // go backwards till the genesis block (do not worry about forks)	
		
	}
	
	private void cleanMemoryPool(Block block) {
		//for(Transaction t: block.transactions.keySet().) {
			
		//}
	}
	
	private boolean receiveTransaction(Transaction t) {
		
		//if(memPoolOfTransactions.containsKey(t.transID)) return false; // already there
		//if (UTXO.containsKey(t.transID))
		
		/** there is only one input
		for each input (h, i) in t do
		*/
		
		if output (h, i) is not in local UTXO // (or signature invalid NOT FOR THIS PRJ) \\	
			then
				Drop t and stop
			end if
		end for

		/** This check is unnecessary, only one input, one output with same bitcoin value
		//if sum of values of inputs < sum of values of new outputs then
			Drop t and stop	
		end if
		*/
		
		for each input (h, i) in t do	
			Remove (h, i) from local UTXO
		end for
				
		// Append t to local memory pool	
		//memPoolOfTransactions.put(t.transID, t); return true; // added
				
		// todo in protocol:
		// Forward t to neighbors in the Bitcoin network
	}
}
