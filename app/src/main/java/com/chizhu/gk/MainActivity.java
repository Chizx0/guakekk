package com.chizhu.gk;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private Button mainButton, subButton1, subButton2;
    private boolean isTimerRunning = false;
    private static final int REQUEST_CODE_NOTIFICATION = 1001;
    private static final int OVERLAY_PERMISSION_CODE = 1; // 自定义请求码
    private WindowManager windowManager;
    // Handler定时器
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mHahaRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                jsVideo();  //自动播放视频
                jsRwd();  //任务点完成情况
                mHandler.postDelayed(this, 6000); // 递归调用
            } catch (Exception e) {
                Log.e("Timer", "定时任务异常", e);
            }
        }
    };


    // 处理权限请求结果（网页7）
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_NOTIFICATION) {
            boolean hasPermission = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED;
            }

            if (hasPermission) {
                // 双重验证系统级设置
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager.areNotificationsEnabled()) {
                    ToastUtil.showCustomToast(this, "通知权限已开启");
                } else {
                    showSystemEnableDialog(); // 需要引导用户开启系统级开关
                }
            } else {
                showManualEnableDialog();
            }
        }
    }

    private void showSystemEnableDialog() {
        new AlertDialog.Builder(this)
                .setTitle("系统级通知开关未开启")
                .setMessage("请前往系统设置开启全局通知开关")
                .setPositiveButton("立即设置", (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    startActivity(intent);
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkRealNotificationStatus();
    }

    private void checkRealNotificationStatus() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        boolean systemEnabled = manager.areNotificationsEnabled();

        boolean runtimeGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runtimeGranted = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }

        if (!systemEnabled || !runtimeGranted) {
            new AlertDialog.Builder(this)
                    .setTitle("通知权限异常")
                    .setMessage("应用通知权限未完全开启\n请确保以下两项均已启用：\n1. 系统全局通知开关\n2. 运行时通知权限")
                    .setPositiveButton("一键设置", (d, w) -> {
                        openAppSettings();
                        openNotificationSettings();
                    })
                    .show();
        }
    }



    private void showManualEnableDialog() {
        new AlertDialog.Builder(this)
                .setTitle("需要手动授权")
                .setMessage("请前往系统设置开启通知权限")
                .setPositiveButton("去设置", (d, w) -> {
                    // 同时打开应用设置和通知设置
                    openAppSettings();
                    openNotificationSettings();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 打开应用通用设置
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    // 打开通知设置（适配不同Android版本）
    private void openNotificationSettings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:" + getPackageName()));
        }
        startActivity(intent);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 动态请求通知权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                new AlertDialog.Builder(this)
                        .setTitle("需要通知权限")
                        .setMessage("请允许显示通知以保证后台任务运行")
                        .setPositiveButton("去授权", (d, w) -> {
                            ActivityCompat.requestPermissions(
                                    this,
                                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                    REQUEST_CODE_NOTIFICATION
                            );
                        })
                        .show();
            }
        } else {
            // Android 12 及以下默认授予通知权限
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (!manager.areNotificationsEnabled()) {
                // 跳转到系统通知设置页
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                startActivity(intent);
            }
        }


        setContentView(R.layout.activity_main);

        mainButton = findViewById(R.id.mainButton);
        subButton1 = findViewById(R.id.subButton1);
        subButton2 = findViewById(R.id.subButton2);
        webView = findViewById(R.id.webView);

        initWebView();

        // 主按钮点击事件
        mainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (subButton1.getVisibility() == View.VISIBLE) {
                    // 隐藏子按钮
                    hideSubButton(subButton2, 200);
                    hideSubButton(subButton1, 300);
                } else {
                    // 显示子按钮
                    subButton1.setVisibility(View.VISIBLE);
                    subButton2.setVisibility(View.VISIBLE);
                    showSubButton(subButton1, 200);
                    showSubButton(subButton2, 300);
                }
            }
        });

        subButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isTimerRunning = !isTimerRunning;
                if (isTimerRunning) {
                    subButton1.setText("点击停止");
                    mHandler.post(mHahaRunnable); // 启动定时器
                } else {
                    subButton1.setText("开始");
                    mHandler.removeCallbacks(mHahaRunnable); // 停止定时器
                }
            }
        });


        // 子按钮2点击事件
        subButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSubButton(subButton1, 200);
                hideSubButton(subButton2, 300);
                moveToBackground();
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    private void hideSubButton(final Button button, long duration) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                1.0f, 0.0f,
                1.0f, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(duration);
        scaleAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                button.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        button.startAnimation(scaleAnimation);
    }

    private void showSubButton(final Button button, long duration) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                0.0f, 1.0f,
                0.0f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(duration);
        button.startAnimation(scaleAnimation);
    }

    private void initWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        // 新增硬件加速和缓存优化
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setDomStorageEnabled(true);
        settings.setSupportZoom(true);        // 启用缩放
        settings.setBuiltInZoomControls(true);// 显示缩放控件
        settings.setDisplayZoomControls(false); // 隐藏默认缩放按钮
        settings.setMediaPlaybackRequiresUserGesture(false); // 允许自动播放
        // 强制启用宽视口模式
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);//跨域处理
        settings.setAllowUniversalAccessFromFileURLs(true);
        String pcUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36";
        settings.setUserAgentString(pcUA);


        webView.loadUrl("https://v8.chaoxing.com");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    view.loadUrl(request.getUrl().toString());
                    return true;
                }
                return false;
            }
        });
    }


    private void jsVideo(){
        String jsPlay = "(function(){" +
                "var outerIframe = parent.document.querySelector('#iframe');" +
                "var innerIframe = outerIframe.contentDocument.querySelector('iframe.ans-insertvideo-online');" +
                "var videoPlayer = innerIframe.contentWindow.videojs('video');" +
                "videoPlayer.muted(true);" +    // 强制静音
                "videoPlayer.playbackRate(2);" + // 2倍速播放
                "videoPlayer.play();" +          // 触发播放
                "})()";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(jsPlay, null);
        }
    }
    private void jsXyj(){//下一节
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //webView.evaluateJavascript("setTimeout(() => document.getElementById(\"prevNextFocusNext\").click(),1000);", null);
            webView.evaluateJavascript("document.querySelector('.nextChapter').click()", null);//强制下一节
        }
    }
    private void jsRwd(){
        String jsCode = "(function(){" +
                "const response = {" +
                "success: data => JSON.stringify({status:'success', data})," +
                "warning: msg => JSON.stringify({status:'warning', message:msg})," +
                "error: (type, msg) => JSON.stringify({status:'error', type, message:msg})" +
                "};" +
                "try {" +
                "const iframe = document.getElementById('iframe');" +
                "if (!iframe) return response.error('iframe_missing', '未找到iframe元素');" +
                "const checkIframeContent = () => {" +
                "try {" +
                "const iframeDoc = iframe.contentDocument || iframe.contentWindow.document;" +
                "if (!iframeDoc) return response.warning('iframe文档未就绪');" +
                "const element = iframeDoc.querySelector('.ans-job-icon.ans-job-icon-clear, .ans-job-icon');" +
                "return element ? " +
                "response.success({label: element.getAttribute('aria-label')}) : " +
                "response.warning('目标元素不存在');" +
                "} catch(e) {" +
                "return response.error('cross_origin', '跨域错误: ' + e.message);" +
                "}" +
                "};" +
                "const syncResult = checkIframeContent();" +
                "if (syncResult) return syncResult;" +
                "return new Promise(resolve => {" +
                "const handler = () => {" +
                "resolve(checkIframeContent());" +
                "iframe.removeEventListener('load', handler);" +
                "};" +
                "iframe.addEventListener('load', handler);" +
                "setTimeout(() => resolve(response.error('timeout', 'iframe加载超时')), 5000);" +
                "});" +
                "} catch(e) {" +
                "return response.error('runtime', '运行时错误: ' + e.message);" +
                "}" +
                "})()";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(jsCode, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    try {
                        String jsonStr = value.startsWith("\"") ?
                                value.substring(1, value.length()-1) : value;
                        jsonStr = jsonStr.replace("\\\"", "\"");
                        JSONObject result = new JSONObject(jsonStr);

                        switch (result.getString("status")) {
                            case "success":
                                JSONObject data = result.getJSONObject("data");
                                String label = data.getString("label");
                                if (label.equals("任务点已完成") || !label.equals("任务点未完成")) {
                                    ToastUtil.showCustomToast(MainActivity.this, label + " 下一节");
                                    jsXyj();
                                }
                                break;
                            case "warning":
                                ToastUtil.showCustomToast(MainActivity.this, "警告：" + result.getString("message")+" 下一节");
                                jsXyj();
                                break;
                            case "error":
                                ToastUtil.showCustomToast(MainActivity.this, result.getString("type") + ": " + result.getString("message"));
                                jsXyj();
                                break;
                        }
                    } catch (JSONException e) {
                        ToastUtil.showCustomToast(MainActivity.this, e.getMessage());
                    }
                }
            });
        }
    }

    private void moveToBackground() {
        startForegroundService();
        moveTaskToBack(true);
    }

    private void startForegroundService() {
        Intent intent = new Intent(this, WebService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacks(mHahaRunnable);
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        // 移除窗口并销毁 WebView[2,6](@ref)
        if (windowManager != null && webView != null) {
            windowManager.removeView(webView);
            webView.destroy();
        }
        super.onDestroy();
    }
}