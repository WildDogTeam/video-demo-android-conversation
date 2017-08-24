package com.wilddog.quickstart;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.wilddog.client.SyncReference;
import com.wilddog.client.WilddogSync;
import com.wilddog.video.CallStatus;
import com.wilddog.video.Conversation;
import com.wilddog.video.LocalStream;
import com.wilddog.video.LocalStreamOptions;
import com.wilddog.video.RemoteStream;
import com.wilddog.video.WilddogVideo;
import com.wilddog.video.WilddogVideoError;
import com.wilddog.video.WilddogVideoView;
import com.wilddog.video.WilddogVideoViewLayout;
import com.wilddog.video.core.stats.LocalStreamStatsReport;
import com.wilddog.video.core.stats.RemoteStreamStatsReport;
import com.wilddog.wilddogauth.WilddogAuth;

import java.text.DecimalFormat;
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

    private static final String TAG = ConversationActivity.class.getSimpleName();

    private boolean isInConversation = false;

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

    @BindView(R.id.tv_local_dimensions)
    TextView tvLocalDimensions;
    @BindView(R.id.tv_local_fps)
    TextView tvLocalFps;
    @BindView(R.id.tv_local_rate)
    TextView tvLocalRate;
    @BindView(R.id.tv_local_sentbytes)
    TextView tvLocalSendBytes;
    @BindView(R.id.tv_remote_dimensions)
    TextView tvRemoteDimensions;
    @BindView(R.id.tv_remote_fps)
    TextView tvRemoteFps;
    @BindView(R.id.tv_remote_rate)
    TextView tvRemoteRate;
    @BindView(R.id.tv_remote_recbytes)
    TextView tvRemoteRecBytes;
    @BindView(R.id.tv_data)
    TextView tvData;


    private WilddogVideo video;
    private LocalStream localStream;
    private Conversation mConversation;
    DecimalFormat decimalFormat = new DecimalFormat("0.00");
    private AlertDialog alertDialog;
    private Map<Conversation, AlertDialog> conversationAlertDialogMap;
    //AlertDialog列表
    private WilddogVideo.Listener inviteListener = new WilddogVideo.Listener() {
        @Override
        public void onCalled(final Conversation conversation, String s) {
            if(!TextUtils.isEmpty(s)){
                Toast.makeText(ConversationActivity.this,"对方邀请时候携带的信息是:"+s,Toast.LENGTH_SHORT).show();
                tvData.setText("对方携带数据为:"+s);
            }
            mConversation = conversation;
            mConversation.setConversationListener(conversationListener);
            mConversation.setStatsListener(statsListener);
            AlertDialog.Builder builder = new AlertDialog.Builder(ConversationActivity.this);
            builder.setMessage("邀请你加入会话");
            builder.setTitle("加入邀请");
            builder.setNegativeButton("拒绝邀请", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mConversation.reject();
                }
            });
            builder.setPositiveButton("确认加入", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    conversationAlertDialogMap.remove(conversation);
                    mConversation.accept(localStream);
                    isInConversation = true;

                }
            });

            alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
            conversationAlertDialogMap.put(conversation, alertDialog);
        }

        @Override
        public void onTokenError(WilddogVideoError wilddogVideoError) {

        }

    };

    private Conversation.StatsListener statsListener = new Conversation.StatsListener() {
        @Override
        public void onLocalStreamStatsReport(LocalStreamStatsReport localStreamStatsReport) {
            changeLocalData(localStreamStatsReport);
        }

        @Override
        public void onRemoteStreamStatsReport(RemoteStreamStatsReport remoteStreamStatsReport) {
            changeRemoteData(remoteStreamStatsReport);
        }
    };

    public void changeLocalData(final LocalStreamStatsReport localStats) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvLocalDimensions.setText("dimension:" + localStats.getWidth() + "x" + localStats.getHeight());
                tvLocalFps.setText("fps:" + localStats.getFps());
                tvLocalRate.setText("rate:" + localStats.getBitsSentRate() + "Kb/s");
                tvLocalSendBytes.setText("sent:" + convertToMB(localStats.getBytesSent()) + "MB");
            }
        });

    }

    public void changeRemoteData(final RemoteStreamStatsReport remoteStats) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvRemoteDimensions.setText("dimension:" + remoteStats.getWidth() + "x" + remoteStats.getHeight());
                tvRemoteFps.setText("fps:" + remoteStats.getFps());
                tvRemoteRecBytes.setText("received:" + convertToMB(remoteStats.getBytesReceived()) + "MB");
                tvRemoteRate.setText("rate:" + remoteStats.getBitsReceivedRate() + "Kb/s" + " delay" + remoteStats.getDelay() + "ms");
            }
        });

    }

    public String convertToMB(long value) {
        float result = Float.parseFloat(String.valueOf(value)) / (1024 * 1024);
        return decimalFormat.format(result);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams
                .FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View
                .SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_conversation);

        ButterKnife.bind(this);


        String uid = WilddogAuth.getInstance().getCurrentUser().getUid();
        tvUid.setText(uid);
        //初始化Video
        WilddogVideo.initialize(getApplicationContext(), Constants.VIDEO_APPID, WilddogAuth.getInstance().getCurrentUser().getToken(false).getResult()
                .getToken());
        //获取video对象
        video = WilddogVideo.getInstance();

        initVideoRender();
        createAndShowLocalStream();
        conversationAlertDialogMap = new HashMap<>();
        //在使用inviteToConversation方法前需要先设置会话邀请监听，否则使用邀请功能会抛出IllegalStateException异常
        video.setListener(inviteListener);
    }

    private void createAndShowLocalStream() {

        LocalStreamOptions.Builder builder = new LocalStreamOptions.Builder();
        LocalStreamOptions options = builder.dimension(LocalStreamOptions.Dimension.DIMENSION_480P).build();
        //创建本地视频流，通过video对象获取本地视频流
        localStream = video.createLocalStream(options);
        //开启音频/视频，设置为 false 则关闭声音或者视频画面
        //localStream.enableAudio(true);
        // localStream.enableVideo(true);
        //为视频流绑定播放控件
        localStream.attach(localView);
    }

    //初始化视频展示控件
    private void initVideoRender() {
        //获取EglBase对象

        //初始化视频展示控件位置，大小
        localViewLayout.setPosition(LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED, LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED);
        localView.setZOrderMediaOverlay(true);
        localView.setMirror(true);

        remoteViewLayout.setPosition(REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT);

    }

    @OnClick(R.id.btn_invite)
    public void invite() {
        //取消发起会话邀请
        showLoginUsers();

    }

    @OnClick(R.id.btn_invite_cancel)
    public void inviteCancel() {

        closeConversation();
    }

    private void closeConversation() {
        if (mConversation != null) {
            mConversation.close();
            mConversation = null;
            //挂断时会释放本地流，如需继续显示本地流，则挂断后要重新获取一次本地流
            createAndShowLocalStream();
        }
        btnInvite.setText("用户列表");
    }


    private void showLoginUsers() {
        startActivityForResult(new Intent(ConversationActivity.this, UserListActivity.class), 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            //选取用户列表中的用户，获得其 Wilddog UID
            String participant = data.getStringExtra("participant");
            //调用inviteToConversation 方法发起会话
            inviteToConversation(participant);
            btnInvite.setText("用户列表");
        }
    }


    private void inviteToConversation(String participant) {
        String data = "{\"type\":\"doctor\",\"usedTime\":19,\"uid\":3492,\"name\":\"彬子\",\"consult_id\":112,\"avatar\":\"\\/uploads\\/avatars\\/2017\\/08\\/02\\/1501643194728229.jpeg\"}";
        //创建连接参数对象
        mConversation = video.call(participant, localStream, data);
        mConversation.setConversationListener(conversationListener);
        mConversation.setStatsListener(statsListener);

    }

    private Conversation.Listener conversationListener = new Conversation.Listener() {
        @Override
        public void onCallResponse(CallStatus callStatus) {
            switch (callStatus) {
                case ACCEPTED:

                    isInConversation = true;
                    break;
                case REJECTED:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ConversationActivity.this, "对方拒绝你的邀请", Toast.LENGTH_SHORT).show();
                            isInConversation = false;
                            btnInvite.setText("用户列表");
                        }
                    });
                    break;
                case BUSY:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ConversationActivity.this, "对方正在通话中,稍后再呼叫", Toast.LENGTH_SHORT).show();
                            isInConversation = false;
                            btnInvite.setText("用户列表");
                        }
                    });
                    break;
                case TIMEOUT:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ConversationActivity.this, "呼叫对方超时,请稍后再呼叫", Toast.LENGTH_SHORT).show();
                            isInConversation = false;
                            btnInvite.setText("用户列表");
                        }
                    });
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onStreamReceived(RemoteStream remoteStream) {
            remoteStream.attach(remoteView);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btnInvite.setText("用户已加入");
                }
            });
        }

        @Override
        public void onClosed() {
            Log.e(TAG, "onClosed");
            if (alertDialog != null && alertDialog.isShowing()) {
                alertDialog.dismiss();
            }
            isInConversation = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    closeConversation();
                    Toast.makeText(ConversationActivity.this, "对方挂断", Toast.LENGTH_SHORT).show();
                }
            });

        }

        @Override
        public void onError(WilddogVideoError wilddogVideoError) {
            if (wilddogVideoError != null) {
                Toast.makeText(ConversationActivity.this, "通话中出错,请查看日志", Toast.LENGTH_SHORT).show();
                Log.e("error", wilddogVideoError.getMessage());
                btnInvite.setText("用户列表");
                isInConversation = false;
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //需要离开会话时调用此方法，并做资源释放和其他自定义操作
        if (localView != null) {
            localView.release();
            localView = null;
        }
        if (remoteView != null) {
            remoteView.release();
            remoteView = null;
        }
        if (mConversation != null) {
            mConversation.close();
        }
        if (localStream != null) {
            if (!localStream.isClosed()) {
                localStream.close();
            }
        }

    }
}
