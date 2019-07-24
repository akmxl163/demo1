package iosocket;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

@Slf4j
public class NioSocket {

    @Test
    public void client() throws IOException {
        //1.添加发送的地址和端口号,获取通道
        SocketChannel sc = SocketChannel.open(new InetSocketAddress("127.0.0.1", 8081));
        //切换为非阻塞模式
        sc.configureBlocking(false);
        //2.分配指定大小的缓冲区
        ByteBuffer bf = ByteBuffer.allocate(50);

        int offset=0,len=0,capacity=bf.capacity();
        int head=0;//头序号，相同的头号，代表相同的一次数据
        while (true) {
            //3.获取输入信息
            Scanner sn = new Scanner(System.in);
            if(!sn.hasNext()) continue;
            String inStr = sn.next();
            if("exit".equals(inStr)) break;//退出标记

            head++;
            //4.写入消息
            String msg = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toString() + "\n" + inStr;
            int length = msg.length();

            //写入头信息
            bf.put((head + "@").getBytes());
            bf.flip();//切换为读模式
            sc.write(bf);
            bf.clear();

            //循环写入缓冲区
            while (offset < length){
                if (length-offset >= capacity) len = capacity;//剩余读取内容大于缓冲区最大容量，则读满缓冲区
                else len = length-offset;
                bf.put(msg.getBytes(), offset, len);
                bf.flip();//切换为读模式
                sc.write(bf);
                bf.clear();
                offset += len;
            }

            //写入尾信息
            bf.put(("#" + head).getBytes());
            bf.flip();//切换为读模式
            sc.write(bf);
            bf.clear();

            sn.close();
        }
        sc.close();
    }

    @Test
    public void server() throws IOException{
        //1.获取服务器通道
        ServerSocketChannel ssc = ServerSocketChannel.open();
        //2.切换为非阻塞模式
        ssc.configureBlocking(false);
        //3.绑定端口号
        ssc.bind(new InetSocketAddress(8081));

        //4.获取Selector
        Selector selector = Selector.open();

        //5.注册到选择器中，设置监听事件为ACCEPT
        ssc.register(selector, SelectionKey.OP_ACCEPT);

        String head="",tail="";//头尾标记，相同则代表是同一个消息
        //不同的头信息作为key，消息作为value,value还是消息累加变量
        Map<String, String> map = new HashMap<>();
        //6.阻塞轮询选择器上就绪的事件
        while (selector.select() > 0){ //大于0代表有准备就绪的事件了
            //7.获取所有选择键，事件keys
            Iterator<SelectionKey> isk = selector.selectedKeys().iterator();
            while (isk.hasNext()){
                //8.获取准备就的事件
                SelectionKey sk = isk.next();
                //9.判断是什么事件，并做处理
                if(sk.isAcceptable()){//是接收事件
                    SocketChannel sc = ssc.accept();
                    //切换非阻塞
                    sc.configureBlocking(false);
                    //10.注册到选择器,使用读事件，在读事件判断中处理
                    sc.register(selector, SelectionKey.OP_READ);
                } else if(sk.isReadable()){
                    log.info("begin read.");
                    ByteBuffer bf = ByteBuffer.allocate(50);
                    //11.从当前选择器上获取“读就绪”的通道
                    SocketChannel sc = (SocketChannel) sk.channel();

                    String newMsg = "";
                    int len;
                    if ((len = sc.read(bf)) != -1){
                        bf.flip();//切换为读模式，服务通道读取到缓冲区
                        newMsg = new String(bf.array(), 0, len);
                        bf.clear();
                    }
                    log.info("read value:" + newMsg);

                    if(newMsg.endsWith("@") ){
                        //头为空则写入头，并清空尾
                        if("".equals(head)){
                            head = newMsg.substring(0, newMsg.length()-1);
                            tail = "";
                            log.info("get head:" + head);
                            map.put(head, "");
                        }
                    }else if(newMsg.startsWith("#")) {//尾处理
                        tail = newMsg.substring(1);
                        log.info("MESSAGE OUTPUT ====>" + map.get(tail));
                        head = tail = "";//清空
                    } else{//不是头尾，则消息体累加
                        map.put(head, map.get(head)+newMsg);
                    }
//                    sc.close();//不关闭而是保持连接
                }
                //12事件处理完毕后，取消当前选择键，让选择器回复初始状态
                isk.remove();
            }
        }

        // 13.不关闭通道，一直接收
        // -- 13.关闭通道
//        ssc.close();
    }

    @Test
    public void dateTest(){
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println(dateStr);
    }
}
