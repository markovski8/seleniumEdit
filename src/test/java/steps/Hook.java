package steps;

import Base.BaseUtil;
import io.cucumber.java.*;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.chrome.ChromeDriverService;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Hook extends BaseUtil {

    private BaseUtil base;
    private ExtentTest features;
    private WebDriverWait wait;  // Make WebDriverWait reusable

    public Hook(BaseUtil base) {
        this.base = base;
    }

    @Before
    public void InitializeTest(Scenario scenario) {
        startExtentReports();
        base.extentReports.createTest(scenario.getName());  // Create a test in extentReports

        // WebDriver setup
        WebDriverManager.chromedriver().setup();  // Use WebDriverManager to set up the driver
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage");

        // Setup the ChromeDriverService with logging
        ChromeDriverService service = new ChromeDriverService.Builder()
                .usingDriverExecutable(new File(WebDriverManager.chromedriver().getBinaryPath()))  // Using binary from WebDriverManager
                .usingAnyFreePort()
                .withLogFile(new File("target/chromedriver_logs.txt"))
                .build();

        // Start the service and pass it to ChromeDriver
        base.Driver = new ChromeDriver(service, chromeOptions);

        // Initialize WebDriverWait here to be reused
        wait = new WebDriverWait(base.Driver, Duration.ofSeconds(10));

        try {
            WebElement usernameField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("*[name='UserName']")));

            String username = System.getenv("BUILD_USER");  // Retrieve username dynamically from Jenkins

            // Fallback for local testing if BUILD_USER is not set
            if (username == null || username.isEmpty()) {
                username = "defaultTestUser";  // Static test username for local environment
            }

            // Interact with the username field after it's visible
            usernameField.sendKeys(username);

        } catch (Exception e) {
            System.out.println("Error finding UserName field: " + e.getMessage());
        }
    }

    @After
    public void TearDownTest(Scenario scenario) {
        if (scenario.isFailed()) {
            try {
                // Capture screenshot for failed scenario
                File screenshot = ((TakesScreenshot) base.Driver).getScreenshotAs(OutputType.FILE);
                FileUtils.copyFile(screenshot, new File("target/screenshots/" + scenario.getName() + ".png"));
                System.out.println("Screenshot saved for failed scenario: " + scenario.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (base.Driver != null) {
            base.Driver.quit();  // Quit the WebDriver
        }
        endExtentReports();
    }

    // Initialize ExtentReports
    private void startExtentReports() {
        if (base.extentReports == null) {
            // Setup ExtentReports with the Spark reporter
            ExtentSparkReporter sparkReporter = new ExtentSparkReporter("target/extent-report.html");
            base.extentReports = new ExtentReports();
            base.extentReports.attachReporter(sparkReporter);  // Attach the reporter to the report
        }
    }

    // End the ExtentReports
    private void endExtentReports() {
        if (base.extentReports != null) {
            base.extentReports.flush();  // Make sure to flush the report at the end
        }
    }
}
