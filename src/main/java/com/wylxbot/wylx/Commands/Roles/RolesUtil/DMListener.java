package com.wylxbot.wylx.Commands.Roles.RolesUtil;

import com.wylxbot.wylx.Wylx;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class DMListener extends ListenerAdapter {
    private final RoleMenu menu;
    private final User user;
    private final PrivateChannel privateChannel;
    private final Guild guild;
    private Timer timer = new Timer();
    private static final long TIMEOUT = 15 * 60 * 1000; // 15 Minutes

    public DMListener(RoleMenu menu, User user, Guild guild) throws ErrorResponseException {
        this.menu = menu;
        this.user = user;
        this.guild = guild;

        privateChannel = user.openPrivateChannel().complete();
        sendUsageAndMenu(privateChannel);
        resetTimer();
    }

    private void resetTimer() {
        timer.cancel();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                DMListenerUserManager.getInstance().removeDMListener(user, DMListenerQuitReason.TIMEOUT);
            }
        }, TIMEOUT);
    }

    public void quit(DMListenerQuitReason reason) {
        switch (reason) {
            case TIMEOUT -> privateChannel.sendMessage("Timed out. Finished editing Role Menu").queue();
            case QUIT_COMMAND -> privateChannel.sendMessage("Finished editing Role Menu").queue();
            case INTERRUPTED -> {
                String str = String.format("Existing session interrupted! Ending edit on %s to start next session", menu.getMessageID());
                privateChannel.sendMessage(str).queue();
            }
        }

        timer.cancel();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().getIdLong() != user.getIdLong() ||
                !event.getChannel().equals(privateChannel)) {
            return;
        }

        String[] rawArgs = event.getMessage().getContentRaw().split(" ");
        List<String> filteredArgs = Arrays.stream(rawArgs).filter(arg -> arg.length() != 0).toList();

        String command = filteredArgs.get(0).toLowerCase();
        String options = event.getMessage().getContentRaw().substring(command.length()).trim();

        resetTimer();

        switch (command) {
            case "quit" -> {
                DMListenerUserManager.getInstance().removeDMListener(user, DMListenerQuitReason.QUIT_COMMAND);
                return;
            }
            case "settitle" -> {
                if (filteredArgs.size() < 2) {
                    event.getChannel().sendMessage("Please give a title").queue();
                    return;
                } else {
                    menu.setTitle(options);
                }
            }
            case "addrole" -> {
                if (!addRole(filteredArgs, options, event)) {
                    return;
                }
            }
            case "removerole" -> {
                try {
                    menu.removeReaction(options.trim());
                } catch (IllegalArgumentException e) {
                    event.getChannel().sendMessage(e.getMessage()).queue();
                }
            }
            default -> { return; }
        }


        // Save changes
        Wylx.getInstance().getDb().setRoleMenu(menu.getMessageID(), menu.getDBEntry());

        // Display new menu
        try {
            sendUsageAndMenu(event.getChannel());
        } catch (ErrorResponseException e) {
            event.getJDA().removeEventListener(this);
        }
    }

    private boolean addRole(List<String> args, String options, MessageReceivedEvent event) {
        EmojiUnion emoji;
        String roleName;

        if (args.size() <= 1) {
            event.getChannel().sendMessage("Please provide an emoji and role").queue();
            return false;
        }

        // Get emote from message
        if (options.startsWith(":")) {
            // Get emote name from :emote name:
            int secondColon = options.indexOf(":", 1);
            if (secondColon == -1) {
                event.getChannel().sendMessage("Invalid emote").queue();
                return false;
            }

            // Get emote by name
            List<RichCustomEmoji> emotes = guild.getEmojisByName(options.substring(1, secondColon), true);
            if (emotes.size() == 0) {
                event.getChannel().sendMessage("Invalid emote").queue();
                return false;
            }

            emoji = (EmojiUnion) emotes.get(0);
            roleName = options.substring(emoji.getName().length() + 2);
        } else {
            // Unicode emote or custom emote
            emoji = Emoji.fromFormatted(args.get(1));
            roleName = options.substring(emoji.getFormatted().length());
        }

        // Get role now
        if (roleName.length() == 0) {
            event.getChannel().sendMessage("No role provided").queue();
            return false;
        }

        List<Role> roles = guild.getRolesByName(roleName.trim(), true);
        if (roles.size() == 0) {
            event.getChannel().sendMessage("Role does not exist").queue();
            return false;
        }

        // Check that Wylx can assign the role to other users
        Role role = roles.get(0);
        if(!guild.getSelfMember().canInteract(role)){
            event.getChannel().sendMessage("Wylx cannot assign this role, please move role below Wylx in the role hierarchy or choose another role").queue();
            return false;
        }

        // Add reaction to menu
        try {
            menu.addReaction(new RoleReaction(role, emoji));
            return true;
        } catch (IllegalArgumentException e) {
            event.getChannel().sendMessage(e.getMessage()).queue();
            return false;
        }
    }

    private void sendUsageAndMenu(MessageChannel channel) throws ErrorResponseException {
        channel.sendMessageEmbeds(menu.getEmbed(false)).complete();
        channel.sendMessage("""
                    The above embed is how your menu currently looks.
                    Use the below commands to edit the menu:
                    ```
                    addrole <emoji> <role name> - Add role to menu
                    removerole <role name>      - Remove role from menu
                    settitle <new title>        - Set title for menu
                    quit                        - Stop modifying the menu
                    ```
                    """).complete();
    }
}
