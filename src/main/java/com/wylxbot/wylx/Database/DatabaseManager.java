package com.wylxbot.wylx.Database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.wylxbot.wylx.Core.WylxEnvConfig;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.wylxbot.wylx.Database.Pojos.*;
import org.bson.Document;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;

import java.util.concurrent.TimeUnit;

import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;
import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class DatabaseManager {

    private static final String POJOS_PACKAGE = "com.wylxbot.wylx.Database.Pojos";

    private final MongoClient client;
    private final DatabaseCollection<DBRoleMenu> roleMenuCollection;
    private final DatabaseCollection<DBServer> serversCollection;
    private final DatabaseCollection<DBUser> usersCollection;
    private final MongoCollection<DBCommandStats> statsCollection;

    public DatabaseManager(WylxEnvConfig config) {
        ConnectionString connStr = new ConnectionString(config.dbURL);
        client = MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(connStr)
                .applicationName("Wylx")
                .applyToConnectionPoolSettings(
                        builder -> builder.maxWaitTime(1000, TimeUnit.MILLISECONDS)
                )
                .build());

        // Combine default codec with our POJOs to represent data objects
        CodecProvider pojoCodecProvider = PojoCodecProvider.builder().register(POJOS_PACKAGE).build();
        CodecRegistry pojoCodecRegistry = fromRegistries(getDefaultCodecRegistry(), fromProviders(pojoCodecProvider));
        MongoDatabase database = client.getDatabase("Wylx").withCodecRegistry(pojoCodecRegistry);

        roleMenuCollection = new DatabaseCollection<>(() -> null, database, "Role Menus", DBRoleMenu.class);
        serversCollection = new DatabaseCollection<>(DBServer::new, database, "Servers", DBServer.class);
        usersCollection = new DatabaseCollection<>(DBUser::new, database, "Users", DBUser.class);
        statsCollection = database.getCollection("Stats", DBCommandStats.class);
    }

    public void readCluster() {
        System.out.println(" --- EXISTING DATABASES ---");
        for(String mongoDatabase : client.listDatabaseNames()) {
            MongoDatabase database = client.getDatabase(mongoDatabase);
            System.out.println(mongoDatabase + ": ");
            for(String mongoCollection : database.listCollectionNames()) {
                System.out.println("\t" + mongoCollection + ": ");
                for(Document document : database.getCollection(mongoCollection).find()) {
                    System.out.println("\t\t" + document.toJson());
                }
            }

        }
        System.out.println(" --- END OF EXISTING DATABASES ---");
    }

    public DBServer getServer(String serverId) {
        return serversCollection.getEntryOrDefault(serverId);
    }

    public void setServer(String serverId, DBServer replace) {
        serversCollection.setEntry(serverId, replace);
    }

    public DBUser getUser(String userID) {
        return usersCollection.getEntryOrDefault(userID);
    }

    public void setUser(String userID, DBUser replace) {
        usersCollection.setEntry(userID, replace);
    }

    public DBRoleMenu getRoleMenu(String messageID) {
        return roleMenuCollection.getEntryOrNull(messageID);
    }

    public void setRoleMenu(String messageID, DBRoleMenu replace) {
        roleMenuCollection.setEntry(messageID, replace);
    }

    public DBCommandStats getCmdStats() {
        var iter = statsCollection.find(eq("_id", DBCommandStats.KEY_ID));
        DBCommandStats stats = iter.first();
        if (stats == null) {
            stats = new DBCommandStats();
            statsCollection.insertOne(stats);
        }

        return stats;
    }

    public void setCmdStats(DBCommandStats stats) {
        statsCollection.replaceOne(
                eq("_id", DBCommandStats.KEY_ID),
                stats,
                new ReplaceOptions().upsert(true));
    }
}
