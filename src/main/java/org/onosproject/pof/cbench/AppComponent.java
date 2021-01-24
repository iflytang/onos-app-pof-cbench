/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.pof.cbench;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;

import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.util.Tools;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;

import org.onosproject.floodlightpof.protocol.OFMatch20;
import org.onosproject.floodlightpof.protocol.action.OFAction;
import org.onosproject.floodlightpof.protocol.table.OFFlowTable;
import org.onosproject.floodlightpof.protocol.table.OFTableType;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.instructions.DefaultPofActions;
import org.onosproject.net.flow.instructions.DefaultPofInstructions;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.table.DefaultFlowTable;
import org.onosproject.net.table.FlowTable;
import org.onosproject.net.table.FlowTableService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class AppComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ReactivePacketProcessor processor = new ReactivePacketProcessor();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceAdminService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private FlowTableService flowTableService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private FlowRuleService flowRuleService;

    private ApplicationId appId;
    private NodeId local;

    private ArrayList<DeviceId> deviceIdList = new ArrayList<>();

    private static final int PACKET_BUFFER = 50000;

    private static final int BATCH_SIZE = 5;

    private static final int AVERAGE_COUNT = 5;

    boolean hasBegin = false;

    boolean hasEnd = false;

    /**
     * PacketIn message handle queue
     */
    private BlockingDeque<PacketContext> handleQueue =
            new LinkedBlockingDeque<>(PACKET_BUFFER);

    /**
     * Single thread executor for PacketIn handling.
     */
    private ExecutorService executorService;

