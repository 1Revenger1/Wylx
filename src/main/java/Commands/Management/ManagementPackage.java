package Commands.Management;

import Core.Events.SilentEvent;
import Core.Commands.ServerCommand;
import Core.ProcessPackage.ProcessPackage;

public class ManagementPackage extends ProcessPackage {

	public ManagementPackage() {
		super(new ServerCommand[] {
				new SystemCommand(),
				new PingCommand(),
				new RepeatCommand(),
				new CleanCommand(),
				new ClearCommand(),
				new UpdateCommand(),
				new RestartCommand(),
				new InviteCommand()},
				new SilentEvent[]{});
	}

	@Override
	public String getHeader() {
		return "Server Management Commands";
	}
}
