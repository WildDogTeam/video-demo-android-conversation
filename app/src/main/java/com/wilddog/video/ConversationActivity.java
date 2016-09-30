package com.wilddog.video;

import android.content.DialogInterface;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.widget.TextView;

import com.wilddog.client.SyncReference;
import com.wilddog.client.WilddogSync;
import com.wilddog.video.bean.ConversationException;
import com.wilddog.video.bean.ConversationMode;
import com.wilddog.video.bean.InviteOptions;
import com.wilddog.video.bean.LocalStreamOptions;
import com.wilddog.video.listener.CompleteListener;
import com.wilddog.video.listener.ConversationCallback;
import com.wilddog.wilddogauth.WilddogAuth;

import org.webrtc.RendererCommon;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ConversationActivity extends AppCompatActivity {

    @BindView(R.id.tv_uid)
    TextView tvUid;
    @BindView(R.id.surface)
    GLSurfaceView mGLSurfaceView;

    private VideoRenderer.Callbacks localCallbacks;
    private VideoRenderer.Callbacks remoteCallbacks;

    private ConversationClient client;
    private Video video;
    private LocalStream localStream;
    private Conversation mConversation;
    private OutgoingInvite outgoingInvite;
    private Map<IncomingInvite, AlertDialog> incomingDialogMap;

    private  ConversationClient.Listener inviteListener =  new ConversationClient.Listener() {
        @Override
        public void onStartListeningForInvites(ConversationClient client) {

        }

        @Override
        public void onStopListeningForInvites(ConversationClient client) {

        }

        @Override
        public void onFailedToStartListening(ConversationClient client, ConversationException e) {

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
                        public void onConversation(Conversation conversation, ConversationException exception) {
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
                                        //在视频展示控件中播放媒体流
                                        remoteStream.attach(remoteCallbacks);
                                    }

                                    @Override
                                    public void onFailedToConnectParticipant(Conversation conversation,
                                                                             Participant participant,
                                                                             ConversationException exception) {

                                    }

                                    @Override
                                    public void onParticipantDisconnected(Conversation conversation, Participant
                                            participant) {

                                    }

                                    @Override
                                    public void onConversationEnded(Conversation conversation,
                                                                    ConversationException exception) {

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        ButterKnife.bind(this);

        SyncReference mRef = WilddogSync.getReference().child("wilddog");
        String uid = WilddogAuth.getInstance().getCurrentUser().getUid();
        tvUid.setText(uid);
        //初始化Video 时需要初始化两个类，Video和ConversationClient类，分别对其进行初始化
        //初始化Video，传入Context
        Video.initializeWilddogVideo(getApplicationContext());
        //初始化视频根节点，mRef=WilddogSync.getReference().child([视频控制面板中配置的自定义根节点]);
        ConversationClient.init(mRef, new CompleteListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onError(String s) {

            }
        });
        //获取video对象
        video = Video.getInstance();
        //获取client对象
        client = video.getClient();

        initVideoRender();
        //通过video对象获取本地视频流
        localStream = video.createLocalStream(LocalStreamOptions.DEFAULT_OPTIONS, new CompleteListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onError(String s) {

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
        mGLSurfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mGLSurfaceView.setPreserveEGLContextOnPause(true);
        mGLSurfaceView.setKeepScreenOn(true);
        VideoRendererGui.setView(mGLSurfaceView, null);

        localCallbacks = VideoRendererGui.createGuiRenderer(0, 0, 50, 100, RendererCommon.ScalingType
                .SCALE_ASPECT_FILL, true);
        remoteCallbacks = VideoRendererGui.createGuiRenderer(50, 0, 50, 100, RendererCommon.ScalingType
                .SCALE_ASPECT_FILL, false);
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
        InviteOptions options = new InviteOptions(ConversationMode.SERVER_BASED, participants, stream);
        //inviteToConversation 方法会返回一个OutgoingInvite对象，
        //通过OutgoingInvite对象可以进行取消邀请操作
        outgoingInvite = client.inviteToConversation(options, new ConversationCallback() {
            @Override
            public void onConversation(Conversation conversation, ConversationException exception) {
                if (conversation != null) {
                    //对方接受邀请并成功建立会话，conversation不为空，exception为空
                    mConversation = conversation;
                    mConversation.setConversationListener(new Conversation.Listener() {
                        @Override
                        public void onParticipantConnected(Conversation conversation, Participant participant) {

                            RemoteStream remoteStream = participant.getRemoteStream();
                            remoteStream.attach(remoteCallbacks);
                        }

                        @Override
                        public void onFailedToConnectParticipant(Conversation conversation, Participant participant,
                                                                 ConversationException exception) {

                        }

                        @Override
                        public void onParticipantDisconnected(Conversation conversation, Participant participant) {

                        }

                        @Override
                        public void onConversationEnded(Conversation conversation, ConversationException exception) {

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
