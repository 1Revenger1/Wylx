package com.wylxbot.wylx.Commands.BotUtil;

import com.wylxbot.wylx.Core.Events.Commands.CommandContext;
import com.wylxbot.wylx.Core.Events.Commands.ServerCommand;
import com.wylxbot.wylx.Core.Util.EnvUtils;
import com.wylxbot.wylx.Core.Util.ProgressBar;
import com.wylxbot.wylx.Core.Util.WylxStats;
import com.wylxbot.wylx.Database.DbElements.DiscordGlobal;
import com.wylxbot.wylx.Database.DbElements.GlobalIdentifiers;
import com.wylxbot.wylx.Wylx;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;


public class StatusCommand extends ServerCommand {
    private final String commitID = getCommitID();

    private static final long BYTES_PER_MEGABYTE = 1024 * 1024;
    private static final long MILLI_PER_SECOND = 1000;
    private static final long MILLI_PER_MINUTES = MILLI_PER_SECOND * 60;
    private static final long MILLI_PER_HOURS = MILLI_PER_MINUTES * 60;
    private static final long MILLI_PER_DAYS = MILLI_PER_HOURS * 24;

    public StatusCommand() {
        super("status", CommandPermission.EVERYONE, "Provides information on the host machine of the bot", "system", "stats");
    }

    private String getCommitID() {
        Runtime rt = Runtime.getRuntime();
        String[] commands = {"git", "rev-parse", "--short", "HEAD"};
        try {
            Process proc = rt.exec(commands);

            if (proc.waitFor() != 0) {
                return "Unknown";
            }

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            String commit = stdInput.readLine().trim();
            stdInput.close();
            return commit;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private long milliToDays(long val) {
        return val / MILLI_PER_DAYS;
    }

    private long milliToHours(long val) {
        return (val / MILLI_PER_HOURS) % 24;
    }

    private long milliToMinutes(long val) {
        return (val / MILLI_PER_MINUTES) % 60;
    }

    @Override
    public void runCommand(CommandContext ctx) {
        MessageReceivedEvent event = ctx.event();
        DiscordGlobal globalDB = Wylx.getInstance().getDb().getGlobal();
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(event.getGuild().getSelfMember().getColor());
        embed.setTitle("Wylx Status");

        // Build elements for system section
        String systemName = "System: ";
        if (mIsDocker) {
            systemName += "Docker";
        } else {
            try {
                systemName += InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                systemName += "Unknown";
            }
        }

        Runtime rt = Runtime.getRuntime();
        long maxMem = rt.maxMemory() / BYTES_PER_MEGABYTE;
        long usedMem = (rt.totalMemory() - rt.freeMemory()) / BYTES_PER_MEGABYTE;
        double ratio = (double) usedMem / maxMem;
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();

        String systemBuilder =
                String.format("OS: %s\n", System.getProperty("os.name")) +
                String.format("Commit: %s\n", commitID) +
                String.format("Threads: %d\n", rt.availableProcessors()) +
                String.format("Bot Uptime: %d Days, %d Hours, and %d minutes\n",
                         milliToDays(uptime), milliToHours(uptime), milliToMinutes(uptime)) +
                String.format("Memory: %dMB of %dMB\n", usedMem, maxMem) +
                ProgressBar.progressBar(ratio);

        embed.addField(systemName, systemBuilder, false);

        // Build stats section
        WylxStats wylxStats = globalDB.getSetting(GlobalIdentifiers.BotStats);
        embed.addField("Wylx Stats",
                String.format("""
                        | \u2005\u2005\u2005\u2005\u2005\u2005\u200A\u200ACommands Processed:\u2005\u2005\u2005\u2005\t%d commands
                        | \u2005\u2005\u200AAverage Command Time:\u2005\u2005\u2005\u2005\t%.02f ms
                        | \u2005\u2005\u2005\u2005\u2008Silent Events Processed:\u2005\u2005\u2005\u2005\t%d events
                        | \u200AAverage Silent Event Time:\u2005\u2005\u2005\u2005\t%.02f ms
                        | \u2005\u2005\u2005\u2008\u200ANoOp Events Processed:\u2005\u2005\u2005\u2005\t%d noops
                        | \u2005\u2005\u2005\u2005\u2005\u2005\u2005\u2005\u2005\u2005Average NoOp Time:\u2005\u2005\u2005\u2005\t%.02f ms
                        """,
                        wylxStats.getCommandsProcessed(),
                        wylxStats.getAverageCommandTime(),
                        wylxStats.getSilentEventsProcessed(),
                        wylxStats.getAverageSilentEventTime(),
                        wylxStats.getNoOpsProcessed(),
                        wylxStats.getAverageNoOpTime()),false);


        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }
}
