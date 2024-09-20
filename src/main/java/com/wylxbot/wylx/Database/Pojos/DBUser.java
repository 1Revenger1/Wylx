package com.wylxbot.wylx.Database.Pojos;

import com.wylxbot.wylx.Database.CollectionPojo;

public class DBUser extends CollectionPojo {
    public String timezone = "LOL";
    public boolean timezonePrompted = false;
    public DBUserFightStats fightStats = new DBUserFightStats();
}
