package com.huang.lochy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

public class DKCloudID {
    private final static String ip1 = "yjm1.dkcloudid.cn";
    private final static String ip2 = "www.dkcloudid.cn";
    private final static int port = 20006;
	public static final int PACKET_HEAD_LENGTH = 2;
    private Socket client;
    private OutputStream out;
    private InputStream in;
    private boolean closed = false;

    public DKCloudID (){
        //创建一个客户端socket
        client = new Socket();
        SocketAddress socketAddress = new InetSocketAddress(ip1, port);
        try {
            client.connect(socketAddress, 300);
        }catch (IOException e) {
            //连接服务器失败
            System.err.println("连接服务器失败：" + ip1 + ":" + port);
            e.printStackTrace();

            //连接到备用服务器
            client = new Socket();
            socketAddress = new InetSocketAddress(ip2, port);
            try {
                client.connect(socketAddress, 300);
            }catch (IOException e2) {
                Close();
                //连接备用服务器失败
                System.err.println("连接服务器失败：" + ip2 + ":" + port);
                e.printStackTrace();
                return;
            }
        }

        try {
            client.setTcpNoDelay(true);
            client.setSoTimeout(3000);

            //向服务器端传递信息
            out = client.getOutputStream();
            //获取服务器端传递的数据
            in = client.getInputStream();
        } catch (UnknownHostException e) {
            Close();
            e.printStackTrace();
        } catch (IOException e) {
            Close();
            e.printStackTrace();
        }
    }

    /**
     * 获取client连接的状态
     * @return true - 已经连接， false - 已经断开
     */
    public boolean isConnected() {
        return client.isConnected();
    }
    
    /**
     * 使用TCP与云解析服务器进行数据交换，同步阻塞方式，必须在子线程中运行
     * @param initData NB-IOT发过来的数据（不要包含长度）
     * @return 服务器返回的数据，如果如果返回数据的长度大于300，则可以用AESKEY进行解密得到数据。
     */
    public byte[] dkCloudTcpDataExchange(byte[] initData) {
            if ( (initData == null) || closed ) {
                return null;
            }

            //发送解析请求
            SendPacket(initData);
            //等待接收数据，一直循环到关闭连接
            byte[] bodyBuff = ReadPacket();
            return bodyBuff;
    }
    
    // send packet to Server
    private void SendPacket( byte[] res ) {
        byte[] headLen = UtilTool.shortToByte((short) res.length);
        byte[] body = UtilTool.mergeByte(headLen, res, 0, res.length);
        try {
            out.write(body);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // read tcp stream
    @SuppressWarnings("unused")
	private byte[] ReadPacket() {
        byte[] bodyBuff = new byte[0];
        byte[] headBuff = new byte[0];
    	
        while (true) {
            if (closed) {
                System.out.println("请求已被关闭");
                return null;
            }
            try {
                // packet head size
                if (headBuff.length < PACKET_HEAD_LENGTH) {
                    byte[] head = new byte[PACKET_HEAD_LENGTH - headBuff.length];
//                    if (in.available() <= 0) {
//                        Thread.sleep(1);
//                        continue;
//                    }
                    int couter = in.read(head);
                    if (couter < 0) {
                        continue;
                    }

                    headBuff = UtilTool.mergeByte(headBuff, head, 0, couter);
                }

                // packet body length
                short bodyLen = UtilTool.byteToShort(headBuff);

                if (bodyBuff.length < bodyLen) {
                    byte[] body = new byte[bodyLen - bodyBuff.length];
                    int couter = in.read(body);
                    if (couter < 0) {
                        continue;
                    }

                    bodyBuff = UtilTool.mergeByte(bodyBuff, body, 0, couter);
                    if (couter < body.length) {
                        continue;
                    }
                }
                
                return bodyBuff;
            } catch (Exception e) {
                e.printStackTrace();
                Close();
                return null;
            }
        }
    }
    
    // close the tcp connection
    public void Close() {
        this.closed = true;
        if (client != null) {
            try {
                client.close();

                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
