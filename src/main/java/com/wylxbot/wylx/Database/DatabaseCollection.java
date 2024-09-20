package com.wylxbot.wylx.Database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.conversions.Bson;

import java.util.function.Function;
import java.util.function.Supplier;

import static com.mongodb.client.model.Filters.eq;

public class DatabaseCollection<T extends CollectionPojo> {
    private final Supplier<T> defaultSupplier;
    private final MongoCollection<T> collection;

    public DatabaseCollection(Supplier<T> defaultSupplier, MongoDatabase db, String name, Class<T> collectionType) {
        this.defaultSupplier = defaultSupplier;
        this.collection = db.getCollection(name, collectionType);
    }

    public T getEntryOrNull(String id) {
        var iter = collection.find(eq("_id", id));
        return iter.first();
    }

    public T getEntryOrDefault(String id) {
        T entry = getEntryOrNull(id);
        if (entry == null) {
            entry = defaultSupplier.get();
            entry._id = id;
            collection.insertOne(entry);
        }

        return entry;
    }

    public void setEntry(String id, T entry) {
        Bson filter = eq("_id", id);
        ReplaceOptions opts = new ReplaceOptions().upsert(true);
        entry._id = id;
        collection.replaceOne(filter, entry, opts);
    }
}
