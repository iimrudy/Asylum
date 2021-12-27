package eu.asylum.cloud;

import eu.asylum.common.cloud.redis.RedisAsylumServerAdd;
import eu.asylum.common.cloud.redis.RedisAsylumServerDelete;
import eu.asylum.common.cloud.redis.RedisAsylumServerUpdate;
import eu.asylum.common.configuration.AsylumConfiguration;
import eu.asylum.common.configuration.ConfigurationContainer;
import eu.asylum.common.configuration.PropertiesConfiguration;
import eu.asylum.common.database.AsylumDB;
import eu.asylum.common.utils.Constants;
import io.lettuce.core.pubsub.RedisPubSubAdapter;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

public class Cloud {
    private static Cloud singleton;


    private final ConfigurationContainer<?> configurationContainer;
    private AsylumDB asylumDB;

    public Cloud() throws Exception {
        Cloud.singleton = this;

        // configuration handling
        File file = new File("./configuration.properties");
        if (!file.exists()) {
            file.createNewFile();
        }
        Properties prop = new Properties();
        prop.load(new FileReader(file));
        this.configurationContainer = new PropertiesConfiguration(prop, file);
        AsylumConfiguration.setConfigurationContainer(this.configurationContainer);

        this.asylumDB = new AsylumDB(AsylumConfiguration.REDIS_URI.getString(), AsylumConfiguration.MONGODB_URI.getString(),
                "asylum_cloud_server_update", "asylum_cloud_server_delete", "asylum_cloud_server_add");
        this.asylumDB.getPubSubConnectionReceiver().addListener(new RedisPubSubAdapter<>() {
            @Override
            public void message(String channel, String message) {
                if (channel.equals("asylum_cloud_server_update")) {
                    // a server, send his own information, ram usage, online players, tps, etc
                    var update = Constants.get().getGson().fromJson(message, RedisAsylumServerUpdate.class);
                    System.out.println(update);
                } else if (channel.equals("asylum_cloud_server_delete")) {
                    var delete = Constants.get().getGson().fromJson(message, RedisAsylumServerDelete.class);
                } else if (channel.equals("asylum_cloud_server_add")) {
                    var add = Constants.get().getGson().fromJson(message, RedisAsylumServerAdd.class);
                }
            }
        });
    }

    // Thread-Safe singleton getter.
    public static synchronized Cloud getInstance() {
        if (singleton == null) {
            synchronized (Cloud.class) {
                if (singleton == null) {
                    try {
                        new Cloud();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return singleton;
    }

}
