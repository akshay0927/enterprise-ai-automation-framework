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
                String binary = System.getenv("SHOP_CHROME_BINARY");
                if (binary != null) options.setBinary(binary);
                String executable = System.getenv("SHOP_CHROMEDRIVER");
                if (executable != null) System.setProperty("webdriver.chrome.driver", executable);
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
            if (driver instanceof ChromeDriver chrome) {
                var capabilities = chrome.getCapabilities();
                var metadata = (java.util.Map<?,?>) capabilities.getCapability("chrome");
                String browserVersion = capabilities.getBrowserVersion();
                String driverVersion = String.valueOf(metadata.get("chromedriverVersion")).split(" ")[0];
                if (!browserVersion.split("\\.")[0].equals(driverVersion.split("\\.")[0]))
                    throw new IllegalStateException("Incompatible Chrome " + browserVersion + " and driver " + driverVersion);
                System.out.println("Chrome " + browserVersion + ", ChromeDriver " + driverVersion);
            }
            driver.manage().timeouts().pageLoadTimeout(config.pageTimeout());
            return driver;
        } catch (RuntimeException failure) {
            try { driver.quit(); } catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
}