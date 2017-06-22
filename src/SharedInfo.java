import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import peersim.config.Configuration;
import peersim.core.Node;


// Singleton class to keep information about nodes, their computational power and other things
// Set up by the initializer

public class SharedInfo {
    private static SharedInfo instance = null;

    
    // Since Peersim is sequential (no concurrency) I can use this to be sure that the id is unique
    public static int latestBlockID = 0;     
    public static int transactionID = 0;
	    
    public static final Random random   = new Random(Configuration.getLong("random.seed"));
    
    public static final int NORMAL      = 0,
    						CPU_MINER   = 1,
    						GPU_MINER   = 2,
    						FPGA_MINER  = 3,
    						ASIC_MINER  = 4;
    
    public static final int blockReward = Configuration.getInt("BLOCK_REWARD"),
    						maxTransPerBlock = Configuration.getInt("MAX_TRANS_PER_BLOCK"),
     						latency = Configuration.getInt("LATENCY"),
    						addLatencyPerTransaction = Configuration.getInt("EXTRA_LATENCY_PER_TRANS"),
     						addRewardPerTransaction = Configuration.getInt("EXTRA_REWARD_PER_TRANS"),
     						transGenerationThreshold = Configuration.getInt("PROB_GENERATE_TRANS");

    
    // Gaussian distribution for the oracle
    public static final int oracleMean = Configuration.getInt("ORACLE_MEAN"), 
    		                oracleVariance = Configuration.getInt("ORACLE_VARIANCE");
    
    // Probability to CREATE one type of miner or normal node
    public static final int normal = Configuration.getInt("PROB_NORMAL"),
    						cpu = Configuration.getInt("PROB_CPU"),
    						gpu = Configuration.getInt("PROB_GPU"),
    						fpga = Configuration.getInt("PROB_FPGA"),
    						asic = Configuration.getInt("PROB_ASIC");

    // Probability for a miner to be selfish
    public static final int prob_selfish = Configuration.getInt("PROB_SELFISH"),
                            max_selfishMiners = Configuration.getInt("MAX_SELFISH");

    // Probability to CHOOSE one type of miner
    public static final int cpuPower = Configuration.getInt("PROB_CPU_POWER"),
			gpuPower = Configuration.getInt("PROB_GPU_POWER"),
			fpgaPower = Configuration.getInt("PROB_FPGA_POWER"),
			asicPower = Configuration.getInt("PROB_ASIC_POWER");
 
    public static final int maxInitialAmount = Configuration.getInt("MAX_INITIAL_BITCOINS");
    
    //----------------------------------------------------------------------------------//
    
    // NOTE: it works because the network is kept static!!
    public HashMap<Long, Node> idToNode; // I want a mapping from ID to Node, not from index to node   
    
    public HashMap<Long, Integer> miners; // nodeId --> cpu, gpu, fpga, asic;
    public HashSet<Long> normals;
    public List<Long> cpus;
    public List<Long> gpus;
    public List<Long> fpgas;
    public List<Long> asics;
    
    private SharedInfo() {
    	idToNode = new HashMap<>(); // nodeId --> Node;
    	miners = new HashMap<>();
    	normals = new HashSet<>();
    	cpus  = new ArrayList<Long>();
    	gpus  = new ArrayList<Long>();
    	fpgas  = new ArrayList<Long>();
    	asics  = new ArrayList<Long>();
    }

    public static int getNextBlockID () { return latestBlockID++; } 
    public static int getNextTransactionID () { return transactionID++; } 
    
    public static /*synchronized*/ SharedInfo getSharedInfo() {
        if (instance == null) {
            instance = new SharedInfo();
        }
        return instance;
    }
}