package com.wylxbot.wylx.Commands.Fight;

import com.wylxbot.wylx.Commands.Fight.Util.*;
import com.wylxbot.wylx.Core.Events.Commands.CommandContext;
import com.wylxbot.wylx.Core.Events.Commands.ServerCommand;
import com.wylxbot.wylx.Core.Util.Helper;
import com.wylxbot.wylx.Database.DatabaseManager;
import com.wylxbot.wylx.Database.DbElements.DiscordUser;
import com.wylxbot.wylx.Database.DbElements.UserIdentifiers;
import com.wylxbot.wylx.Wylx;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class SkillPointsCommand extends ServerCommand {

    public SkillPointsCommand() {
        super("skillpoints", CommandPermission.EVERYONE, "Run to spend skill points acquired by leveling up while fighting",
                "sp", "skill");
    }

    @Override
    public void runCommand(CommandContext ctx) {
        Member member = ctx.event().getMember();
        assert member != null;

        if(FightCommand.isFightingList.getUserStatus(member) != UserFightStatus.NONE){
            ctx.event().getMessage().reply("Cannot spend skill points while fighting").queue();
            return;
        }

        FightCommand.isFightingList.setUserFightStatus(member, UserFightStatus.SKILLPOINTS);

        ctx.event().getChannel().sendMessageEmbeds(buildEmbed(ctx.event().getGuild(), member)).queue(message -> {
            Helper.chooseFromListWithReactions(message,
                    member.getUser(),
                    5,
                    this::upgradeSkill,
                    true,
                    this::endSelection);
        });
    }

    private MessageEmbed buildEmbed(Guild guild, Member member){
        DatabaseManager db = Wylx.getInstance().getDb();
        DiscordUser dbUser = db.getUser(member.getUser().getId());
        FightUserStats stats = dbUser.getSetting(UserIdentifiers.FightStats);
        EmbedBuilder embed = new EmbedBuilder();

        int exp = FightUtil.calcEXPForLevel(stats.getLvl());
        int pointsToSpend = stats.getLvl() - stats.getUsedPoints();

        embed.setColor(guild.getSelfMember().getColor());
        embed.setAuthor(member.getEffectiveName() + "'s stats in " + guild.getName(),
                null, member.getAvatarUrl());
        embed.setDescription("Level: " + stats.getLvl() + "\nEXP: (" + stats.getExp() + " / " + exp + ")");
        embed.addField("Stat Levels", String.format(
                """
                :one: HP: %d
                :two: Speed: %d
                :three: Damage: %d
                :four: EXP Multiplier: %d
                :five: Reset Skill Points
                :x: Stop Editing
                """,
                stats.getStatLvl(FightStatTypes.HP),
                stats.getStatLvl(FightStatTypes.SPEED),
                stats.getStatLvl(FightStatTypes.DAMAGE),
                stats.getStatLvl(FightStatTypes.EXP)), false);
        embed.addField("Skill points to spend", String.format("%d\n", pointsToSpend), false);
        embed.setDescription("Select reaction correlated to skill to increase skill");

        return embed.build();
    }

    private void upgradeSkill(Helper.SelectionResults selectionResults){
        Member member = selectionResults.event().getGuild().getMemberById(selectionResults.event().getUserId());

        // Check that the member is actually loaded
        if(member == null){
            // Wait for server members to be loaded before attempting again
            selectionResults.event().getGuild().loadMembers(members -> {
                upgradeSkill(selectionResults);
            });
        }

        // There should be no way to get here without member being loaded, check anyway
        assert member != null;

        int skillPos = selectionResults.result();

        FightUserStats stats = FightUserStats.getUserStats(member);

        switch(skillPos){
            case 1 -> stats.upgradeStat(FightStatTypes.HP);
            case 2 -> stats.upgradeStat(FightStatTypes.SPEED);
            case 3 -> stats.upgradeStat(FightStatTypes.DAMAGE);
            case 4 -> stats.upgradeStat(FightStatTypes.EXP);
            case 5 -> stats.resetStats();
        }

        stats.save();

        selectionResults.event().retrieveMessage().queue(message -> {
            message.editMessageEmbeds(buildEmbed(selectionResults.event().getGuild(), member)).queue();
        });
    }

    public void endSelection(Guild guild, String userId){
        Member member = guild.getMemberById(userId);
        if(member == null){
            guild.loadMembers(members -> {
                endSelection(guild, userId);
            });
        }

        assert member != null;

        FightCommand.isFightingList.setUserFightStatus(member, UserFightStatus.NONE);
    }
}
