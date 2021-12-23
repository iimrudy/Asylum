package eu.asylum.common.configuration;

// simple enum creation :)
public interface IConfigurationEnum {

    public default int getInt(ConfigurationContainer cc) {
        return cc.getInteger(this.getKey(), (int) getDefaultValue());
    }

    public default double getDouble(ConfigurationContainer cc) {
        return cc.getDouble(this.getKey(), (double) getDefaultValue());
    }

    public default boolean getBoolean(ConfigurationContainer cc) {
        return cc.getBoolean(this.getKey(), (boolean) getDefaultValue());
    }

    public default String getString(ConfigurationContainer cc) {
        return cc.getString(this.getKey(), (String) getDefaultValue());
    }

    public default <T> T get(ConfigurationContainer cc, Class<T> c) {
        return (T) cc.get(this.getKey(), c, getDefaultValue());
    }

    public default void set(ConfigurationContainer cc, Object value) {
        cc.setKey(this.getKey(), value);
    }

    public String getKey();

    public Object getDefaultValue();


}
