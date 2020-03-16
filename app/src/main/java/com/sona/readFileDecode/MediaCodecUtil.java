package com.sona.readFileDecode;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import com.sona.test.Counts;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.sona.test.NCUtils;

import static java.lang.Thread.sleep;

/**
 * 用于硬件解码(MediaCodec)H264的工具类
 */
public class MediaCodecUtil {

    private String TAG = "MediaCodecUtil";
    //解码后显示的surface
    private SurfaceHolder holder;
    private int width, height;
    //解码器
    private MediaCodec mCodec;
    private boolean isFirst = true;
    long starttime,sumt=0;
    //解码器序号
    // 需要解码的类型
    private final static String MIME_TYPE = "video/avc"; // H.264 Advanced Video
    private final static int HEAD_OFFSET = 512;
    private byte[] sendbuf = new byte[1500];
    private int seq_num = 0;
    private int timestamp_increse = (int) (90000.0 / 20);
    private int bytes = 0;
    private int packageSize = 1400;
    private int ts_current = 0;
    private DatagramSocket mSocket;
    private String ipa="192.168.43.255";
    private InetAddress mInetAddress;
    private int mPort=5004;
    private byte[] h264=new byte[15000];
    private  int h264len = 0;
    private int countp=0;
    private int countz=0;
    private int n=0;
    private int pk=0;
    private int redundancek=0;
    private byte[] res;
    private byte[] result=new byte[60000];
//    private String path = Environment.getExternalStorageDirectory().toString() + "/log1.txt";
//    File file;
//    FileOutputStream outStream;
    ExecutorService fixThreadPool =Executors.newFixedThreadPool(35);
    ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
    ExecutorService cachethreadpool =Executors.newCachedThreadPool();
    Runnable task =new Runnable(){
        public void run(){
//            NCUtils.encode(res,pk,1400,result);
            try {
                h264ToRtp(result,h264len);
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    };
    Runnable task1 =new Runnable(){
        public void run(){
            try {
                h264ToRtp(result,h264len);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    /**
     * 初始化解码器
     *
     * @param holder 用于显示视频的surface
     * @param width  surface宽
     * @param height surface高
     *
     **/
    public MediaCodecUtil(SurfaceHolder holder, int width, int height) {
        this.holder = holder;
        this.width = width;
        this.height = height;
    }
    public MediaCodecUtil(SurfaceHolder holder) {
        this(holder, holder.getSurfaceFrame().width(), holder.getSurfaceFrame().height());
    }
    public void startCodec() {
        if (isFirst) {
            //第一次打开则初始化解码器
            initDecoder();
        }
    }
    private void initDecoder() {
        try {
            mInetAddress = InetAddress.getByName(ipa);
            mSocket = new DatagramSocket();
            mSocket.setBroadcast(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    int mCount = 0;
    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

    public boolean onFrame(byte[] buf, int offset, int length) {

        Log.d("lkb", "util onFrame: length="+ length);
        long startDecodeTime = System.currentTimeMillis();
        //获取输入buffer
        Log.d("lkb", "queue Input Buffer +");
        int inputBufferId = mCodec.dequeueInputBuffer(-1);
        Log.d("lkb", "queue Input Buffer - id="+inputBufferId);
        if (inputBufferId >= 0){
            ByteBuffer inputBuffer = mCodec.getInputBuffer(inputBufferId);
            //inputBuffer.clear();
            inputBuffer.put(buf, offset, length);
            Log.d("lkb", "> inputBuffer");
            //mCodec.queueInputBuffer(inputBufferId, 0, length, mCount * TIME_INTERNAL, 0);
            mCodec.queueInputBuffer(inputBufferId, 0, length, System.currentTimeMillis(), 0);
            //mCodec.queueInputBuffer(inputBufferId, 0, length, 0, 0); //lkb???
            mCount++;
        }else {
            Log.d("lkb", "no avai input buffer");
        }
        Log.d("lkb", "dequeue Output Buffer +");
        while(true) {
            int outputBufferId = mCodec.dequeueOutputBuffer(bufferInfo, 0); //-1);
            Log.d("lkb", "dequeue Output Buffer - id=" + outputBufferId);
            if (outputBufferId >= 0) {
                Log.d("lkb", "> surfaceView");
                ByteBuffer outputBuffer = mCodec.getOutputBuffer(outputBufferId);
                MediaFormat bufferFormat = mCodec.getOutputFormat(outputBufferId); //bufferFormat is identical to outputFormat
                int videoWidth = bufferFormat.getInteger("width");
                int videoHeight = bufferFormat.getInteger("height");
                Log.d("lkb", "decoded video: " + videoWidth + " x " + videoHeight);
                mCodec.releaseOutputBuffer(outputBufferId, true); //true : 将解码的数据显示到surface上
            } else {
                Log.d("lkb", "no avai output buffer");
                break;
            }
        }
        return true;
    }

    public void stopCodec() {
        try {
            mCodec.stop();
            mCodec.release();
            mCodec = null;
            isFirst = true;
        } catch (Exception e) {
            e.printStackTrace();
            mCodec = null;
        }
    }
    //发送端 主要是控制发送帧的频率 发送的间隔 以及将广播放在线程里面进行 使他们能够按照顺序进行
    //测试下更多手机的时候成功解码率会不会上升
    public  void SendThread(byte[] data,int length)
    {
        //redundancek 为冗余数值
//        h264=data;//*****这里是传过来的一帧数据  应该是在这里进行分片 编码然后传输过去
//        h264len=length;
        if(length%1400==0)
        {pk=length/1400;}
        else
        {
            pk=length/1400+1;
        }
        res=new byte[pk*1400];
        packageSize=1400+pk+1;
        h264len=(pk+redundancek)*(1400+pk+1);
        System.arraycopy(data,0,res,0,length);
//        System.arraycopy(data,0,res,0,length);
        NCUtils.encode(res,pk,1400,result,redundancek);
        countz++;
        H264FileDecodeActivity.handle1(countz,countp);
        try {
            h264ToRtp(result,h264len);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        fixThreadPool.execute(task);
//        cachethreadpool.execute(task);
//        singleThreadExecutor.execute(task);
//        try {
//            starttime=System.currentTimeMillis();
//            h264ToRtp(result,h264len);
//            sumt+=System.currentTimeMillis()-starttime;
//            Log.e("编码时间是", sumt+"ms");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
    public void h264ToRtp(byte[] r, int h264len) throws Exception {
//      memset(sendbuf, 0, 1500);
        sendbuf[1] = (byte) (sendbuf[1] | 96); // 负载类型号96,其值为：01100000
        sendbuf[0] = (byte) (sendbuf[0] | 0x80); // 版本号,此版本固定为2
        sendbuf[1] = (byte) (sendbuf[1] & 254); //标志位，由具体协议规定其值，其值为：01100000
        sendbuf[11] = 10;//随机指定10，并在本RTP回话中全局唯一,java默认采用网络字节序号 不用转换（同源标识符的最后一个字节）
        if (h264len <= packageSize) {
            sendbuf[1] = (byte) (sendbuf[1] | 0x80); // 设置rtp M位为1，其值为：11100000，分包的最后一片，M位（第一位）为0，后7位是十进制的96，表示负载类型
            sendbuf[3] = (byte) seq_num++;
            System.arraycopy(intToByte(seq_num++), 0, sendbuf, 2, 2);//send[2]和send[3]为序列号，共两位
            {
                // java默认的网络字节序是大端字节序（无论在什么平台上），因为windows为小字节序，所以必须倒序
                /**参考：
                 * http://blog.csdn.net/u011068702/article/details/51857557
                 * http://cpjsjxy.iteye.com/blog/1591261
                 */
                byte temp = 0;
                temp = sendbuf[3];
                sendbuf[3] = sendbuf[2];
                sendbuf[2] = temp;
            }
            // FU-A HEADER, 并将这个HEADER填入sendbuf[12]
            sendbuf[12] = (byte) (sendbuf[12] | ((byte) (r[0] & 0x80)) << 7);
            sendbuf[12] = (byte) (sendbuf[12] | ((byte) ((r[0] & 0x60) >> 5)) << 5);
            sendbuf[12] = (byte) (sendbuf[12] | ((byte) (r[0] & 0x1f)));
            // 同理将sendbuf[13]赋给nalu_payload
            //NALU头已经写到sendbuf[12]中，接下来则存放的是NAL的第一个字节之后的数据。所以从r的第二个字节开始复制
            System.arraycopy(r, 1, sendbuf, 13, h264len - 1);
            ts_current = ts_current + timestamp_increse;
            System.arraycopy(intToByte(ts_current), 0, sendbuf, 4, 4);//序列号接下来是时间戳，4个字节，存储后也需要倒序
            {
                byte temp = 0;
                temp = sendbuf[4];
                sendbuf[4] = sendbuf[7];
                sendbuf[7] = temp;
                temp = sendbuf[5];
                sendbuf[5] = sendbuf[6];
                sendbuf[6] = temp;
            }
            bytes = h264len + 12;//获sendbuf的长度,为nalu的长度(包含nalu头但取出起始前缀,加上rtp_header固定长度12个字节)
            countp++;
            sendPacket(sendbuf,0,bytes);
//          sendPacket2(sendbuf,0,bytes);
        } else if (h264len > packageSize) {
            int k = 0, l = 0;
            k = h264len / packageSize;
            l = h264len % packageSize;
            int t = 0;
            ts_current = ts_current + timestamp_increse;
            System.arraycopy(intToByte(ts_current), 0, sendbuf, 4, 4);//时间戳，并且倒序
            {
                byte temp = 0;
                temp = sendbuf[4];
                sendbuf[4] = sendbuf[7];
                sendbuf[7] = temp;
                temp = sendbuf[5];
                sendbuf[5] = sendbuf[6];
                sendbuf[6] = temp;
            }
            while (t <= k) {
                System.arraycopy(intToByte(seq_num++), 0, sendbuf, 2, 2);//序列号，并且倒序
                {
                    byte temp = 0;
                    temp = sendbuf[3];
                    sendbuf[3] = sendbuf[2];
                    sendbuf[2] = temp;
                }
                if (t == 0) {//分包的第一片
                    sendbuf[1] = (byte) (sendbuf[1] & 0x7F);//其值为：01100000，不是最后一片，M位（第一位）设为0
                    //FU indicator，一个字节，紧接在RTP header之后，包括F,NRI，header
                    sendbuf[12] = (byte) (sendbuf[12] | ((byte) (r[0] & 0x80)) << 7);//禁止位，为0
                    sendbuf[12] = (byte) (sendbuf[12] | ((byte) ((r[0] & 0x60) >> 5)) << 5);//NRI，表示包的重要性
                    sendbuf[12] = (byte) (sendbuf[12] | (byte) (28));//TYPE，表示此FU-A包为什么类型，一般此处为28
                    //FU header，一个字节，S,E，R，TYPE
                    sendbuf[13] = (byte) (sendbuf[13] & 0xBF);//E=0，表示是否为最后一个包，是则为1
                    sendbuf[13] = (byte) (sendbuf[13] & 0xDF);//R=0，保留位，必须设置为0
                    sendbuf[13] = (byte) (sendbuf[13] | 0x80);//S=1，表示是否为第一个包，是则为1
                    sendbuf[13] = (byte) (sendbuf[13] | ((byte) (r[0] & 0x1f)));//TYPE，即NALU头对应的TYPE
                    //将除去NALU头剩下的NALU数据写入sendbuf的第14个字节之后。前14个字节包括：12字节的RTP Header，FU indicator，FU header
//                    System.arraycopy(r, 1, sendbuf, 14, packageSize);
                    n++;
                    n=n%100;
                    sendbuf[14]=(byte)n;
                    System.arraycopy(r, 0, sendbuf, 15, packageSize);
                    sendPacket(sendbuf,0,packageSize+15);
                    t++;
                    countp++;
                } else if (t == k) {//分片的最后一片
                    sendbuf[1] = (byte) (sendbuf[1] | 0x80);

                    sendbuf[12] = (byte) (sendbuf[12] | ((byte) (r[0] & 0x80)) << 7);
                    sendbuf[12] = (byte) (sendbuf[12] | ((byte) ((r[0] & 0x60) >> 5)) << 5);
                    sendbuf[12] = (byte) (sendbuf[12] | (byte) (28));

                    sendbuf[13] = (byte) (sendbuf[13] & 0xDF); //R=0，保留位必须设为0
                    sendbuf[13] = (byte) (sendbuf[13] & 0x7F); //S=0，不是第一个包
                    sendbuf[13] = (byte) (sendbuf[13] | 0x40); //E=1，是最后一个包
                    sendbuf[13] = (byte) (sendbuf[13] | ((byte) (r[0] & 0x1f)));//NALU头对应的type

                    if (0 != l) {//如果不能整除，则有剩下的包，执行此代码。如果包大小恰好是1400的倍数，不执行此代码。
//                        System.arraycopy(r, t * packageSize + 1, sendbuf, 14, l - 1);//l-1，不包含NALU头
                        sendbuf[14]=(byte)n;
                        System.arraycopy(r, t * packageSize, sendbuf, 15, l - 1);//l-1，不包含NALU头
                        bytes = l - 1 + 15; //bytes=l-1+14;
                        sendPacket(sendbuf,0,bytes);
                    }
                    t++;
                    countp++;
                } else if (t < k && 0 != t) {//既不是第一片，又不是最后一片的包
                    sendbuf[1] = (byte) (sendbuf[1] & 0x7F); //M=0，其值为：01100000，不是最后一片，M位（第一位）设为0.
                    sendbuf[12] = (byte) (sendbuf[12] | ((byte) (r[0] & 0x80)) << 7);
                    sendbuf[12] = (byte) (sendbuf[12] | ((byte)((r[0] & 0x60) >> 5)) << 5);
                    sendbuf[12] = (byte) (sendbuf[12] | (byte) (28));
                    sendbuf[13] = (byte) (sendbuf[13] & 0xDF); //R=0，保留位必须设为0
                    sendbuf[13] = (byte) (sendbuf[13] & 0x7F); //S=0，不是第一个包
                    sendbuf[13] = (byte) (sendbuf[13] & 0xBF); //E=0，不是最后一个包
                    sendbuf[13] = (byte) (sendbuf[13] | ((byte) (r[0] & 0x1f)));//NALU头对应的type
//                    System.arraycopy(r, t * packageSize + 1, sendbuf, 14, packageSize);//不包含NALU头
                    sendbuf[14]=(byte)n;
                    System.arraycopy(r, t * packageSize, sendbuf, 15, packageSize);//不包含NALU头
                    sendPacket(sendbuf,0,packageSize+15);
                    t++;
                    countp++;
                }
            }
        }
    }
    public static byte[] intToByte(int number) {
        int temp = number;
        byte[] b = new byte[4];
        for (int i = 0; i < b.length; i++) {
            b[i] = new Integer(temp & 0xff).byteValue();// 将最低位保存在最低位
            temp = temp >> 8; // 向右移8位
        }
        return b;
    }
    // 清空buf的值
    public static void memset(byte[] buf, int value, int size) {
        for (int i = 0; i < size; i++) {
            buf[i] = (byte) value;
        }
    }
    public void sendPacket(final byte[] data,final int offset, final int size) {
        try{
            DatagramPacket p;
            p = new DatagramPacket(data, offset, size, mInetAddress, mPort);
            mSocket.send(p);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
