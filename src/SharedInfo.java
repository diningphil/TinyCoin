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

    public static final Random random   = new Random(Configuration.getLong("random.seed"));
    
    public static final int NORMAL      = 0;
    public static final int CPU_MINER   = 1;
    public static final int GPU_MINER   = 2;
    public static final int FPGA_MINER  = 3;
    public static final int ASIC_MINER  = 4;
    
    public static final int blockReward = 10;
    public static final int addLatencyPerTransaction = 1;
    public static final int addRewardPerTransaction = 1;
    
    // Gaussian distribution for the oracle
    public static final int oracleMean = 5, oracleVariance = 3;
    // Probability to CREATE one type of miner or normal node
    public static final int normal = 75;
    public static final int cpu = 25, gpu = 50, fpga = 75, asic = 100;     
    // Probability to CHOOSE one type of miner
    public static final int cpuPower = 5, gpuPower = 15, fpgaPower = 45, asicPower = 100;
 
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

    public static synchronized SharedInfo getSharedInfo() {
        if (instance == null) {
            instance = new SharedInfo();
        }
        return instance;
    }
}