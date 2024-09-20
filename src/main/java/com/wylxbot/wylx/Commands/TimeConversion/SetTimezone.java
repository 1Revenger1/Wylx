package com.wylxbot.wylx.Commands.TimeConversion;

import com.wylxbot.wylx.Core.Events.Commands.CommandContext;
import com.wylxbot.wylx.Core.Events.Commands.ServerCommand;
import com.wylxbot.wylx.Database.Pojos.DBUser;
import com.wylxbot.wylx.Wylx;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class SetTimezone extends ServerCommand {

    public SetTimezone() {
        super("settimezone", CommandPermission.EVERYONE, """
                Set the timezone that you belong to
                Usage: %{p}settimezone <timezone_abbreviation> - set your timezone
                       %{p}settimezone - lists all supported timezones
                """, "timezone");
    }

    @Override
    public void runCommand(CommandContext ctx) {
        // If theres more than 2 args the user did something wrong
        if(ctx.args().length > 2) {
            ctx.event().getMessage().reply(getDescription(ctx.prefix())).queue();
            return;
        }

        // One arg we need to tell the user what their options are
        if(ctx.args().length == 1) {
            ctx.event().getChannel().sendMessageEmbeds(optionsEmbed(ctx)).queue();
            return;
        }

        Timezone timezone = Timezone.getTimezone(ctx.args()[1]);
        if(timezone == null) {
            ctx.event().getChannel().sendMessageEmbeds(optionsEmbed(ctx)).queue();
            return;
        }

        DBUser dUser = Wylx.getInstance().getDb().getUser(ctx.authorID());
        dUser.timezone = timezone.abrv;
        Wylx.getInstance().getDb().setUser(ctx.authorID(), dUser);
        ctx.event().getChannel().sendMessage("Set Timezone to: " + timezone.abrv).queue();
    }

    private MessageEmbed optionsEmbed(CommandContext ctx){
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Wylx Timezones");
        builder.setColor(ctx.event().getGuild().getSelfMember().getColor());
        for(Timezone zone : Timezone.values()) {
            builder.appendDescription(zone.abrv + ":    " + zone.name + "\n");
        }
        return builder.build();
    }
}
