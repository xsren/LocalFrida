package com.mhook.sample;


import android.Manifest;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.mhook.sample.task.FridaTaskWrapper;
import com.mhook.sample.tool.App;
import com.mhook.sample.tool.BuildTool;
import com.mhook.sample.tool.Debug;
import com.mhook.sample.tool.FileTool;
import com.mhook.sample.tool.ShellUtil;
import com.mhook.sample.tool.bean.JsBean;
import com.mhook.sample.tool.go.Go;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.github.kbiakov.codeview.CodeView;


public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    private static final String BOLD = "<b>Bold</b><br><br>";
    private static final String ITALIT = "<i>Italic</i><br><br>";
    private static final String UNDERLINE = "<u>Underline</u><br><br>";
    private static final String STRIKETHROUGH = "<s>Strikethrough</s><br><br>"; // <s> or <strike> or <del>
    private static final String BULLET = "<ul><li>asdfg</li></ul>";
    private static final String QUOTE = "<blockquote>Quote</blockquote>";
    private static final String LINK = "<a href=\"https://github.com/mthli/Knife\">Link</a><br><br>";
    private static final String EXAMPLE = BOLD + ITALIT + UNDERLINE + STRIKETHROUGH + BULLET + QUOTE + LINK;
    private CodeView codeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permissions();
        javaScript();
    }

    private void permissions() {
        Permissions.check(this/*context*/, Manifest.permission.WRITE_EXTERNAL_STORAGE, null, new PermissionHandler() {
            @Override
            public void onGranted() {
                // do your task.
            }
        });
    }

    private void javaScript() {
        codeView = findViewById(R.id.code_view);
        codeView.setCode(FileTool.assetsText(this,"frida.js"), "js");
    }


    public void test(View view) {
        Toast.makeText(MainActivity.this, "测试...." + test(), Toast.LENGTH_LONG).show();
    }

    public static int test() {
        return 12345;
    }

    public void go(View view) {
        Go.go(() -> {

            Debug.LogI(TAG, "getCpuType...");
            String fridaServerName;
            switch (BuildTool.getCpuType()) {
                case ARM:
                    fridaServerName = "fs12116arm";
                    break;
                case ARM64:
                    fridaServerName = "fs12116arm64";
                    break;
                case X86:
                    fridaServerName = "fs12116x86";
                    break;
                default:
                    Debug.LogE(TAG, "error BuildTool.getCpuType()");
                    return;
            }
            Debug.LogI(TAG, "getCpuType...", fridaServerName);
            String targetPath = getFilesDir().getAbsolutePath() + "/" + fridaServerName;
            if (!FileTool.copyToFiles(this, fridaServerName, targetPath)) {
                Debug.LogE(TAG, "error FileTool.copyToFiles");
                return;
            }
            if (!ShellUtil.permission()) {
                Debug.LogE(TAG, "not root permission!!");
                return;
            }
            ShellUtil.execCommandNoWait(
                    new String[]{
                            StringUtils.join("cd ", getFilesDir().getAbsolutePath()),
                            StringUtils.join("chmod 777 ", fridaServerName),
                            StringUtils.join("./", fridaServerName, " -D -l 127.0.0.1:", App.FRIDA_SERVER_PORT)
                    },
                    true, false);
//            if (commandResult.result != 0) {
//                Debug.LogE(TAG, "error commandResult.result!=0:", commandResult.errorMsg);
//                return;
//            }
//            Debug.LogI(TAG,"commandResult:",commandResult.successMsg);
            Debug.LogI(TAG, "start frida task...");
            List<JsBean> jsBeanList = JsBean.parse(new File("/sdcard/LocalFrida/"));
            for (JsBean jsBean:jsBeanList){
                new FridaTaskWrapper(this, jsBean.getProcess(),
                        jsBean.getJs(), App.FRIDA_SERVER_PORT).setFridaTaskListener(new FridaTaskWrapper.OnFridaTaskListener() {
                    @Override
                    public void onStarted() {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "已注入:"+jsBean.getProcess(), Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onStopped() {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "已停止...."+jsBean.getProcess(), Toast.LENGTH_LONG).show();
                        });

                    }
                }).start();

            }

        });

    }
}
