package com.wilddog.quickstart;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.wilddog.client.SyncReference;
import com.wilddog.client.WilddogSync;
import com.wilddog.wilddogauth.WilddogAuth;
import com.wilddog.wilddogauth.core.Task;
import com.wilddog.wilddogauth.core.listener.OnCompleteListener;
import com.wilddog.wilddogauth.core.result.AuthResult;
import com.wilddog.wilddogcore.WilddogApp;
import com.wilddog.wilddogcore.WilddogOptions;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.et_app_id)
    EditText etAppId;
    @BindView(R.id.tv_prompt)
    TextView tvPrompt;

    private String mAppId;
    private WilddogAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
    }

    @OnClick(R.id.btn_login_anonymously)
    public void login() {
        mAppId = etAppId.getText().toString();
        if (TextUtils.isEmpty(mAppId)) {
            Toast.makeText(MainActivity.this, "请输入你的AppId", Toast.LENGTH_SHORT).show();
            etAppId.setText("");
            return;
        }
        //初始化WilddogApp,完成初始化之后可在项目任意位置通过getInstance()获取Sync & Auth对象
        WilddogOptions.Builder builder = new WilddogOptions.Builder().setSyncUrl("https://" + mAppId + ".wilddogio" +
                ".com");
        WilddogOptions options = builder.build();
        WilddogApp.initializeApp(getApplicationContext(), options);
        //获取Sync & Auth 对象
        auth = WilddogAuth.getInstance();
        //采用匿名登录方式认证
        //还可以选择其他登录方式
        //auth.signInWithEmailAndPassword();
        //auth.signInWithCredential();
        //auth.signInWithCustomToken();
        auth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    //身份认证成功
                    tvPrompt.setVisibility(View.INVISIBLE);
                    String uid = auth.getCurrentUser().getUid();
                    //用户可以使用任意自定义节点来保存用户数据，但是不要使用 [wilddogVideo]节点存放私有数据
                    //以防和Video SDK 数据发生冲突
                    //本示例采用根节点下的[users] 节点作为用户列表存储节点
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put(uid, true);
                    SyncReference userRef=WilddogSync.getInstance().getReference("users");
                    userRef.updateChildren(map);
                    userRef.child(uid).onDisconnect().removeValue();
                    if (!TextUtils.isEmpty(uid)) {
                        Intent intent = new Intent(getApplicationContext(), ConversationActivity.class);
                        intent.putExtra("app_id", mAppId);
                        startActivity(intent);
                    }
                } else {
                    //处理失败
                    //throw new RuntimeException("auth 失败" + task.getException().getMessage());
                }
            }
        });
    }


}