//    private Thread intProcessor;
    private ExecutorService threadPool;
    protected SocketINTProcessor socketINTProcessor = null;


    @Activate
    protected void activate() throws InterruptedException {
        log.info("Started");

        appId = coreService.registerApplication("org.onosproject.vn.pof.cbench");

        local = clusterService.getLocalNode().id();

        packetService.addProcessor(processor, PacketProcessor.director(2));

        local = clusterService.getLocalNode().id();

        for (Device device: deviceService.getAvailableDevices()) {
            DeviceId deviceId = device.id();
            deviceIdList.add(deviceId);
            NodeId master = mastershipService.getMasterFor(deviceId);
            log.info("master for device: {} is {}", deviceId, master);
            if (Objects.equals(local, master)) {
                List<Port> portList = deviceService.getPorts(deviceId);
                log.info("begin enable ports of specific deviceId: {}", deviceId.toString());
                for (Port port: portList) {
                    log.info("port in portList: {}", port.toString());
                    deviceService.changePortState(deviceId, port.number(), true);
                    /*if(!port.isEnabled()) {
                        log.info("begin enable ports:" + port.toString());
                        deviceService.changePortState(deviceId, port.number(), true);
                    }*/
                }

                byte tableId = sendPofFlowTable(deviceId);
                log.info("send flow table to device: {} with flow table id: {}", deviceId, tableId);
//                long start = System.currentTimeMillis();
//                Thread.sleep(1000);
//                long end = System.currentTimeMillis();
//                System.out.println(end - start + "ms");
//                String srcIpv4 = "10.0.0.1";
//                String dstIpv4 = "10.0.0.2";
//                sendPofFlowEntry(deviceId, (byte) 0, 0, (short)2, dstIpv4);
//                sendPofFlowEntry(deviceId, (byte) 0, 1, (short)1, srcIpv4);
            }
        }

//        executorService = Executors.newFixedThreadPool(2, Tools.groupedThreads("onos/cbench/pof/", "-%d", log));
//        executorService = Executors.newSingleThreadExecutor(Tools.groupedThreads("onos/cbench/packetin/handler", "1", log));

//        executorService.submit(new FlowRuleInstaller(deviceIdList, AVERAGE_COUNT, BATCH_SIZE));
//        executorService.submit(() -> {
//            log.warn("Thread is submitted: {}", Thread.currentThread().getName());
//            while (true) {
//                if(Thread.currentThread().isInterrupted()) {
//                    return;
//                }
//            }
//        });
//        log.info("flow rule installer started: {}", executorService);


        threadPool = Executors.newCachedThreadPool();
        socketINTProcessor = new SocketINTProcessor(threadPool);
        threadPool.execute(socketINTProcessor);

//
//        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(processor);
        processor = null;
        //flowRuleService.removeFlowRulesById(appId);


//        if (executorService != null) {
//            log.info("flow rule installer shutdown: {}", executorService);
//            executorService.shutdown();
//        }

        threadPool.shutdown();

        log.info("Stopped");
    }

    public byte sendPofFlowTable(DeviceId deviceId) {
        /**
         * begin send flow table
         */
        byte tableId = (byte) flowTableService.getNewGlobalFlowTableId(deviceId, OFTableType.OF_MM_TABLE);
        //byte smallTableId = tableStore.parseToSmallTableId(deviceId, tableId);

        OFMatch20 srcIP = new OFMatch20();
        srcIP.setFieldId((short) 1);
        srcIP.setFieldName("srcIp");
        srcIP.setOffset((short) 208);
        srcIP.setLength((short) 32);

        OFMatch20 dstIP = new OFMatch20();
        dstIP.setFieldId((short) 2);
        dstIP.setFieldName("dstIp");
        dstIP.setOffset((short) 240);
        dstIP.setLength((short) 32);

        ArrayList<OFMatch20> match20List = new ArrayList<OFMatch20>();
        match20List.add(srcIP);
        match20List.add(dstIP);
        /**
         * construct OFFlowTable
         */
        OFFlowTable ofFlowTable = new OFFlowTable();
        ofFlowTable.setTableId(tableId);
        ofFlowTable.setTableName("FirstEntryTable");
        ofFlowTable.setMatchFieldNum((byte) 1);
        ofFlowTable.setTableSize(64);
        ofFlowTable.setTableType(OFTableType.OF_MM_TABLE);
        ofFlowTable.setCommand(null);
        ofFlowTable.setKeyLength((short) 64);

        ofFlowTable.setMatchFieldList(match20List);
//        ofFlowTable.setKeyLength((short) 48);
//        ofFlowTable.setMatchFieldNum((byte) 1);

        /**
         * send flow table to device
         */
        FlowTable.Builder flowTable = DefaultFlowTable.builder()
                .withFlowTable(ofFlowTable)
                .forTable(tableId)
                .forDevice(deviceId)
                .fromApp(appId);
        log.info("before applyFlowTables");
        flowTableService.applyFlowTables(flowTable.build());
        return tableId;
    }

    public long sendPofFlowEntry(DeviceId deviceId, byte tableId, int entryId, short outPort, String srcIpv4,String dstIpv4) {
        /**
         * send flow entry: newFlowEntryId
         */
        long newFlowEntryId = entryId;
        long srcIpv4Address = Ip4Address.valueOf(srcIpv4).toInt();
        String srcToHex = Long.toHexString(0x00000000FFFFFFFFL & srcIpv4Address | 0xFFFFFFFF00000000L).substring(8);
        long dstIpv4Address = Ip4Address.valueOf(dstIpv4).toInt();
        String dstToHex = Long.toHexString(0x00000000FFFFFFFFL & dstIpv4Address | 0xFFFFFFFF00000000L).substring(8);

        /**
         * build traffic selector
         */
        TrafficSelector.Builder pbuilder = DefaultTrafficSelector.builder();
        //pbuilder.matchInPort(PortNumber.portNumber(1));
        ArrayList<Criterion> entryList = new ArrayList<Criterion>();
        entryList.add(Criteria.matchOffsetLength((short)1,(short)208,(short)32,srcToHex,"ffffffff"));
        entryList.add(Criteria.matchOffsetLength((short)2,(short)240,(short)32,dstToHex,"ffffffff"));
        pbuilder.add(Criteria.matchOffsetLength(entryList));
        /**
         * instructions/actions: output
         */
        TrafficTreatment.Builder ppbuilder = DefaultTrafficTreatment.builder();
        List<OFAction> actions = new ArrayList<OFAction>();

        actions.add(DefaultPofActions.output((short)0, (short)0, (short)0, outPort).action());
        ppbuilder.add(DefaultPofInstructions.applyActions(actions));
        /**
         * construct flow rule
         */
        TrafficSelector selector = pbuilder.build();
        TrafficTreatment treatment = ppbuilder.build();

        FlowRule.Builder flowRule = DefaultFlowRule.builder()
                .forTable(tableId)
                .forDevice(deviceId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(1)
                .makePermanent()
                .withCookie(newFlowEntryId);//to set flow entryId

        flowRuleService.applyFlowRules(flowRule.build());
        return newFlowEntryId;
    }

    public FlowRule generatePofFlowEntry(DeviceId deviceId, byte tableId, int entryId, short outPort) {
        /**
         * send flow entry: newFlowEntryId
         */
        long newFlowEntryId = entryId;
        long srcIpv4Address = RandomUtils.nextInt();
        String srcToHex = Long.toHexString(0x00000000FFFFFFFFL & srcIpv4Address | 0xFFFFFFFF00000000L).substring(8);
        long dstIpv4Address = RandomUtils.nextInt();
        String dstToHex = Long.toHexString(0x00000000FFFFFFFFL & dstIpv4Address | 0xFFFFFFFF00000000L).substring(8);
        /**
         * match
         */
        TrafficSelector.Builder pbuilder = DefaultTrafficSelector.builder();
        //pbuilder.matchInPort(PortNumber.portNumber(1));
        ArrayList<Criterion> entryList = new ArrayList<Criterion>();
        entryList.add(Criteria.matchOffsetLength((short)1,(short)208,(short)32,srcToHex,"ffffffff"));
        entryList.add(Criteria.matchOffsetLength((short)2,(short)240,(short)32,dstToHex,"ffffffff"));
        pbuilder.add(Criteria.matchOffsetLength(entryList));
        /**
         * instructions/actions: output
         */
        TrafficTreatment.Builder ppbuilder = DefaultTrafficTreatment.builder();
        List<OFAction> actions = new ArrayList<OFAction>();

        actions.add(DefaultPofActions.output((short)0, (short)0, (short)0, outPort).action());
        ppbuilder.add(DefaultPofInstructions.applyActions(actions));
        /**
         * construct flow rule
         */
        TrafficSelector selector = pbuilder.build();
        TrafficTreatment treatment = ppbuilder.build();

        FlowRule flowRule = DefaultFlowRule.builder()
                .forTable(tableId)
                .forDevice(deviceId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(1)
                .makePermanent()
                .withCookie(newFlowEntryId)
                .build();

        return flowRule;
    }

    private class ReactivePacketProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {
            /**
             *Stop processing if the packet has been handled, since we
             *can't do any more to it.
             */
            if (context.isHandled()) {
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();

            if (ethPkt == null) {
                log.info("eth packet is null");
                return;
            }

//            if (!hasBegin) {
//                synchronized (this) {
//                    if(!hasBegin) {
//                        hasBegin = true;
//                        hasEnd = false;
//                        deviceIdList = new ArrayList<>();
//                        for (Device device: deviceService.getAvailableDevices()) {
//                            DeviceId deviceId = device.id();
//                            deviceIdList.add(deviceId);
//                        }
//                        log.info("{} devices: {}", deviceIdList.size(), deviceIdList);
//
//                        executorService.execute(new FlowRuleInstaller(deviceIdList, AVERAGE_COUNT, BATCH_SIZE));
//                        log.info("flow rule installer started: {}", executorService);
//                    }
//                }
//            }

//            deviceIdList = new ArrayList<>();
//            for (Device device: deviceService.getAvailableDevices()) {
//                DeviceId deviceId = device.id();
//                deviceIdList.add(deviceId);
//            }
//            log.info("{} devices: {}", deviceIdList.size(), deviceIdList);
//
//            executorService.execute(new FlowRuleInstaller(deviceIdList, AVERAGE_COUNT, BATCH_SIZE));
//            log.info("flow rule installer started: {}", executorService);
//
//            try {
//                handleQueue.put(context);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                log.warn("put packet to queue exception");
//            }

            packetOut(context, (short)2);

            /*DeviceId deviceId = pkt.receivedFrom().deviceId();
            sendPofFlowEntry(deviceId, (byte) 0, 0, (short) 2, "10.0.0.1", "10.0.0.2");*/

            /*try {
                handleQueue.put(context);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("put packet to queue exception");
            }*/
        }

    }

    private void packetOut(PacketContext context, short outPort) {

        List<OFAction> actions = new ArrayList<>();

        actions.add(DefaultPofActions.output((short)0, (short)0, (short)0, outPort).action());
        context.treatmentBuilder().add(DefaultPofInstructions.applyActions(actions));
//        for (int i=0;i<5;i++)
//        {context.send();}
        context.send();



//        DeviceId deviceid = context.inPacket().receivedFrom().deviceId();
//        String srcIpv4 = "10.0.0.1";
//        String dstIpv4 = "10.0.0.2";
//        sendPofFlowEntry(deviceid, (byte) 0, 0, (short)2, srcIpv4, dstIpv4);
//        sendPofFlowEntry(deviceid, (byte) 0, 1, (short)1, dstIpv4, srcIpv4);
    }

    private class FlowRuleInstaller implements Runnable {

        int batchSize;
        int averageCount;
        List<DeviceId> deviceIds;

        public FlowRuleInstaller(List<DeviceId> deviceIds, int averageCount, int batchSize) {
            this.deviceIds = deviceIds;
            this.averageCount = averageCount;
            this.batchSize = batchSize;
        }

        @Override
        public void run() {
            FlowRuleOperations.Builder rules = FlowRuleOperations.builder();
            int size = 0;

            while (true) {
                //wait for a new packetIn message


                for (int i = 0; i < averageCount; i++) {
                    DeviceId deviceId = deviceIds.get(RandomUtils.nextInt(deviceIds.size()));

                    FlowRule flowRule = generatePofFlowEntry(deviceId, (byte) 0, 0, (short) 2);

                    rules.add(flowRule);
                    size++;
                }
                //apply flow rules as a batch
                if(size >= batchSize) {
                    flowRuleService.apply(rules.build());

                    rules = FlowRuleOperations.builder();

                    size = 0;
                }
                //PacketContext packetContext = handleQueue.take();

            }
        }
    }

    /* public class SocketServer */
    private class SocketINTProcessor implements Runnable {
        private static final String SERVER_ADDR = "192.168.109.215";
        private static final int PORT = 2020;

        int socket_num = 0;

        private ExecutorService threadPool;

        public SocketINTProcessor(ExecutorService threadPool) {
            this.threadPool = threadPool;
        }

        @Override
        public void run() {
//        public void runThread() {
            try {  // server
                ServerSocket serverSocket = new ServerSocket(PORT);
                log.info("server<{}> socket is waiting to be connected ...", SERVER_ADDR);
                log.info("listening port is {}.", PORT);

                Socket client = null;
                InetAddress inetAddress = null;

                String result_file = "/home/tsf/onos-app-pof-cbench/src/main/java/org/onosproject/pof/cbench/result_processing_throughput.txt";
                File wrt_fd = null;
                BufferedWriter buf_wrt = null;
                try {
                    wrt_fd = new File(result_file);
                    wrt_fd.createNewFile();
                    buf_wrt = new BufferedWriter(new FileWriter(wrt_fd, true));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                while (true) {
                    // client connection
                    client = serverSocket.accept();
                    inetAddress = client.getInetAddress();

//                    Thread serverThread = new Thread(new SocketServerThreadONOS(client, wrt_fd));
//                    serverThread.start();

                    threadPool.execute(new SocketServerThreadONOS(client, buf_wrt));

//                    if (Thread.currentThread().isInterrupted()) {
//                        break;
//                    }

                    // client statistics
                    socket_num++;
                    log.info("client<{}> connected! current connected_num: {}", inetAddress, socket_num);

                }

            } catch (IOException io) {
                io.printStackTrace();
            }
        }
    }

    private class SocketServerThreadONOS implements Runnable {

        private Socket client;

        private BufferedWriter buf_wrt;

        public SocketServerThreadONOS(Socket client, BufferedWriter buf_wrt) {
            this.client = client;
            this.buf_wrt = buf_wrt;
            log.info("client: {}, wrt_fd: {}.", this.client, this.buf_wrt);
        }

        private Date start_time, end_time;

        private int cnt = 0;

        public int bytes2Int(byte[] arr, int k){
            int value=0;
            for(int i=0;i< 4;i++){
                value|=((arr[k]&0xff))<<(4*i);
                k++;
            }
            return value;
        }

        public float bytes2float(byte[] b, int index) {
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

        @Override
        public void run() {
            InetAddress inetAddress = null;

            InputStream inputStream = null;   // receive data from client
            InputStreamReader inputStreamReader = null;
            BufferedReader bufferedReader = null;

            OutputStream outputStream = null;  // send data to client
            PrintWriter printWriter = null;

//            log.info("run here 1");

            try {
//                log.info("run here 2");
                inetAddress = client.getInetAddress();
//                log.info("run here 3, inetAddress: {}.", inetAddress);

                inputStream = client.getInputStream();
//                inputStreamReader = new InputStreamReader(inputStream);
//                bufferedReader = new BufferedReader(inputStreamReader);
//                log.info("run here 4, inputStream: {}.", inputStream);

                outputStream = client.getOutputStream();
                printWriter = new PrintWriter(outputStream);

//                log.info("run here 5");

                /* record result. */
//                buf_wrt = new BufferedWriter(new FileWriter(wrt_fd, true));

//                log.info("run here 6, buf_wrt: {}", buf_wrt);

                start_time = new Date();
                log.info("start_time: {}, get_time: {} ms.", start_time, start_time.getTime());

                String msg = null;

                int int_byte_size = 4;
                int data_num = 6;
                byte[] receive = new byte[data_num * int_byte_size];

                float[] recv_float_data = new float[data_num];
                int[] recv_int_data = new int[data_num];

                int i, j;

                while (true) {

                    /* RECEIVE DATA FROM CLIENT */

                    int len = inputStream.read(receive, 0, receive.length);

//                    log.info("server, from client< {}, cnt: {}" , inetAddress, cnt);

                    for (i = 0, j = 0; i < len; i = i + int_byte_size, j++) {
                        if (j < data_num) {   // first 'monitor_nodes' points
                            recv_float_data[j] = bytes2float(receive, i);
                            recv_int_data[j] = bytes2Int(receive, i);
//                        System.out.println("recv_float_data: " + j + ", " + recv_float_data[j]);
//                           log.info("recv_int_data: {}, {}", j, recv_int_data[j]);
                        }
                    }

                    if (len < 0) {
                        break;
                    }

                    cnt += 1;

                    /* SEND DATA TO CLIENT */
//                printWriter.write("Server: " + msg);
//                printWriter.flush();

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
                        log.info("in recv_thread, client<{}> disconnected! current connected_num: {}" , inetAddress, SocketServer.socket_num);
                        end_time = new Date();
                        log.info("start_time: {}, get_time: {} ms. end_time: {}, get_time: {} ms. delta_time: {} s, cnt: {}.\n",
                                start_time, start_time.getTime(), end_time, end_time.getTime(),
                                String.format("%.3f", (end_time.getTime() - start_time.getTime()) / 1000.0), cnt);

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


}
