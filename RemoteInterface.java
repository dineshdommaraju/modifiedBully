package modifiedBully1;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;


public interface RemoteInterface extends Remote{
	
	public HashMap<Integer,String> remoteInsertNode(String IP,Integer port, Integer nodeID);
	void remoteInitiateElection();
	void remoteBroadcastNewNodeID();
	void remoteBroadcastCoordinatorNodeID();
}

