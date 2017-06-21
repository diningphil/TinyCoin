import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import peersim.core.CommonState;
import peersim.core.Network;

public class CachedBlockchain {
	/** 
	 * This class maintains:confirmedTransactions.add
	 * - memory pool of received transactions (which are not in confirmed blocks)
	 * - blockchain
	 * - UTXO
	 * - ?
	 * And it provides utilities to
	 * - update blockchain and handle forks
	 * - confirm blocks and assign rewards
	 * - select a group of transactions to put into a block
	 * - discard inconsistencies
	 */
	
	public long nodeID = -1;

	public Block head;
	
	private HashMap<Integer, Block> blockchain; // This blockchain represents the UTXO if we want, we have only output transactions!
	
	/** DEBUG */
	//public TreeSet<Long> receivedBlockIDs;
	//public TreeSet<Long> refusedBlockIDs;
	//private TreeSet<Long> waitingBlockIDs;
	
	private TreeSet<Integer> acceptedTransactions; // Those added to the longest path in the block chain
	
	public ArrayList<Integer> UTXO; // bitcoin address --> amount of bitcoins in the blockchain (considering the longest chain)
	
	// These fields are useful to simulate a local pool of transactions
	private ArrayList<Integer> tempUTXO; // bitcoin address --> temporary amount of bitcoins, needed to accept a transaction
	
	// TODO make this less memory consuming, while preserving the order of arrival
	private HashMap<Integer, Transaction> memPoolOfTransactions;
	private ArrayList<Integer> orderedTransactionsInPool; // FIFO QUEUE

	
	// What if I receive a block but not yet its predecessor? Eventually I am sure I will receive it
	private HashMap<Integer, ArrayList<Block>> waitingBlocks; // The key corresponds to the blockID that I am waiting for
	
	private void zeroUTXO(){
		for(int i = 0; i < Network.size(); i++)
			UTXO.set(i, 0);;
	}
	
	@SuppressWarnings("unchecked")
	public CachedBlockchain() {
		blockchain = new HashMap<>();
		
		//receivedBlockIDs = new TreeSet<>();
		//refusedBlockIDs = new TreeSet<>();
		//waitingBlockIDs = new TreeSet<>();
		
		acceptedTransactions = new TreeSet<>();
		UTXO = new ArrayList<>(Network.size());
			for(int i = 0; i < Network.size(); i++)
				UTXO.add(i, 0);;

		tempUTXO = (ArrayList<Integer>) UTXO.clone();
		memPoolOfTransactions = new HashMap<>();
		orderedTransactionsInPool = new ArrayList<>();
		
		waitingBlocks = new HashMap<>();
	}

	public Transaction buildTransaction(long nodeID) { // use tempUTXO since

		// TODO: DANGEROUS CAST TO INT! SORT THINGS OUT!
		int nodeBTCs = tempUTXO.get((int) nodeID);
		
		if (nodeBTCs == 0) {
			//System.out.println("Node " + nodeID + " does not have money");
			return null;
		} else if(nodeBTCs < 0) {
			System.err.println("Bitcoins for " + nodeID + " are < 0! " + nodeBTCs);
			System.out.println(longestPathToJSON(head));
			System.exit(0);
		}
		
		int transBTCs = SharedInfo.random.nextInt(nodeBTCs) + 1; // avoid 0 BTC transactions
		int destNode = SharedInfo.random.nextInt(Network.size());

		// TODO: DANGEROUS CAST TO INT! SORT THINGS OUT!
		Transaction newTrans = new Transaction(SharedInfo.getNextTransactionID(), transBTCs, (int) nodeID, destNode);

		return newTrans;
	}
	
	public Block mineBlock(long nodeID) {
		
		Block block = buildBlock(nodeID); // pass the minerID
		
		if(block != null) {
			return block;			
		}
		return null;
	}
	
