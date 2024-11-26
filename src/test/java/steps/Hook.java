package steps;

import Base.BaseUtil;
import io.cucumber.java.*;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;  // Updated for ExtentReports 5.x
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chrome.ChromeDriverService;
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
        // Initialize ExtentReports and create a test node for the scenario
        startExtentReports();
        base.scenarioDef = base.extentReports.createTest(scenario.getName()); // Create a node for each scenario in ExtentReports

        // WebDriver setup
        WebDriverManager.chromedriver().setup();  // This line sets up the driver automatically
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage");

        // Setup the ChromeDriverService with logging
        ChromeDriverService service = new ChromeDriverService.Builder()
                .usingDriverExecutable(new File(WebDriverManager.chromedriver().getBinaryPath()))  // Use the chromedriver binary from WebDriverManager
                .usingAnyFreePort()  // Automatically use an available port
                .withLogFile(new File("target/chromedriver_logs.txt")) // Specify the log file for ChromeDriver
                .build();

        // Start the service and pass it to the ChromeDriver
        base.Driver = new ChromeDriver(service, chromeOptions);

        // Implement WebDriverWait to ensure the UserName field is visible before interaction
        WebDriverWait wait = new WebDriverWait(base.Driver, Duration.ofSeconds(10));

        try {
            // Wait for the UserName field to be visible and interactable
            WebElement usernameField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("*[name='UserName']")));

            // Get the username dynamically from Jenkins or default to a static value
            String username = System.getenv("BUILD_USER");  // This pulls the Jenkins build user

            // Fallback for local testing if the environment variable is not set
            if (username == null || username.isEmpty()) {
                username = "defaultTestUser";  // Replace with the default or hardcoded test username
            }

            // Interact with the element after it's visible
            usernameField.sendKeys(username);  // Use the dynamically retrieved username

        } catch (Exception e) {
            System.out.println("Error finding UserName field: " + e.getMessage());
        }
    }

    @After
    public void TearDownTest(Scenario scenario) {
        if (scenario.isFailed()) {
            try {
                // Capture a screenshot if the scenario failed
                File screenshot = ((TakesScreenshot) base.Driver).getScreenshotAs(OutputType.FILE);
                FileUtils.copyFile(screenshot, new File("target/screenshots/" + scenario.getName() + ".png"));
                System.out.println("Screenshot saved for failed scenario: " + scenario.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (base.Driver != null) {
            base.Driver.quit();  // Gracefully quit the browser
        }
        // End the test report
        endExtentReports();
    }

    @BeforeStep
    public void BeforeEveryStep(Scenario scenario) {
        System.out.println("Starting step: " + scenario.getId());
    }

    @AfterStep
    public void AfterEveryStep(Scenario scenario) {
        System.out.println("Finished step: " + scenario.getId());
    }

    // Initialize ExtentReports
    private void startExtentReports() {
        if (base.extentReports == null) {
            // Initialize the Spark reporter for ExtentReports 5.x
            ExtentSparkReporter sparkReporter = new ExtentSparkReporter("target/extent-report.html");
            base.extentReports = new ExtentReports();
            base.extentReports.attachReporter(sparkReporter);
        }
    }

    // End the report after test completion
    private void endExtentReports() {
        if (base.extentReports != null) {
            base.extentReports.flush();  // Ensure that the report is flushed to the file
        }
    }
}
