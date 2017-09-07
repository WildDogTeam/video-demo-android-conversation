package com.wilddog.quickstart;

import android.app.Application;

import com.wilddog.wilddogcore.WilddogApp;
import com.wilddog.wilddogcore.WilddogOptions;

/**
 * Created by chaihua on 16-8-22.
 */
public class MediaApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //初始化WilddogApp,完成初始化之后可在项目任意位置通过getInstance()获取Sync & Auth对象
        WilddogOptions.Builder builder = new WilddogOptions.Builder().setSyncUrl("https://" + Constants.SYNC_APPID + ".wilddogio" + ".com");
        WilddogOptions options = builder.build();
        WilddogApp.initializeApp(getApplicationContext(), options);
    }
}
