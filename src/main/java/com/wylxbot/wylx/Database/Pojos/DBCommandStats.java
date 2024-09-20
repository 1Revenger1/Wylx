package com.wylxbot.wylx.Database.Pojos;

import org.bson.codecs.pojo.annotations.BsonId;

public class DBCommandStats {
    public static String KEY_ID = "STATS";

    @BsonId()
    public String id = KEY_ID;
    public long commandsProcessed = 0;
    public double averageCommandTime = 0;
    public long silentEventsProcessed = 0;
    public double averageSilentEventTime = 0;
    public long noOpsProcessed = 0;
    public double averageNoOpTime = 0;

    public DBCommandStats() {}
}
