package modifiedBully;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.String;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

/**
 * Created by Shreyas on 5/1/2014.
 */
public class ModifiedBully extends UnicastRemoteObject implements RemoteInterface {

	private static final long serialVersionUID = 1L;
	int portNumber;					//Node's port
    int nodeID;						//Node's ID
    String nodeIP;					//Node's IP
    int timeOut;					//To set the time out for every request
    
    int currentCriticalSectionNode;	//Node which is using the critical section
    ArrayList<Integer> criticalSectionQueue;	//Nodes which are waiting to enter CS
    int coordinatorID;				//Current coordinator
    Registry registry;				//Common Registry
    boolean isCoordinator;    // Yes - If this node is the coordinator; No - otherwise
    boolean criticalSectionAvailable = true;	//Is CS Available or not
    boolean electionFlag=false;			//State of the election

    HashMap<Integer,String> nodeInfo; // Key - "NodeID" ::: Value - "IP;portNumber"
    
    int incomingMessageCount;
    
    void intializeNode(int nodeID,int portNumber)
    {
    	this.portNumber=portNumber;
    	this.nodeID = nodeID;
        this.nodeIP = InetAddress.getLocalHost().toString();
        this.setupConnection();
    }
    

    public void setupConnection() throws RemoteException, AlreadyBoundException {
    	registry = LocateRegistry.createRegistry(portNumber);
        registry.bind("" + this.nodeID,this);
        
    }

    
    //A new client joining the network
    public void join(String IP, int port, int nodeID) throws AccessException, RemoteException, NotBoundException{
    	
    	
    	registry = LocateRegistry.getRegistry(IP,port);	//Connecting to the given host
    	RemoteInterface ri = (RemoteInterface)registry.lookup(""+nodeID);	//Looking for the nodeId in the registry
    	this.nodeInfo = ri.getDetails(this.nodeID,this.nodeIP);	//Getting all the info from the node and copying to the current new node
    	this.coordinatorID = ri.getCoordinatorID();		//Getting the coordinator id
    	
    }
    
    
    //If the new client is the first client
    public void join(int nodeID, int portNumber){
    	
    	intializeNode(nodeID,portNumber);
    	this.coordinatorID=nodeID;	//Updating the coordinator as itself
    	isCoordinator=true;
    	
    }
    
    //Returning the coordinator
    public int getCoordinatorID(){
    	
    	return coordinatorID;
    	
    }
<<<<<<< HEAD
    
    //Get the details of the nodes in the network and updating other nodes with the new node joined
    public HashMap<Integer,String> getDetails(int nodeID, String nodeIP) throws RemoteException, NotBoundException{
    	
    	Registry registry;
    	RemoteInterface ri;
    	for(Entry<Integer, String> entry: nodeInfo.entrySet()){
    		String[] tempIP = entry.getValue().split("|");
    		registry = LocateRegistry.getRegistry(tempIP[0],Integer.parseInt(tempIP[1]));
    		ri = (RemoteInterface)registry.lookup(""+entry.getKey());
    		ri.newNodeJoined(nodeID,nodeIP);
    	}
    	HashMap<Integer,String> peerDetails =  this.nodeInfo;
    	this.nodeInfo.put(nodeID, nodeIP);
    	return peerDetails;
    }
    
    //Other nodes adding the new node joined into their list
    @Override
	public void newNodeJoined(int nodeID, String nodeIP) {
		
    	this.nodeInfo.put(nodeID, nodeIP);
		
	}
    
    //Giving response to a client which has initiated the election and trying to elect itself 
    @Override
	public String giveResponse(String message) throws InterruptedException, NotBoundException, RemoteException {
		
    	announceLeader();
		return "I am alive";
	
    }
    
