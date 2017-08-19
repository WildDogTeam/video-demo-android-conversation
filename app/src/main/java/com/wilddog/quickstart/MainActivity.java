package com.wilddog.quickstart;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
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

    private static final int REQUEST_CODE = 0; // 请求码
    private WilddogAuth auth;
    private boolean isInlogin =false;
    // 所需的全部权限
    static final String[] PERMISSIONS = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        //动态申请权限
        int sdk=android.os.Build.VERSION.SDK_INT;
        if (sdk>=23){
            Intent intent=new Intent(this,PermissionActivity.class);

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Bundle bundle=new Bundle();
            bundle.putStringArray("permission",PERMISSIONS);
            PermissionActivity.startActivityForResult(this,REQUEST_CODE,PERMISSIONS);
        }
    }

    @OnClick(R.id.btn_login_anonymously)
    public void login() {
        if(isInlogin){return;}
        isInlogin = true;
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
                        startActivity(intent);
                        isInlogin = false;
                        finish();
                    }
                } else {
                    //处理失败
                    //throw new RuntimeException("auth 失败" + task.getException().getMessage());
                    Log.e("error",task.getException().getMessage());
                    Toast.makeText(MainActivity.this,"登录失败!",Toast.LENGTH_SHORT).show();
                    isInlogin = false;
                }
            }
        });
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 拒绝时, 关闭页面, 缺少主要权限, 无法运行
        if (requestCode == REQUEST_CODE && resultCode == PermissionHelper.PERMISSIONS_DENIED) {
            finish();
        }else {

        }
    }

}