	private Block buildBlock(long nodeID) {
		
		/*
		Quindi devo assicurarmi solamente che il blocco sia valido nel
		caso venga appeso alla blockchain. a quel punto son sicuro che le transazioni che ci 
		metto non sono a caso! Inoltre l'ordine di ricezione va bene per me,
		ma non per gli altri in principio! Quindi devo basarmi sulla blockchain. lo stato può divergere localmente
		ma la blockchain alla fine sarà la stessa con alta probabilità.
		*/
		
		// TODO: DANGEROUS CAST TO INT! SORT THINGS OUT!
		Block b = new Block((int) nodeID, head.blockID);
	
		@SuppressWarnings("unchecked")
		ArrayList<Integer> helperUTXO = (ArrayList<Integer>) UTXO.clone();
			
		int added = 0;
		for(int i = 0; i < orderedTransactionsInPool.size() && added < SharedInfo.maxTransPerBlock; i++) {
			Transaction t = memPoolOfTransactions.get(orderedTransactionsInPool.get(i));
			if(canSpendMoney(helperUTXO, t)) { // at least one transaction can be added
				b.addTransaction(t);			
				added++;
				
				updateUTXO(t, helperUTXO);
			}	
		}
		
		if (added > 0) {
			b.blockID = SharedInfo.getNextBlockID();	
			
			if(b.blockID == 77) {
				
				System.out.println(longestPathToJSON(head));
				System.out.println("NODE "+nodeID+" Broadcaster UTXO");
				
				for(int i : UTXO)
					System.out.print(i + " ");
				System.out.println();
							
			}
			
			if(receiveBlock(b)) return b;
			else { System.err.println("Add block should be true!"); return null; }
		}
		
		return null;

	}
	// TODO STESSA BLOCKCHAIN MA DIVERSA UTXO?
	public boolean receiveBlock(Block block) {
		
		if(blockchain.containsKey(block.blockID))
			return false; // already present
	
		if(block.blockID == -1 && head == null) { // Check if I'm adding the genesis block			
		// Executed just 1 time
			
			head = block;
			
			blockchain.put(block.blockID, block);
			
			// Assign initial bitcoins
			zeroUTXO();
			
			for(int i = 0; i < block.transactions.size(); i++) {
				Transaction t = block.transactions.get(i);
								
				//int srcAmount = UTXO.get(t.srcAddress);
				//UTXO.set(t.srcAddress, srcAmount - t.bitcoins);
				
				int destAmount = UTXO.get(t.destAddress);
				UTXO.set(t.destAddress, destAmount + t.bitcoins);
			}

			cleanupMemoryPool(head);
			
		} else if(blockchain.containsKey(block.prevBlockID)) {

			// This is necessary because I do not use a tree to represent the blockchain
			if(block.prevBlockID == head.blockID) {
				// I want to append on the local longest path 
				// I can use the local UTXO
				
				if(isBlockValid(block, UTXO)) { // this function recomputes and already updates the UTXO. if the block is not valid, the state is kept consistent
					
					//receivedBlockIDs.add(block.blockID); // DEBUGGING PURPOSES
					
					
					block.height = blockchain.get(block.prevBlockID).height + 1;
					
					blockchain.put(block.blockID, block);

					for(Transaction t: block.transactions)
						if(!acceptedTransactions.contains(t.transID))
							acceptedTransactions.add(t.transID);
					
					if(block.height > head.height) {
						head = block;
					} else {
						System.err.println("This cannot happen, fix your program");
					}
					
					// the block has been successfully added to the blockchain. If the same block is the parent of one/many of the waiting blocks => handle the situation
					if(waitingBlocks.containsKey(block.blockID)) {
						System.out.println("Block " + block.blockID + " was being waited by other blocks");
						ArrayList<Block> blocksToAdd = waitingBlocks.get(block.blockID);
						for(int i = 0; i < blocksToAdd.size(); i++)
							receiveBlock(blocksToAdd.get(i));
				
						waitingBlocks.remove(block.blockID);
					}
					
					// If the block has been accepted, then cleanMemoryPool
					cleanupMemoryPool(block);
			
				}else {
					System.err.println("Normal case");
					System.err.println("NODE "+ nodeID +" Block " + block.blockID + " has been refused. This should not happen");
					System.err.println(longestPathToJSON(head));
					return false;
				}
			} else {
				// I want to create or extend another path which does not correspond to the longest
				// I need to efficiently compute the UTXO for the branch
				// ARGS: the id of my parent in the fork
				TempForkData tmp = computeForkedUTXO(block.prevBlockID);
				
				if(isBlockValid(block, tmp.forkedUTXO)) { // this function recomputes and already updates the UTXO. if the block is not valid, the state is kept consistent
					
					//receivedBlockIDs.add(block.blockID); // DEBUGGING PURPOSES
					
					
					block.height = blockchain.get(block.prevBlockID).height + 1;
					
					blockchain.put(block.blockID, block);

					if(block.height > head.height) {
						int oldHeadID = head.blockID;
						head = block;
						// CRUCIAL!
						UTXO = tmp.forkedUTXO;		
						System.out.println("NODE "+ nodeID +" A forked branch has replaced head "+ oldHeadID +", head is " + head.blockID);
					} else {
						restoreAcceptedTransactions(tmp); // computeForkedUTXO has a post-condition: acceptedTransaction is modified
  					}
					
					
					// the block has been successfully added to the blockchain. If the same block is the parent of one/many of the waiting blocks => handle the situation
					if(waitingBlocks.containsKey(block.blockID)) {
						System.out.println("Block " + block.blockID + " was necessary to other blocks");
						ArrayList<Block> blocksToAdd = waitingBlocks.get(block.blockID);
						for(int i = 0; i < blocksToAdd.size(); i++)
							receiveBlock(blocksToAdd.get(i));
				
						waitingBlocks.remove(block.blockID);
					}
					
					// If the block has been accepted, then cleanMemoryPool
					cleanupMemoryPool(block);
			
				}else {
					System.err.println("Forked case");
					System.err.println("NODE "+ nodeID +" Block " + block.blockID + " has been refused. This should not happen");
					System.err.println(longestPathToJSON(head));
					
				}
			}
			
		} else { // block with ID "prevBlockID" has not been received yet 
		
			// See P2P Q&A on Moodle
			
			if( ! waitingBlocks.containsKey(block.prevBlockID) ) 
				waitingBlocks.put(block.prevBlockID, new ArrayList<Block>());
			
			//waitingBlockIDs.add(block.blockID);
			
			
			ArrayList<Block> blocksToAdd = waitingBlocks.get(block.prevBlockID);
			
			for(int i = 0; i < blocksToAdd.size(); i++)
				if (blocksToAdd.get(i).blockID == block.blockID)
					return false; // already present
	
			//if(head.height == 251)
			//	System.out.print("");
			
			System.out.println("NODE "+ nodeID +" Putting block "+ block.blockID +" in waiting queue for " + block.prevBlockID + " my blockchain has max height " + head.height);
			waitingBlocks.get(block.prevBlockID).add(block);
			return true;
		}
				
		// If an attacker broadcasts invalid nodes? In theory one should verify the PoW. In this implementation I keep and broadcast the "waiting" block.
		return true;
	}
	
