package com.wylxbot.wylx.Commands.Fight;

import com.wylxbot.wylx.Commands.Fight.Util.*;
import com.wylxbot.wylx.Core.Events.Commands.CommandContext;
import com.wylxbot.wylx.Core.Events.Commands.ServerCommand;
import com.wylxbot.wylx.Core.Util.Helper;
import com.wylxbot.wylx.Database.DatabaseManager;
import com.wylxbot.wylx.Wylx;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkillPointsCommand extends ServerCommand {

    private static final FightUserManager userManager = FightUserManager.getInstance();

    private final Pattern mentionPattern = Message.MentionType.USER.getPattern();

    public SkillPointsCommand() {
        super("skillpoints", CommandPermission.EVERYONE, """
                        %{p}skillpoints - View and change your own skill points
                        %{p}skillpoints <user> - View the skillpoints of another user
                        """,
                "sp", "skill");
    }

    @Override
    public void runCommand(CommandContext ctx) {

        // If another user is mentioned
        if(ctx.args().length == 2){
            // attempt to find user in args
            Matcher matcher = mentionPattern.matcher(ctx.args()[1]);

            // If no user is found error
            if(!matcher.find()){
                ctx.event().getMessage().reply("Please mention a user to view their stats").queue();
                return;
            }

            String targetId = matcher.group(1);
            Member target = ctx.event().getGuild().retrieveMemberById(targetId).complete();

            // If the member is null error
            if(target == null){
                ctx.event().getMessage().reply("Target user does not exist").queue();
                return;
            }

            // Send the embed of the users skills
            ctx.event().getChannel().sendMessageEmbeds(buildEmbedOther(ctx.event().getGuild(), target)).queue();

            return;
        }

        // If the user is changing their own sp
        Member member = ctx.event().getMember();
        assert member != null;

        if(userManager.getUserStatus(member) != UserFightStatus.NONE){
            ctx.event().getMessage().reply("Cannot spend skill points while fighting").queue();
            return;
        }

        userManager.setUserFightStatus(member, UserFightStatus.SKILLPOINTS);

        ctx.event().getChannel().sendMessageEmbeds(buildEmbedPersonal(ctx.event().getGuild(), member)).queue(message -> {
            Helper.chooseFromListWithReactions(message,
                    member,
                    5,
                    this::upgradeSkill,
                    true,
                    this::endSelection);
        });
    }

    private MessageEmbed buildEmbedPersonal(Guild guild, Member member){
        FightUserStats stats = FightUserStats.getUserStats(member);
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

    private MessageEmbed buildEmbedOther(Guild guild, Member member){
        FightUserStats stats = FightUserStats.getUserStats(member);
        EmbedBuilder embed = new EmbedBuilder();

        int exp = FightUtil.calcEXPForLevel(stats.getLvl());

        embed.setColor(guild.getSelfMember().getColor());
        embed.setAuthor(member.getEffectiveName() + "'s stats in " + guild.getName(),
                null, member.getAvatarUrl());
        embed.setDescription("Level: " + stats.getLvl() + "\nEXP: (" + stats.getExp() + " / " + exp + ")");
        embed.addField("Stat Levels", String.format(
                """
                HP: %d
                Speed: %d
                Damage: %d
                EXP Multiplier: %d
                """,
                stats.getStatLvl(FightStatTypes.HP),
                stats.getStatLvl(FightStatTypes.SPEED),
                stats.getStatLvl(FightStatTypes.DAMAGE),
                stats.getStatLvl(FightStatTypes.EXP)), false);

        return embed.build();
    }

    private void upgradeSkill(Helper.SelectionResults selectionResults){
        Member member = selectionResults.event().retrieveMember().complete();

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
            message.editMessageEmbeds(buildEmbedPersonal(selectionResults.event().getGuild(), member)).queue();
        });
    }

    public void endSelection(Guild guild, Member member){
        userManager.setUserFightStatus(member, UserFightStatus.NONE);
    }
}
