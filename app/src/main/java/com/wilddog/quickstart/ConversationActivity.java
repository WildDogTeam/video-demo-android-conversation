package com.wilddog.quickstart;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import com.wilddog.client.WilddogSync;
import com.wilddog.video.base.LocalStream;
import com.wilddog.video.base.LocalStreamOptions;
import com.wilddog.video.base.WilddogVideoError;
import com.wilddog.video.base.WilddogVideoInitializer;
import com.wilddog.video.base.WilddogVideoView;
import com.wilddog.video.base.WilddogVideoViewLayout;
import com.wilddog.video.base.util.LogUtil;
import com.wilddog.video.base.util.logging.Logger;
import com.wilddog.video.call.CallStatus;
import com.wilddog.video.call.Conversation;
import com.wilddog.video.call.RemoteStream;
import com.wilddog.video.call.WilddogVideoCall;
import com.wilddog.video.call.WilddogVideoCallOptions;
import com.wilddog.video.call.stats.LocalStreamStatsReport;
import com.wilddog.video.call.stats.RemoteStreamStatsReport;
import com.wilddog.wilddogauth.WilddogAuth;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class ConversationActivity extends AppCompatActivity {

    private static final String TAG = ConversationActivity.class.getSimpleName();
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

    private boolean isInConversation = false;
    private boolean isAudioEnable = false;
    @BindView(R.id.btn_invite)
    Button btnInvite;
    @BindView(R.id.btn_mic)
    Button btnMic;
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

    PermissionHelper mHelper;

    // 所需的全部权限
    static final String[] PERMISSIONS = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };
    private String participant;

    DecimalFormat decimalFormat = new DecimalFormat("0.00");
    private WilddogVideoCall video;
    private LocalStream localStream;
    private Conversation mConversation;
    private AlertDialog alertDialog;
    private Map<Conversation, AlertDialog> conversationAlertDialogMap;
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
                    dismissDialog();
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onStreamReceived(RemoteStream remoteStream) {
            remoteStream.attach(remoteView);
            remoteStream.enableAudio(true);
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
            dismissDialog();
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
        public void onError(final WilddogVideoError wilddogVideoError) {
            // 41007 表示超时,在接受前表示呼叫超时,在接受后表示对方异常退出
            if (wilddogVideoError != null && 41007 == wilddogVideoError.getErrCode()) {
                if (isInConversation) {
                    // 处理异常退出逻辑
                } else {
                    // 处理超时逻辑
                    dismissDialog();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeConversation();
                        if (isInConversation) {
                            Toast.makeText(ConversationActivity.this, "对方异常退出,等待超时", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ConversationActivity.this, "对方呼叫超时,本端未作相应", Toast.LENGTH_SHORT).show();
                        }

                        Log.e("error", wilddogVideoError.getMessage());
                    }
                });
                if (isInConversation) {
                    isInConversation = false;
                }
            } else {
                // 其他类型错误
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeConversation();
                        Toast.makeText(ConversationActivity.this, "通话中出错,请查看日志", Toast.LENGTH_SHORT).show();
                        Log.e("error", wilddogVideoError.getMessage());
                    }
                });
                if (isInConversation) {
                    isInConversation = false;
                }
            }
        }
    };
    //AlertDialog列表
    private WilddogVideoCall.Listener inviteListener = new WilddogVideoCall.Listener() {
        @Override
        public void onCalled(final Conversation conversation, String s) {
            if (!TextUtils.isEmpty(s)) {
                Toast.makeText(ConversationActivity.this, "对方邀请时候携带的信息是:" + s, Toast.LENGTH_SHORT).show();
                tvData.setText("对方携带数据为:" + s);
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
            Log.e(TAG, "onTokenError: " + wilddogVideoError.toString());
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
                tvLocalRate.setText("rate:" + localStats.getBitsSentRate() + "Kb/s " + localStats.getLocalCandidateType());
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
                tvRemoteRate.setText("rate:" + remoteStats.getBitsReceivedRate() + "Kb/s  " + remoteStats.getRemoteCandidateType() + " delay" + remoteStats.getDelay() + "ms");
            }
        });
    }

    public String convertToMB(BigInteger value) {
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
        mHelper = new PermissionHelper(this);
        String uid = WilddogAuth.getInstance().getCurrentUser().getUid();
        tvUid.setText(uid);
        LogUtil.setLogLevel(Logger.Level.DEBUG);
        //初始化Video
        WilddogVideoInitializer.initialize(getApplicationContext(), Constants.VIDEO_APPID, WilddogAuth.getInstance().getCurrentUser().getToken(false).getResult()
                .getToken());
        //获取video对象
        video = WilddogVideoCall.getInstance();
        video.start();
        initVideoRender();
        if (mHelper.lacksPermissions(PERMISSIONS)) {
            mHelper.requestPermissions(PERMISSIONS);
        } else {
            createAndShowLocalStream();
        }
        conversationAlertDialogMap = new HashMap<>();
        video.setListener(inviteListener);
    }

    private void createAndShowLocalStream() {
        LocalStreamOptions.Builder builder = new LocalStreamOptions.Builder();
        LocalStreamOptions options = builder.dimension(LocalStreamOptions.Dimension.DIMENSION_240P).build();
        //创建本地视频流，通过video对象获取本地视频流
        localStream = LocalStream.create(options);
        //开启音频/视频，设置为 false 则关闭声音或者视频画面
        localStream.enableAudio(true);
        localStream.enableVideo(true);
        //为视频流绑定播放控件
        localStream.attach(localView);
    }

    //初始化视频展示控件
    private void initVideoRender() {
        //初始化视频展示控件位置，大小
        localViewLayout.setPosition(LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED, LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED);
        localView.setZOrderMediaOverlay(true);
        localView.setMirror(true);
        remoteViewLayout.setPosition(REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT);
    }

    @OnClick(R.id.btn_invite)
    public void invite() {
        showLoginUsers();
    }

    @OnClick(R.id.btn_mic)
    public void mic() {
        if (localStream != null) {
            isAudioEnable = !isAudioEnable;
            localStream.enableAudio(isAudioEnable);
        }
    }

    @OnClick(R.id.btn_invite_cancel)
    public void inviteCancel() {
        closeConversation();
    }

    private void closeConversation() {
        if (mConversation != null) {
            mConversation.close();
            mConversation = null;
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
            participant = data.getStringExtra("participant");
            inviteToConversation(participant);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.PERMISSION_REQUEST_CODE && hasAllPermissionsGranted(grantResults)) {
            createAndShowLocalStream();
        }
    }

    // 含有全部的权限
    private boolean hasAllPermissionsGranted(@NonNull int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    private void inviteToConversation(String participant) {
        btnInvite.setText("用户列表");
        String data = "extra data";
        //创建连接参数对象
        WilddogVideoCallOptions option = new WilddogVideoCallOptions.Builder()
                .data(data)
                .build();
        mConversation = video.call(participant, localStream, option);
        mConversation.setConversationListener(conversationListener);
        mConversation.setStatsListener(statsListener);
    }

    private void dismissDialog() {
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
    }

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
        // video 断开与服务端链接,将无法收到请求,需要重新 start 因为video在本界面会重新初始化故释放资源
        video.stop();
        // 将写入数据实时引擎的数据用户在线状态数据移除
        WilddogSync.getInstance().goOffline();
    }
}
