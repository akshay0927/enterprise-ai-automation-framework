package shop;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.edge.*;
import org.openqa.selenium.firefox.*;

final class DriverFactory {
    static WebDriver open(BrowserConfig config) {
        WebDriver driver = switch (config.browser()) {
            case CHROME -> {
                var options = new ChromeOptions();
                if (config.headless()) options.addArguments("--headless=new");
                yield new ChromeDriver(options);
            }
            case EDGE -> {
                var options = new EdgeOptions();
                if (config.headless()) options.addArguments("--headless=new");
                yield new EdgeDriver(options);
            }
            case FIREFOX -> {
                var options = new FirefoxOptions();
                if (config.headless()) options.addArguments("-headless");
                yield new FirefoxDriver(options);
            }
        };
        try {
            driver.manage().timeouts().pageLoadTimeout(config.pageTimeout());
            return driver;
        } catch (RuntimeException failure) {
            try { driver.quit(); } catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
}