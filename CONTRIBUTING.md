此APP用作测试ONOS集群的吞吐量。
方法：
1.将ONOS切换到pof-virtualization分支，并且ONOS_APPS="drivers,pof-base"
2.在跑有ONOS实例的本地服务器上安装pof-cbench，并且输入该指令发packetin包：pof-cbench -c 192.168.109.211 -p 6643 -m 1000 -l 18 -s 100 -M 100000 -w 3 -D 1000 -t -x 5000（假
如该实例跑在211上）
3.7个cbench同时发包，然后将所有cbench显示的平均值加起来即为集群的吞吐量