package ResponseQueue.Server;

import RequestQueue.Server.RequestQueueServerMain;
import ResponseQueue.Service.ResponseQueueHandler;
import Util.NetworkConstants;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.net.UnknownHostException;
import java.util.Collections;

@SpringBootApplication(exclude = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class
})
@ComponentScan({ "ResponseQueue" })
public class ResponseQueueServerMain {
    public static void main(String[] args) throws UnknownHostException {

        SpringApplication app = new SpringApplication(ResponseQueueServerMain.class);
        System.out.println("RESPONSE QUEUE IS RUNNING");
        app.setDefaultProperties(Collections
                .singletonMap("server.port", Integer.toString(NetworkConstants.RESPONSE_QUEUE_PORT)));

        ApplicationContext context = app.run(args);
        ResponseQueueHandler responseQueueMicroService = context.getBean(ResponseQueueHandler.class);

        ResponseQueueWebServer responseQueueWebServer = new ResponseQueueWebServer(NetworkConstants.RESPONSE_QUEUE_SERVER_PORT, responseQueueMicroService);
        Thread responseQueueWebServerThread = new Thread(responseQueueWebServer);
        responseQueueWebServerThread.start();
        System.out.println("Response queue web server is running...");
    }
}
