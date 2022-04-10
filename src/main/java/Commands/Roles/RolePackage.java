package Commands.Roles;

import Core.Events.Commands.ServerCommand;
import Core.Events.EventPackage;
import Core.Events.SilentEvents.SilentEvent;

public class RolePackage extends EventPackage {

    public RolePackage() {
        super(
                new ServerCommand[]{
                        new AddRoleCommand(),
                        new RoleCommand()
                }, new SilentEvent[]{}
        );
    }

    @Override
    public String getHeader() {
        return "Have users self-assign roles";
    }
}
