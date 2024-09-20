package com.wylxbot.wylx.Database.Pojos;

public class DBUserFightStats {
    public int exp;
    public int level;
    public int hpLevel;     // Level used for HP multiplier
    public int speedLevel;  // Level used for Damage multiplier
    public int expLevel;    // Level used for Experience multiplier
    public int damageLevel; // Level used for initial turn bias

    public DBUserFightStats() {
        this.exp = 0;
        this.level = 0;
        this.hpLevel = 0;
        this.speedLevel = 0;
        this.expLevel = 0;
        this.damageLevel = 0;
    }
}
