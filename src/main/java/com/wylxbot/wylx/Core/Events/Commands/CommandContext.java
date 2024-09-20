package com.wylxbot.wylx.Core.Events.Commands;

import com.wylxbot.wylx.Core.Music.GuildMusicManager;
import com.wylxbot.wylx.Core.Util.WylxStats;
import com.wylxbot.wylx.Database.Pojos.DBCommandStats;
import com.wylxbot.wylx.Database.Pojos.DBServer;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public record CommandContext(
        MessageReceivedEvent event,
        String[] args,
        String parsedMsg,
        String prefix,
        String guildID,
        String authorID,
        GuildMusicManager musicManager,
        DBServer db,
        WylxStats stats
) {}