	private boolean isBlockValid(Block block, ArrayList<Integer> currentUTXO) {
		 /** Tieniti una copia di UTXO che non viene modificata
		 * e che serve solo per quando aggiungi un blocco. Per controllare se è valido, puoi evitare computeUTXO.
		 * Al più duplichi solo la struttura per quando devi ricominciare a lavorare sulle transazioni
		 * 
		 il fatto è che ho mischiato la mempool con la UTXO. Mantenendo una copia dei bitcoin che riflette la blockchain
		 e una che riflette lo stato locale delle transazioni, rappresento meglio il discorso di togliere un input
		 dalla mempool!
		 */
		
		
		ArrayList<Transaction> accepted = new ArrayList<>();
		
		// The order in which I visit transactions matters. I have just an amount of bitcoins, not a specific output associated to a specific transaction 
		for (int i = 0; i < block.transactions.size(); i++) {
			Transaction t = block.transactions.get(i);
				
			if(acceptedTransactions.contains(t.transID)) { 
				System.err.println("NODE "+ nodeID +" The block "+ block.blockID +" with prevBlockID "+ block.prevBlockID +" contains transaction "+ t.transID +" already confirmed (in a previous block in the blockchain)");
				return false;
			}
			
			if(canSpendMoney(currentUTXO, t)) {
			
				//!confirmedTransactions.contains(t.transID)
				
				accepted.add(t);
				// Modify the temporary UTXO (needed for creation of the block)
				updateUTXO(t, currentUTXO);
				
			}else {
				// This branch should never occur 
				System.err.println("This branch in isBlockValid should never occurr");
				// REVERT ACCEPTED TRANSACTIONS!
				for(Transaction t2: accepted){
					// NOTE: REVERTED SIGN!
					undoUpdateUTXO(t2, currentUTXO);
				}
				return false;
			}
		}
		
		// Assign reward to miner
		if(block.minerID != -1)
			currentUTXO.set(block.minerID, currentUTXO.get(block.minerID) + block.extraReward);
		
		return true;
	}

