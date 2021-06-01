import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebServer {

    private final int port = 8383;
    private ServerSocket serverSocket;
    private final ExecutorService pool;

    public WebServer() throws IOException {
        System.out.println("Starting server...");
        this.serverSocket = new ServerSocket(port);
        System.out.println("Server socket started...");
        pool = Executors.newFixedThreadPool(2);
        while (true){
            Socket newClient = null;
            newClient = serverSocket.accept();
            pool.execute(new Handler(newClient));
        }

    }

    public static void main(String[] args) throws IOException {
        WebServer webServer = new WebServer();
    }

}
