package com.wilddog.video;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.wilddog.client.ChildEventListener;
import com.wilddog.client.DataSnapshot;
import com.wilddog.client.SyncError;
import com.wilddog.client.SyncReference;
import com.wilddog.client.WilddogSync;
import com.wilddog.wilddogauth.WilddogAuth;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class UserListActivity extends AppCompatActivity {

    @BindView(R.id.lv_user_list) ListView lvUsers;
    private SyncReference mRef;

    private List<String> userList = new ArrayList<>();
    private ChildEventListener childEventListener;
    private String mUid;
    private String participantId;
    private MyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        ButterKnife.bind(this);
        mRef = WilddogSync.getReference("users");
        mUid = WilddogAuth.getInstance().getCurrentUser().getUid();
        lvUsers = (ListView) findViewById(R.id.lv_user_list);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot != null) {
                    //获取用户Wilddog ID并添加到用户列表中
                    String uid = dataSnapshot.getKey();
                    if (!mUid.equals(uid)) {
                        userList.add(uid);
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null) {
                    //用户离开时，从用户列表中删除用户数据
                    String uid = dataSnapshot.getKey();
                    if (!mUid.equals(uid)) {
                        userList.remove(uid);
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(SyncError wilddogError) {

            }
        };
        //监听users节点
        mRef.child("users").addChildEventListener(childEventListener);


        adapter = new MyAdapter(userList, this);
        lvUsers.setAdapter(adapter);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRef.child("users").removeEventListener(childEventListener);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent();
            setResult(RESULT_CANCELED, intent);
            finish();
        }

        return super.onKeyDown(keyCode, event);

    }

    class MyAdapter extends BaseAdapter {
        private List<String> mList = new ArrayList<>();
        private LayoutInflater mInflater;

        MyAdapter(List<String> userList, Context context) {
            mList = userList;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int i) {
            return mList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(final int i, View view, ViewGroup viewGroup) {


            view = mInflater.inflate(R.layout.layout_participent_list, null);
            Button invite = (Button) view.findViewById(R.id.btn_item_invite);
            TextView id = (TextView) view.findViewById(R.id.tv_item_participent);
            id.setText(mList.get(i));
            invite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    participantId = mList.get(i);
                    Intent intent = new Intent();
                    intent.putExtra("participant", participantId);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });
            return view;
        }
    }
}
