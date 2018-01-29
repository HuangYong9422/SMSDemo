package com.example.a10553.mysms;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;

public class LoginActivity extends Activity {

   static String TAG = "LoginActivity";

    @BindView(R.id.etPhone)
    EditText etPhone;
    @BindView(R.id.etCode)
    EditText etCode;
    @BindView(R.id.btGet)
    Button btGet;
    @BindView(R.id.btLogin)
    Button btLogin;

    EventHandler eventHandler;
    String strPhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        SMSSDK.initSDK(this, "23c67a3515831", "dd64d801205b2cd417df689c37dbc74d");

        eventHandler = new EventHandler() {
            /**
             *在操作之后被触发
             @param
             event 参数1
             *
             @param
             result 参数2
             SMSSDK.RESULT_COMPLETE表示操作成功，为SMSSDK.RESULT_ERROR表示操作失败
             *
             @param
             data 事件操作的结果
             */
            @Override
            public void afterEvent(int event, int result, Object data) {
                Message message = myHandler.obtainMessage(0x00);
                message.arg1 = event;
                message.arg2 = result;
                message.obj = data;
                myHandler.sendMessage(message);
            }
        };

        SMSSDK.registerEventHandler(eventHandler);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SMSSDK.unregisterEventHandler(eventHandler);
    }

    @OnClick({R.id.btGet, R.id.btLogin})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btGet:
                strPhoneNumber = etPhone.getText().toString();
                if (null == strPhoneNumber || "".equals(strPhoneNumber) || strPhoneNumber.length() != 11) {
                    Toast.makeText(this, "电话号码输入有误", Toast.LENGTH_SHORT).show();
                    return;
                }
                SMSSDK.getVerificationCode("86", strPhoneNumber);
                btGet.setClickable(false);
                //开启线程去更新button的text
                new Thread() {
                    @Override
                    public void run() {
                        int totalTime = 60;
                        for (int i = 0; i < totalTime; i++) {
                            Message message = myHandler.obtainMessage(0x01);
                            message.arg1 = totalTime - i;
                            myHandler.sendMessage(message);
                            try {
                                sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        myHandler.sendEmptyMessage(0x02);
                    }
                }.start();
                break;
            case R.id.btLogin:
                String strCode = etCode.getText().toString();
                if (null != strCode && strCode.length() == 4) {
                    Log.d(TAG, etCode.getText().toString());
                    SMSSDK.submitVerificationCode("86", strPhoneNumber, etCode.getText().toString());
                } else {
                    Toast.makeText(this, "密码长度不正确", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public  Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0x00:
                    int event = msg.arg1;
                    int result = msg.arg2;
                    Object data = msg.obj;
                    Log.e(TAG, "result : " + result + ", event: " + event + ", data : " + data);
                    if (result == SMSSDK.RESULT_COMPLETE) { //回调  当返回的结果是complete
                        if (event == SMSSDK.EVENT_GET_VERIFICATION_CODE) { //获取验证码
                            Toast.makeText(LoginActivity.this, "发送验证码成功", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "get verification code successful.");
                        } else if (event == SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE) { //提交验证码
                            Log.d(TAG, "submit code successful");
                            Toast.makeText(LoginActivity.this, "提交验证码成功", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                        } else {
                            Log.d(TAG, data.toString());
                        }
                    } else { //进行操作出错，通过下面的信息区分析错误原因
                        try {
                            Throwable throwable = (Throwable) data;
                            throwable.printStackTrace();
                            JSONObject object = new JSONObject(throwable.getMessage());
                            String des = object.optString("detail");//错误描述
                            int status = object.optInt("status");//错误代码
                            //错误代码：  http://wiki.mob.com/android-api-%E9%94%99%E8%AF%AF%E7%A0%81%E5%8F%82%E8%80%83/
                            Log.e(TAG, "status: " + status + ", detail: " + des);
                            if (status > 0 && !TextUtils.isEmpty(des)) {
                                Toast.makeText(LoginActivity.this, des, Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case 0x01:
                    btGet.setText("重新发送(" + msg.arg1 + ")");
                    break;
                case 0x02:
                    btGet.setText("获取验证码");
                    btGet.setClickable(true);
                    break;
            }
        }
    };
}