	// PRE CONDITION: acceptedTransactions has been updated with data in the forked branch,
	//                revert using head (which must point to the other and longest branch)
	private void restoreAcceptedTransactions(TempForkData data) {
		Block tmpBlock = head;
		Block forkEnd = data.forkEnd;
		
		// Remove transactions in the forked branch
		while(forkEnd.blockID != data.bifurcationID) {
			
			for (Transaction t: forkEnd.transactions) {
				// Undo
				acceptedTransactions.remove(t.transID);
			}
			forkEnd = blockchain.get(forkEnd.prevBlockID);
		}
		
		
		// Restore previous ones, belonging to the longest branch
		while(tmpBlock.blockID != data.bifurcationID) {
			
			for (Transaction t: tmpBlock.transactions) {
				// Restore
				acceptedTransactions.add(t.transID);
			}
			tmpBlock = blockchain.get(tmpBlock.prevBlockID);
		}
		
	}
	
	// POST CONDITION: acceptedTransaction is modified to reflect the forkedUTXO.
	// To restore accepted transactions, use the restoreTransactions method
	private TempForkData computeForkedUTXO(int forkHeadID) {
		
		// The height of the block is lower or equal than the head. From the head, go back until you reach
		// the same height, then go backwards until you find the common parent, from which the fork was originated 
		// At this point, compute the UTXO of the shorter branch
		
		
		Block forkEnd = blockchain.get(forkHeadID);
		Block tmpBlock = head;
		
		@SuppressWarnings("unchecked")
		ArrayList<Integer> forkedUTXO = (ArrayList<Integer>) UTXO.clone();
		
		assert head.height >= forkEnd.height;
		
		while(tmpBlock.height > forkEnd.height) {
			
			// UNDO CHANGES!
			for(Transaction t : tmpBlock.transactions) {
				
				undoUpdateUTXO(t, forkedUTXO);
					
				acceptedTransactions.remove(t.transID);
				
			}
			
			// Undo reward 
			if(tmpBlock.minerID != -1)
				forkedUTXO.set(tmpBlock.minerID, forkedUTXO.get(tmpBlock.minerID) - tmpBlock.extraReward);
			
			
			tmpBlock = blockchain.get(tmpBlock.prevBlockID);
		}
	
		// I have reached the same height, go back until you find the father
		Block tmpFork = forkEnd;
		ArrayList<Block> visitedBlocksInForkedBranch = new ArrayList<>();
		
		while(tmpBlock.blockID != tmpFork.blockID) {
			// UNDO CHANGES!
			for(Transaction t : tmpBlock.transactions) {
				
				undoUpdateUTXO(t, forkedUTXO);
				
				acceptedTransactions.remove(t.transID);
				
			}

			// Undo reward 
			if(tmpBlock.minerID != -1)
				forkedUTXO.set(tmpBlock.minerID, forkedUTXO.get(tmpBlock.minerID) - tmpBlock.extraReward);
			
			visitedBlocksInForkedBranch.add(tmpFork);
			tmpFork = blockchain.get(tmpFork.prevBlockID);
			tmpBlock = blockchain.get(tmpBlock.prevBlockID);
						
		}
		
		assert tmpBlock.blockID == tmpFork.blockID;
		
		int bifurcationID = tmpBlock.blockID;
		
		// Now we have discovered the point where the fork was created
		for(int i = visitedBlocksInForkedBranch.size() - 1; i >= 0; i--) {
			// The last added is the first in the forked blockchain branch
			Block visitedBlock = visitedBlocksInForkedBranch.remove(i);
			for(Transaction t : visitedBlock.transactions) {
				
				updateUTXO(t, forkedUTXO);
					
				acceptedTransactions.add(t.transID);

			}
			
			// Assign reward 
			if(visitedBlock.minerID != -1)
				forkedUTXO.set(visitedBlock.minerID, forkedUTXO.get(visitedBlock.minerID) + visitedBlock.extraReward);

		}
		
		/* DEBUG: CHECK THAT ALL NODES HAVE AN AMOUNT OF BITCOIN >= 0, otherwise you have done smth wrong */
		for(int i = 0; i < forkedUTXO.size(); i++) {
			if(forkedUTXO.get(i) < 0) {
				System.err.println("FORKED UTXO BROKEN! value = " + forkedUTXO.get(i) + " for node " + i + " time " + CommonState.getTime());		
				System.exit(0);
			}
		}
		
		return new TempForkData(forkedUTXO, forkEnd, bifurcationID);				
	}

	private void updateUTXO(Transaction t, ArrayList<Integer> currentUTXO) {
		int srcAmount = currentUTXO.get(t.srcAddress);
		currentUTXO.set(t.srcAddress, srcAmount - t.bitcoins);
		int destAmount = currentUTXO.get(t.destAddress);
		currentUTXO.set(t.destAddress, destAmount + t.bitcoins);
	}
	
