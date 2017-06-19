import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import peersim.core.CommonState;
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
	
	// TODO I should keep just the UTXO (but I would need output transactions, not just bitcoins)
	private HashMap<Long, Block> blockchain;
	private ArrayList<Integer> UTXO; // bitcoin address --> amount of bitcoins
	
	
	private HashMap<Long, Transaction> memPoolOfTransactions;
	private ArrayList<Long> orderedTransactionsInPool; // FIFO QUEUE

	private void zeroUTXO(){
		for(int i = 0; i < Network.size(); i++)
			UTXO.set(i, 0);;
	}
	
	public CachedBlockchain() {
		blockchain = new HashMap<>();
		UTXO = new ArrayList<>(Network.size());
			for(int i = 0; i < Network.size(); i++)
				UTXO.add(i, 0);;
	
		memPoolOfTransactions = new HashMap<>();
		orderedTransactionsInPool = new ArrayList<>();
	}

	public Transaction buildTransaction(long nodeID) {

		// TODO: DANGEROUS CAST TO INT! SORT THINGS OUT!
		int nodeBTCs = UTXO.get((int) nodeID);
		
		if (nodeBTCs <= 0) // TODO Metti == 0 quando hai risolto problema
			return null;
		
		int transBTCs = SharedInfo.random.nextInt(nodeBTCs) + 1; // avoid 0 BTC transactions
		int destNode = SharedInfo.random.nextInt(Network.size());

		// TODO: DANGEROUS CAST TO INT! SORT THINGS OUT!
		Transaction newTrans = new Transaction(SharedInfo.getNextTransactionID(), transBTCs, (int) nodeID, destNode);

		return newTrans;
	}
	
	public Block mineBlock(long nodeID) {
		
		// Assumption: after buildBlock addBlock is called
		Block block = buildBlock(nodeID); // pass the minerID
		
		if(block != null) {
			// Compute algo di ricezione blocco, false se non va bene
			if(addBlock(block)) return block;			
		}
		return null;
	}
	
	private Block buildBlock(long nodeID) {
		
		/*
		quello che non capisco è: se ho potuto costruire un blocco significa che io ho potuto 
		accettare le transazioni. Quindi devo assicurarmi solamente che il blocco sia valido nel
		caso venga appeso alla blockchain. a quel punto son sicuro che le transazioni che ci 
		metto non sono a caso! Possibile che l'ordine di ricezione non vada bene? Va bene per me,
		ma non per gli altri in principio! Quindi devo basarmi sulla blockchain. lo stato può divergere localmente
		ma la blockchain alla fine sarà la stessa con alta probabilità.
		*/
		// IMPORTANT: I KNOW THAT THE BLOCK WILL BE ADDED LATER: IT'S MY ASSUMPTION! THIS METHOD
		// SHOULD BE PRIVATE!!
		
		
		// TODO: DANGEROUS CAST TO INT! SORT THINGS OUT!
		Block b = new Block((int) nodeID, head.blockID);
	
		computeUTXO(head); 
		
		int size = orderedTransactionsInPool.size();

		int i = 0;
		for(i = 0; i < orderedTransactionsInPool.size() && i < SharedInfo.maxTransPerBlock; i++) {
			// No overhead due to left shifting of the array
			Transaction t = memPoolOfTransactions.get(orderedTransactionsInPool.remove(size-1));
			
			if(iCanSpendMoney(t)) {
				b.addTransaction(t.transID, t.bitcoins, t.srcAddress, t.destAddress);			
				// Modify the temporary UTXO (needed for creation of the block)
				int srcAmount = UTXO.get(t.srcAddress);
				UTXO.set(t.srcAddress, srcAmount - t.bitcoins);
				int destAmount = UTXO.get(t.destAddress);
				UTXO.set(t.destAddress, destAmount + t.bitcoins);
			}
			
			size--;
		}
		
		if (i > 0) return b;
		
		return null;
	}
	
	public boolean addBlock(Block block) {
		
		/*
		Inoltre può accadere che per alcuni il blocco contenga transazioni non conosciute
		Si potrebbero incorporare le transazioni non conosciute? Potrebbe non funzionare lo stesso.
				
		In sostanza, come mi assicuro che il blocco verrà accettato "eventually"?
		Ho bisogno che la blockchain sia consistente.
		
		Possibile soluzione: quando creo il blocco, mi assicuro che sia consistente con la blockchain locale!
		E gestisco il fatto della Q&A.
		*/
		
		if(blockchain.containsKey(block.blockID)) 
			return false; // already present
	
		
		if(block.prevBlockID == -1 && head == null) { // Check if I'm adding the genesis block			
		// Executed just 1 time
			
			head = block;
			
			blockchain.put(block.blockID, block);
			
			computeUTXO(head);
			cleanMemoryPool(head);
			
		} else if(blockchain.containsKey(block.prevBlockID)) {

			if(isBlockValid(block)) { // recomputes and already updates the UTXO. if the block is not valid, just recomputes UTXO
			
				block.height = blockchain.get(block.prevBlockID).height + 1;
				block.confirmed = false; // Not necessary, given prj assumptions
				blockchain.put(block.blockID, block);
				
				if(block.height > head.height) {
					head = block;
				}
				//System.out.println("Time " + CommonState.getTime());
				//System.out.println(blockchainToJSON());
				cleanMemoryPool(head);
				
			}else {
				cleanMemoryPool(head);
				return false;
			}			
	
		} else {
			// TODO see P2P Q&A on Moodle
			System.out.println("NOT YET IMPLEMENTED, prevBlockID requested is " + block.prevBlockID);
			return false;
		}
		return true;
	}
	
	private boolean isBlockValid(Block block) {
		 TODO è troppo inefficiente? SI. Tieniti una copia di UTXO che non viene modificata
		 * e che serve solo per quando aggiungi un blocco. Per controllare se è valido, puoi evitare computeUTXO.
		 * Al più duplichi solo la struttura per quando devi ricominciare a lavorare sulle transazioni
		 * */
		
		computeUTXO(head); 
		
		ArrayList<Transaction> accepted = new ArrayList<>();
		
		for (Transaction t: block.transactions.values()) {
			if(iCanSpendMoney(t)) {
				accepted.add(t);
				// Modify the temporary UTXO (needed for creation of the block)
				int srcAmount = UTXO.get(t.srcAddress);
				UTXO.set(t.srcAddress, srcAmount - t.bitcoins);
				int destAmount = UTXO.get(t.destAddress);
				UTXO.set(t.destAddress, destAmount + t.bitcoins);
			}else {
				// REVERT ACCEPTED TRANSACTIONS!
				for(Transaction t2: accepted){
					// NOTE: REVERTED SIGN!
					int srcAmount = UTXO.get(t2.srcAddress);
					UTXO.set(t2.srcAddress, srcAmount + t2.bitcoins); // + instead of -
					int destAmount = UTXO.get(t2.destAddress);
					UTXO.set(t2.destAddress, destAmount - t2.bitcoins); // - instead of +
				}
				return false;
			}
		}
		return true;
	}

	public void computeUTXO(Block start) { // go backwards till the genesis block (do not worry about forks)	
		boolean genesisBlockFound = false;
		
		Block tmpBlock = start;
		int sixConfirmRule = 0;
		
		zeroUTXO();
		
		do {
			if(tmpBlock.prevBlockID == -1) genesisBlockFound = true;
			
			if(sixConfirmRule >= 6 || tmpBlock.confirmed == true) {
				// Confirm the block (for no particular reasons :) )
				tmpBlock.confirmed = true;
			}
			sixConfirmRule++;
			
			Iterator<Long> it = tmpBlock.transactions.keySet().iterator();
			while(it.hasNext()) {
				long nextTransID = it.next();
				Transaction t = tmpBlock.transactions.get(nextTransID);
								
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
					
			
			// Assign the parent of the current block to tmpBlock
			if(blockchain.containsKey(tmpBlock.prevBlockID))
					tmpBlock = blockchain.get(tmpBlock.prevBlockID);
			
		} while(!genesisBlockFound);
		
		/** DEBUG: CHECK THAT ALL NODES HAVE AN AMOUNT OF BITCOIN >= 0, otherwise you have done smth wrong */
		for(int i = 0; i < UTXO.size(); i++) {
			if(UTXO.get(i) < 0) {
				System.err.println("UTXO BROKEN! value = " + UTXO.get(i) + " for node " + i + " time " + CommonState.getTime());	
				// DEBUG 	
				System.out.println(blockchainToJSON());
			 	System.exit(0);
			}
		}				
	}

	private void cleanMemoryPool(Block block) {
//		Iterator<Long> it = block.transactions.keySet().iterator();
//		while(it.hasNext()) {
		memPoolOfTransactions.clear();
		orderedTransactionsInPool.clear();
	//	}
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

		// TODO STOP BROADCASTING. Se non c'è nel memory pool possono essere successe 2 cose:
		// o ho minato un blocco (che però potrebbe non essere stato confermato)
		// oppure non l'ho ricevuta
		// Se io la togliessi solo quando viene confermato il blocco, allora sarei sicuro. Però
		// se io all'istante t devo minare un blocco e la seleziono, e nello stesso istante ricevo un blocco e lei viene confermata,
		// allora rischio (non avendo output(h,i) di fare confusione e far spendere 2 volte la stessa 
		// quantità all'utente. Nel caso la transazione arrivi dopo che ho minato 6 blocchi (impossibile) a quel punto sarebbe un altro problema
		// visto che nel progetto il nodo sceglie i nodi quando gli viene chiesto di minare,
		// a parte il caso limite non potrà scegliere mai una transazione confermata!
		
		if(memPoolOfTransactions.containsKey(t.transID)) {
			return false;
		}
		
		
		if(!iCanSpendMoney(t)) {
			// It may be possible that I have not received other transactions that make the amount of bitcoins owned by an account increase!
			//System.out.println(t.srcAddress + " does not have enough money in my local state! "+ t.bitcoins + " required. Discarding");
			return false;
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
		orderedTransactionsInPool.add(t.transID);
		
		return true;
	}
	
	private boolean iCanSpendMoney(Transaction t) {
		int ownedAmount = UTXO.get(t.srcAddress); 
		if (ownedAmount < t.bitcoins) {
			return false; // discard
		}
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
