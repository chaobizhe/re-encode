package com.sona.readFileDecode;

import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.sona.test.NCUtils;
/**
 * Created by ZhangHao on 2017/5/5.
 * 读取H264文件送入解码器解码线程
 */
public class ReadH264FileThread extends Thread {
    //解码器
    private MediaCodecUtil util;
    //文件路径
    private String path;
    //文件读取完成标识
    private boolean isFinish = false;
    //这个值用于找到第一个帧头后，继续寻找第二个帧头，如果解码失败可以尝试缩小这个值
    private int FRAME_MIN_LEN = 1024;
    //一般H264帧大小不超过200k,如果解码失败可以尝试增大这个值
    private static int MAX_FRAME_BUF_LEN = 300 * 1024;
    //根据帧率获取的解码每帧需要休眠的时间,根据实际帧率进行操作
    private int PRE_FRAME_TIME = 1000 /15;
    /**
     * 初始化解码器
     *
     * @param util 解码Util
     * @param path 文件路径
     */
    public ReadH264FileThread(MediaCodecUtil util, String path) {
        this.util = util;
        this.path = path;
    }

    private int findHead(byte[] data, int start, int len)
    {
        Log.d("lkb", "findHead: start="+start+" len="+len);
        int offset = start;
        int end = offset + len;
        int found = -1;
//寻找头部 可以试下00 00 01而不是00 00 00 01
        while(offset + 3 < end){
            if (data[offset+0]==0x0 && data[offset+1]==0x0 && data[offset+2]==0x0&& data[offset+3]==0x1){
//            if (data[offset+0]==0x0 && data[offset+1]==0x0 && data[offset+2]==0x1){
                found = offset;
                break;
            }
            offset++;
        }
        return found;
    }

    @Override
    public void run() {
        super.run();
        Log.d("lkb", "run +");

        int frameDataLen; //frame buffer里已有数据偏移
        int head1,head2;
        int numRead;
        int head_id;
        int findOffset;
        boolean bNeedMoreData;
        //long dataOffsetInFile_debug; //for debug

        head1 = 0;
        head2 = 0;
        numRead = 0;
        head_id = 1; //首先寻找head1
        bNeedMoreData = true;
        frameDataLen = 0;
        findOffset = 0;
        //dataOffsetInFile_debug = 0;

        File file = new File(path);
        //判断文件是否存在
        if (file.exists()) {
            Log.d("lkb", "file exist"+file.getAbsolutePath());
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] frameBuffer = new byte[MAX_FRAME_BUF_LEN];
                byte[] fileBuffer = new byte[10 * 1024];
                long startTime = System.currentTimeMillis();
                while (true){
                    if (bNeedMoreData){
                        if (fileInputStream.available() > 0) {
                            numRead = fileInputStream.read(fileBuffer);
                            Log.d("lkb", "read data len=" + numRead);
                        }else {
                            Log.d("lkb", "file end");
                            break;
                        }
                        bNeedMoreData = false;
                    }
                    switch(head_id){
                        case 1:{
                            head1 = findHead(fileBuffer, 0, numRead);
                            if (head1 != -1){ //找到head1
                                head_id = 2; //寻找head2
                            }else { //找不到head1
                                bNeedMoreData = true; //再读文件
                                //dataOffsetInFile_debug += numRead;
                            }
                            break;
                        }
                        case 2:{
                            head2 = findHead(fileBuffer, head1+findOffset, numRead-head1-findOffset);
                            if (head2 != -1){ //找到head2
                                System.arraycopy(fileBuffer, head1, frameBuffer, frameDataLen, head2-head1);
                                frameDataLen += head2-head1;
                                onFrame(frameBuffer, 0, frameDataLen); //得到一帧
                                frameDataLen = 0;
                                head1 = head2; //将此次head2作为下次head1，再此寻找head2
                                findOffset = 4;
                                //线程休眠
                                sleepThread(startTime, System.currentTimeMillis());
                                //重置开始时间
                                startTime = System.currentTimeMillis();
                            }else { //只找到head1没有找到head2，保存此次数据到frameBuffer
                                System.arraycopy(fileBuffer, head1, frameBuffer, frameDataLen, numRead-head1);
                                frameDataLen += numRead-head1; //增加frame buffer 已有数据偏移
                                head1 = 0; //重新读文件后，寻找head2是从偏移0(head1+4)开始
                                findOffset = 0;
                                bNeedMoreData = true; //再读文件
                                //dataOffsetInFile_debug += numRead;
                            }
                            break;
                        }
                    }

                }//while(1)
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e("ReadH264FileThread", "File not found");
        }
        Log.d("lkb", "run -");
    }

    //视频解码
    private void onFrame(byte[] frame, int offset, int length) {
        Log.d("lkb", "onFrame "+frame[0]+" "+frame[1]+" "+frame[2]+" "+frame[3]);
        if (util != null) {
            try {
//                util.onFrame(frame, offset, length);
//                util.h264ToRtp(frame, length);
                  util.SendThread(frame,length);
//                h264len=length;
//                System.arraycopy(frame, offset, h264, 0, length);
//                singleThreadExecutor.execute(task);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e("MediaCodecRunnable", "mediaCodecUtil is NULL");
        }
    }

    //修眠
    private void sleepThread(long startTime, long endTime) {
        //根据读文件和解码耗时，计算需要休眠的时间
        long s=System.currentTimeMillis() - startTime;
        long time = PRE_FRAME_TIME - (endTime - startTime);
        if (time > 0) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //手动终止读取文件，结束线程
    public void stopThread() {
        isFinish = true;
    }

}
