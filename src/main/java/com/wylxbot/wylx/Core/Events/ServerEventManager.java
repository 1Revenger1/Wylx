package com.wylxbot.wylx.Core.Events;

import com.wylxbot.wylx.Core.Processing.MessageProcessing;
import com.wylxbot.wylx.Database.Pojos.DBServer;
import com.wylxbot.wylx.Wylx;
import com.wylxbot.wylx.Database.DatabaseManager;

import java.util.HashMap;
import java.util.Map;

public class ServerEventManager {

	// List of all process packages copied from message processing
	private static final EventPackage[] eventPackages = MessageProcessing.eventPackages;

	private static final DatabaseManager db = Wylx.getInstance().getDb();

	// Static getting of events manager for a server
	public static ServerEventManager getServerEventManager(String id) {
		DBServer servedDB = db.getServer(id);
		// Try to find cached manager for the server
		return new ServerEventManager(id, servedDB.enabledModules, servedDB.exceptions);
	}

	// Map of enabled and disabled modules
	private final Map<String, Boolean> moduleMap;
	// Map of events that are exceptions to their modules
	private final Map<String, Boolean> eventExceptionMap;
	private final String id;

	public ServerEventManager(String id, Map<String, Boolean> moduleMap, Map<String, Boolean> exceptions) {
		this.moduleMap = moduleMap;
		this.eventExceptionMap = exceptions;
		this.id = id;
	}

	public boolean checkEvent(Event event){
		String eventName = event.getKeyword().toLowerCase();
		Boolean except = eventExceptionMap.get(eventName);
		if (except != null) return except;

		String moduleName = MessageProcessing.commandToModuleMap.get(eventName);
		Boolean moduleEnabled = moduleMap.get(moduleName);
		if (moduleEnabled != null) return moduleEnabled;
		return true;
	}

	public boolean checkPackage(EventPackage eventPackage) {
		Boolean moduleEnabled = moduleMap.get(eventPackage.getName().toLowerCase());
		if (moduleEnabled != null) return moduleEnabled;
		return true;
	}

	public void setModule(String moduleName, boolean value) throws IllegalArgumentException{
		// find the actual class for the module
		EventPackage module = null;
		for(EventPackage eventPackage : eventPackages){
			if(eventPackage.getName().toLowerCase().equals(moduleName))
				module = eventPackage;
		}

		// If the module doesnt exist we cant set anything with it
		if(module == null) throw new IllegalArgumentException("Specified module does not exist");

		// if the values are already the same do nothing
		Boolean currentValue = moduleMap.get(moduleName);
		if(currentValue != null && currentValue == value) return;

		// Write the value to the module map
		moduleMap.put(moduleName, value);

		for(Event event : module.getEvents()){
			String eventName = event.getKeyword().toLowerCase();

			// If there was an exception for this event remove that exception
			eventExceptionMap.remove(eventName);
		}

		// Write the new info to mongodb
		DBServer server = db.getServer(id);
		server.enabledModules = moduleMap;
		server.exceptions = eventExceptionMap;
		db.setServer(id, server);
	}

	public void setEvent(String eventName, boolean value) throws IllegalArgumentException{
		// If the event doesn't exist we can set anything with it
		if(!MessageProcessing.commandToModuleMap.containsKey(eventName))
			throw new IllegalArgumentException("Specified event: `" + eventName +  "` does not exist");

		// If the values are already the same do nothing
		Boolean currentValue = eventExceptionMap.get(eventName);
		if(currentValue != null && currentValue == value) return;

		// Write changes to the exceptions
		eventExceptionMap.put(eventName, value);

		// Write changes to mongodb
		DBServer server = db.getServer(id);
		server.enabledModules = moduleMap;
		server.exceptions = eventExceptionMap;
		db.setServer(id, server);
	}
}