	private void undoUpdateUTXO(Transaction t, ArrayList<Integer> currentUTXO) {
		int srcAmount = currentUTXO.get(t.srcAddress);
		currentUTXO.set(t.srcAddress, srcAmount + t.bitcoins); // + instead of -
		int destAmount = currentUTXO.get(t.destAddress);
		currentUTXO.set(t.destAddress, destAmount - t.bitcoins); // - instead of +
	}
	
	
	@SuppressWarnings("unchecked")
	private void cleanupMemoryPool(Block block) {
		// Remember that tempUTXO is an helper structure for the memPool
		
		this.tempUTXO = (ArrayList<Integer>) UTXO.clone();
		
		for(Transaction t: block.transactions) {
			if(memPoolOfTransactions.containsKey(t.transID)) {
				memPoolOfTransactions.remove(t.transID);
				if(!orderedTransactionsInPool.remove(new Integer(t.transID)))
					System.err.println("Inconsistency between memPool and orderedTransactions, fix your program!");
			}
		}
	//	}
	}
	
	//Transazioni con id minore dovrebbero essere trattate prima, per evitare starvation!
	
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
		
		It means: if I can spend them then it's ok
		*/

		// STOP BROADCASTING. Se non c'è nel memory pool possono essere successe 2 cose:
		// o ho minato un blocco (che però potrebbe non essere stato confermato)
		// oppure non l'ho ricevuta
		// Se io la togliessi solo quando viene AGGIUNTO il blocco, allora sarei sicuro. SE IL BLOCCO DIVENTA ORFANO LA TRANSAZIONE IN BITCOIN VA RIFATTA, MI VA BENE.
		// se io all'istante t devo minare un blocco e la seleziono, e nello stesso istante ricevo un blocco e lei viene confermata,
		// allora rischio (non avendo output(h,i) di fare confusione e far spendere 2 volte la stessa 
		// quantità all'utente. Nel caso la transazione arrivi dopo che ho minato 6 blocchi (impossibile) a quel punto sarebbe un altro problema
		// visto che nel progetto il nodo sceglie i nodi quando gli viene chiesto di minare,
		// a parte il caso limite non potrà scegliere mai una transazione confermata!
		
		// SOLVED: un problema è quindi quello che liberare la mem pool significa uccidere anche le transazioni già ricevute. Però alcune di esse dopo l'AGGIUNTA di un blocco
		// potrebbero non essere più valide, e dovrei controllarlo. Però in questo modo una transazione ancora valida non viene persa potenzialmente da tutti.
		// A QUESTO PUNTO, SE MI TENGO GLI ID DELLE TRANSAZIONI GIA' RICEVUTE, TOGLIENDO QUELLE CHE HO AGGIUNTO NELLA BLOCKCHAIN, SO QUALI DEVO MANTENERE E EVITO IL CASO LIMITE.
		// PERO' RIGUARDA ALGORITMI DOVE SI PARLA DELLA MEMPOOL ANCHE. CHE SI INTENDE PER CLEAR? :) :) FORSE SI INTENDONO PROPRIO QUESTI ULTIMI DISCORSI
		// Questo nasce dal fatto che un miner inizia a minare le transazioni che ha, e quelle che riceve dopo amen.
		// Dopo tutto, la UTXO reale contiene transazioni di output, quindi pure il loro id	
		// C'è anche il caso in cui io aggiungo un blocco con una transazione. Se poi quella transazione è stata aggiunta alla blockchain, me lo devo ricordare!!
		// Altrimenti rischio di ricevere un blocco (o elaborare un blocco che stava aspettando il padre) e posso riaggiungere la stessa transazione!
		
		
		if(memPoolOfTransactions.containsKey(t.transID) || acceptedTransactions.contains(t.transID) || !canSpendMoney(tempUTXO, t)) {
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
		tempUTXO.set(t.srcAddress, tempUTXO.get(t.srcAddress) - t.bitcoins);
		tempUTXO.set(t.destAddress, tempUTXO.get(t.destAddress) + t.bitcoins);
		
		// Append t to local memory pool	
		memPoolOfTransactions.put(t.transID, t);
		orderedTransactionsInPool.add(t.transID);
		
		return true;
	}
	
	private boolean canSpendMoney(ArrayList<Integer> bitcoinStructure, Transaction t) {
		int ownedAmount = bitcoinStructure.get(t.srcAddress); 
		if (ownedAmount < t.bitcoins) {
			return false; // discard
		}
		return true;
	}

	public String longestPathToJSON(Block start) {
		String res = "{ blockchain: [";
		Block curr = null;
		if(start != null) {
			do {
				if(curr == null)
					curr = start;
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
