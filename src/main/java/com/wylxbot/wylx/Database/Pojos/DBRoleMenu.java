package com.wylxbot.wylx.Database.Pojos;

import com.wylxbot.wylx.Database.CollectionPojo;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Map;

public class DBRoleMenu extends CollectionPojo {
    public String channelId;
    public String guildId;
    public String title;
    public Map<String, DBRoleMenuRole> roles;
}
