
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



public class ModifiedBully extends UnicastRemoteObject implements RemoteInterface, Runnable {

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
    boolean isInCriticalSection=false;		//Indicates if there is a node in critical section
    boolean isInWaitingQueue=false;			//Indicates if there is a node in waiting queue waiting to access CS
    int previousCoordinatorID;				//To keep track of the previous coordinator

    HashMap<Integer,String> nodeInfo; // Key - "NodeID" ::: Value - "IP;portNumber"
    
    int incomingMessageCount;		//To keep track of the messages of each node passed during election
    int totalMessageCount;			//Total number of messages passed during election
    
    Thread csThread;				//Thread to montior CS
    
    //Initializes all the nodes joining the network
    void intializeNode(int nodeID,int portNumber) throws UnknownHostException, RemoteException, AlreadyBoundException
    {
    	this.portNumber=portNumber;
    	this.nodeID = nodeID;
    	nodeInfo = new HashMap<Integer,String>();
        this.nodeIP = InetAddress.getLocalHost().getHostAddress();
        this.setupConnection();
        new Thread(this).start();	//Thread to check if the node in the critical section is active
    }
    
    //Method to 
    @Override
    public void run(){
    	
    	while(true){
    		
    		try{
    			if(this.nodeID==coordinatorID && !criticalSectionAvailable){
    				
    				if(currentCriticalSectionNode!=this.nodeID)
    				{
    					String[] nodeValue = nodeInfo.get(currentCriticalSectionNode).split(";");
    					registry = LocateRegistry.getRegistry(nodeValue[0], Integer.parseInt(nodeValue[1]));	
    					RemoteInterface ri = (RemoteInterface)registry.lookup(""+currentCriticalSectionNode);
    					ri.remoteCheckAlive();
    				}
    				Thread.sleep(1000);
    			}
    		}catch (ConnectException e){
    			
    			try {
					Leave(currentCriticalSectionNode);
				} catch (NumberFormatException e1) {
					
					e1.printStackTrace();
				} catch (RemoteException e1) {
					
					e1.printStackTrace();
				} catch (NotBoundException e1) {
					
					e1.printStackTrace();
				}
    		} catch (NumberFormatException e) {
				
				e.printStackTrace();
			} catch (RemoteException e) {
				
				e.printStackTrace();
			} catch (NotBoundException e) {
				
				e.printStackTrace();
			} catch (InterruptedException e) {
				
				e.printStackTrace();
			}
    	}
    }

    //Method to bind each node to the registry
    public void setupConnection() throws RemoteException, AlreadyBoundException {
    	registry = LocateRegistry.createRegistry(portNumber);
        registry.bind("" + this.nodeID,this);
        
    }

    
    //Method that allows a node to access critical section or to put the node into the waiting queue based on availability
    public boolean Access(int nodeID)
    {
    	if(criticalSectionAvailable)
    	{
    		
    		currentCriticalSectionNode=nodeID;
    		criticalSectionAvailable=false;
    		return true;
    		
    	}else{
    		
    		criticalSectionQueue.add(nodeID);
    		return false;
    	}
    }
    
    //Remote method that allows nodes to access CS
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
    
