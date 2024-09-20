package com.wylxbot.wylx.Database.Pojos;

import com.wylxbot.wylx.Database.CollectionPojo;
import org.bson.codecs.pojo.annotations.BsonId;

import java.util.HashMap;
import java.util.Map;

public class DBServer extends CollectionPojo {
    public Map<String, Boolean> enabledModules = new HashMap<>();
    public Map<String, Boolean> exceptions = new HashMap<>();
    public int musicVolume = 20;
    public String prefix = "$";
}
