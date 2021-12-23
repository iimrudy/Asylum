package eu.asylum.common.mongoserializer;

import eu.asylum.common.utils.Constants;
import org.bson.Document;

public class MongoSerializer {

    public static Document serialize(Object object) {

        return Document.parse(Constants.get().getGson().toJson(object));
        /*Document d = new Document();

        for (Field f : object.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            if (f.isAnnotationPresent(MongoSerialized.class)) {
                @NonNull
                MongoSerialized annotation = f.getDeclaredAnnotation(MongoSerialized.class);
                String value;
                if (annotation.value().equals("")) {
                    value = f.getName();
                } else {
                    value = annotation.value();
                }
                try {
                    Object filedObject = f.get(object);
                    if (filedObject != null) {
                        d.append(value, filedObject);

                        if (f.getType().isPrimitive()) { // if is a primitive type
                            d.append(value, f.get(object));
                        } else if (f.getType().isArray()) { // serialized the array, recusrive function
                            Object[] arr = (Object[]) filedObject;
                            List<Document> documentList = new ArrayList<>();
                            for (Object a : arr) {
                                documentList.add(serialize(a));
                            }
                            d.append(value, arr);
                        } else if (f.getType().equals(String.class) || f.getType().isEnum()) { // if is a string or an enum
                            d.append(value, filedObject.toString());
                        } else {
                            Document sub = serialize(filedObject);
                            if (sub.keySet().size() == 0) {
                                if (!filedObject.toString().equals(filedObject.getClass().getName() + "@" + Integer.toHexString(filedObject.hashCode()))) { // is not the default toString
                                    d.append(value, filedObject.toString());
                                }
                            } else {
                                d.append(value, sub);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return d;*/
    }

    public static <T> T deserialize(Document d, Class<T> clazz) {

        return Constants.get().getGson().fromJson(d.toJson(), clazz);
        /*try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object instance = constructor.newInstance();
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);

                @NonNull
                MongoSerialized annotation = f.getDeclaredAnnotation(MongoSerialized.class);
                String value;
                if (annotation.value().equals("")) {
                    value = f.getName();
                } else {
                    value = annotation.value();
                }

                if (f.getType().isEnum()) {
                    Method valueOf = f.getType().getMethod("valueOf", String.class);
                    Object enumValueOf = valueOf.invoke(null, d.getString(value));
                    f.set(instance, enumValueOf);
                } else {
                    Object dObject = d.get(value, f.getType());
                    f.set(instance, dObject);
                }
            }
            return Optional.of(clazz.cast(instance));
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

}
