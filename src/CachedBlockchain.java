import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

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
	 * - discard inconsistencies
	 */
	
	private Block head;
	
	// TODO I should keep just the UTXO (but I would need output transactions, not just bitcoins). I could use a structure that keeps block ids
	private HashMap<Long, Block> blockchain;
	
	
	/** DEBUG */
	public TreeSet<Long> receivedBlockIDs;
	public TreeSet<Long> refusedBlockIDs;
	private TreeSet<Long> waitingBlockIDs;
	
	private HashSet<Long> confirmedTransactions; // Those added to the block chain TODO DA RIVEDERE!! NON VA BENE CON LE FORK
	
	
	private ArrayList<Integer> UTXO; // bitcoin address --> amount of bitcoins in the blockchain (considering the longest chain)
	
	// These fields are useful to simulate a local pool of transactions
	private ArrayList<Integer> tempUTXO; // bitcoin address --> temporary amount of bitcoins, needed to accept a transaction
	private HashMap<Long, Transaction> memPoolOfTransactions;
	private ArrayList<Long> orderedTransactionsInPool; // FIFO QUEUE

	
	// What if I receive a block but not yet its predecessor? Eventually I am sure I will receive it
	private HashMap<Long, ArrayList<Block>> waitingBlocks; // The key corresponds to the blockID that I am waiting for
	
	private void zeroUTXO(){
		for(int i = 0; i < Network.size(); i++)
			UTXO.set(i, 0);;
	}
	
	public CachedBlockchain() {
		blockchain = new HashMap<>();
		
		receivedBlockIDs = new TreeSet<>();
		refusedBlockIDs = new TreeSet<>();
		waitingBlockIDs = new TreeSet<>();
		
		confirmedTransactions = new HashSet<>();
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
		
		if (nodeBTCs == 0)
			return null;
		else if(nodeBTCs < 0) {
			System.err.println("Bitcoins for " + nodeID + " are < 0! " + nodeBTCs);
			System.out.println(blockchainToJSON());
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
		quello che non capisco è: se ho potuto costruire un blocco significa che io ho potuto 
		accettare le transazioni. Quindi devo assicurarmi solamente che il blocco sia valido nel
		caso venga appeso alla blockchain. a quel punto son sicuro che le transazioni che ci 
		metto non sono a caso! Possibile che l'ordine di ricezione non vada bene? Va bene per me,
		ma non per gli altri in principio! Quindi devo basarmi sulla blockchain. lo stato può divergere localmente
		ma la blockchain alla fine sarà la stessa con alta probabilità.
		*/
		// IMPORTANT: I KNOW THAT addBlock WILL BE CALLED LATER: IT'S MY ASSUMPTION! THIS METHOD
		// SHOULD BE PRIVATE!!
		
		
		// TODO: DANGEROUS CAST TO INT! SORT THINGS OUT!
		Block b = new Block((int) nodeID, head.blockID);
	
		ArrayList<Integer> helperUTXO = (ArrayList<Integer>) UTXO.clone();
			
		int added = 0;
		for(int i = 0; i < orderedTransactionsInPool.size() && i < SharedInfo.maxTransPerBlock; i++) {
			Transaction t = memPoolOfTransactions.get(orderedTransactionsInPool.get(i));
			if(canSpendMoney(helperUTXO, t)) { // at least one transaction can be added
				b.addTransaction(t);			
				added++;
				
				int srcAmount = helperUTXO.get(t.srcAddress);
				helperUTXO.set(t.srcAddress, srcAmount - t.bitcoins);
				int destAmount = helperUTXO.get(t.destAddress);
				helperUTXO.set(t.destAddress, destAmount + t.bitcoins);
			}	
		}
		
		if (added > 0) {
			b.blockID = SharedInfo.getNextBlockID();		
			if(receiveBlock(b)) return b;
			else { System.err.println("Add block should be true!"); return null; }
		}
		
		return null;
	}
	
	public boolean receiveBlock(Block block) {
		
		if(blockchain.containsKey(block.blockID)) // I should not keep the block chain, but it is useful for debugging and for the project
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

			// TODO
			/*ATTENZIONE SEMBRA CHE IO, QUANDO DEVO AGGIUNGERE UN BLOCCO CHE CREA UNA FORK, GUARDI ALLA UTXO CORRENTE,
			CHE PERO VA DALLA TESTA IN GIU, OVVERO DALL ALTRO BLOCCO !! QUINDI QUESTO NON VA ASSOLUTAMENTE BENE.
			
			POTREI GUARDARE SI LA BLOCKCHAIN (CHE DATE LE ASSUNZIONI è COME LA UTXO),
			MA SE INSERISCO UN TIMESTAMP NEL BLOCCO QUANDO LO MINO (=> LE TRANSAZIONI HANNO TIMESTAMP MINORE),
			PER VEDERE SE LA TRANSAZIONE NON C è, BASTA ISPEZIONARE LA BLOCKCHAIN FINO A QUANDO NON ARRIVO AD UN TIMESTAMP MINORE
			
			NEL CASO IL BLOCCO NON ABBIA COME PREDECESSORE HEAD, SI POTREBBE CREARE UNA FORK O NE STO ESTENDENDO UNA. SOLO IN QUESTO CASO IO VADO A RICALCOLARE
			LA UTXO A PARTIRE DA ME STESSO. SE POSSO AGGIUNGERE IL BLOCCO (E IL MIO PROGRAMMA CREA SOLO BLOCCHI VALIDI PER ORA, QUINDI SI)
			ALLORA SE IL NUOVO BRANCH è DIVENTATO PIù LUNGO, DEVO DISFARE DALLA HEAD FINO AL PUNTO DI BRANCH E APPLICARE I NUOVI BLOCCHI ALLA UTXO
			QUINDI IO APPLICO LE MODIFICHE A UTXO SOLO NEL CASO CHE SIA NEL BRANCH MAGGIORE.
			
		
			PERò ADESSO, COME FACCIO A DIRE SE POSSO AGGIUNGERE UN BLOCCO NELLA BRANCH PIù CORTA? NON POSSO USARE Nè UTXO
			Nè tempUTXO!
			*/
			
			if(block.blockID == 48){
				System.out.print(blockchainToJSON());
				//C'è un problema: nonostante le blockchains siano uguali, uno di loro ha un valore diverso per il nodo 53..
			}
			
			// This is necessary because I do not use a tree to represent the blockchain
			if(block.prevBlockID == head.blockID) {
				// I want to append on the local longest path 
				// I can use the local UTXO
				
				if(isBlockValid(block, UTXO)) { // this function recomputes and already updates the UTXO. if the block is not valid, the state is kept consistent
					
					receivedBlockIDs.add(block.blockID); // DEBUGGING PURPOSES
					
					
					block.height = blockchain.get(block.prevBlockID).height + 1;
					
					blockchain.put(block.blockID, block);

					for(Transaction t: block.transactions)
						confirmedTransactions.add(t.transID);
					
					if(block.height > head.height) {
						head = block;
					} else {
						System.err.println("This cannot happen, fix your program");
					}
					
					// the block has been successfully added to the blockchain. If the same block is the parent of one/many of the waiting blocks => handle the situation
					if(waitingBlocks.containsKey(block.blockID)) {
						System.err.println("Block " + block.blockID + " was necessary to other blocks");
						System.err.flush();
						ArrayList<Block> blocksToAdd = waitingBlocks.get(block.blockID);
						for(int i = 0; i < blocksToAdd.size(); i++)
							receiveBlock(blocksToAdd.get(i));
				
						waitingBlocks.remove(block.blockID);
					}
					
					// If the block has been accepted, then cleanMemoryPool
					cleanupMemoryPool(block);
			
				}else {
					refusedBlockIDs.add(block.blockID);
					return false;
				}
			} else {
				// I want to create or extend another path which does not correspond to the longest
				// I need to efficiently compute the UTXO for the branch
				ArrayList<Integer> forkedUTXO = computeForkedUTXO(block.prevBlockID);
				if(isBlockValid(block, forkedUTXO)) { // this function recomputes and already updates the UTXO. if the block is not valid, the state is kept consistent
					
					receivedBlockIDs.add(block.blockID); // DEBUGGING PURPOSES
					
					
					block.height = blockchain.get(block.prevBlockID).height + 1;
					
					blockchain.put(block.blockID, block);

					for(Transaction t: block.transactions) {
						if(!confirmedTransactions.contains(t.transID))
							System.err.println("I had not received transaction " + t.transID);
						confirmedTransactions.add(t.transID);
					}
					
					if(block.height > head.height) {
						head = block;
					}
					
					// the block has been successfully added to the blockchain. If the same block is the parent of one/many of the waiting blocks => handle the situation
					if(waitingBlocks.containsKey(block.blockID)) {
						System.err.println("Block " + block.blockID + " was necessary to other blocks");
						System.err.flush();
						ArrayList<Block> blocksToAdd = waitingBlocks.get(block.blockID);
						for(int i = 0; i < blocksToAdd.size(); i++)
							receiveBlock(blocksToAdd.get(i));
				
						waitingBlocks.remove(block.blockID);
					}
					
					// If the block has been accepted, then cleanMemoryPool
					cleanupMemoryPool(block);
			
				}else {
					refusedBlockIDs.add(block.blockID);
					return false;
				}
				
			}
			
		} else { // block with ID "prevBlockID" has not been received yet 
		
			// See P2P Q&A on Moodle
			
			if( ! waitingBlocks.containsKey(block.prevBlockID) ) 
				waitingBlocks.put(block.prevBlockID, new ArrayList<Block>());
			
			waitingBlockIDs.add(block.blockID);
			
			
			ArrayList<Block> blocksToAdd = waitingBlocks.get(block.prevBlockID);
			
			for(int i = 0; i < blocksToAdd.size(); i++)
				if (blocksToAdd.get(i).blockID == block.blockID)
					return false; // already present
	
			//if(head.height == 251)
			//	System.out.print("");
			
			System.out.println("Putting block "+ block.blockID +" in waiting queue for " + block.prevBlockID + " my blockchain has max height " + head.height);
			if(block.height - head.height > 200) {		
				System.out.println(receivedBlockIDs.toString());
				System.out.println(waitingBlockIDs.toString());
				System.out.println(refusedBlockIDs.toString());
			}
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
				
			if(canSpendMoney(currentUTXO, t)) { // TODO DEVE PARTIRE DAL MIO BLOCCO, NON DALLA UTXO CORRENTE, VEDI FORK!
				
			
				//!confirmedTransactions.contains(t.transID)
				
				accepted.add(t);
				// Modify the temporary UTXO (needed for creation of the block)
				int srcAmount = currentUTXO.get(t.srcAddress);
				currentUTXO.set(t.srcAddress, srcAmount - t.bitcoins);
				int destAmount = currentUTXO.get(t.destAddress);
				currentUTXO.set(t.destAddress, destAmount + t.bitcoins);
			}else {
				
				if(confirmedTransactions.contains(t.transID)) 
					System.err.println("The block "+ block.blockID +" with prevBlockID "+ block.prevBlockID +" contains transaction "+ t.transID +" already confirmed (in a previous block in the blockchain)");
					System.err.println(blockchainToJSON());
				
				// REVERT ACCEPTED TRANSACTIONS!
				for(Transaction t2: accepted){
					// NOTE: REVERTED SIGN!
					int srcAmount = currentUTXO.get(t2.srcAddress);
					currentUTXO.set(t2.srcAddress, srcAmount + t2.bitcoins); // + instead of -
					int destAmount = currentUTXO.get(t2.destAddress);
					currentUTXO.set(t2.destAddress, destAmount - t2.bitcoins); // - instead of +
				}
				return false;
			}
		}
		return true;
	}

	public ArrayList<Integer> computeForkedUTXO(long blockID) {
		
		// The height of the block is lower or equal than the head. From the head, go back until you reach
		// the same height, then go backwards until you find the common parent, from which the fork was originated 
		// At this point, compute the UTXO of the shorter branch
		
		
		Block forkEnd = blockchain.get(blockID);
		Block tmpBlock = head;
		ArrayList<Integer> forkedUTXO = (ArrayList<Integer>) UTXO.clone();
		
		assert head.height >= forkEnd.height;
		
		while(tmpBlock.height > forkEnd.height) {
			
			// UNDO CHANGES!
			for(Transaction t : tmpBlock.transactions) {
				int srcAmount = forkedUTXO.get(t.srcAddress);
				forkedUTXO.set(t.srcAddress, srcAmount + t.bitcoins); // + instead of -
			
				int destAmount = forkedUTXO.get(t.destAddress);
				forkedUTXO.set(t.destAddress, destAmount - t.bitcoins); // - instead of +
			}
				tmpBlock = blockchain.get(tmpBlock.prevBlockID);
		}
	
		// I have reached the same height, go back until you find the father
		Block tmpFork = forkEnd;
		ArrayList<Block> visitedBlocksInForkedBranch = new ArrayList<>();
		
		while(tmpBlock.blockID != tmpFork.blockID) {
			// UNDO CHANGES!
			for(Transaction t : tmpBlock.transactions) {
				int srcAmount = forkedUTXO.get(t.srcAddress);
				forkedUTXO.set(t.srcAddress, srcAmount + t.bitcoins); // + instead of -
			
				int destAmount = forkedUTXO.get(t.destAddress);
				forkedUTXO.set(t.destAddress, destAmount - t.bitcoins); // - instead of +
			}
			
			visitedBlocksInForkedBranch.add(tmpFork);
			tmpFork = blockchain.get(tmpFork.prevBlockID);
			tmpBlock = blockchain.get(tmpBlock.prevBlockID);
			
		}
		
		assert tmpBlock.blockID == tmpFork.blockID;
		
		// Now we have discovered the point where the fork was created
		for(int i = visitedBlocksInForkedBranch.size() - 1; i >= 0; i--) {
			// The last added is the first in the forked blockchain branch
			for(Transaction t : visitedBlocksInForkedBranch.remove(i).transactions) {
				int srcAmount = forkedUTXO.get(t.srcAddress);
				forkedUTXO.set(t.srcAddress, srcAmount - t.bitcoins);
			
				int destAmount = forkedUTXO.get(t.destAddress);
				forkedUTXO.set(t.destAddress, destAmount + t.bitcoins);
			}
		}
		
		/* DEBUG: CHECK THAT ALL NODES HAVE AN AMOUNT OF BITCOIN >= 0, otherwise you have done smth wrong */
		for(int i = 0; i < forkedUTXO.size(); i++) {
			if(forkedUTXO.get(i) < 0) {
				System.err.println("FORKED UTXO BROKEN! value = " + forkedUTXO.get(i) + " for node " + i + " time " + CommonState.getTime());		
				System.exit(0);
			}
		}
		
		return forkedUTXO;				
	}

	private void cleanupMemoryPool(Block block) {
		// Remember that tempUTXO is an helper structure for the memPool
		tempUTXO = (ArrayList<Integer>) UTXO.clone();
		
		for(Transaction t: block.transactions) {
			if(memPoolOfTransactions.containsKey(t.transID)) {
				memPoolOfTransactions.remove(t.transID);
				if(!orderedTransactionsInPool.remove(t.transID))
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
		
		// TODO un problema è quindi quello che liberare la mem pool significa uccidere anche le transazioni già ricevute. Però alcune di esse dopo l'AGGIUNTA di un blocco
		// potrebbero non essere più valide, e dovrei controllarlo. Però in questo modo una transazione ancora valida non viene persa potenzialmente da tutti.
		// A QUESTO PUNTO, SE MI TENGO GLI ID DELLE TRANSAZIONI GIA' RICEVUTE, TOGLIENDO QUELLE CHE HO AGGIUNTO NELLA BLOCKCHAIN, SO QUALI DEVO MANTENERE E EVITO IL CASO LIMITE.
		// PERO' RIGUARDA ALGORITMI DOVE SI PARLA DELLA MEMPOOL ANCHE. CHE SI INTENDE PER CLEAR? :) :) FORSE SI INTENDONO PROPRIO QUESTI ULTIMI DISCORSI
		// Questo nasce dal fatto che un miner inizia a minare le transazioni che ha, e quelle che riceve dopo amen.
		// Dopo tutto, la UTXO reale contiene transazioni di output, quindi pure il loro id	
		// C'è anche il caso in cui io aggiungo un blocco con una transazione. Se poi quella transazione è stata aggiunta alla blockchain, me lo devo ricordare!!
		// Altrimenti rischio di ricevere un blocco (o elaborare un blocco che stava aspettando il padre) e posso riaggiungere la stessa transazione!
		
		
		if(memPoolOfTransactions.containsKey(t.transID) || confirmedTransactions.contains(t.transID) || !canSpendMoney(tempUTXO, t)) {
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