    //Announcing that the current process is the leader to all the processes
    public void announceLeader() throws RemoteException, NotBoundException{
    	nodeInfo.remove(this.coordinatorID);
    	coordinatorID = nodeID;
    	coordinator = true;
    	Registry registry;
    	RemoteInterface ri;
    	for(Entry<Integer, String> entry: nodeInfo.entrySet()){
    		registry = LocateRegistry.getRegistry(""+entry.getValue(),portNumber);
    		ri = (RemoteInterface)registry.lookup(""+entry.getKey());
    		ri.announce(nodeID);
    	}
    }
  
    //Change the coordinator id and remove the previous coordinator from the coordinator list
    public void announce(String node){
    	nodeInfo.remove(this.coordinatorID);
    	this.coordinatorID = node;

    }
    
    //A client trying to access critical section
    public void accessCriticalSection() throws RemoteException, NotBoundException, InterruptedException{
    	Registry registry = LocateRegistry.getRegistry(nodeInfo.get(coordinatorID), portNumber);
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

    public HashMap<Integer,String> remoteInsertNode(String IP, int port, int nodeID) throws RemoteException {
        HashMap<Integer, String> returnInfo = this.nodeInfo;
        this.nodeInfo.put(nodeID, IP + "|" + port);
        this.broadcastNewNodeInfo(IP,port,nodeID);
        return returnInfo;
    }

    void broadcastNewNodeInfo(String IP, int port, int nodeID) throws NotBoundException {
        ArrayList<Integer> nodeIDs = new ArrayList<Integer>(this.nodeInfo.keySet());
        for(int node : nodeIDs ) {
            String nodeValue = this.nodeInfo.get(node);
            String[] nodeIpPort = nodeValue.split("|");
            Registry aRegistry = LocateRegistry.getRegistry(nodeIpPort[0], nodeIpPort[1]);
            RemoteInterface aNode = (RemoteInterface)aRegistry.lookup(node);
            aNode.remoteBroadcastNewNodeInfo(IP, port, nodeID);
        }
    }

    public void remoteBroadcastNewNodeInfo(String IP, int port, int nodeID) throws RemoteException {
        this.nodeInfo.put(nodeID,IP + "|" + port);
    }

    void broadcastCoordinatorNodeID() throws NotBoundException {
        ArrayList<Integer> nodeIDs = new ArrayList<Integer>(this.nodeInfo.keySet());
        for(int node : nodeIDs ) {
            String nodeValue = this.nodeInfo.get(node);
            String[] nodeIpPort = nodeValue.split("|");
            Registry aRegistry = LocateRegistry.getRegistry(nodeIpPort[0], nodeIpPort[1]);
            RemoteInterface aNode = (RemoteInterface)aRegistry.lookup(node);
            aNode.remoteBroadcastCoordinatorNodeID(this.nodeID);
        }

    }

    public void remoteBroadcastCoordinatorNodeID(int nodeID) throws RemoteException {
        this.coordinatorID = nodeID;
        this.electionFlag = false;
    }


    void userPrompt() throws IOException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while(true)
		{
			String command=br.readLine();
			String[] commandTokens=command.split(" ");
			if(commandTokens[0].equals("join"))
			{
				if(commandTokens.length ==3)
				{
					//Format : NodeID, Port Number
					join(Integer.parseInt(commandTokens[1]), Integer.parseInt(commandTokens[2]));
				}else if(commandTokens.length==4)
				{
					//Format: IPAddress,Port Number, NodeID
					join(commandTokens[2],Integer.parseInt(commandTokens[3]),Integer.parseInt(commandTokens[1]));
				}
				
			}else if(commandTokens[0].equals("request"))
			{
				
			}else if(commandTokens[0].equals("leave"))
			{
				
			}else if(commandTokens[0].equals("help"))
			{
				
			}else{
				System.out.println("Wrong command!.Type help for list of commands");
			}
		}	
	}
    public static void main(String[] args) throws AlreadyBoundException, IOException, NotBoundException {
    	
    	ModifiedBully obj=new ModifiedBully();
    	obj.userPrompt();		
    }
    private String getIP() {
        return this.nodeIP;
    }

	

	



	
}