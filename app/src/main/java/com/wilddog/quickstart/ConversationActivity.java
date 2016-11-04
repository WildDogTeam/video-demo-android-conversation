package com.wilddog.quickstart;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.wilddog.client.SyncReference;
import com.wilddog.client.WilddogSync;
import com.wilddog.video.Conversation;
import com.wilddog.video.ConversationClient;
import com.wilddog.video.IncomingInvite;
import com.wilddog.video.LocalStream;
import com.wilddog.video.OutgoingInvite;
import com.wilddog.video.Participant;
import com.wilddog.video.RemoteStream;
import com.wilddog.video.WilddogVideo;
import com.wilddog.video.WilddogVideoView;
import com.wilddog.video.bean.ConversationMode;
import com.wilddog.video.bean.InviteOptions;
import com.wilddog.video.bean.LocalStreamOptions;
import com.wilddog.video.bean.VideoException;
import com.wilddog.video.listener.CompleteListener;
import com.wilddog.video.listener.ConversationCallback;
import com.wilddog.wilddogauth.WilddogAuth;

import org.webrtc.EglBase;
import org.webrtc.RendererCommon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    @BindView(R.id.tv_uid)
    TextView tvUid;

    @BindView(R.id.local_video_layout)
    PercentFrameLayout localRenderLayout;

    @BindView(R.id.remote_video_layout)
    PercentFrameLayout remoteRenderLayout;

    @BindView(R.id.local_video_view)
    WilddogVideoView localCallbacks;

    @BindView(R.id.remote_video_view)
    WilddogVideoView remoteCallbacks;

