package com.wilddog.video;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.wilddog.client.AuthData;
import com.wilddog.client.Wilddog;
import com.wilddog.client.WilddogError;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btn_login_anonymously;
    private EditText et_app_id;
    private TextView tv_prompt;

    private String mAppId;
    private Wilddog mRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        et_app_id = (EditText) findViewById(R.id.et_app_id);
        tv_prompt = (TextView) findViewById(R.id.tv_prompt);

        btn_login_anonymously = (Button) findViewById(R.id.btn_login_anonymously);
        btn_login_anonymously.setOnClickListener(this);


    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btn_login_anonymously:
                mAppId = et_app_id.getText().toString();
                if (TextUtils.isEmpty(mAppId)) {
                    Toast.makeText(MainActivity.this, "请输入你的AppId", Toast.LENGTH_SHORT).show();
                    et_app_id.setText("");
                    return;
                }

                mRef = new Wilddog("http://" + mAppId + ".wilddogio.com");


                mRef.authAnonymously(new Wilddog.AuthResultHandler() {
                    @Override
                    public void onAuthenticated(AuthData authData) {
                        if (authData != null) {
                            tv_prompt.setVisibility(View.INVISIBLE);
                            String uid = authData.getUid();
                            Log.e("Login", "authWithPassword uid ::" + uid);
                            Map<String, Object> map = new HashMap<String, Object>();
                            map.put(uid, true);
                            mRef.child("users").updateChildren(map);
                            mRef.child("users/" + uid).onDisconnect().removeValue();
                            if (!TextUtils.isEmpty(uid)) {
                                Intent intent = new Intent(getApplicationContext(), ConversationActivity.class);
                                intent.putExtra("app_id", mAppId);
                                startActivity(intent);
                            }
                        }
                    }

                    @Override
                    public void onAuthenticationError(WilddogError wilddogError) {
                        if (wilddogError != null) {
                            if (wilddogError.getCode() == WilddogError.AUTHENTICATION_PROVIDER_DISABLED) {
                                tv_prompt.setVisibility(View.VISIBLE);
                            }
                        }

                    }
                });

                break;
        }
    }
}
