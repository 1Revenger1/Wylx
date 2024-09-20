package com.wylxbot.wylx.Core.Processing;

import com.wylxbot.wylx.Commands.Roles.RolesUtil.RoleMenu;
import com.wylxbot.wylx.Database.Pojos.DBRoleMenu;
import com.wylxbot.wylx.Wylx;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;

public class ReactionProcessing extends ListenerAdapter {

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        Role role = checkReactionForMenu(event);
        if (role == null) {
            return;
        }

        Member user = event.retrieveMember().complete();
        Guild guild = event.getGuild();
        try {
            guild.addRoleToMember(user, role).complete();
        } catch (ErrorResponseException e) {
            if (e.getErrorResponse() == ErrorResponse.UNKNOWN_ROLE) {
                removeRole(event, role);
                dmErrorMessage(user, role, ReactionActionType.Assign, ReactionFailureReason.UnknownRole);
            }
        } catch (HierarchyException e) {
            // Attempt to DM user that an error occurred
            dmErrorMessage(user, role, ReactionActionType.Assign, ReactionFailureReason.Hierarchy);
        }
    }

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
        Role role = checkReactionForMenu(event);
        if (role == null) {
            return;
        }

        Member user = event.retrieveMember().complete();
        Guild guild = event.getGuild();
        try {
            guild.removeRoleFromMember(user, role).complete();
        } catch (ErrorResponseException e) {
            if (e.getErrorResponse() == ErrorResponse.UNKNOWN_ROLE) {
                removeRole(event, role);
                dmErrorMessage(user, role, ReactionActionType.Remove, ReactionFailureReason.UnknownRole);
            }
        } catch (HierarchyException e) {
            // Attempt to DM user that an error occurred
            dmErrorMessage(user, role, ReactionActionType.Remove, ReactionFailureReason.Hierarchy);
        }
    }


    private enum ReactionActionType {
        Assign,
        Remove,
    }

    private enum ReactionFailureReason {
        Hierarchy,
        UnknownRole
    }

    private void dmErrorMessage(Member user, Role role, ReactionActionType type, ReactionFailureReason reason) {
        Guild guild = user.getGuild();
        String typeStr = switch(type) {
            case Assign -> "assign";
            case Remove -> "remove";
        };

        String reasonStr = switch(reason) {
            case Hierarchy -> "is above me in the role list";
            case UnknownRole -> "does not exist anymore";
        };

        String msg = MessageFormat.format(
                "Was unable to {0} the role `@{1}` from `{2}`. " +
                "This is because `@{1}` {3}.\n" +
                "Please contact an admin from `{2}` to fix this.",
                typeStr, role.getName(), guild.getName(), reasonStr);

        user.getUser().openPrivateChannel().queue((var thread) ->
                thread.sendMessage(msg).queue(null,
                        new ErrorHandler().ignore(ErrorResponse.CANNOT_SEND_TO_USER))
        );
    }

    private void removeRole(@NotNull GenericMessageReactionEvent event, Role roleToRemove) {
        DBRoleMenu roleDb = Wylx.getInstance().getDb().getRoleMenu(event.getMessageId());
        RoleMenu menu = new RoleMenu(roleDb);
        menu.removeReaction(roleToRemove.getName());
        Wylx.getInstance().getDb().setRoleMenu(event.getMessageId(), menu.getDBEntry());
    }

    private Role checkReactionForMenu(@NotNull GenericMessageReactionEvent event) {
        User selfUser = event.getJDA().getSelfUser();

        // Don't give roles to Wylx
        if (event.getUserIdLong() == selfUser.getIdLong()) {
            return null;
        }

        // Check for menu
        DBRoleMenu roleDb = Wylx.getInstance().getDb().getRoleMenu(event.getMessageId());
        if (roleDb == null) {
            return null;
        }

        EmojiUnion emoji = event.getEmoji();
        RoleMenu menu = new RoleMenu(roleDb);
        return menu.getReactionFromEmote(emoji);
    }
}