    //Local method that allows the node to leave the CS
    public int Leave(int nodeID) throws NumberFormatException, RemoteException, NotBoundException 
    {
    	while(true)
		{
    		if(currentCriticalSectionNode==nodeID)
    		{
    			if(criticalSectionQueue.size() > 0)
    			{
    				currentCriticalSectionNode=criticalSectionQueue.get(0);
    				criticalSectionQueue.remove(0);
    			
    				if(currentCriticalSectionNode!=this.nodeID)
    				{
    					try{
    						
    						String[] nodeValue = nodeInfo.get(currentCriticalSectionNode).split(";");
    						registry = LocateRegistry.getRegistry(nodeValue[0], Integer.parseInt(nodeValue[1]));	
    						RemoteInterface ri = (RemoteInterface)registry.lookup(""+currentCriticalSectionNode);	
    						ri.displayEnteringCS();
    						break;
    					}catch(ConnectException e)
    					{
    						
    					}
    				}else{
    					LocaldisplayEnteringCS();
    					break;
    				}	
    			
    			}else{
    				criticalSectionAvailable=true;
    				currentCriticalSectionNode=-1;
    				break;
    			}
    			
    		}else if(criticalSectionQueue.contains(nodeID)){
    			return 1; 
    		}else{
    			return 2;
    		}
		
    	 
    }
    	return 0; 
    }
    
    
    //Remote method that allows the nodes to leave the CS
    @Override
    public int remoteLeave(int nodeID) throws NumberFormatException, RemoteException, NotBoundException 
    {
    	System.out.println("currentCriticalSectionNode :"+currentCriticalSectionNode);
    	System.out.println("nodeID :"+nodeID);
    	while(true)
		{
    		if(currentCriticalSectionNode==nodeID)
    		{
  
    			if(criticalSectionQueue.size() > 0)
    			{
    			
    				currentCriticalSectionNode=criticalSectionQueue.get(0);
    				criticalSectionQueue.remove(0);
    				if(currentCriticalSectionNode!=this.nodeID)
    				{
    					String[] nodeValue = nodeInfo.get(currentCriticalSectionNode).split(";");
    					System.out.println("Node Value: " + nodeValue);
    					System.out.println("current critical node" + currentCriticalSectionNode);
    					try{
    						nodeID=currentCriticalSectionNode;
    						registry = LocateRegistry.getRegistry(nodeValue[0], Integer.parseInt(nodeValue[1]));	
    						RemoteInterface ri = (RemoteInterface)registry.lookup(""+currentCriticalSectionNode);	
    						ri.displayEnteringCS();
    						break;
    					}catch(ConnectException e)
    					{
    						
    					}
    				}else{
    				LocaldisplayEnteringCS();
    				break;
    				}
    			
    		}else{
    			criticalSectionAvailable=true;
    			currentCriticalSectionNode=-1;
    			break;
    		}
    		
    	}else if(criticalSectionQueue.contains(nodeID)){
    		
    		return 1; // Node is waiting in the queue
    	}else{
    		return 2;//Node is not present in the queue
    	}
		}
    	return 0;	//Node successfully exited from the critical section
			
    	
    }
    //A new client joining the network
      public void join(int myID,int myPort, String IP, int port, int nodeID) throws AccessException, RemoteException, NotBoundException, UnknownHostException, AlreadyBoundException{
    	
    	  
    
    	intializeNode(myID,myPort);
    	isCoordinator=false;
    	registry = LocateRegistry.getRegistry(IP,port);	//Connecting to the given host
    	RemoteInterface ri = (RemoteInterface)registry.lookup(""+nodeID);	//Looking for the nodeId in the registry
    	this.nodeInfo = ri.remoteInsertNode(this.nodeIP,this.portNumber,this.nodeID);	//Getting all the info from the node and copying to the current new node
    	this.nodeInfo.put(nodeID, ""+IP+";"+port);
    	this.nodeInfo.remove(myID);
    	this.coordinatorID = ri.getCoordinatorID();		//Getting the coordinator id
    	this.previousCoordinatorID=coordinatorID;
    	
    }
    
    
    //If the new client is the first client
    public void join(int nodeID, int portNumber) throws UnknownHostException, RemoteException, AlreadyBoundException{
    	
    	intializeNode(nodeID,portNumber);
    	this.coordinatorID=nodeID;	//Updating the coordinator as itself
    	this.previousCoordinatorID=coordinatorID;
    	isCoordinator=true;
    	
    }
    
    //Returning the coordinator
    public int remoteGetCoordinatorID(){
    	return coordinatorID;
    }

    //If a coordinator fails, this method is called to start the election
    public void initiateElection() throws AccessException, NotBoundException {
       try{
    	if(!this.electionFlag) {
            this.coordinatorID = 0;
            this.electionFlag = true;
            
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
                		
                	}
                }
            }

