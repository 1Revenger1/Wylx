package Commands.Fight;

import Core.Events.Commands.CommandContext;
import Core.Events.Commands.ThreadedCommand;
import Core.Fight.FightMessages;
import Core.Fight.FightStatTypes;
import Core.Fight.FightUserManager;
import Core.Fight.FightUserStats;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FightCommand extends ThreadedCommand {

    private final Random random = new Random();
    private final FightUserManager isFightingList = new FightUserManager();

    private final Pattern mentionPattern = Message.MentionType.USER.getPattern();
    private final String noExpStr = "\nThere is no EXP to be found here";

    public FightCommand() {
        super("fight", CommandPermission.EVERYONE, "Fight another user");
    }



    @Override
    protected void runCommandThread(CommandContext ctx) {
        MessageReceivedEvent event = ctx.event();
        Message msg = ctx.event().getMessage();
        Member player1 = msg.getMember();
        Member player2;

        assert player1 != null;

        if (msg.mentionsEveryone()) {
            msg.reply("How are you supposed to fight everyone here? That seems *really* difficult...").queue();
            return;
        }

        // Check for mentioned users
        Matcher matcher = mentionPattern.matcher(ctx.parsedMsg());
        if (!matcher.find()) {
            msg.reply("Please mention a user").queue();
            return;
        }

        // ID is in group 1
        String userid = matcher.group(1);
        player2 = event.getGuild().retrieveMemberById(userid).complete();
        if (player2 == null) {
            msg.reply("User does not exist").queue();
            return;
        }

        // Check if users are fighting
        if (isFightingList.userIsFighting(player1) ||
            isFightingList.userIsFighting(player2)) {
            msg.reply("One of the above users is currently fighting! Please wait").queue();
            return;
        }

        // Prevent the user from fighting multiple times
        isFightingList.setUserIsFighting(player1, true);
        isFightingList.setUserIsFighting(player2, true);

        FightUserStats player1Stats = FightUserStats.getUserStats(player1);
        FightUserStats player2Stats = FightUserStats.getUserStats(player2);

        String headerStr = String.format("__**%s** [lvl: %d] vs **%s** [lvl: %d]__",
                player1.getEffectiveName(), player1Stats.getLvl(),
                player2.getEffectiveName(), player2Stats.getLvl());

        msg.getChannel().sendMessage(headerStr).complete();

        if (player2.getUser().isBot() || msg.getAuthor().isBot()) {
            // Bot should be user 1, as they do the attack
            FightUserStats bot = msg.getAuthor().isBot() ? player1Stats : player2Stats;
            FightUserStats user = msg.getAuthor().isBot() ? player2Stats : player1Stats;

            // Get attack msg
            int idx = random.nextInt(FightMessages.botMessages.length);
            String botMsg = FightMessages.botMessages[idx];
            botMsg = replaceStringWithUsers(botMsg, bot, user);

            msg.getChannel().sendMessage(botMsg + noExpStr).complete();
            isFightingList.setUserIsFighting(player1, false);
            isFightingList.setUserIsFighting(player2, false);
            return;
        }

        if (player1.getIdLong() == player2.getIdLong()) {
            // Get attack msg
            int idx = random.nextInt(FightMessages.duplicateFighterMessages.length);
            String dupMsg = FightMessages.duplicateFighterMessages[idx];
            dupMsg = replaceStringWithUsers(dupMsg, player1Stats, player2Stats);

            msg.getChannel().sendMessage(dupMsg + noExpStr).complete();
            isFightingList.setUserIsFighting(player1, false);
            isFightingList.setUserIsFighting(player2, false);
            return;
        }

        // Do fight loop and decide victor
        fight (ctx, player1Stats, player2Stats);
    }

    private void fight(CommandContext ctx, FightUserStats player1, FightUserStats player2) {
        ArrayList<Message> messages = new ArrayList<>();    // Messages to delete
        MessageChannel channel = ctx.event().getChannel();
        FightUserStats attacker, defender;

        // Use difference in speed when checking which player should go first
        double speedDiff = player1.getStatMultiplier(FightStatTypes.SPEED) - player2.getStatMultiplier(FightStatTypes.SPEED);
        boolean player1Turn = random.nextDouble() < (speedDiff + 0.5);

        do {
            attacker = player1Turn ? player1 : player2;
            defender = player1Turn ? player2 : player1;

            int damage = (int) (getRandomDamage() * attacker.getStatMultiplier(FightStatTypes.DAMAGE));
            defender.hp -= Math.min(damage, defender.hp);

            String attackMessage = FightMessages.attackMessages[random.nextInt(FightMessages.attackMessages.length)];

            if (defender.hp <= 0) {
                // TODO: Finisher move
            }

            attackMessage = replaceStringWithUsers(attackMessage, attacker, defender);
            String msgStr = String.format("%s! [-%d] [%d hp left]", attackMessage, damage, defender.hp);
            Message msgObj = channel.sendMessage(msgStr).complete();
            messages.add(msgObj);

            player1Turn = !player1Turn;

            try {
                Thread.sleep(1200);
            } catch (Exception e) {
                // NOOP
            }
        } while (player1.hp > 0 && player2.hp > 0);

        int exp = (int) (random.nextDouble() * 10) + 15;
        int attackerExp = (int) (exp * 2 * attacker.getStatMultiplier(FightStatTypes.EXP));
        int defenderExp = (int) (exp / 3 * defender.getStatMultiplier(FightStatTypes.EXP));

        String fightEnd = String.format("%s won and gained %d EXP!\n%s lost but gained %d EXP.\n\n",
                attacker.user.getEffectiveName(), attackerExp,
                defender.user.getEffectiveName(), defenderExp);

        if (attacker.addExp(attackerExp)) {
            fightEnd += String.format("%s leveled up! New level %d\n",
                    attacker.user.getEffectiveName(), attacker.getLvl());
        }

        if (defender.addExp(defenderExp)) {
            fightEnd += String.format("%s leveled up! New level %d",
                    defender.user.getEffectiveName(), attacker.getLvl());
        }

        channel.sendMessage(fightEnd).complete();

        // Fight is done, save and let other fights occur
        player1.save();
        player2.save();
        isFightingList.setUserIsFighting(player1.user, false);
        isFightingList.setUserIsFighting(player2.user, false);

        // Wait 30 seconds before cleaning up fight messages
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            // NOOP
        }

        channel.purgeMessages(messages);
    }

    private double getRandomDamage() {
        double randomPercent = random.nextDouble();
        // (1.4x - 0.49) ^ 3 + 0.25
        //\left(9.38x-3.28\right)^{3}+60
        return Math.pow(9.38 * randomPercent - 3.28, 3) + 60;
    }

    private String replaceStringWithUsers(String msg, FightUserStats attacker, FightUserStats defender) {
        msg = msg.replace("{p1}", String.format("**%s**", attacker.user.getEffectiveName()));
        msg = msg.replace("{p2}", String.format("**%s**", defender.user.getEffectiveName()));
        msg = msg.replace("{r}", Integer.toString(random.nextInt(8) + 2));
        return msg;
    }
}
