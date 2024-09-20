package com.wylxbot.wylx.Commands.Music;

import com.wylxbot.wylx.Core.Events.Commands.CommandContext;
import com.wylxbot.wylx.Core.Events.Commands.ServerCommand;
import com.wylxbot.wylx.Core.Music.MusicUtils;
import com.wylxbot.wylx.Database.DatabaseManager;
import com.wylxbot.wylx.Wylx;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class VolumeCommand extends ServerCommand {
    private static final String USAGE = "Usage: %{p}volume <number between 0 and 100>";

    VolumeCommand () {
        super("volume",
                CommandPermission.EVERYONE,
                "Set playback volume\n" + USAGE,
                "v");
    }

    @Override
    public void runCommand(CommandContext ctx) {
        MessageReceivedEvent event = ctx.event();
        String[] args = ctx.args();

        if (args.length != 2) {
            String message = String.format("Current volume is: %d\n%s",
                    ctx.musicManager().getVolume(),
                    ServerCommand.replacePrefix(USAGE, ctx.prefix()));
            event.getChannel().sendMessage(message).queue();
            return;
        }

        MusicUtils.VoiceCommandBlockedReason blocked = MusicUtils.voiceCommandBlocked(ctx);
        if (blocked != MusicUtils.VoiceCommandBlockedReason.COMMAND_OK) {
            event.getChannel().sendMessage(blocked.reason).queue();
            return;
        }

        int number = Integer.parseInt(args[1]);

        if (number > 100 || number < 0) {
            event.getChannel().sendMessage("Volume out of range, keep it between 0-100").queue();
            return;
        }

        ctx.db().musicVolume = number;
        Wylx.getInstance().getDb().setServer(ctx.guildID(), ctx.db());
        ctx.musicManager().setVolume(number);
    }
}
