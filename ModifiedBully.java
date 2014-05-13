import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by Shreyas on 5/1/2014.
 */
public class ModifiedBully extends UnicastRemoteObject implements RemoteInterface {

	private static final long serialVersionUID = 1L;
	int port=5000;
    String nodeID;
    String nodeIP;

    int coordinatorID;

    Registry registry;

    boolean isCoordinator;    // Yes - If this node is the coordinator; No - otherwise
    boolean criticalSectionAvailable = true;

    HashMap<Integer,String> peers; // Key - "NodeID" ::: Value - "IP;Port"
    
    //String myBiggestID;
    //String bigIP;
    

    ModifiedBully(String id, int coordId, String coordIP)
            throws AlreadyBoundException, RemoteException, UnknownHostException {
        //this.port = port;
        this.nodeID = id;
        this.nodeIP = InetAddress.getLocalHost().toString();

        this.coordinatorID = coordId;

        peers.put(coordId,coordIP);
        this.isCoordinator = false;

        this.setupConnection();
        
        System.setProperty("sun.rmi.transport.proxy.connectTimeout", "1000");
 
    }

    public void setupConnection() throws RemoteException, AlreadyBoundException {
        registry = LocateRegistry.createRegistry(port);
        registry.bind("" + this.nodeID,this);
    }
    
    //Initiating Election
    public void initiateElection() throws NotBoundException, InterruptedException{
    	try{
    		String message = "Coordinator down";
    	
    	Registry registry = LocateRegistry.getRegistry(bigIP,port);
    	RemoteInterface ri = (RemoteInterface)registry.lookup(myBiggestID);
    	String response = ri.giveResponse(message);
    	if(response.equals("I am alive")){
    		ri.announceLeader();
    	}
    	}catch(RemoteException e){
    		System.out.println("Client " + nodeID + "has crashed");
    	}
    	
    	
    }
    
    //A new client joining the network
    public void join() throws AccessException, RemoteException, NotBoundException{
    	Scanner sc = new Scanner(System.in);
    	System.out.print("Enter the IP of any host: ");
    	String ip = sc.next();
    	System.out.print("Enter the nodeID: ");
    	String node = sc.next();
    	Registry registry = LocateRegistry.getRegistry(ip,5000);
    	RemoteInterface ri = (RemoteInterface)registry.lookup(node);
    	this.peers = ri.getDetails(this.nodeID,this.nodeIP);
    	this.coordinatorID = ri.getCoordinatorID();
    	
    }
    
    //Returning the coordinator
    public String getCoordinatorID(){
    	
    	return this.coordinatorID;
    	
    }
    
    //Get the details of the nodes in the network and updating other nodes with the new node joined
    public Hashtable<String,String> getDetails(String nodeID, String nodeIP) throws RemoteException, NotBoundException{
    	
    	Registry registry;
    	RemoteInterface ri;
    	for(Map.Entry<String,String> entry: peers.entrySet()){
    		registry = LocateRegistry.getRegistry(""+entry.getValue(),port);
    		ri = (RemoteInterface)registry.lookup(""+entry.getKey());
    		ri.newNodeJoined(nodeID,nodeIP);
    	}
    	Hashtable<String,String> peerDetails =  this.peers;
    	this.peers.put(nodeID, nodeIP);
    	return peerDetails;
    }
    
    //Other nodes adding the new node joined into their list
    @Override
	public void newNodeJoined(String nodeID, String nodeIP) {
		
    	this.peers.put(nodeID,nodeIP);
		
	}
    
    //Giving response to a client which has initiated the election and trying to elect itself 
    @Override
	public String giveResponse(String message) throws InterruptedException, NotBoundException, RemoteException {
		
    	announceLeader();
		return "I am alive";
	
    }
    
    //Announcing that the current process is the leader to all the processes
    public void announceLeader() throws RemoteException, NotBoundException{
    	peers.remove(this.coordinatorID);
    	coordinatorID = nodeID;
    	coordinator = true;
    	Registry registry;
    	RemoteInterface ri;
    	for(Map.Entry<String,String> entry: peers.entrySet()){
    		registry = LocateRegistry.getRegistry(""+entry.getValue(),port);
    		ri = (RemoteInterface)registry.lookup(""+entry.getKey());
    		ri.announce(nodeID);
    	}
    }
  
    //Change the coordinator id and remove the previous coordinator from the coordinator list
    public void announce(String node){
    	peers.remove(this.coordinatorID);
    	this.coordinatorID = node;

    }
    
    //A client trying to access critical section
    public void accessCriticalSection() throws RemoteException, NotBoundException, InterruptedException{
    	Registry registry = LocateRegistry.getRegistry(peers.get(coordinatorID), port);
    	RemoteInterface ri = (RemoteInterface) registry.lookup(coordinatorID);
    	ri.givePermission(this.nodeID);
    	ri.accessCS();
    }
    
    //Remote Method to give access to critical section
	@Override
	public boolean givePermission(String nodeID) throws InterruptedException {
		
		while(criticalSectionInUse){
			wait();
		}
		criticalSectionInUse = true;
		return true;
	}
	
	//Process accessing critical section after getting the permission from the coordinator
	@Override
	public void accessCS() throws InterruptedException {
		
		Thread.sleep(1000);
		criticalSectionInUse = false;
		
	}

    public static void main(String[] args) throws AlreadyBoundException, IOException, NotBoundException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Hey! I'm a new node.. Let's set me up!");
      //  System.out.print("Enter the port number: ");
        //int port = br.read();

        System.out.print("Enter the node ID: ");
        String nodeID = br.readLine();

        System.out.print("Enter nodeID of the coordinator: ");
        String coordnodeId = br.readLine();
        System.out.print("\nEnter the IP address of the coordinator: ");
        String coordinatorIP = br.readLine();
       // System.out.print("\nEnter the port number of the coordinator");
        //String coordinatorPort = br.readLine();

        Bully aBully = new Bully(nodeID, coordnodeId, coordinatorIP);
        aBully.join();

        System.out.println("\nI'm up and running at " + aBully.getIP() +
                " and listening at 5000");
    }

    private String getIP() {
        return this.nodeIP;
    }

	

	



	
}