            if(this.coordinatorID == 0) {
            	
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

    
    //Method to set the new coordinator
    public void remoteSetCoordinator() throws RemoteException, NotBoundException {
        this.electionFlag = false;	//Indicates that any node can start the election when there is a coordinator failure
        this.initiateElection();
    }

    //Remote method that initiates the election by connecting to the other nodes
    public void remoteInitiateElection(int nodeId) throws RemoteException, NotBoundException {
        this.electionFlag = true;
        this.coordinatorID = 0;
        
        if(this.nodeID > nodeId) {
            String[] nodeValues = this.nodeInfo.get(nodeId).split(";");
            Registry registry = LocateRegistry.getRegistry(nodeValues[0], Integer.parseInt(nodeValues[1]));
            RemoteInterface node = (RemoteInterface)registry.lookup(""+nodeId);
            incomingMessageCount++;
            node.remoteGetElectionResponse(this.nodeID);
        }
    }

    //Method that is used by the other nodes to reply for the request sent by the node who identified the coordinator failure
    public void remoteGetElectionResponse(int nodeId) throws RemoteException {
    	
    	
        if(this.coordinatorID < nodeId) {
            this.coordinatorID = nodeId;
        }
    }
    
    
    //Remote Method that allows other nodes to insert the details of the new node whioch just joined to the network
    public HashMap<Integer,String> remoteInsertNode(String IP, int port, int nodeID) throws RemoteException, NotBoundException {
        
    	
    	HashMap<Integer, String> returnInfo = this.nodeInfo;
        if(!nodeInfo.isEmpty())
        	this.broadcastNewNodeInfo(IP,port,nodeID);
        this.nodeInfo.put(nodeID, IP + ";" + port);
        return returnInfo;
    }

    //Local Method to broadcast the details about the new node to other nodes in the network
    void broadcastNewNodeInfo(String IP, int port, int nodeID) throws NotBoundException, NumberFormatException, RemoteException {

    	ArrayList<Integer> nodeIDs = new ArrayList<Integer>(this.nodeInfo.keySet());
   
        for(int node : nodeIDs ) {
        	
            String nodeValue = this.nodeInfo.get(node);
            String[] nodeIpPort = nodeValue.split(";");
           Registry aRegistry = LocateRegistry.getRegistry(nodeIpPort[0], Integer.parseInt(nodeIpPort[1]));
            RemoteInterface aNode = (RemoteInterface)aRegistry.lookup(""+node);
            aNode.remoteBroadcastNewNodeInfo(IP, port, nodeID);
        }
    }
    
    //Remote method that broadcasts the message about the new node and allowing other nodes to add the new node details
    public void remoteBroadcastNewNodeInfo(String IP, int port, int nodeID) throws RemoteException {
        
    	this.nodeInfo.put(nodeID,IP + ";" + port);
    }
    
    //Method that allows the coordinator to broadcast messages saying that it is the new coordinator
    void broadcastCoordinatorNodeID() throws NotBoundException, NumberFormatException {
    	
    	totalMessageCount=0;
    	
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
        		int status=aNode.remoteGetCriticalSectionStatus();
        		if(status==1)
        		{
        			this.currentCriticalSectionNode=node;
        			this.criticalSectionAvailable=false;
        		}else if(status==2)
        		{
        			this.criticalSectionQueue.add(node);
        		}
        		
        	}catch(ConnectException e)
        	{
        		
        	} catch (RemoteException e) {
				
				e.printStackTrace();
			}
        }
   
        if(isInCriticalSection)
        {
        	this.criticalSectionAvailable=false;
        	this.currentCriticalSectionNode=this.nodeID;
        }else if(isInWaitingQueue)
        {
        	this.criticalSectionQueue.add(this.nodeID);
        }
        
