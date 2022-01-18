package Core.Processing;

import Commands.DND.DNDPackage;
import Commands.Management.ManagementPackage;
import Core.Commands.CommandPackage;
import Core.Commands.ServerCommand;
import Core.Events.SilentEvent;
import Core.Main;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class MessageProcessing extends ListenerAdapter {
	private static final CommandPackage[] commandPackages = {new ManagementPackage(), new DNDPackage()};
	private static final HashMap<String, ServerCommand> commandMap = new HashMap<>();
	private static final ArrayList<SilentEvent> events = new ArrayList<>();

	private static final Logger logger = LoggerFactory.getLogger(MessageProcessing.class);

	static {
		for(CommandPackage commandPackage : commandPackages){
			for(ServerCommand command : commandPackage.getCommands()){
				commandMap.put(command.getKeyword(), command);
			}
			events.addAll(Arrays.asList(commandPackage.getEvents()));
		}
	}

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		//Ignore messages from the bot
		if(event.getAuthor().getIdLong() == Main.getBotID()) return;

		String prefix = "$"; // TODO: Get as server preference
		String msg = event.getMessage().getContentRaw();

		if(!event.isFromGuild()) return;
		// Check Commands if aimed at bot
		if (msg.startsWith(prefix)) {
			String[] args = msg.split(" ");
			String commandString = args[0].toLowerCase().replace(prefix, "");
			ServerCommand command = commandMap.get(commandString);

			if (command != null) {
				if(command.checkPermission(event)) {
					logger.debug("Command ({}) Called With {} Args", commandString, args.length);
					command.runCommand(event, args);
					return;
				} else {
					event.getMessage().reply("You don't have permission to use this command!").queue();
				}
			}
		}
		for(SilentEvent silentEvent : events){
			if(silentEvent.check(event)){
				silentEvent.runEvent(event);
				return;
			}
		}
	}
}
