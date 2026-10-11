import task.ConnectionProcessor;
import util.Constants;
import util.executor.Executor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static util.Constants.SERVER_PORT;

public class Server {
    private static boolean active = true;

    public static void main(String[] args) {
        String backendHost = Constants.DEFAULT_BACKEND_HOST;
        if (args != null && args.length > 0 && args[0] != null && !args[0].isBlank()) {
            backendHost = args[0].trim();
        }
        Constants.BACKEND_HOST = backendHost;
        System.out.println("Starting web-client server on port " + SERVER_PORT);
        System.out.println("Configured backend server-host: " + Constants.BACKEND_HOST);

        try(ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            Executor executor = new Executor()){
            executor.start();
            while(active){
                Socket socket = serverSocket.accept();
                try{
                    executor.submit(new ConnectionProcessor(socket, backendHost));
                } catch (Exception e) {
                    try{
                        System.err.println("Task rejected "+socket);
                        socket.close();
                    } catch (IOException ex) {
                        System.err.println("Fail to close socket "+socket);
                    }
                }

            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void stop(){
        active = false;
    }
}
