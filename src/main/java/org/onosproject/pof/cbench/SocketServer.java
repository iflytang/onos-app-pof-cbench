package org.onosproject.pof.cbench;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

/**
 * @author tsf
 * @date 2020-05-12
 * @desp
 */
public class SocketServer {


    private static final String SERVER_ADDR = "192.168.109.212";
    private static final int PORT = 2020;

    static int socket_num = 0;

    public static void main (String[] args) {

        try {  // server
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("server<" + SERVER_ADDR + "> socket is waiting to be connected ...");
            System.out.println("listening port is " + PORT);

            Socket client = null;
            InetAddress inetAddress = null;

            String result_file = "src/main/java/org/onosproject/pof/cbench/result_processing_time.txt";
            File wrt_fd = null;
            BufferedWriter buf_wrt;
            try {
                wrt_fd = new File(result_file);
                wrt_fd.createNewFile();
                buf_wrt = new BufferedWriter(new FileWriter(wrt_fd));
//                buf_wrt.write("test\n");
//                buf_wrt.flush();
//                buf_wrt = new BufferedWriter(new FileWriter(wrt_fd, true));
//                buf_wrt.write("I am appended test.\n");
//                buf_wrt.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }

            while (true) {
                // client connection
                client = serverSocket.accept();
                inetAddress = client.getInetAddress();
                Thread serverThread = new Thread(new SocketServerThread(client, wrt_fd));
                serverThread.start();

                // client statistics
                socket_num++;
                System.out.println("client<" + inetAddress + "> connected! current connected_num: " + socket_num);

                if (!serverThread.isAlive()) {
                    socket_num--;
                    System.out.println("client<" + inetAddress + "> disconnected! current connected_num: " + socket_num);
                }

                /* stop thread. */
                Thread.sleep(2000);
                serverThread.interrupt();
            }

        } catch (IOException | InterruptedException io) {
            io.printStackTrace();
        }

    }

}
