
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
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;



public class ModifiedBully extends UnicastRemoteObject implements RemoteInterface {

	protected ModifiedBully() throws RemoteException {
		super();
		
	}
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
    boolean electionFlag = false;			//State of the election

    HashMap<Integer,String> nodeInfo; // Key - "NodeID" ::: Value - "IP;portNumber"
    
    int incomingMessageCount;
    
    void intializeNode(int nodeID,int portNumber) throws UnknownHostException, RemoteException, AlreadyBoundException
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

     public boolean remoteAccess(int nodeID) throws RemoteException
    {
    	if(criticalSectionAvailable)
    	{
    		//Add it to the queue
    		currentCriticalSectionNode=nodeID;
    		criticalSectionAvailable=false;
    		return true;
    		
    	}else{
    		criticalSectionQueue.add(nodeID);
    		return false;
    	}
    }
    
    public int remoteLeave(int nodeID) throws RemoteException
    {
    	if(currentCriticalSectionNode==nodeID)
    	{
    		if(criticalSectionQueue.size() > 0)
    		{
    			currentCriticalSectionNode=criticalSectionQueue.at(0);
    			criticalSectionQueue.remove(0);
    		}else{
    			criticalSectionAvailable=true;
    		}
    		return 0; //Leave successfully executed
    		
    	}else if(criticalSectionQueue.contains(nodeID)){
    		return 1 // Node is waiting in the queue
    	}else{
    		return 2;//Node is not present in the queue
    	}
    }
    //A new client joining the network
      public void join(String IP, int port, int nodeID) throws AccessException, RemoteException, NotBoundException, UnknownHostException, AlreadyBoundException{
    	
    	intializeNode(nodeID,portNumber);
    	isCoordinator=false;
    	registry = LocateRegistry.getRegistry(IP,port);	//Connecting to the given host
    	RemoteInterface ri = (RemoteInterface)registry.lookup(""+nodeID);	//Looking for the nodeId in the registry
    	this.nodeInfo = ri.remoteInsertNode(this.nodeID,this.nodeIP);	//Getting all the info from the node and copying to the current new node
    	this.coordinatorID = ri.getCoordinatorID();		//Getting the coordinator id
    	
    }
    
    
    //If the new client is the first client
    public void join(int nodeID, int portNumber) throws UnknownHostException, RemoteException, AlreadyBoundException{
    	
    	intializeNode(nodeID,portNumber);
    	this.coordinatorID=nodeID;	//Updating the coordinator as itself
    	isCoordinator=true;
    	
    }
    
    //Returning the coordinator
    public int remoteGetCoordinatorID(){
    	return coordinatorID;
    }

    public void initiateElection() {
        if(!this.electionFlag) {
            this.coordinatorID = 0;
            this.electionFlag = true;

            ArrayList<Integer> nodeIDs = new ArrayList<Integer>(this.nodeInfo.keySet());
            for(int node : nodeIDs ) {
                if(node > this.nodeID) {
                    String nodeValue = this.nodeInfo.get(node);
                    String[] nodeIpPort = nodeValue.split("|");
                    Registry aRegistry = LocateRegistry.getRegistry(nodeIpPort[0], Integer.parseInt(nodeIpPort[1]));
                    RemoteInterface aNode = (RemoteInterface) aRegistry.lookup("" + node);
                    aNode.remoteInitiateElection(this.nodeID);
                }
            }

            if(this.coordinatorID == 0) {
                this.isCoordinator = true;
                this.electionFlag = false;
                this.broadcastCoordinatorNodeID();
            }
            else {
                String[] nodeValues = this.nodeInfo.get(this.coordinatorID).split("|");
                Registry registry = LocateRegistry.getRegistry(nodeValues[0], Integer.parseInt(nodeValues[1]));
                RemoteInterface node = (RemoteInterface) registry.lookup(nodeId);
                node.remoteSetCoordinator();
            }
        }
    }

    public void remoteSetCoordinator() throws RemoteException {
        this.electionFlag = false;
        this.initiateElection();
    }

