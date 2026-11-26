package shop;

import java.time.Duration;
import java.util.Locale;
import java.util.function.Function;
import java.util.Properties;

record BrowserConfig(Browser browser, boolean headless, Duration waitTimeout, Duration pageTimeout) {
    enum Browser { CHROME, FIREFOX, EDGE }

    static BrowserConfig load() {
        String environment = System.getProperty("shop.environment", System.getenv().getOrDefault("SHOP_ENVIRONMENT", "local"));
        if (!environment.matches("[a-zA-Z0-9_-]+")) throw new IllegalArgumentException("Invalid environment");
        Properties defaults = new Properties();
        try (var input = BrowserConfig.class.getResourceAsStream("/environments/" + environment + ".properties")) {
            if (input != null) defaults.load(input);
            else if (!environment.equals("local")) throw new IllegalArgumentException("Unknown environment: " + environment);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot load environment configuration", e);
        }
        return parse(System::getProperty, System::getenv, defaults);
    }

    static BrowserConfig parse(Function<String,String> properties, Function<String,String> env, Properties profile) {
        Function<String,String> value = key -> {
            String setting = properties.apply("shop." + key);
            if (setting == null) setting = env.apply("SHOP_" + key.toUpperCase(Locale.ROOT).replace('.', '_'));
            return setting == null ? profile.getProperty(key) : setting;
        };
        String flag = java.util.Objects.requireNonNullElse(value.apply("headless"), "true");
        if (!flag.equalsIgnoreCase("true") && !flag.equalsIgnoreCase("false"))
            throw new IllegalArgumentException("headless must be true or false");
        return new BrowserConfig(Browser.valueOf(java.util.Objects.requireNonNullElse(value.apply("browser"), "chrome").toUpperCase(Locale.ROOT)),
            Boolean.parseBoolean(flag), seconds(value.apply("wait.seconds"), 10), seconds(value.apply("page.seconds"), 30));
    }

    private static Duration seconds(String setting, int fallback) {
        int count = setting == null ? fallback : Integer.parseInt(setting);
        if (count < 1 || count > 300) throw new IllegalArgumentException("Timeout must be 1 through 300 seconds");
        return Duration.ofSeconds(count);
    }
}