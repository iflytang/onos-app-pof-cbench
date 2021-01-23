package org.onosproject.pof.cbench;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author tsf
 * @date 2020-05-12
 * @desp
 */
public class SocketServerThread implements Runnable {

    private Socket client = null;
//    private static int socket_num = 0;

    private File wrt_fd = null;

    public SocketServerThread(Socket client, File wrt_fd) {
        this.client = client;
        this.wrt_fd = wrt_fd;
    }

    private Date start_time, end_time;
    private BufferedWriter buf_wrt;

    private int cnt = 0;

    @Override
    public void run() {
        InetAddress inetAddress = null;

        InputStream inputStream = null;   // receive data from client
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;

        OutputStream outputStream = null;  // send data to client
        PrintWriter printWriter = null;

        try {
            inetAddress = client.getInetAddress();

            inputStream = client.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(inputStreamReader);

            outputStream = client.getOutputStream();
            printWriter = new PrintWriter(outputStream);

            /* record result. */
            buf_wrt = new BufferedWriter(new FileWriter(wrt_fd, true));
//            buf_wrt.write("I am appended test.\n");

            start_time = new Date();
            System.out.println("start_time: " + start_time + ", get_time: " + start_time.getTime() + " ms.");

            String msg = null;

            while ((msg = bufferedReader.readLine()) != null) {

                /* RECEIVE DATA FROM CLIENT */

                /*JsonObject object = Json.parse(msg).asObject();
                String name = object.get("name").asString();
                System.out.println(name); */

                System.out.println("server, from client<" + inetAddress + ">: " + msg);
                cnt += 1;

                /* SEND DATA TO CLIENT */
//                printWriter.write("Server: " + msg);
//                printWriter.flush();

                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

            }
            client.shutdownInput();   // close input stream

        } catch (IOException e) {
            e.printStackTrace();
        } finally {  // close socket resource
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }

                if (inputStream != null) {
                    inputStream.close();
                }

                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }

                if (client != null) {
                    client.close();
                    SocketServer.socket_num--;
                    System.out.println("client<" + inetAddress + "> disconnected! current connected_num: " + SocketServer.socket_num);
                    end_time = new Date();
                    System.out.printf("start_time: %s, get_time: %d ms.\nend_time: %s, get_time: %d ms.\ndelta_time: %.3f s, cnt: %d.\n",
                            start_time, start_time.getTime(), end_time, end_time.getTime(),
                            (end_time.getTime() - start_time.getTime()) / 1000.0, cnt);

                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
                    buf_wrt.write(df.format(start_time) + "\t" + start_time.getTime() + "\t"
                                    + df.format(end_time) + "\t" + end_time.getTime() + "\t"
                                    + String.format("%.3f", (end_time.getTime() - start_time.getTime()) / 1000.0) + "\t\n");
                    buf_wrt.flush();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
