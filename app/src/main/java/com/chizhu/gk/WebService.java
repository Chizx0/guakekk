package com.chizhu.gk;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;

import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;


import androidx.core.app.NotificationCompat;

public class WebService extends Service {
    private static final String CHANNEL_ID = "playback_channel";
    private static final int NOTIFICATION_ID = 1001;
    private PowerManager.WakeLock wakeLock;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        // 添加唤醒锁保持CPU运行
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::WakeLock");
        wakeLock.acquire(10 * 60 * 1000);
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        // 彻底停止服务
        stopForeground(true);
        stopSelf();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release(); // 必须释放唤醒锁
        }
        // Android 12+适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.deleteNotificationChannel(CHANNEL_ID);
            sendBroadcast(new Intent("RESTART_SERVICE"));
        }

    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("后台运行中")
                .setContentText("正在刷课")
                .setSmallIcon(R.drawable.ic)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)  // 添加持续显示标志
                .setAutoCancel(false)  // 禁用自动取消
                .setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setTimeoutAfter(Long.MAX_VALUE)  // 永久显示
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "媒体播放",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("课程自动播放服务通知");
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // 彻底停止所有后台线程
        stopSelf();
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.cancel(NOTIFICATION_ID);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.deleteNotificationChannel(CHANNEL_ID);
        }
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}