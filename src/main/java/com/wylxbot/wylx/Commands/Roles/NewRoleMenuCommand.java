package com.wylxbot.wylx.Commands.Roles;

import com.wylxbot.wylx.Core.Events.Commands.CommandContext;
import com.wylxbot.wylx.Core.Events.Commands.ServerCommand;
import com.wylxbot.wylx.Commands.Roles.RolesUtil.RoleMenu;
import com.wylxbot.wylx.Wylx;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import java.util.List;

public class NewRoleMenuCommand extends ServerCommand {
    public NewRoleMenuCommand() {
        super("newrolemenu", CommandPermission.DISCORD_PERM, Permission.ADMINISTRATOR,
            """
            Create a new Role menu
            Usage:
            %{p}newrolemenu #channel-mention
            """);
    }

    @Override
    public void runCommand(CommandContext ctx) {
        MessageReceivedEvent event = ctx.event();
        List<TextChannel> channels = event.getMessage().getMentions().getChannels(TextChannel.class);
        if (channels.size() != 1) {
            event.getMessage().reply("Please mention 1 channel").queue();
            return;
        }

        TextChannel newChannel = channels.get(0);
        if (!newChannel.canTalk()) {
            event.getMessage().reply("Wylx does not have permission to send messages in that channel!").queue();
            return;
        }

        if (!newChannel.getGuild().getId().equals(ctx.guildID())) {
            event.getMessage().reply("This text channel is not from this server!").queue();
            return;
        }

        Member self = event.getGuild().getSelfMember();
        if (!self.hasPermission(newChannel, Permission.MESSAGE_ADD_REACTION)) {
            event.getMessage().reply("Wylx does not have permission to react in that channel!").queue();
            return;
        }

        if (!self.hasPermission(newChannel, Permission.MESSAGE_MANAGE)) {
            event.getMessage().reply("Wylx does not have the permission MESSAGE_MANAGE in that channel!").queue();
            return;
        }

        Message newMessage = newChannel.sendMessageEmbeds(RoleMenu.getEmptyEmbed()).complete();

        event.getChannel().sendMessage(String.format("""
        Role menu created in %s! To modify the menu, use `%smodifyRoleMenu %s`
        """, newChannel.getAsMention(), ctx.prefix(), newMessage.getId())).queue();

        try {
            RoleMenu menu = new RoleMenu(newMessage.getId(), newChannel.getId(), event.getGuild().getId());
            Wylx.getInstance().getDb().setRoleMenu(newMessage.getId(), menu.getDBEntry());
        } catch (IllegalArgumentException | ErrorResponseException e) {
            // This really shouldn't happen, as we know channel + message exist
            logger.error("Failed to create new Role Menu");
            e.printStackTrace();
            event.getMessage().reply("Could not create new Role Menu").queue();
        }
    }
}
