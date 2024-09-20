package com.wylxbot.wylx.Core.Util;

import com.wylxbot.wylx.Database.DatabaseManager;
import com.wylxbot.wylx.Database.Pojos.DBCommandStats;
import com.wylxbot.wylx.Wylx;

import java.util.Timer;
import java.util.TimerTask;

public class WylxStats {
    DBCommandStats stats;

    private final Timer writeTimer = new Timer("WylxStatsWriteTimer");

    private static final long TEN_MINUTES_IN_MS = 600000;

    public WylxStats(DBCommandStats stats) {
        this.stats = stats;

        // Schedule updating of the db every 10 minutes
        writeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                DatabaseManager db = Wylx.getInstance().getDb();
                db.setCmdStats(stats);
            }
        }, 0, TEN_MINUTES_IN_MS);
    }

    public void updateAverageCommand(long startTime) {
        // time from this run
        long runTime = System.currentTimeMillis() - startTime;
        // Calc new avg
        stats.averageCommandTime = runningAverageCalculation(
                stats.averageCommandTime,
                stats.commandsProcessed++,
                runTime
        );
    }

    public void updateAverageNoOp(long startTime) {
        // time from this run
        long runTime = System.currentTimeMillis() - startTime;
        // Calc new avg
        stats.averageNoOpTime = runningAverageCalculation(
                stats.averageNoOpTime,
                stats.noOpsProcessed++,
                runTime
        );
    }

    public void updateAverageSilentEvent(long startTime) {
        // time from this run
        long runTime = System.currentTimeMillis() - startTime;
        // Calc new avg
        stats.averageSilentEventTime = runningAverageCalculation(
                stats.averageSilentEventTime,
                stats.silentEventsProcessed++,
                runTime
        );
    }

    private double runningAverageCalculation(double oldAvg, long oldCount, long newDataPoint) {
        double adjustedOldAvg = oldAvg * oldCount;
        double newAvg = (adjustedOldAvg + newDataPoint) / (oldCount + 1);
        return newAvg;
    }

    public long getCommandsProcessed() {
        return stats.commandsProcessed;
    }

    public double getAverageCommandTime() {
        return stats.averageCommandTime;
    }

    public long getSilentEventsProcessed() {
        return stats.silentEventsProcessed;
    }

    public double getAverageSilentEventTime() {
        return stats.averageSilentEventTime;
    }

    public long getNoOpsProcessed() {
        return stats.noOpsProcessed;
    }

    public double getAverageNoOpTime() {
        return stats.averageNoOpTime;
    }

    public void close() {
        writeTimer.cancel();
        DatabaseManager db = Wylx.getInstance().getDb();
        db.setCmdStats(stats);
    }
}
