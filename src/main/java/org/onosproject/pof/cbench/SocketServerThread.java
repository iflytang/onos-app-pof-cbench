package org.onosproject.pof.cbench;

import org.onosproject.net.DeviceId;

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

    /**
     * util tools.
     */

    public String short2HexStr(short shortNum) {
        StringBuilder hex_str = new StringBuilder();
        byte[] b = new byte[2];
        b[1] = (byte) (shortNum & 0xff);
        b[0] = (byte) ((shortNum >> 8) & 0xff);

        return bytes_to_hex_str(b);
    }

    public String byte2HexStr(byte byteNum) {
        String hex = Integer.toHexString(   byteNum & 0xff);
        if (hex.length() == 1) {
            hex = '0' + hex;
        }
        return hex;
    }

    public String funcByteHexStr(DeviceId deviceId) {
        String device = deviceId.toString().substring(18, 20);   /* for 'pof:000000000000000x', get '0x' */
        byte dpid = Integer.valueOf(device).byteValue();
        int k = 2, b = 1;
        byte y = (byte) (k * dpid + b);   // simple linear function
        return byte2HexStr(y);
    }

    public String bytes_to_hex_str(byte[] b) {
        StringBuilder hex_str = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xff);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            hex_str.append(hex);
        }
        return hex_str.toString();
    }

    public static double bytes2Double(byte[] arr, int k){
        long value=0;
        for(int i=0;i< 8;i++){
            value|=((long)(arr[k]&0xff))<<(8*i);
            k++;
        }
        return Double.longBitsToDouble(value);
    }

    public static int bytes2Int(byte[] arr, int k){
        int value=0;
        for(int i=0;i< 4;i++){
            value|=((arr[k]&0xff))<<(4*i);
            k++;
        }
        return value;
    }

    public static float bytes2float(byte[] b, int index) {
        int l;
        l = b[index + 0];
        l &= 0xff;
        l |= ((long) b[index + 1] << 8);
        l &= 0xffff;
        l |= ((long) b[index + 2] << 16);
        l &= 0xffffff;
        l |= ((long) b[index + 3] << 24);
        return Float.intBitsToFloat(l);
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
//            inputStreamReader = new InputStreamReader(inputStream);
//            bufferedReader = new BufferedReader(inputStreamReader);

            outputStream = client.getOutputStream();
            printWriter = new PrintWriter(outputStream);

            /* record result. */
            buf_wrt = new BufferedWriter(new FileWriter(wrt_fd, true));
//            buf_wrt.write("I am appended test.\n");

            start_time = new Date();
            System.out.println("start_time: " + start_time + ", get_time: " + start_time.getTime() + " ms.");

            String msg = null;

            int int_byte_size = 4;
            int data_num = 6;
            byte[] receive = new byte[data_num * int_byte_size];

            float[] recv_float_data = new float[data_num];
            int[] recv_int_data = new int[data_num];

            int i, j;

            while (true) {

                /* RECEIVE DATA FROM CLIENT */

                /*JsonObject object = Json.parse(msg).asObject();
                String name = object.get("name").asString();
                System.out.println(name); */

                int len = inputStream.read(receive, 0, receive.length);

                System.out.println("server, from client<" + inetAddress + ", cnt: " + cnt);

                for (i = 0, j = 0; i < len; i = i + int_byte_size, j++) {
                    if (j < data_num) {   // first 'monitor_nodes' points
                        recv_float_data[j] = bytes2float(receive, i);
                        recv_int_data[j] = bytes2Int(receive, i);
//                        System.out.println("recv_float_data: " + j + ", " + recv_float_data[j]);
                        System.out.println("recv_int_data: " + j + ", " + recv_int_data[j]);
                    }
                }

                if (len < 0) {
                    break;
                }

                cnt += 1;

                /* SEND DATA TO CLIENT */
//                printWriter.write("Server: " + msg);
//                printWriter.flush();

//                if (Thread.currentThread().isInterrupted()) {
//                    break;
//                }

            }
//            client.shutdownInput();   // close input stream

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

                    cnt = 0;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