    public void remoteInitiateElection(int nodeId) throws RemoteException {
        this.electionFlag = true;
        this.coordinatorID = 0;
        if(this.nodeID > nodeId) {
            String[] nodeValues = this.nodeInfo.get(nodeId).split("|");
            Registry registry = LocateRegistry.getRegistry(nodeValues[0], Integer.parseInt(nodeValues[1]));
            RemoteInterface node = (RemoteInterface)registry.lookup(nodeId);
            node.remoteGetElectionResponse(this.nodeID);
        }
    }

    public void remoteGetElectionResponse(int nodeId) throws RemoteException {
        if(this.coordinatorID < nodeId) {
            this.coordinatorID = nodeId;
        }
    }
    
    public HashMap<Integer,String> remoteInsertNode(String IP, int port, int nodeID) throws RemoteException, NotBoundException {
        HashMap<Integer, String> returnInfo = this.nodeInfo;
        this.nodeInfo.put(nodeID, IP + "|" + port);
        this.broadcastNewNodeInfo(IP,port,nodeID);
        return returnInfo;
    }

    void broadcastNewNodeInfo(String IP, int port, int nodeID) throws NotBoundException, NumberFormatException, RemoteException {
        ArrayList<Integer> nodeIDs = new ArrayList<Integer>(this.nodeInfo.keySet());
        for(int node : nodeIDs ) {
            String nodeValue = this.nodeInfo.get(node);
            String[] nodeIpPort = nodeValue.split("|");
            Registry aRegistry = LocateRegistry.getRegistry(nodeIpPort[0], Integer.parseInt(nodeIpPort[1]));
            RemoteInterface aNode = (RemoteInterface)aRegistry.lookup(""+node);
            aNode.remoteBroadcastNewNodeInfo(IP, port, nodeID);
        }
    }

    public void remoteBroadcastNewNodeInfo(String IP, int port, int nodeID) throws RemoteException {
        this.nodeInfo.put(nodeID,IP + "|" + port);
    }

    void broadcastCoordinatorNodeID() throws NotBoundException, NumberFormatException, RemoteException {
        ArrayList<Integer> nodeIDs = new ArrayList<Integer>(this.nodeInfo.keySet());
        for(int node : nodeIDs ) {
            String nodeValue = this.nodeInfo.get(node);
            String[] nodeIpPort = nodeValue.split("|");
            Registry aRegistry = LocateRegistry.getRegistry(nodeIpPort[0], Integer.parseInt(nodeIpPort[1]));
            RemoteInterface aNode = (RemoteInterface)aRegistry.lookup(""+node);
            aNode.remoteBroadcastCoordinatorNodeID(this.nodeID);
        }

    }

    public void remoteBroadcastCoordinatorNodeID(int nodeID) throws RemoteException {
        this.coordinatorID = nodeID;
        this.electionFlag = false;
    }


    void userPrompt() throws IOException, NumberFormatException, AlreadyBoundException, NotBoundException
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
				String nodeValue = this.nodeInfo.get(node);
        			String[] nodeIpPort = nodeValue.split("|");
        			Registry aRegistry = LocateRegistry.getRegistry(nodeIpPort[0], Integer.parseInt(nodeIpPort[1]));
            			RemoteInterface aNode = (RemoteInterface)aRegistry.lookup(""+node);
            			if(aNode.remoteAccess(nodeID))
            				System.out.println("Node entered critical section");
				else
					System.out.println("Node waiting in the queue");
            			
				
			}else if(commandTokens[0].equals("leave"))
			{
				String nodeValue = this.nodeInfo.get(node);
        			String[] nodeIpPort = nodeValue.split("|");
        			Registry aRegistry = LocateRegistry.getRegistry(nodeIpPort[0], Integer.parseInt(nodeIpPort[1]));
            			RemoteInterface aNode = (RemoteInterface)aRegistry.lookup(""+node);
            			int status=aNode.remoteLeave(nodeID);
            			if(status==0)
            				System.out.println("Node successfully left the critical region");
            			else
            				System.out.println("Node still waiting in the queue for critical region");
				else
					System.out.println("Node not present in the queue");
	            			
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


	@Override
	public void remoteInitiateElection() throws RemoteException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public HashMap<Integer, String> remoteInsertNode(int nodeID, String nodeIP) {
		// TODO Auto-generated method stub
		return null;
	}

	

	



	
}
