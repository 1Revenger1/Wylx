package com.wylxbot.wylx.Commands.Fight.Util;

import com.wylxbot.wylx.Database.Pojos.DBUser;
import com.wylxbot.wylx.Database.Pojos.DBUserFightStats;
import com.wylxbot.wylx.Wylx;
import com.wylxbot.wylx.Database.DatabaseManager;
import net.dv8tion.jda.api.entities.Member;

public class FightUserStats {
    private final static int DEFAULT_HP = 500;
    public int hp = 500;
    public Member user;
    DBUserFightStats stats;

    public FightUserStats(DBUserFightStats stats, Member user){
        this.stats = stats;
        this.user = user;

        resetHP();
    }

    private static final DatabaseManager db = Wylx.getInstance().getDb();
    public static FightUserStats getUserStats(Member user) {
        DBUserFightStats stats = db.getUser(user.getId()).fightStats;
        return new FightUserStats(stats, user);
    }

    public boolean addExp(int exp) {
        stats.exp += exp;
        int expNextLvl = FightUtil.calcEXPForLevel(stats.level);
        if (stats.exp > expNextLvl) {
            stats.exp -= expNextLvl;
            stats.level++;
            return true;
        }

        return false;
    }

    public void resetHP(){
        hp = (int) (DEFAULT_HP * FightUtil.calcMultiplier(stats.hpLevel));
    }

    public int getLvl() {
        return stats.level;
    }

    public int getExp() {
        return stats.exp;
    }

    // Returns level of a skill
    public int getStatLvl(FightStatTypes stat) {
        return switch (stat) {
            case HP -> stats.hpLevel;
            case EXP -> stats.expLevel;
            case SPEED -> stats.speedLevel;
            case DAMAGE -> stats.damageLevel;
        };
    }

    public void upgradeStat(FightStatTypes stat){
        if(getLvl() - getUsedPoints() <= 0) return;
        switch(stat){
            case HP -> stats.hpLevel++;
            case EXP -> stats.expLevel++;
            case SPEED -> stats.speedLevel++;
            case DAMAGE -> stats.damageLevel++;
        }
    }

    public void resetStats(){
        stats.hpLevel = 0;
        stats.expLevel = 0;
        stats.speedLevel = 0;
        stats.damageLevel = 0;
    }

    public int getUsedPoints() {
        return stats.hpLevel + stats.expLevel + stats.speedLevel + stats.damageLevel;
    }

    // Returns multiplier based off of the level of the skill
    public double getStatMultiplier(FightStatTypes stat) {
        return FightUtil.calcMultiplier(getStatLvl(stat));
    }

    public void save() {
        DBUser dbUserData = db.getUser(user.getId());
        dbUserData.fightStats = stats;
        db.setUser(user.getId(), dbUserData);
    }
}
