package com.wilddog.quickstart;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.wilddog.client.SyncReference;
import com.wilddog.client.WilddogSync;
import com.wilddog.video.Conversation;
import com.wilddog.video.IncomingInvite;
import com.wilddog.video.LocalStream;
import com.wilddog.video.OutgoingInvite;
import com.wilddog.video.Participant;
import com.wilddog.video.RemoteStream;
import com.wilddog.video.WilddogVideo;
import com.wilddog.video.WilddogVideoClient;
import com.wilddog.video.WilddogVideoView;
import com.wilddog.video.WilddogVideoViewLayout;
import com.wilddog.video.bean.ConnectOptions;
import com.wilddog.video.bean.LocalStreamOptions;
import com.wilddog.video.bean.VideoException;
import com.wilddog.video.listener.CompleteListener;
import com.wilddog.video.listener.ConversationCallback;
import com.wilddog.wilddogauth.WilddogAuth;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class ConversationActivity extends AppCompatActivity {

    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 0;
    private static final int LOCAL_Y_CONNECTED = 0;
    private static final int LOCAL_WIDTH_CONNECTED = 50;
    private static final int LOCAL_HEIGHT_CONNECTED = 100;
    // Remote video screen position
    private static final int REMOTE_X = 50;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 50;
    private static final int REMOTE_HEIGHT = 100;

    @BindView(R.id.btn_invite)
    Button btnInvite;

    @BindView(R.id.tv_uid)
    TextView tvUid;

    @BindView(R.id.local_video_layout)
    WilddogVideoViewLayout localViewLayout;

    @BindView(R.id.remote_video_layout)
    WilddogVideoViewLayout remoteViewLayout;

    @BindView(R.id.local_video_view)
    WilddogVideoView localView;

    @BindView(R.id.remote_video_view)
    WilddogVideoView remoteView;

    private WilddogVideoClient client;
    private WilddogVideo video;
    private LocalStream localStream;
    private Conversation mConversation;
    private OutgoingInvite outgoingInvite;
    //AlertDialog列表
    private Map<IncomingInvite, AlertDialog> incomingDialogMap;

    private WilddogVideoClient.Listener inviteListener = new WilddogVideoClient.Listener() {
        @Override
        public void onIncomingInvite(WilddogVideoClient wilddogVideoClient, final IncomingInvite incomingInvite) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ConversationActivity.this);
            builder.setMessage("邀请你加入会话");
            builder.setTitle("加入邀请");
            builder.setNegativeButton("拒绝邀请", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    incomingInvite.reject();
                }
            });
            builder.setPositiveButton("确认加入", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    incomingDialogMap.remove(incomingInvite);
                    ConnectOptions connectOptions = new ConnectOptions(localStream, "");
                    incomingInvite.accept(connectOptions, new ConversationCallback() {
                        @Override
                        public void onConversation(Conversation conversation, VideoException exception) {
                            //对方接受邀请并成功建立会话，conversation不为空，exception为空
                            if (conversation != null) {
                                mConversation = conversation;
                                //获取到conversation后，设置ConversationListener
                                mConversation.setConversationListener(conversationListener);

                            } else {
                                //处理会话建立失败逻辑
                            }

                        }
                    });

                }
            });

            AlertDialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
            incomingDialogMap.put(incomingInvite, alertDialog);
        }

        @Override
        public void onIncomingInviteCanceled(WilddogVideoClient wilddogVideoClient, IncomingInvite incomingInvite) {
            AlertDialog alertDialog = incomingDialogMap.get(incomingInvite);
            alertDialog.dismiss();
            alertDialog = null;
            incomingDialogMap.remove(incomingInvite);
        }


    };

    Conversation.Listener conversationListener = new Conversation.Listener() {
        @Override
        public void onConnected(Conversation conversation) {

        }

        @Override
        public void onConnectFailed(Conversation conversation, VideoException e) {

        }

        @Override
        public void onDisconnected(Conversation conversation, VideoException e) {

        }

        @Override
        public void onParticipantConnected(Conversation conversation, Participant participant) {
            outgoingInvite=null;

            participant.setListener(new Participant.Listener() {
                @Override
                public void onStreamAdded(RemoteStream remoteStream) {
                    //有参与者成功加入会话后，会触发此方法
                    remoteStream.enableAudio(false);
                    //在视频展示控件中播放其他端媒体流
                    remoteStream.attach(remoteView);
                }

                @Override
                public void onStreamRemoved(RemoteStream remoteStream) {

                }

                @Override
                public void onError(VideoException e) {

                }
            });

        }

        @Override
        public void onParticipantDisconnected(Conversation conversation, Participant participant) {

            Toast.makeText(ConversationActivity.this, "用户：" + participant.getParticipantId() +
                    "离开会话", Toast.LENGTH_SHORT).show();
            btnInvite.setText("用户列表");

        }


    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams
                .FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams
                .FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View
                .SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_conversation);

        ButterKnife.bind(this);

        /*int checker=PermissionChecker.checkCallingPermission(this,"android.permission.CAMERA",null);
        if (checker==-1){
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }*/


        String uid = WilddogAuth.getInstance().getCurrentUser().getUid();
        tvUid.setText(uid);

        SyncReference reference = WilddogSync.getReference();
        String path = reference.getRoot().toString();
        int startIndex = path.indexOf("https://") == 0 ? 8 : 7;
        String appid = path.substring(startIndex, path.length() - 14);
        //初始化Video
        WilddogVideo.initializeWilddogVideo(getApplicationContext(), appid);
        //初始化Video 时需要初始化两个类，Video和ConversationClient类，分别对其进行初始化
        //初始化Video，传入Context

        //获取video对象
        video = WilddogVideo.getInstance();
        //获取client对象
        client = video.getClient();
        initVideoRender();


        LocalStreamOptions.Builder builder = new LocalStreamOptions.Builder();
        LocalStreamOptions options = builder.height(240).width(320).build();
        //创建本地视频流，通过video对象获取本地视频流
        localStream = video.createLocalStream(options, new CompleteListener() {
            @Override
            public void onCompleted(VideoException e) {

            }
        });
        //为视频流绑定播放控件
        localStream.attach(localView);

        incomingDialogMap = new HashMap<>();
        //在使用inviteToConversation方法前需要先设置会话邀请监听，否则使用邀请功能会抛出IllegalStateException异常
        this.client.setInviteListener(inviteListener);
    }

    //初始化视频展示控件
    private void initVideoRender() {
        //获取EglBase对象

        //初始化视频展示控件位置，大小
        localViewLayout.setPosition(LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED, LOCAL_WIDTH_CONNECTED,
                LOCAL_HEIGHT_CONNECTED);
        localView.setZOrderMediaOverlay(true);
        localView.setMirror(true);

        remoteViewLayout.setPosition(REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT);

    }

    @OnClick(R.id.btn_invite)
    public void invite() {

        //取消发起会话邀请
        if (outgoingInvite != null) {
            outgoingInvite.cancel();
            btnInvite.setText("用户列表");
            outgoingInvite = null;
        } else {

            showLoginUsers();
        }

    }

    @OnClick(R.id.btn_invite_cancel)
    public void inviteCancel() {

        if (mConversation != null){
            mConversation.disconnect();
            mConversation=null;
        }
    }


    private void showLoginUsers() {
        startActivityForResult(new Intent(ConversationActivity.this, UserListActivity.class), 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            //选取用户列表中的用户，获得其 Wilddog ID
            String participant = data.getStringExtra("participant");
            //调用inviteToConversation 方法发起会话
            inviteToConversation(participant);
            btnInvite.setText("取消邀请");
        }
    }


    private void inviteToConversation(String participant) {

        //创建连接参数对象
        //localStream 为video.createLocalStream()获取的本地视频流
        //第二个参数为用户自定义的数据，类型为字符串
        ConnectOptions options = new ConnectOptions(localStream, "chaih");
        //inviteToConversation 方法会返回一个OutgoingInvite对象，
        //通过OutgoingInvite对象可以进行取消邀请操作
        outgoingInvite = client.inviteToConversation(participant, options, new ConversationCallback() {
            @Override
            public void onConversation(Conversation conversation, VideoException exception) {
                if (conversation != null) {
                    //对方接受邀请并成功建立会话，conversation不为空，exception为空
                    mConversation = conversation;

                    mConversation.setConversationListener(conversationListener);
                } else {
                    //对方拒绝时，exception不为空
                }

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //需要离开会话时调用此方法，并做资源释放和其他自定义操作

        if (mConversation != null) {
            mConversation.disconnect();
        }
        localStream.detach();
        localStream.close();
        video.dispose();
    }
}