        totalMessageCount+=incomingMessageCount;	//Total number of messages passed during the election
        this.nodeInfo.remove(previousCoordinatorID);
        previousCoordinatorID=coordinatorID;
    }

    //Remote method that allows other nodes to change their coordinator and remove the details of the previous coordinator
    public int remoteBroadcastCoordinatorNodeID(int nodeID) throws RemoteException {
    	this.nodeInfo.remove(previousCoordinatorID);
    	this.coordinatorID = nodeID;
        this.electionFlag = false;
        return this.incomingMessageCount;
    }

    //Method that allows other nodes to perform leave operation when in CS
    void performLeave() throws NumberFormatException, RemoteException, NotBoundException
    {
    	int status=-1;
		if(this.nodeID!=coordinatorID)
		{
			try{
				String nodeValue = this.nodeInfo.get(coordinatorID);
				String[] nodeIpPort = nodeValue.split(";");
				Registry aRegistry = LocateRegistry.getRegistry(nodeIpPort[0], Integer.parseInt(nodeIpPort[1]));
				RemoteInterface aNode = (RemoteInterface)aRegistry.lookup(""+coordinatorID);
				status=aNode.remoteLeave(nodeID);
				
			}catch (ConnectException e)
			{
				initiateElection();
				performLeave();
				
			}
		}else{
			status=Leave(this.nodeID);
			
		}
		if(status==0)
		{
			isInCriticalSection=false;
			System.out.println("Node successfully left the critical region");
		}
		else if(status==1)
			System.out.println("Node still waiting in the queue for critical region");
		else{
			//System.out.println("Node not present in the queue");
		}		
    }
    
  //Method that allows other nodes to request for access to CS
    void performRequest() throws NumberFormatException, RemoteException, NotBoundException
    {
    	boolean status =false;
		if(this.nodeID!=coordinatorID)
		{
			try{
				
				String nodeValue = this.nodeInfo.get(coordinatorID);
				String[] nodeIpPort = nodeValue.split(";");
				Registry aRegistry = LocateRegistry.getRegistry(nodeIpPort[0], Integer.parseInt(nodeIpPort[1]));
				RemoteInterface aNode = (RemoteInterface)aRegistry.lookup(""+coordinatorID);
				status=aNode.remoteAccess(nodeID);	
			
			}catch(ConnectException e){
				initiateElection();
				performRequest();
			}
		}else{
			status =Access(this.nodeID);
		}
		if(status)
		{
			System.out.println("Node entered critical section");
			isInCriticalSection=true;
		}
		else{
			isInWaitingQueue=true;
			System.out.println("Node waiting in the queue");
		}
    }
    
    //Method that is used to allow the users to enter their commands
    void userPrompt() throws IOException, NumberFormatException, AlreadyBoundException, NotBoundException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		while(true)
		{
			String command=br.readLine();
			String[] commandTokens=command.split(" ");
			if(commandTokens[0].equals("join"))			//If a node wants to join
			{
				if(commandTokens.length ==3)			//If it is the first node in the network
				{
					//Format : NodeID, Port Number
					join(Integer.parseInt(commandTokens[1]), Integer.parseInt(commandTokens[2]));
				}else if(commandTokens.length==6)		//If it is not the first node in the network
				{
					//Format: IPAddress,Port Number, NodeID
					join(Integer.parseInt(commandTokens[1]),Integer.parseInt(commandTokens[2]),commandTokens[4],Integer.parseInt(commandTokens[5]),Integer.parseInt(commandTokens[3]));
				}
				
			}else if(commandTokens[0].equals("request"))	//If a node wants to access critical section
			{
				performRequest();
				
				
			}else if(commandTokens[0].equals("leave"))		//If a node wants to leave the CS
			{
				performLeave();
	            			
			}else if(commandTokens[0].equals("help"))		//If the user needs help regarding the commands
			{
				System.out.println("1. join -- Join the network \n2. request -- Request to enter critical section \n3. leave --request to leave the network ");
			}else if(commandTokens[0].equals("count"))		//If the user needs to know the total message count during the election
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
				
			}else if(commandTokens[0].equals("view"))		//If the user needs to know the current details of the node and about the coordinator
			{
				System.out.println("_________________________________________");
				System.out.println("|  "+nodeID+" "+nodeIP+" "+portNumber+"  |");
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
				 System.out.println("Is in Critical Section :"+isInCriticalSection);
				 System.out.println("_________________________________________");
				 System.out.println("Is in Waiting Queue :"+isInWaitingQueue);
				 System.out.println("_________________________________________");
				 
				 if(this.nodeID==coordinatorID)
				 {
					 System.out.println("Critical section node :"+ currentCriticalSectionNode);
					 System.out.println("_________________________________________");
					 System.out.println("Critical section queue :"+criticalSectionQueue);
					 System.out.println("_________________________________________");
					 System.out.println("Critical section available :"+ criticalSectionAvailable);
					 System.out.println("_________________________________________");
					 
				 }
				
			}else{		//If the command is invalid then a meesage is displayed to enter the valid command
				System.out.println("Wrong command!.Type help for list of commands");
			}
		}	
	}
    
    //Main method that creates an object of Modified bully class and a method that allows users to enter their commands
    public static void main(String[] args) throws AlreadyBoundException, IOException, NotBoundException {
    	
    	ModifiedBully obj=new ModifiedBully();
    	obj.userPrompt();		
    }
    
    //Method to get the details of the current coordinator
    @Override
	public int getCoordinatorID() {
		
		return this.coordinatorID;
	}

    //Local method to display the details about the critical section
	public void LocaldisplayEnteringCS()
	{
		isInWaitingQueue=false;
		isInCriticalSection=true;
		System.out.println("You have entered Critical Section");
	}

	//Remote method to display the details about the critical section
	@Override
	public void displayEnteringCS() throws RemoteException,
			NotBoundException {
		isInWaitingQueue=false;
		isInCriticalSection=true;
		System.out.println("You have entered Critical Section");
	}


	//Remote method to get the message count of individual nodes
	@Override
	public int remoteGetMessageCount() throws RemoteException,
			NotBoundException {
		
		return this.totalMessageCount;
	}
	
	//Remote method that allows nodes to get the status of the critical section
	@Override
	public int remoteGetCriticalSectionStatus() {
		// TODO Auto-generated method stub
		
		if(isInCriticalSection)
			return 1;
		else if(isInWaitingQueue)
			return 2;
		else
			return 0;
		
	}

	// Remote Method that checks if a node is alive
	@Override
	public void remoteCheckAlive() throws RemoteException, NotBoundException {
		// TODO Auto-generated method stub
		
	}


	


	
	

	



	
}
