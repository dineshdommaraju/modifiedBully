
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.String;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.*;
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
    ArrayList<Integer> criticalSectionQueue=new ArrayList<Integer>();	//Nodes which are waiting to enter CS
    int coordinatorID;				//Current coordinator
    Registry registry;				//Common Registry
    boolean isCoordinator;    // Yes - If this node is the coordinator; No - otherwise
    boolean criticalSectionAvailable = true;	//Is CS Available or not
    boolean electionFlag = false;			//State of the election

    HashMap<Integer,String> nodeInfo; // Key - "NodeID" ::: Value - "IP;portNumber"
    
    int incomingMessageCount;
    int totalMessageCount;
    void intializeNode(int nodeID,int portNumber) throws UnknownHostException, RemoteException, AlreadyBoundException
    {
    	this.portNumber=portNumber;
    	this.nodeID = nodeID;
    	nodeInfo = new HashMap<Integer,String>();
        this.nodeIP = InetAddress.getLocalHost().getHostAddress();
        this.setupConnection();
    }
    

    public void setupConnection() throws RemoteException, AlreadyBoundException {
    	registry = LocateRegistry.createRegistry(portNumber);
        registry.bind("" + this.nodeID,this);
        
    }

    @Override
     public boolean remoteAccess(int nodeID) throws RemoteException, NotBoundException
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
    @Override
    public int remoteLeave(int nodeID) throws NumberFormatException, RemoteException, NotBoundException 
    {
    	
    	if(currentCriticalSectionNode==nodeID)
    	{
    		if(criticalSectionQueue.size() > 0)
    		{
    			currentCriticalSectionNode=criticalSectionQueue.get(0);
    			criticalSectionQueue.remove(0);
    			String[] nodeValue = nodeInfo.get(currentCriticalSectionNode).split(";");
    			System.out.println("Node Value: " + nodeValue);
    			System.out.println("current critical node" + currentCriticalSectionNode);
    			registry = LocateRegistry.getRegistry(nodeValue[0], Integer.parseInt(nodeValue[1]));	
    	    	RemoteInterface ri = (RemoteInterface)registry.lookup(""+currentCriticalSectionNode);	
    	    	ri.displayEnteringCS();		
    			
    		}else{
    			criticalSectionAvailable=true;
    		}
    		return 0; //Leave successfully executed
    		
    	}else if(criticalSectionQueue.contains(nodeID)){
    		return 1; // Node is waiting in the queue
    	}else{
    		return 2;//Node is not present in the queue
    	}
    	
			
    	
    }
    //A new client joining the network
      public void join(int myID,int myPort, String IP, int port, int nodeID) throws AccessException, RemoteException, NotBoundException, UnknownHostException, AlreadyBoundException{
    	
    	  
    	System.out.println("New node ID : "+ myID);
    	System.out.println("New node port: "+ myPort);
    	System.out.println("Existing node's ID: "+ nodeID);
    	System.out.println("Existing node's IP: "+ IP);
    	System.out.println("Existing node's port: "+ port);
    	
    	intializeNode(myID,myPort);
    	isCoordinator=false;
    	System.out.println("Before calling remote insert node");
    	registry = LocateRegistry.getRegistry(IP,port);	//Connecting to the given host
    	RemoteInterface ri = (RemoteInterface)registry.lookup(""+nodeID);	//Looking for the nodeId in the registry
    	this.nodeInfo = ri.remoteInsertNode(this.nodeIP,this.portNumber,this.nodeID);	//Getting all the info from the node and copying to the current new node
    	this.nodeInfo.put(nodeID, ""+IP+";"+port);
    	this.nodeInfo.remove(myID);
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

    public void initiateElection() throws AccessException, NotBoundException {
       try{
    	if(!this.electionFlag) {
            this.coordinatorID = 0;
            this.electionFlag = true;
            System.out.println("In initiate election");
            ArrayList<Integer> nodeIDs = new ArrayList<Integer>(this.nodeInfo.keySet());
            for(int node : nodeIDs ) {
                if(node > this.nodeID) {
                	try{
                	incomingMessageCount++;
                    String nodeValue = this.nodeInfo.get(node);
                    String[] nodeIpPort = nodeValue.split(";");
                    Registry aRegistry = LocateRegistry.getRegistry(nodeIpPort[0], Integer.parseInt(nodeIpPort[1]));
                    RemoteInterface aNode = (RemoteInterface) aRegistry.lookup("" + node);
                    aNode.remoteInitiateElection(this.nodeID);
                	}catch(ConnectException e){
                		System.out.println("Inside connect exception");
                	}
                }
            }

            if(this.coordinatorID == 0) {
            	
            	System.out.println("inside 1");
            	this.coordinatorID=this.nodeID;
                this.isCoordinator = true;
                this.electionFlag = false;
                this.broadcastCoordinatorNodeID();
            }
            else {
            	System.out.println("new Coordinator ID :"+this.coordinatorID);
            	
                String[] nodeValues = this.nodeInfo.get(this.coordinatorID).split(";");
                Registry registry = LocateRegistry.getRegistry(nodeValues[0], Integer.parseInt(nodeValues[1]));
                RemoteInterface node = (RemoteInterface) registry.lookup(""+this.coordinatorID);
                incomingMessageCount++;
                node.remoteSetCoordinator();
            }
        }
       }catch(RemoteException e){
        	
        }
    }

    public void remoteSetCoordinator() throws RemoteException, NotBoundException {
        this.electionFlag = false;
        this.initiateElection();
    }

    public void remoteInitiateElection(int nodeId) throws RemoteException, NotBoundException {
        this.electionFlag = true;
        this.coordinatorID = 0;
        System.out.println("In remote initiate election");
        if(this.nodeID > nodeId) {
            String[] nodeValues = this.nodeInfo.get(nodeId).split(";");
            Registry registry = LocateRegistry.getRegistry(nodeValues[0], Integer.parseInt(nodeValues[1]));
            RemoteInterface node = (RemoteInterface)registry.lookup(""+nodeId);
            incomingMessageCount++;
            node.remoteGetElectionResponse(this.nodeID);
        }
    }

    public void remoteGetElectionResponse(int nodeId) throws RemoteException {
    	System.out.println("Inside remoteGetElectionResponse");
    	
        if(this.coordinatorID < nodeId) {
            this.coordinatorID = nodeId;
        }
    }
    
    public HashMap<Integer,String> remoteInsertNode(String IP, int port, int nodeID) throws RemoteException, NotBoundException {
        
    	System.out.println("In Remote Insert Node method");
    	HashMap<Integer, String> returnInfo = this.nodeInfo;
        if(!nodeInfo.isEmpty())
        	this.broadcastNewNodeInfo(IP,port,nodeID);
        this.nodeInfo.put(nodeID, IP + ";" + port);
        System.out.println("After Remote Insert");
        return returnInfo;
    }

    void broadcastNewNodeInfo(String IP, int port, int nodeID) throws NotBoundException, NumberFormatException, RemoteException {
        System.out.println("Inside broadcastNewNodeInfo method");
    	ArrayList<Integer> nodeIDs = new ArrayList<Integer>(this.nodeInfo.keySet());
    	System.out.println("Map: "+ nodeInfo);
   
        for(int node : nodeIDs ) {
        	
            String nodeValue = this.nodeInfo.get(node);
            System.out.println("Node: "+node);
            System.out.println("NodeValue: "+nodeValue);
            
            String[] nodeIpPort = nodeValue.split(";");
            System.out.println("IP: "+ nodeIpPort[0]);
            System.out.println("Port: "+ nodeIpPort[1]);
            Registry aRegistry = LocateRegistry.getRegistry(nodeIpPort[0], Integer.parseInt(nodeIpPort[1]));
            RemoteInterface aNode = (RemoteInterface)aRegistry.lookup(""+node);
            aNode.remoteBroadcastNewNodeInfo(IP, port, nodeID);
        }
    }

    public void remoteBroadcastNewNodeInfo(String IP, int port, int nodeID) throws RemoteException {
        System.out.println("Inside remoteBroadcastNewNodeInfo");
    	this.nodeInfo.put(nodeID,IP + ";" + port);
    }

    void broadcastCoordinatorNodeID() throws NotBoundException, NumberFormatException {
    	totalMessageCount=0;
    	System.out.println("Inside broadcastCoordinatorNodeID");
        ArrayList<Integer> nodeIDs = new ArrayList<Integer>(this.nodeInfo.keySet());
        for(int node : nodeIDs ) {
        	try
        	{
        		incomingMessageCount++;
        		
        		String nodeValue = this.nodeInfo.get(node);
        		String[] nodeIpPort = nodeValue.split(";");
        		Registry aRegistry = LocateRegistry.getRegistry(nodeIpPort[0], Integer.parseInt(nodeIpPort[1]));
        		RemoteInterface aNode = (RemoteInterface)aRegistry.lookup(""+node);
        		totalMessageCount+=aNode.remoteBroadcastCoordinatorNodeID(this.nodeID);
        	}catch(ConnectException e)
        	{
        		
        	} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        totalMessageCount+=incomingMessageCount;

    }

    public int remoteBroadcastCoordinatorNodeID(int nodeID) throws RemoteException {
    	this.coordinatorID = nodeID;
        this.electionFlag = false;
        return this.incomingMessageCount;
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
				}else if(commandTokens.length==6)
				{
					//Format: IPAddress,Port Number, NodeID
					join(Integer.parseInt(commandTokens[1]),Integer.parseInt(commandTokens[2]),commandTokens[4],Integer.parseInt(commandTokens[5]),Integer.parseInt(commandTokens[3]));
				}
				
			}else if(commandTokens[0].equals("request"))
			{
				try{
				String nodeValue = this.nodeInfo.get(coordinatorID);
        			String[] nodeIpPort = nodeValue.split(";");
        			Registry aRegistry = LocateRegistry.getRegistry(nodeIpPort[0], Integer.parseInt(nodeIpPort[1]));
            			RemoteInterface aNode = (RemoteInterface)aRegistry.lookup(""+coordinatorID);
            			if(aNode.remoteAccess(nodeID))
            				System.out.println("Node entered critical section");
				else
					System.out.println("Node waiting in the queue");
				}catch(ConnectException e){
					initiateElection();
				}
				
			}else if(commandTokens[0].equals("leave"))
			{
				try{
					String nodeValue = this.nodeInfo.get(coordinatorID);
        			String[] nodeIpPort = nodeValue.split(";");
        			Registry aRegistry = LocateRegistry.getRegistry(nodeIpPort[0], Integer.parseInt(nodeIpPort[1]));
            		RemoteInterface aNode = (RemoteInterface)aRegistry.lookup(""+coordinatorID);
            		int status=aNode.remoteLeave(nodeID);
            		if(status==0)
            			System.out.println("Node successfully left the critical region");
            		else if(status==1)
            			System.out.println("Node still waiting in the queue for critical region");
            		else
            			System.out.println("Node not present in the queue");
				}catch (ConnectException e)
				{
					System.out.println("Inside leave");
					initiateElection();
				}
	            			
			}else if(commandTokens[0].equals("help"))
			{
				
			}else if(commandTokens[0].equals("count"))
			{
				if(this.coordinatorID==this.nodeID)
					System.out.println("Number of messages: " + this.totalMessageCount);
				else{
					String nodeValue = this.nodeInfo.get(this.coordinatorID);
	        		String[] nodeIpPort = nodeValue.split(";");
	        		Registry aRegistry = LocateRegistry.getRegistry(nodeIpPort[0], Integer.parseInt(nodeIpPort[1]));
	        		RemoteInterface aNode = (RemoteInterface)aRegistry.lookup(""+this.coordinatorID);
	        		this.totalMessageCount= aNode.remoteGetMessageCount();
	        		System.out.println("Number of messages: " + this.totalMessageCount);
				}
				
			}else if(commandTokens[0].equals("view"))
			{
				System.out.println("_________________________________________");
				System.out.println("| "+nodeID+" "+nodeIP+" "+portNumber+" |");
				System.out.println("_________________________________________");
				ArrayList<Integer> nodeIDs = new ArrayList<Integer>(this.nodeInfo.keySet());
				 for(int node : nodeIDs )
				 {
					 System.out.println(" "+node+ " " + this.nodeInfo.get(node));
				 }
				 System.out.println("_________________________________________");
				 System.out.println("Coordinator ID: "+coordinatorID);
				 System.out.println("_________________________________________");
				 System.out.println("Message Count: "+this.incomingMessageCount);
				 System.out.println("_________________________________________");
				
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
	public int getCoordinatorID() {
		
		return this.coordinatorID;
	}


	@Override
	public void displayEnteringCS() throws RemoteException,
			NotBoundException {
		System.out.println("You have entered Critical Section");
	}


	@Override
	public int remoteGetMessageCount() throws RemoteException,
			NotBoundException {
		
		return this.totalMessageCount;
	}


	


	
	

	



	
}
