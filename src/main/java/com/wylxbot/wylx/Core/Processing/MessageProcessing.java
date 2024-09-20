package com.wylxbot.wylx.Core.Processing;

import com.wylxbot.wylx.Commands.BotUtil.BotUtilPackage;
import com.wylxbot.wylx.Commands.DND.TTRPGPackage;
import com.wylxbot.wylx.Commands.Fight.FightPackage;
import com.wylxbot.wylx.Commands.Frog.FrogPackage;
import com.wylxbot.wylx.Commands.Music.MusicPackage;
import com.wylxbot.wylx.Commands.Roles.RolePackage;
import com.wylxbot.wylx.Commands.ServerUtil.ServerUtilPackage;
import com.wylxbot.wylx.Commands.ServerSettings.ServerSettingsPackage;
import com.wylxbot.wylx.Commands.TimeConversion.TimePackage;
import com.wylxbot.wylx.Core.Events.Commands.CommandContext;
import com.wylxbot.wylx.Core.Events.Commands.ServerCommand;
import com.wylxbot.wylx.Core.Events.Event;
import com.wylxbot.wylx.Core.Events.EventPackage;
import com.wylxbot.wylx.Core.Events.ServerEventManager;
import com.wylxbot.wylx.Core.Events.SilentEvents.SilentEvent;
import com.wylxbot.wylx.Core.Music.WylxPlayerManager;
import com.wylxbot.wylx.Core.Util.WylxStats;
import com.wylxbot.wylx.Database.Pojos.DBServer;
import com.wylxbot.wylx.Wylx;
import com.wylxbot.wylx.Core.WylxEnvConfig;
import com.wylxbot.wylx.Database.DatabaseManager;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageProcessing extends ListenerAdapter {
    private static final WylxPlayerManager musicPlayerManager = WylxPlayerManager.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(MessageProcessing.class);
    public static final HashMap<String, ServerCommand> commandMap = new HashMap<>();
    public static final HashMap<String, String> commandToModuleMap = new HashMap<>();
    public static final ArrayList<SilentEvent> silentEvents = new ArrayList<>();
    public static final HashMap<String, Event> eventMap = new HashMap<>();
    public static final EventPackage[] eventPackages = {
            new ServerUtilPackage(),
            new ServerSettingsPackage(),
            new MusicPackage(),
            new TTRPGPackage(),
            new BotUtilPackage(),
            new FrogPackage(),
            new FightPackage(),
            new RolePackage(),
            new TimePackage(),
    };

    static {
        for(EventPackage eventPackage : eventPackages){
            for(ServerCommand command : eventPackage.getCommands()){
                commandMap.putAll(command.getCommandMap());
                commandToModuleMap.put(command.getKeyword().toLowerCase(), eventPackage.getName().toLowerCase());
            }
            silentEvents.addAll(Arrays.asList(eventPackage.getSilentEvents()));
            for(Event event : eventPackage.getEvents()){
                eventMap.putAll(event.getEventMap());
            }
        }
    }

    private final WylxStats stats;

    private final Pattern mentionPattern = Message.MentionType.USER.getPattern();

    public MessageProcessing(DatabaseManager db) {
        stats = new WylxStats(db.getCmdStats());
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        long startTime = System.currentTimeMillis();

        Wylx wylx = Wylx.getInstance();
        DatabaseManager dbManager = wylx.getDb();
        String memberID = event.getAuthor().getId();

        //Ignore messages from the bot and in DMs
        if(memberID.equals(wylx.getBotID()) ||
            !event.getChannel().canTalk() ||
            !event.isFromGuild()) {
                return;
        }

        String guildID = event.getGuild().getId();
        DBServer serverEntry = dbManager.getServer(guildID);
        ServerEventManager eventManager = ServerEventManager.getServerEventManager(guildID);
        String serverPrefix = getPrefix(serverEntry.prefix, wylx);
        String msgStr = event.getMessage().getContentRaw();
        String msgPrefix = null;

        // Check for @Wylx
        Matcher matcher = mentionPattern.matcher(msgStr);

        // Does message start with prefix?
        if (msgStr.startsWith(serverPrefix)) {
            msgPrefix = serverPrefix;

        // Does message start with @Wylx?
        } else if (matcher.find() && matcher.start() == 0 && matcher.group(1).equals(wylx.getBotIDString())) {
            msgPrefix = msgStr.substring(0, matcher.end());
        }

        // Run command if prefix was used
        if (msgPrefix != null) {
            msgStr = msgStr.substring(msgPrefix.length()).trim();
            String[] args = msgStr.split(" ");
            ServerCommand command = commandMap.get(args[0].toLowerCase());

            if (command != null && eventManager.checkEvent(command)) {
                if(command.checkPermission(event)) {
                    CommandContext ctx = new CommandContext(
                        event, args, msgStr, serverPrefix, guildID, memberID,
                        musicPlayerManager.getGuildManager(guildID),
                        serverEntry, stats
                    );
                    logger.debug("Command ({}) Called With {} Args", args[0], args.length);
                    command.runCommand(ctx);
                } else {
                    event.getMessage().reply("You don't have permission to use this command!").queue();
                }
                stats.updateAverageCommand(startTime);
                return;
            }
        }

        for(SilentEvent silentEvent : silentEvents){
            if(eventManager.checkEvent(silentEvent) && silentEvent.check(event, serverPrefix)){
                silentEvent.runEvent(event, serverPrefix);
                stats.updateAverageSilentEvent(startTime);
                return;
            }
        }

        stats.updateAverageNoOp(startTime);
    }

    private String getPrefix(String prefix, Wylx wylx) {
        WylxEnvConfig config = wylx.getWylxConfig();
        if (config.release) {
            return prefix;
        }

        return config.betaPrefix;
    }

    public void close() {
        stats.close();
    }
}
