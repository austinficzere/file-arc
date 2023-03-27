package MainServer.ExecutionCore;

import DatabaseManager.ReplicationRunner;
import MainServer.ServerState;
import Util.DB;
import Util.NetworkConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.*;
import java.util.ArrayList;
import Util.NetworkUtil;
import static com.mongodb.client.model.Filters.eq;

public class ExecutionCoreHandler {
    public static void obtainLock(String IP, JsonNode request) {
        System.out.println("trying to obtain lock");
        int headOrder = -1;
        int currOrder = request.get("orderValue").asInt();
        while (headOrder != currOrder) {
            headOrder = NetworkUtil.obtainLock(IP, request.get("fileName").asText());
            System.out.println("headOrder: " + headOrder);
            System.out.println("Current order: " + currOrder);
        }
    }


    public static void releaseLock(String filename){
        return;
    }

//    public static void sendToResponseQueue(JsonNode rq, String IP){
//        RestTemplateBuilder builder = new RestTemplateBuilder();
//        RestTemplate restTemplate = builder.setConnectTimeout(Duration.ofMillis(1000)).build();
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        String uri = NetworkConstants.getResponseQueueURI(IP);
//
//        HttpEntity<String> request =
//                new HttpEntity<String>(rq.toString(), headers);
//
//        try {
//            restTemplate.postForEntity(uri, request, String.class);
//        } catch(RestClientException e){
//        }
//    }

    public static void processEvent(JsonNode request) throws IOException {
        DB db = new DB();
        // Parse HTML
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (request == null) return;


        // Check type of request
        if(request.get("requestType").asText().equalsIgnoreCase("READ")){ // locking
            System.out.println("database requestType" + System.currentTimeMillis());
            String readType = request.get("readType").asText();

            System.out.println("database requestType" + System.currentTimeMillis());
            if(readType.equals("allFiles")){
                ArrayList<JsonNode> files = db.findFiles(request.get("userName").asText());

                JsonNode response;

                ObjectMapper objectMapper = new ObjectMapper();
                if (!files.isEmpty()){
                    response = objectMapper.valueToTree(files);
                }
                else{
                    response = objectMapper.createObjectNode();
                    ((ObjectNode)response).put("readType", "ALLFILESEMPTY");
                    ((ObjectNode)response).put("userName", request.get("userName").asText());
                }

                for(String IP : NetworkConstants.RESPONSE_QUEUE_IPS){
//                    for (JsonNode file : files) {
//                        System.out.println("the actual file"+file);
//                        sendToResponseQueue(file, IP);
//                    }
                    NetworkUtil.sendToResponseQueue(response, IP);

//                    sendToResponseQueue(response, IP);
                }
                System.out.println("database blah" + System.currentTimeMillis());
            }

            else if(readType.equals("SINGLE")){
                System.out.println("DATABASE SINGLE BEFORE" + System.currentTimeMillis());
                JsonNode singleFile = db.loadFile(request.get("fileName").asText());
                System.out.println("DATABASE SINGLE AFTER LOAD" + System.currentTimeMillis());

                ((ObjectNode)singleFile).put("readType", "SINGLE");
                for(String IP : NetworkConstants.RESPONSE_QUEUE_IPS){
//                    for (JsonNode file : files) {
//                        System.out.println("the actual file"+file);
//                        sendToResponseQueue(file, IP);
//                    }
                    NetworkUtil.sendToResponseQueue(singleFile, IP);
                }
                System.out.println("DATABASE SINGLE FOR LOOP" + System.currentTimeMillis());

            }

            else if (readType.equals("LOGIN")){
                FindIterable<Document> entry = db.getLoginReplica(true).find(eq("userName", request.get("userName").asText()));

                ObjectMapper mapper = new ObjectMapper();
                JsonNode response = mapper.createObjectNode();
                ((ObjectNode)response).put("userName", request.get("userName").asText());
                ((ObjectNode)response).put("readType", "LOGIN");

                for (Document doc : entry) {
                    String actualUserName = doc.getString("userName");
                    String actualPassword = doc.getString("password");

                    if (request.get("userName").asText().equals(actualUserName) && request.get("password").asText().equals(actualPassword)) {
                        ((ObjectNode) response).put("loggedIn", "SUCCESS");
                        break; // break the loop once a match is found
                    } else {
                        ((ObjectNode) response).put("loggedIn", "FAILURE");
                    }
                }

                if (!response.has("loggedIn")) {
                    // handle case where no match was found
                    ((ObjectNode) response).put("loggedIn", "FAILURE");
                }

                for(String IP : NetworkConstants.RESPONSE_QUEUE_IPS){
                    NetworkUtil.sendToResponseQueue(response, IP);
                }
            }

        }
        else if(request.get("requestType").asText().equalsIgnoreCase("WRITE")){
            String writeType = request.get("writeType").asText();
            String fileName = request.get("fileName").asText();
            obtainLock(ServerState.requestQueueIP,request);

            if(writeType.equals("DELETE")){
                String deleteList = NetworkUtil.sendDelete(request);
                System.out.println("PRINITNG  DELETE LIST: " + deleteList);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode response = mapper.createObjectNode();
                ((ObjectNode)response).put("userName", request.get("userName").asText());
                ((ObjectNode)response).put("readType", "DELETE");
                ((ObjectNode)response).put("delete", deleteList);



                System.out.println("PRINITNG RESPONSE LIST: " + response);


                for(String IP : NetworkConstants.RESPONSE_QUEUE_IPS){
                    NetworkUtil.sendToResponseQueue(response, IP);
                }
            }

            else if (writeType.equals("SHARE")) {
                ArrayList<String> arr = new ObjectMapper().convertValue(request.get("sharedWith"), ArrayList.class);
                db.editSharedWith(fileName, arr);
            }

            else if (writeType.equals("REGISTER")){
                boolean wasSuccessful = NetworkUtil.sendRegister(request);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode response = mapper.createObjectNode();
                ((ObjectNode)response).put("userName", request.get("userName").asText());
                ((ObjectNode)response).put("writeType", "REGISTER");

                if (wasSuccessful) {
                    ((ObjectNode) response).put("registered", "SUCCESS");
                } else {
                    ((ObjectNode) response).put("registered", "FAILURE");
                }

                for(String IP : NetworkConstants.RESPONSE_QUEUE_IPS){
                    NetworkUtil.sendToResponseQueue(response, IP);
                }
            }
            else {

                System.out.println("Send to database" + System.currentTimeMillis());

                System.out.println("Request is: " + request);


                NetworkUtil.sendWrite(request);

                System.out.println("database write done" + System.currentTimeMillis());
                for(String IP : NetworkConstants.RESPONSE_QUEUE_IPS){
                    NetworkUtil.sendToResponseQueue(request, IP);
                }

                System.out.println("Responsequeue sent" + System.currentTimeMillis());


            }

            NetworkUtil.releaseLock(ServerState.requestQueueIP,fileName);
        }
        else{
            System.out.println("invalid request type (must be READ or WRITE)");
            return;
        }

    }


}
