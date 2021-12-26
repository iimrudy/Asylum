package eu.asylum.common.mongoserializer;

import eu.asylum.common.utils.Constants;
import org.bson.Document;

public class MongoSerializer {

    public static Document serialize(Object object) {
        return Document.parse(Constants.get().getGson().toJson(object));
    }

    public static <T> T deserialize(Document d, Class<T> clazz) {
        return Constants.get().getGson().fromJson(d.toJson(), clazz);
    }

}
