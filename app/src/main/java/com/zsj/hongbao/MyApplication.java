package com.zsj.hongbao;

import android.app.Application;
import android.content.Context;

/**
 * Created by zhangshijie on 2019/12/10
 */
public class MyApplication extends Application {

    private static Context mContext;

    public static Context getInstance(){
        return mContext;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }
}
