package com.chizhu.gk;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import java.util.List;

public class KeepAliveJob extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        if (!isServiceRunning(getApplicationContext())) {
            Intent serviceIntent = new Intent(this, WebService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

    private boolean isServiceRunning(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            UsageStatsManager usageStatsManager = (UsageStatsManager)
                    context.getSystemService(Context.USAGE_STATS_SERVICE);
            long currentTime = System.currentTimeMillis();
            List<UsageStats> stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY, currentTime - 1000 * 10, currentTime);

            for (UsageStats stat : stats) {
                if (stat.getPackageName().equals(getPackageName()) &&
                        stat.getLastTimeUsed() > currentTime - 1000 * 5) {
                    return true;
                }
            }
            return false;
        } else {
            // 兼容旧版本
            ActivityManager manager = (ActivityManager)
                    context.getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo service :
                    manager.getRunningServices(Integer.MAX_VALUE)) {
                if (WebService.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
            return false;
        }
    }
}