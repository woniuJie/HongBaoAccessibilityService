package com.zsj.hongbao;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;

import com.zsj.hongbao.utils.DetachableClickListener;

import java.util.List;

import static com.zsj.hongbao.utils.DetachableClickListener.wrap;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "zsj-main";
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.tv_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showAccessibilitySettingPromptIfDisabled();
    }

    public void showAccessibilitySettingPromptIfDisabled() {
        if (isAccessibleEnabled()) {
            textView.setText(getResources().getString(R.string.open_access));
            return;
        }

        DetachableClickListener clickListener = wrap(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                goSetting();
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this) //
                .setMessage(getResources().getString(R.string.go_accessiblity_title))
                .setNegativeButton(getResources().getString(R.string.cancel), null)
                .setPositiveButton(getResources().getString(R.string.sure), clickListener) //
                .create();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            clickListener.clearOnDetach(dialog);
        }
        dialog.show();
    }

    private boolean isAccessibleEnabled() {
        AccessibilityManager manager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);

        List<AccessibilityServiceInfo> runningServices = manager.getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK);
        for (AccessibilityServiceInfo info : runningServices) {
            if (info.getId().equals(getPackageName() + "/.HongBaoAccessibilityService")) {
                return true;
            }
        }
        return false;
    }

    private void goSetting() {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    /**
     *  通过ADB授权 自动开启无障碍服务
     */
    public void openAccessiblityByAdb(){
        try {
            String enabledService = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            Log.e(TAG, "onResume: enabledService--" + enabledService);
            String service = enabledService + ":com.zsj.hongbao/com.zsj.hongbao.HongBaoAccessibilityService";
            Settings.Secure.putString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, service);
            Settings.Secure.putString(getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, "1");

            Log.e(TAG, "onResume: 成功开启service");
        } catch (Exception e) {
            Log.e(TAG, "onResume: 失败" + e.getMessage());
            //adb shell pm grant com.zsj.hongbao android.permission.WRITE_SECURE_SETTINGS
        }
    }

}
