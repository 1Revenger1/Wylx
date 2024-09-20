package com.wylxbot.wylx.Commands.ServerSettings;

import com.wylxbot.wylx.Core.Events.Commands.CommandContext;
import com.wylxbot.wylx.Core.Events.Commands.ServerCommand;
import com.wylxbot.wylx.Core.Events.ServerEventManager;
import com.wylxbot.wylx.Core.Processing.MessageProcessing;
import net.dv8tion.jda.api.Permission;

import java.util.Locale;

public class EnableCommand extends ServerCommand {
	public EnableCommand() {
		super("enablecommand", CommandPermission.DISCORD_PERM, Permission.ADMINISTRATOR, """
				Enable and disable specific commands
				Usage: %{p}enablecommand <command_name> <true or false>
				""", "command");
	}

	@Override
	public void runCommand(CommandContext ctx) {
		if(ctx.args().length != 3){
			ctx.event().getChannel().sendMessage(getDescription(ctx.prefix())).queue();
			return;
		}

		ServerEventManager eventManager = ServerEventManager.getServerEventManager(ctx.event().getGuild().getId());
		String eventName;

		try{
			eventName = MessageProcessing.eventMap.get(ctx.args()[1].toLowerCase()).getKeyword().toLowerCase(Locale.ROOT);;
			boolean value = Boolean.parseBoolean(ctx.args()[2]);
			eventManager.setEvent(eventName, value);
		} catch (IllegalArgumentException e){
			ctx.event().getChannel().sendMessage(e.getMessage()).queue();
			return;
		}

		ctx.event().getChannel().sendMessage("Set \"" + eventName + "\" to " + Boolean.parseBoolean(ctx.args()[2])).queue();
	}
}
