package com.sona.test;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.sona.readFileDecode.H264FileDecodeActivity;

public class MainActivity extends AppCompatActivity {

//    static {
//        System.loadLibrary("native-lib");
//    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView tv =(TextView)findViewById(R.id.text);
//        tv.setText(stringFromJNI());
//        byte[] a ={1,3,5,7,9,11,13,15,17};
//        byte[] b ={2,4,6,8};
//        byte[] c =new byte[4];
//        byte[] result=new byte[4];
//        byte[] randomMatrix = new byte[4];
//        Random random = new Random();
//        random.nextBytes(randomMatrix);
//        NCUtils.Multiply2(randomMatrix,2,2,b,2,2,result);
//        int i;
//        for(i=0;i<4;i++) {
//            Log.e("cc",(int)result[i]+"");
//        }
//        c=NCUtils.InverseMatrix(randomMatrix,2);
//        NCUtils.Multiply2(c,2,2,result,2,2,result);
//        for(i=0;i<4;i++) {
//            Log.e("cc",(int)result[i]+"");
//        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play_video:
                Intent i = new Intent(this, H264FileDecodeActivity.class);
                startActivity(i);
                break;
        }
    }
}
