package com.wilddog.video;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.wilddog.client.ChildEventListener;
import com.wilddog.client.DataSnapshot;
import com.wilddog.client.Wilddog;
import com.wilddog.client.WilddogError;


import java.util.ArrayList;
import java.util.List;

public class UserListActivity extends AppCompatActivity {

    private ListView lv_users;
    private Wilddog mRef;

    private List<String> userList = new ArrayList<>();
    private ChildEventListener childEventListener;
    private String mUid;
    private String participantId;
    private MyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);
        String appId = getIntent().getStringExtra("app_id");
        mRef = new Wilddog("http://" + appId + ".wilddogio.com");
        mUid = mRef.getAuth().getUid();
        lv_users = (ListView) findViewById(R.id.lv_user_list);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot != null) {

                    String key = dataSnapshot.getKey();
                    if (!mUid.equals(key)) {
                        userList.add(key);
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
                    String key = dataSnapshot.getKey();
                    if (!mUid.equals(key)) {
                        userList.remove(key);
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(WilddogError wilddogError) {

            }
        };
        mRef.child("users").addChildEventListener(childEventListener);


        adapter = new MyAdapter(userList, this);
        lv_users.setAdapter(adapter);

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
