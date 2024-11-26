import Base.BaseUtil;
import io.cucumber.java.*;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

public class Hook extends BaseUtil {

    private BaseUtil base;
    private ExtentTest features;

    public Hook(BaseUtil base) {
        this.base = base;
    }

    @Before
    public void InitializeTest(Scenario scenario) {
        // Initialize ExtentReports if not done already
        if (base.extentReports == null) {
            base.extentReports = new ExtentReports();
        }

        base.scenarioDef = base.extentReports.createTest(scenario.getName()); // Create a node for each scenario

        // Setup WebDriver with WebDriverManager
        WebDriverManager.chromedriver().setup();
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage");

        ChromeDriverService service = new ChromeDriverService.Builder()
                .usingAnyFreePort()
                .withLogFile(new File("target/chromedriver_logs.txt"))
                .build();

        base.Driver = new ChromeDriver(service, chromeOptions);

        // Implement WebDriverWait for dynamic elements
        WebDriverWait wait = new WebDriverWait(base.Driver, Duration.ofSeconds(10));

        try {
            WebElement usernameField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("*[name='UserName']")));
            String username = System.getenv("BUILD_USER");  // From Jenkins or local fallback
            if (username == null || username.isEmpty()) {
                username = "defaultTestUser";
            }
            usernameField.sendKeys(username);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    @After
    public void TearDownTest(Scenario scenario) {
        if (scenario.isFailed()) {
            try {
                File screenshot = ((TakesScreenshot) base.Driver).getScreenshotAs(OutputType.FILE);
                FileUtils.copyFile(screenshot, new File("target/screenshots/" + scenario.getName() + ".png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (base.Driver != null) {
            base.Driver.quit();
        }
        // End the report
        if (base.extentReports != null) {
            base.extentReports.flush();
        }
    }
}
