package com.wilddog.video;

import android.content.DialogInterface;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.wilddog.client.Wilddog;
import com.wilddog.video.bean.ConversationException;
import com.wilddog.video.bean.ConversationMode;
import com.wilddog.video.bean.InviteOptions;
import com.wilddog.video.bean.LocalStreamOptions;
import com.wilddog.video.listener.CompleteListener;
import com.wilddog.video.listener.ConversationCallback;

import org.webrtc.RendererCommon;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConversationActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btn_invite;
    private Button btn_invite_cancel;
    private TextView tv_uid;
    private GLSurfaceView mGLSurfaceView;
    private VideoRenderer.Callbacks localCallbacks;
    private VideoRenderer.Callbacks remoteCallbacks;

    private ConversationClient client;
    private OutgoingInvite invite;

    private Video video;

    private LocalStream localStream;

    private Map<String, RemoteStream> remoteStreamMap = new HashMap<>();
    private String mParticipentId;
    private static List<String> participentList = new ArrayList<>();

    private Conversation mConversation;


    private OutgoingInvite outgoingInvite;
    private Map<IncomingInvite, AlertDialog> incomingDialogMap;
    private String mAppId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);


        tv_uid = (TextView) findViewById(R.id.tv_uid);
        btn_invite = (Button) findViewById(R.id.btn_invite);
        btn_invite.setOnClickListener(this);
        btn_invite_cancel = (Button) findViewById(R.id.btn_invite_cancel);
        btn_invite_cancel.setOnClickListener(this);
        mAppId = getIntent().getStringExtra("app_id");
        Wilddog mRef = new Wilddog("http://" + mAppId + ".wilddogio.com");
        String uid = mRef.getAuth().getUid();
        tv_uid.setText( uid);
        Video.initializeWilddogVideo(getApplicationContext());
        ConversationClient.init(mRef, new CompleteListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onError(String s) {

            }
        });
        video = Video.getInstance();

        client = video.getClient();
        mGLSurfaceView = (GLSurfaceView) findViewById(R.id.surface);
        mGLSurfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mGLSurfaceView.setPreserveEGLContextOnPause(true);
        mGLSurfaceView.setKeepScreenOn(true);
        VideoRendererGui.setView(mGLSurfaceView, null);

        localCallbacks = VideoRendererGui.createGuiRenderer(0, 0, 50, 100, RendererCommon.ScalingType
                .SCALE_ASPECT_FILL, true);
        remoteCallbacks = VideoRendererGui.createGuiRenderer(50, 0, 50, 100, RendererCommon.ScalingType
                .SCALE_ASPECT_FILL, false);

        localStream = video.createLocalStream(LocalStreamOptions.DEFAULT_OPTIONS, new CompleteListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onError(String s) {

            }
        });

        localStream.attach(localCallbacks);

        incomingDialogMap = new HashMap<>();
        this.client.setInviteListener(new ConversationClient.Listener() {
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

                                mConversation = conversation;
                                conversation.setConversationListener(new Conversation.Listener() {
                                    @Override
                                    public void onParticipantConnected(Conversation conversation, Participant
                                            participant) {
                                        RemoteStream remoteStream = participant.getRemoteStream();
                                        remoteStream.attach(remoteCallbacks);
                                    }

                                    @Override
                                    public void onFailedToConnectParticipant(Conversation conversation, Participant
                                            participant, ConversationException exception) {

                                    }

                                    @Override
                                    public void onParticipantDisconnected(Conversation conversation, Participant
                                            participant) {

                                    }

                                    @Override
                                    public void onConversationEnded(Conversation conversation, ConversationException
                                            exception) {

                                    }
                                });
                            }
                        });

                    }
                });
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {

                    }
                });
                //builder.setCancelable(false);
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
        });
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.btn_invite:
                showLoginUsers();

                break;

            case R.id.btn_invite_cancel:
                outgoingInvite.cancel();
        }
    }

    private void showLoginUsers() {
        startActivityForResult(new Intent(ConversationActivity.this, UserListActivity.class).putExtra("app_id",
                mAppId), 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            String participant = data.getStringExtra("participant");
            Set<String> participants = new HashSet<>();
            participants.add(participant);
            inviteParticipant(participants);
        }
    }

    private void inviteParticipant(Set<String> participants) {

        LocalStream stream = new LocalStream();
        stream.setMediaStream(localStream.getMediaStream());
        InviteOptions options = new InviteOptions(ConversationMode.BASIC, participants, stream);
        outgoingInvite = client.inviteToConversation(options, new ConversationCallback() {
            @Override
            public void onConversation(Conversation conversation, ConversationException exception) {
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
            }
        });
    }
}
