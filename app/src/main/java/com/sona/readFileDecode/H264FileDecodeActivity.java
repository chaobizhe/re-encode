package com.sona.readFileDecode;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sona.test.Counts;
import com.sona.test.R;
import com.sona.test.NCUtils;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class H264FileDecodeActivity extends AppCompatActivity {
    //SurfaceView
    private SurfaceView playSurface;
    private SurfaceHolder holder;
    //解码器
    public static Handler handler;
    private MediaCodecUtil codecUtil;
    //读取文件解码线程
    private ReadH264FileThread thread;
    //文件路径
    private String path = Environment.getExternalStorageDirectory().toString() + "/test.h264";
    Button but;
    TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_h264_file_decodec);
        but=(Button)findViewById(R.id.zhanshi);
        handler =new MyHandler2();
        textView=(TextView)findViewById(R.id.textView);
        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int c = Counts.getInstance().getCount();
                Toast.makeText(H264FileDecodeActivity.this,""+c,Toast.LENGTH_SHORT).show();
            }
        });
        initSurface();
    }
    private void initSurface() {
        playSurface = (SurfaceView) findViewById(R.id.play_surface);
        holder = playSurface.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d("lkb", "surface created");
                if (codecUtil == null) {
                    codecUtil = new MediaCodecUtil(holder);
                    Log.d("lkb", "start codec");
                    codecUtil.startCodec();
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d("lkb", "surface destroy");
                if (codecUtil != null) {
                    Log.d("lkb", "stop codec");
                    codecUtil.stopCodec();
                    codecUtil = null;
                }
            }
        });
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.play:
                if (thread == null) {
                    thread = new ReadH264FileThread(codecUtil, path);
                    thread.start();
                }
                break;
        }
    }
    public static void handle1(int a,int b)
    {
        Message message = Message.obtain();
        message.what=0;
        message.arg1 = a;
        message.arg2 = b;
        handler.sendMessage(message);
    }
    class MyHandler2 extends Handler {
        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            int a = msg.arg1;
            int b = msg.arg2;
            String name = "发送帧数：";
            String name1 = "发送包数：";
            switch (what) {
                case 0:
                    textView.setText(name + a + name1 + b);
                    break;
                default:
                    break;
            }
        }
    }
}