/*    private VideoRenderer.Callbacks localCallbacks;
    private VideoRenderer.Callbacks remoteCallbacks;*/

    private ConversationClient client;
    private WilddogVideo video;
    private LocalStream localStream;
    private Conversation mConversation;
    private OutgoingInvite outgoingInvite;
    private Map<IncomingInvite, AlertDialog> incomingDialogMap;

    private ConversationClient.Listener inviteListener = new ConversationClient.Listener() {
        @Override
        public void onStartListeningForInvites(ConversationClient client) {

        }

        @Override
        public void onStopListeningForInvites(ConversationClient client) {

        }

        @Override
        public void onFailedToStartListening(ConversationClient client, VideoException e) {

        }

        @Override
        public void onIncomingInvite(ConversationClient client, final IncomingInvite invite) {

            AlertDialog.Builder builder = new AlertDialog.Builder(ConversationActivity.this);
            builder.setMessage("邀请你加入会话");
            builder.setTitle("加入邀请");
            builder.setNegativeButton("拒绝邀请", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    invite.reject();
                }
            });
            builder.setPositiveButton("确认加入", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    incomingDialogMap.remove(invite);

                    LocalStream stream = new LocalStream();
                    stream.setMediaStream(localStream.getMediaStream());
                    invite.accept(stream, new ConversationCallback() {
                        @Override
                        public void onConversation(Conversation conversation, VideoException exception) {
                            //对方接受邀请并成功建立会话，conversation不为空，exception为空
                            if (conversation != null) {
                                mConversation = conversation;
                                //获取到conversation后，设置ConversationListener
                                mConversation.setConversationListener(new Conversation.Listener() {
                                    @Override
                                    public void onParticipantConnected(Conversation conversation, Participant
                                            participant) {
                                        //有参与者成功加入会话后，会触发此方法
                                        //通过Participant.getRemoteStream()获取远端媒体流
                                        RemoteStream remoteStream = participant.getRemoteStream();
                                        remoteStream.enableAudio(false);
                                        //在视频展示控件中播放媒体流
                                        remoteStream.attach(remoteCallbacks);
                                    }

                                    @Override
                                    public void onFailedToConnectParticipant(Conversation conversation, Participant
                                            participant, VideoException exception) {

                                    }

                                    @Override
                                    public void onParticipantDisconnected(Conversation conversation, Participant
                                            participant) {

                                    }

                                    @Override
                                    public void onConversationEnded(Conversation conversation, VideoException
                                            exception) {

                                    }
                                });

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
            incomingDialogMap.put(invite, alertDialog);
        }

        @Override
        public void onIncomingInviteCanceled(ConversationClient client, IncomingInvite invite) {
            AlertDialog alertDialog = incomingDialogMap.get(invite);
            alertDialog.dismiss();
            alertDialog = null;
            incomingDialogMap.remove(invite);
        }
    };
    private EglBase eglBase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_conversation);

        ButterKnife.bind(this);

        //SupportAvcCodec();
        SyncReference mRef = WilddogSync.getReference().child("wilddogVideo");
        String uid = WilddogAuth.getInstance().getCurrentUser().getUid();
        tvUid.setText(uid);
        //初始化Video 时需要初始化两个类，Video和ConversationClient类，分别对其进行初始化
        //初始化Video，传入Context
        WilddogVideo.initializeWilddogVideo(getApplicationContext());
        //初始化视频根节点，mRef=WilddogSync.getReference().child([视频控制面板中配置的自定义根节点]);
        ConversationClient.init(mRef, new CompleteListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onError(VideoException s) {

            }
        });
        //获取video对象
        video = WilddogVideo.getInstance();
        //获取client对象
        client = video.getClient();

        initVideoRender();
        //通过video对象获取本地视频流
        localStream = video.createLocalStream(LocalStreamOptions.DEFAULT_OPTIONS, eglBase.getEglBaseContext(), new
                CompleteListener() {


            @Override
            public void onSuccess() {

            }

            @Override
            public void onError(VideoException e) {

            }
        });
        //为视频流绑定播放控件
        localStream.attach(localCallbacks);

        incomingDialogMap = new HashMap<>();
        //在使用inviteToConversation方法前需要先设置会话邀请监听，否则使用邀请功能会抛出IllegalStateException异常

        this.client.setInviteListener(inviteListener);
    }

    //初始化视频展示控件
    private void initVideoRender() {
        //获取EglBase对象
        eglBase = EglBase.create();
        //初始化视频展示控件位置，大小
        localRenderLayout.setPosition(LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED, LOCAL_WIDTH_CONNECTED,
                LOCAL_HEIGHT_CONNECTED);
        localCallbacks.init(eglBase.getEglBaseContext(), null);
        localCallbacks.setZOrderMediaOverlay(true);
        localCallbacks.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        localCallbacks.setMirror(true);
        localCallbacks.requestLayout();

        remoteRenderLayout.setPosition(REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT);
        remoteCallbacks.init(eglBase.getEglBaseContext(), null);
        remoteCallbacks.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        remoteCallbacks.setMirror(false);
        remoteCallbacks.requestLayout();
    }

    @OnClick(R.id.btn_invite)
    public void invite() {
        showLoginUsers();
    }

    @OnClick(R.id.btn_invite_cancel)
    public void inviteCancel() {
        //取消发起会话邀请
        outgoingInvite.cancel();
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
            Set<String> participants = new HashSet<>();
            participants.add(participant);
            //调用inviteToConversation 方法发起会话
            inviteToConversation(participants);
        }
    }


    private void inviteToConversation(Set<String> participants) {

        LocalStream stream = new LocalStream();
        stream.setMediaStream(localStream.getMediaStream());
        //ConversationMode可以选择P2P和SERVER_BASED两种
        //participants 为传入的用户Wilddog ID 列表，目前预览版仅支持单人邀请
        InviteOptions options = new InviteOptions(ConversationMode.P2P, participants, stream);
        //inviteToConversation 方法会返回一个OutgoingInvite对象，
        //通过OutgoingInvite对象可以进行取消邀请操作
        outgoingInvite = client.inviteToConversation(options, new ConversationCallback() {
            @Override
            public void onConversation(Conversation conversation, VideoException exception) {
                if (conversation != null) {
                    //对方接受邀请并成功建立会话，conversation不为空，exception为空
                    mConversation = conversation;
                    mConversation.setConversationListener(new Conversation.Listener() {
                        @Override
                        public void onParticipantConnected(Conversation conversation, Participant participant) {

                            RemoteStream remoteStream = participant.getRemoteStream();
                            remoteStream.enableAudio(false);
                            remoteStream.attach(remoteCallbacks);
                        }

                        @Override
                        public void onFailedToConnectParticipant(Conversation conversation, Participant participant,
                                                                 VideoException exception) {

                        }

                        @Override
                        public void onParticipantDisconnected(Conversation conversation, Participant participant) {

                        }

                        @Override
                        public void onConversationEnded(Conversation conversation, VideoException exception) {

                        }
                    });
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
        mConversation.disconnect();
    }
}
