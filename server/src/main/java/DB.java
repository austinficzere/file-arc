package main.java;

import static com.mongodb.client.model.Filters.eq;

import java.awt.List;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
public class DB {
	private final static String URI = "mongodb+srv://cpsc559:cpsc559@cluster0.137nczk.mongodb.net/?retryWrites=true&w=majority";
	public MongoClient mongoClient;
	public MongoDatabase database;
	public MongoCollection<Document> filesCollection;
	
	DB() throws IOException{
		this.mongoClient = MongoClients.create(URI);
        this.database = mongoClient.getDatabase("cpsc559_db");
        this.filesCollection = this.database.getCollection("files_data");
	
	}
	public void uploadFile(String filePath,String ownerName) throws IOException {
		
		byte[] fileContent = FileUtils.readFileToByteArray(new File(filePath));
    	String encodedString = Base64.getEncoder().encodeToString(fileContent);
        
        //adding image file code
        byte[] bytes = Files.readAllBytes(Paths.get(filePath));
        String[] sharedList = {"ragya","sami"};
        Document entry = new Document("_id", new ObjectId())
        	   .append("filename", "placeHolder.txt")
        	   .append("bytes", encodedString)
        	   .append("owner", ownerName)
        	   .append("created", "14/033/2023")
               .append("shared", Arrays.asList("ragya","sami"));
        filesCollection.insertOne(entry);
        
        System.out.println("Uploaded "+ filePath + " as " + ownerName);
	}
	public ArrayList<JSONObject> findFiles(String ownerName) throws ParseException {
		ArrayList<JSONObject> ret = new ArrayList<JSONObject>();
		
		FindIterable<Document> doc = this.filesCollection.find(eq("owner",ownerName));
		if (doc != null) {
			System.out.println("Found files for " + ownerName + "!");
			for(Document d: doc) {
	        	JSONParser tempParser = new JSONParser();
	        	JSONObject tempJson = (JSONObject) tempParser.parse(d.toJson());
	        	ret.add(tempJson);
	        	System.out.println(">" + tempJson.get("filename"));
	        }
        } else {
            System.out.println("No match");
        }
		return ret;
	}
	public void saveFileFromDB(String filename, String dest) throws ParseException, IOException {
		Document doc = this.filesCollection.find(eq("filename", filename)).first();
		if (doc != null) {
			JSONParser parser = new JSONParser();
	    	JSONObject json = (JSONObject) parser.parse(doc.toJson());
	    	System.out.println("bytes: "+ json.get("bytes") + " end");
	    	byte[] fileBytes = javax.xml.bind.DatatypeConverter.parseBase64Binary((String) json.get("bytes"));
	    	ByteArrayOutputStream out = new ByteArrayOutputStream();
	        ObjectOutputStream os = new ObjectOutputStream(out);
	        //os.writeObject(json.get("selectedFile"));
	        FileOutputStream fos = new FileOutputStream(dest);
	        fos.write(fileBytes);
	        fos.close();
	        System.out.println("Saved file at location: " + dest);
	        }
        else {
            System.out.println("File not found");
        }
        
	}
    public static void main( String[] args ) throws IOException, ParseException, JSONException {
        // Replace the placeholder with your MongoDB deployment's connection string 
        	DB myDB = new DB();
        	ArrayList<JSONObject> dbFiles = myDB.findFiles("manbir");
        	System.out.println(dbFiles.get(0).get("filename"));
        	String filePath = "C:\\Users\\rgmit\\OneDrive\\Desktop\\merge.txt";
        	
        	//myDB.uploadFile(filePath);
        	myDB.saveFileFromDB("merge.txt", "C:\\Users\\rgmit\\OneDrive\\Desktop\\ragMerge.txt");
            
            
         
            
     }
}