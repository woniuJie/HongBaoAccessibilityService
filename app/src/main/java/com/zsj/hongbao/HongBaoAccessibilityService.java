package com.zsj.hongbao;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import com.zsj.hongbao.utils.ToastUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK;

/**
 * Created by zhangshijie on 2019/12/6
 */
public class HongBaoAccessibilityService extends BaseAccessibilityService {

    private static final String TAG = "zsj-红包";
    private static final int ENVELOPE_RETURN = 101;

    private static final int MSG_GO_HOME = 201;
    private static final int MSG_CONTINUE_CLICK = 202;

    /**
     * 拆红包类
     */
    private static final String WECHAT_RECEIVER_CALSS = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNotHookReceiveUI";
    /**
     * 红包详情页
     */
    private static final String WECHAT_DETAIL = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI";
    /**
     * 微信主页面或者聊天页面
     */
    private static final String WECHAT_LAUNCHER = "com.tencent.mm.ui.LauncherUI";

    private static String packageName;


    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "onUnbind: ");
        return super.onUnbind(intent);
    }

    @Override
    public void onInterrupt() {
        super.onInterrupt();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        super.onAccessibilityEvent(event);
        Log.d(TAG, "onAccessibilityEvent: " + event);
        if (event == null || event.getPackageName() == null) {
            return;
        }
        packageName = event.getPackageName().toString();
        int eventType = event.getEventType();

        switch (eventType) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                //识别是微信，且为红包
                if ("com.tencent.mm".equals(packageName)) {
                    List<CharSequence> texts = event.getText();
                    if (!texts.isEmpty()) {
                        for (CharSequence text : texts) {
                            String content = text.toString();
                            Log.d(TAG, "onAccessibilityEvent: 通知内容-" + content);
                            if (!TextUtils.isEmpty(content) && content.contains("微信红包")) {
                                notifyWechat(event);
                            }
                        }
                    }
                }

                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                if ("com.tencent.mm".equals(packageName)) {
                    String clazzName = event.getClassName().toString();
                    //监测如果当前页面是 拆红包类，则点击 开 按钮
                    if (clazzName.equals(WECHAT_RECEIVER_CALSS)) {
                        clickOpenHongBao(event.getSource());
                    }
                    //监测如果当前页面是 红包详情页  暂不做处理
                    if (clazzName.equals(WECHAT_DETAIL)) {
                    }
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                //识别含有微信红包，并点击
                checkHongBaoAndClick();
                break;
            default:
                break;
        }
    }

    /**
     * 拉起微信界面
     *
     * @param event event
     */
    private void notifyWechat(AccessibilityEvent event) {
        if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
            Notification notification = (Notification) event.getParcelableData();
            PendingIntent pendingIntent = notification.contentIntent;
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 监测当前页面是否有红包，如果是点击该红包
     */
    public void checkHongBaoAndClick() {
        if ("com.tencent.mm".equals(packageName)) {
            //监测是否是群聊
            AccessibilityNodeInfo nodeInfo = findViewByID("com.tencent.mm:id/lt");
            if (!checkIsQunChats(nodeInfo)) {
                return;
            }

            //红包的父级布局id为 atb (apk升级会发生改变)
            AccessibilityNodeInfo root = getRootInActiveWindow();
            List<AccessibilityNodeInfo> lists = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/atb");

            if (lists != null && lists.size() > 0) {
                //找到当前页面中有红包的组件，选择最后一个红包组件
                AccessibilityNodeInfo info1 = lists.get(lists.size() - 1);
                List<AccessibilityNodeInfo> info_a = info1.findAccessibilityNodeInfosByText("已领取");
                List<AccessibilityNodeInfo> info_b = info1.findAccessibilityNodeInfosByText("已被领完");
                List<AccessibilityNodeInfo> info_c = info1.findAccessibilityNodeInfosByText("微信红包");

                Log.e(TAG, "onAccessibilityEvent: 已领取" + info_a.size());
                Log.e(TAG, "onAccessibilityEvent: 已被领完" + info_b.size());
                Log.e(TAG, "onAccessibilityEvent: 微信红包" + info_c);
                Log.e(TAG, "onAccessibilityEvent: 微信红包" + info_c.size());

                //如果最后一个红包组件，没有被领取或者已被领完，则点击这个红包组件
                if (info_a.size() == 0 && info_b.size() == 0 && info_c.size() > 0) {
                    performViewClick(info_c.get(0));
                }
            }
        }
    }


    /**
     * 点击开启红包按钮
     *
     * @param node
     */
    private void clickOpenHongBao(AccessibilityNodeInfo node) {
        if (null == node) return;
        final int count = node.getChildCount();
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                AccessibilityNodeInfo childNode = node.getChild(i);
                if (null != childNode && null != childNode.getText()) {
                    //如果点进去后，已经被抢完了，则直接返回
                    if (childNode.getText().equals("手慢了，红包派完了")) {
                        clickBack();
                        return;
                    }
                }
                if (null != childNode && childNode.getClassName().equals("android.widget.Button") && childNode.isClickable()) {
                    childNode.performAction(ACTION_CLICK);
                }

                clickOpenHongBao(childNode);
            }
        }
    }

    /**
     * 点击返回按钮
     */
    public void clickBack() {
        performGlobalAction(GLOBAL_ACTION_BACK);
    }


    /**
     * 监测是否是群聊，监测的方式是：
     * 群聊的标题会有(3)成员数量，比如说  一家人(3)
     * 然后根据正则来进行匹配是否有(3)
     *
     * @param nodeInfo
     * @return
     */
    public boolean checkIsQunChats(AccessibilityNodeInfo nodeInfo) {
        boolean isQunChats = false;
        if (nodeInfo == null) {
            return false;
        }
        if (nodeInfo.getText() != null) {
            String description = nodeInfo.getText().toString();
            List<String> xx = extractMessageByRegular(description);
            if (xx != null && xx.size() > 0) {
                isQunChats = true;
            }
        }
        return isQunChats;
    }

    public static List<String> extractMessageByRegular(String msg) {
        List<String> list = new ArrayList<String>();
        Pattern p = Pattern.compile("(\\()([0-9a-zA-Z\\.\\/\\=])*(\\))");
        Matcher m = p.matcher(msg);
        while (m.find()) {
            list.add(m.group(0).substring(1, m.group().length() - 1));
        }
        return list;
    }

}
