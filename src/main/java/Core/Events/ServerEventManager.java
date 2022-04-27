package Core.Events;

import Core.Processing.MessageProcessing;
import Core.Wylx;
import Database.DbCollection;
import Database.DbManager;
import Database.ServerIdentifiers;
import com.mongodb.connection.ServerId;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.HashMap;
import java.util.Map;

public class ServerEventManager {

	// List of all process packages copied from message processing
	private static final EventPackage[] eventPackages = MessageProcessing.eventPackages;
	// Keep managers in cache so they don't have to be loaded from the db for every events (string is server id)
	private static final HashMap<String, ServerEventManager> cachedManagers = new HashMap<>();

	private static final DbManager db = Wylx.getInstance().getDb();

	// Static getting of events manager for a server
	public static ServerEventManager getServerEventManager(String id){
		// Try to find cached manager for the server
		ServerEventManager manager = cachedManagers.get(id);

		// If not cached load the manager from db and cache
		if(manager == null) {
			DbCollection<ServerIdentifiers> discordServer = db.getServerCollection();
			manager = discordServer.getSetting(id, ServerIdentifiers.Modules);
			cachedManagers.put(id, manager);
		}

		return manager;
	}

	// Map used for making comparisons as events are being run
	private final HashMap<String, Boolean> masterEventMap = new HashMap<>();
	// Map of enabled and disabled modules
	private final HashMap<String, Boolean> moduleMap = new HashMap<>();
	// Map of events that are exceptions to their modules
	private final HashMap<String, Boolean> eventExceptionMap = new HashMap<>();

	public ServerEventManager() {
		fillDefaults();
	}

	public boolean checkEvent(Event event){
		return checkEvent(event.getClass().getSimpleName().toLowerCase());
	}

	public boolean checkEvent(String eventName){
		if(masterEventMap.size() == 0) fillDefaults();
		return masterEventMap.get(eventName);
	}

	public Boolean checkPackage(EventPackage eventPackage) {
		return checkPackage(eventPackage.getClass().getSimpleName().toLowerCase());
	}

	public Boolean checkPackage(String packageName){
		return moduleMap.get(packageName);
	}

	public void setModule(String moduleName, boolean value) throws IllegalArgumentException{
		// find the actual class for the module
		EventPackage module = null;
		for(EventPackage eventPackage : eventPackages){
			if(eventPackage.getClass().getSimpleName().toLowerCase().equals(moduleName))
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
			String eventName = event.getClass().getSimpleName().toLowerCase();

			// Write changes to the map so the change to enabled status is reflected
			masterEventMap.put(eventName, value);

			// If there was an exception for this event remove that exception
			eventExceptionMap.remove(eventName);
		}
	}

	public void setEvent(String eventName, boolean value) throws IllegalArgumentException{
		// If the event doesn't exist we can set anything with it
		if(!masterEventMap.containsKey(eventName)) throw new IllegalArgumentException("Specified event: `" + eventName +  "` does not exist");

		// If the values are already the same do nothing
		Boolean currentValue = masterEventMap.get(eventName);
		if(currentValue != null && currentValue == value) return;

		// Write changes to the master map
		masterEventMap.replace(eventName, value);

		// If the exception already exists, an exception is no longer needed
		// Otherwise add an exception
		if(eventExceptionMap.containsKey(eventName)){
			eventExceptionMap.remove(eventName);
		} else {
			eventExceptionMap.put(eventName, value);
		}
	}

	private void fillDefaults(){
		for(EventPackage module : eventPackages){
			String moduleName = module.getClass().getSimpleName().toLowerCase();

			// Load the module that didn't exist
			setModule(moduleName, true);
		}
	}
}
