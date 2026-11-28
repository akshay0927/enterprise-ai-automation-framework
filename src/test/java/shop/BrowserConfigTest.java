package shop;

import java.util.Map;
import java.util.Properties;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class BrowserConfigTest {
    @Test public void configurationPrecedence() {
        Properties profile = new Properties();
        profile.setProperty("browser", "edge");
        profile.setProperty("page.seconds", "45");
        var config = BrowserConfig.parse(Map.of("shop.browser","chrome")::get,
            Map.of("SHOP_BROWSER","firefox","SHOP_HEADLESS","false")::get, profile);
        assertEquals(config.browser(), BrowserConfig.Browser.CHROME);
        assertFalse(config.headless());
        assertEquals(config.pageTimeout().toSeconds(), 45L);
        assertEquals(config.waitTimeout().toSeconds(), 10L);
    }

    @Test public void rejectsInvalidConfiguration() {
        for (String key : new String[]{"shop.browser","shop.headless","shop.wait.seconds","shop.page.seconds"})
            expectThrows(IllegalArgumentException.class, () ->
                BrowserConfig.parse(Map.of(key, "0")::get, k -> null, new Properties()));
    }
}