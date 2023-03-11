package MainServer.ElectionCore;

import MainServer.ElectionCore.State.ElectionState;
import MainServer.Monitor.RequestQueueMonitor;
import MainServer.ServerState;
import Util.NetworkConstants;
import Util.NetworkUtil;
import org.apache.catalina.Server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class ElectionConsumer {

    public static boolean response = false;
    public static boolean isBullied = false;

    public static void sendLeader(String IP){

    }

    public static void sendElection(String IP){

    }

    public static void initiateElection() throws UnknownHostException, InterruptedException {
        ServerState.isElectionRunning = true;
        List<String> higher = new ArrayList<>();

        for(String ip : NetworkConstants.SERVER_IPS){
            InetAddress currIP = InetAddress.getByName(ip);
            if(NetworkUtil.isGreater(currIP, InetAddress.getByName(ServerState.serverIP))){
                higher.add(ip);
            }
        }

        if(higher.size() == 0){

            return;
        }

        for(String IP : higher){
            sendElection(IP);
        }

        ElectionConsumer.isBullied = false;
        ElectionConsumer.response = false;
        Thread.sleep(1000);
        if(!ElectionConsumer.response){
            ElectionConsumer.setLeader();
        }else if(ElectionConsumer.isBullied){
            ElectionConsumer.response = false;
        }

    }

    public static void setLeader(){
        for (String IP : NetworkConstants.SERVER_IPS){
            sendLeader(IP);
        }
        new Thread(new RequestQueueMonitor()).start();
    }

}
