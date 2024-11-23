package steps;

import Base.BaseUtil;
import io.cucumber.java.*;
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

    public Hook(BaseUtil base) {
        this.base = base;
    }

    @Before
    public void InitializeTest(Scenario scenario) {
        base.scenarioDef = base.features.createNode(scenario.getName());
        WebDriverManager.chromedriver().setup();
        
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage");
        
        base.Driver = new ChromeDriver(chromeOptions);

        ChromeDriverService service = new ChromeDriverService.Builder()
    .withLogOutput(System.out)
    .build();
WebDriver driver = new ChromeDriver(service);

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
    }

    @BeforeStep
    public void BeforeEveryStep(Scenario scenario) {
        System.out.println("Starting step: " + scenario.getId());
    }

    @AfterStep
    public void AfterEveryStep(Scenario scenario) {
        System.out.println("Finished step: " + scenario.getId());
    }
}
