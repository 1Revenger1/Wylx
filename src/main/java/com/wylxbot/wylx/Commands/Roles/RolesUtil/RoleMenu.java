package com.wylxbot.wylx.Commands.Roles.RolesUtil;

import com.wylxbot.wylx.Database.Pojos.DBRoleMenu;
import com.wylxbot.wylx.Database.Pojos.DBRoleMenuRole;
import com.wylxbot.wylx.Wylx;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoleMenu {
    private String title;

    private final String guildID;
    private final String messageID;
    private final String channelID;

    private final Message message;

    private final ArrayList<RoleReaction> reactions = new ArrayList<>();

    public RoleMenu(DBRoleMenu dbEntry) {
        this(dbEntry.title, dbEntry._id, dbEntry.channelId, dbEntry.guildId, dbEntry.roles);
    }

    public RoleMenu(String messageID, String channelID, String guildID) throws IllegalArgumentException, ErrorResponseException {
        this("Role Selection", messageID, channelID, guildID, null);
        updateMessage();
    }

    private RoleMenu(String title, String messageID, String channelID, String guildID, Map<String, DBRoleMenuRole> roles) throws IllegalArgumentException, ErrorResponseException {
        this.title = title;
        this.messageID = messageID;
        this.channelID = channelID;
        this.guildID = guildID;

        JDA jda = Wylx.getInstance().getJDA();
        TextChannel channel = jda.getTextChannelById(channelID);
        if (channel == null) {
            throw new IllegalArgumentException("Channel does not exist");
        }

        message = channel.retrieveMessageById(messageID).complete();

        if (roles == null) {
            return;
        }

        boolean updateMenu = false;

        for (var entry : roles.entrySet()) {
            String roleID = entry.getKey();
            DBRoleMenuRole emojiInput = entry.getValue();

            Role role = jda.getRoleById(roleID);
            // Remove if invalid
            if (role == null) {
                updateMenu = true;
                continue;
            }

            EmojiUnion emoji;
            if (!emojiInput.isUnicode()) {
                RichCustomEmoji customEmoji = jda.getEmojiById(emojiInput.emojiStr());
                if (customEmoji == null) {
                    updateMenu = true;
                    continue;
                }
                emoji = (EmojiUnion) customEmoji;
            } else {
                // Unicode
                emoji = Emoji.fromFormatted(emojiInput.emojiStr());
            }

            reactions.add(new RoleReaction(role, emoji));
        }

        if (updateMenu) {
            updateMessage();
        }
    }

    public static MessageEmbed getEmptyEmbed() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor("Role Selection");
        builder.setDescription("React to get a role!\n\nThis list is currently empty. To add roles, please use TODO");
        return builder.build();
    }

    public MessageEmbed getEmbed(boolean mentionRoles) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(this.title);
        StringBuilder string = new StringBuilder("React to get a role!\n\n");

        if (reactions.size() == 0) {
            String prefix = Wylx.getInstance().getDb().getServer(guildID).prefix;
            string.append("This list is currently empty. To add roles, please use\n`");
            string.append(prefix);
            string.append("modifyRoleMenu ");
            string.append(this.messageID);
            string.append("`\nto modify this menu.");
        } else {
            reactions.forEach(reaction -> {
                String roleStr = mentionRoles ? reaction.role().getAsMention() : reaction.role().getName();
                String line = String.format("%s - %s\n",
                        reaction.emoji().getFormatted(),
                        roleStr);
                string.append(line);
            });
        }

        builder.setDescription(string.toString());
        builder.setFooter("Message ID: " + this.messageID);
        return builder.build();
    }

    public String getGuildID() {
        return guildID;
    }

    public String getMessageID() {
        return messageID;
    }

    public List<RoleReaction> getReactions() {
        return reactions;
    }

    public void setTitle(String title) {
        this.title = title;
        updateMessage();
    }

    private void updateMessage() {
        message.editMessageEmbeds(getEmbed(true)).queue();
    }

    public void addReaction(RoleReaction newReaction) throws IllegalArgumentException {
        for (RoleReaction reaction : reactions) {
            if (reaction.role().equals(newReaction.role())) {
                throw new IllegalArgumentException("Role already exists in menu");
            } else if (reaction.emoji().equals(newReaction.emoji())) {
                throw new IllegalArgumentException("Emoji already being used in menu");
            }
        }

        try {
            message.addReaction(newReaction.emoji()).complete();
        } catch (ErrorResponseException e) {
            switch(e.getErrorResponse()) {
                case TOO_MANY_REACTIONS ->
                        throw new IllegalArgumentException("Discord has a max limit of 20 unique emojis per message." +
                                " Please create a new menu or remove other roles from this menu.");
                case UNKNOWN_EMOJI -> throw new IllegalArgumentException("Emoji does not exist!");
                case UNKNOWN_MESSAGE -> throw new IllegalArgumentException("Message was deleted!");
                case MISSING_PERMISSIONS -> throw new IllegalArgumentException("I do not have permission to react" +
                        "to the menu!");
                default -> throw new IllegalArgumentException(e.getMessage());
            }
        }

        reactions.add(newReaction);
        updateMessage();
    }

    public void removeReaction(String name) throws IllegalArgumentException {
        List<RoleReaction> filtered = reactions.stream().filter(roleReaction -> roleReaction.role().getName().equalsIgnoreCase(name)).toList();
        if (filtered.size() != 1) {
            throw new IllegalArgumentException("Could not find role to remove");
        }

        RoleReaction reaction = filtered.get(0);
        reactions.remove(reaction);

        try {
            message.clearReactions(reaction.emoji()).queue();
        } catch (ErrorResponseException e) {
            switch(e.getErrorResponse()) {
                case UNKNOWN_MESSAGE -> throw new IllegalArgumentException("Message was deleted!");
                case MISSING_ACCESS -> throw new IllegalArgumentException("Lost permissions to view role menu!");
            }
        }

        updateMessage();
    }

    public Role getReactionFromEmote(Emoji emoji) {
        for (RoleReaction reaction : reactions) {
            if (reaction.emoji().getAsReactionCode().equals(emoji.getAsReactionCode())) {
                return reaction.role();
            }
        }
        return null;
    }

    public DBRoleMenu getDBEntry() {
        Map<String, DBRoleMenuRole> roles = new HashMap<>();
        reactions.forEach(role -> {
            boolean isUnicode = role.emoji().getType() == Emoji.Type.UNICODE;
            String id = isUnicode ? role.emoji().getName() : role.emoji().asCustom().getId();
            roles.put(role.role().getId(), new DBRoleMenuRole(isUnicode, id));
        });

        DBRoleMenu ret = new DBRoleMenu();
        ret._id = messageID;
        ret.channelId = channelID;
        ret.guildId = guildID;
        ret.title = title;
        ret.roles = roles;
        return ret;
    }
}
