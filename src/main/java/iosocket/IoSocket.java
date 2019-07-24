package iosocket;

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class IoSocket {

    @Test
    public void client()throws IOException {
        //1.添加发送的地址和端口号,获取通道
        SocketChannel sc = SocketChannel.open(new InetSocketAddress("127.0.0.1", 8081));
        //切换为非阻塞模式
        sc.configureBlocking(false);
        //2.分配指定大小的缓冲区
        ByteBuffer bf = ByteBuffer.allocate(1024);

        //3.从本地系统中读取文件到通道
        FileChannel ic = FileChannel.open(Paths.get("1.jpg"), StandardOpenOption.READ);

        //4.循环写入
        while ((ic.read(bf)) != -1){
            bf.flip();//切换为读模式
            sc.write(bf);
            bf.clear();
        }

        //5.轮询监控服务端的反馈
        //阻塞模式下，先要关闭输出，告诉服务端发送完毕
        sc.shutdownOutput();
        int len;
        while ( (len=sc.read(bf)) != -1){
            bf.flip();
            System.out.println(new String(bf.array(), 0, len));
            bf.clear();
        }
        ic.close();
        sc.close();
    }

    @Test
    public void server() throws IOException{

        //1.获取服务器通道
        ServerSocketChannel ssc = ServerSocketChannel.open();
        //指明目的地
        FileChannel oc = FileChannel.open(Paths.get("2.jpg"),StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        //2.绑定端口号
        ssc.bind(new InetSocketAddress(8081));
        //3.获取客户端的连接通多
        SocketChannel sc = ssc.accept();

        //4.新建缓冲数组存储数据
        ByteBuffer bf = ByteBuffer.allocate(1024);

        //5.循环接受，并保存到本地
        while ((sc.read(bf)) != -1){
            bf.flip();//切换为读模式，服务通道读取到缓冲区
            oc.write(bf);//文件通道写入文件
            bf.clear();
        }

        //6.发送处理成功的消息
        bf.put("接收到图片啦！".getBytes());
        bf.flip();
        sc.write(bf);

        //7.关闭通道
        sc.close();
        oc.close();
        ssc.close();
    }
}
