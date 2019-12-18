package com.zsj.hongbao.utils;

import android.os.Handler;
import android.os.Message;

/**
 * Created by zhangshijie on 2019/6/19;
 */
public class SafelyHandlerWarpper extends Handler {
    private Handler impl;

    public SafelyHandlerWarpper(Handler impl) {
        this.impl = impl;
    }

    @Override
    public void dispatchMessage(Message msg) {
        try {
            super.dispatchMessage(msg);
        } catch (Exception e) {}
    }

    @Override
    public void handleMessage(Message msg) {
        impl.handleMessage(msg);//需要委托给原Handler执行
    }
}
