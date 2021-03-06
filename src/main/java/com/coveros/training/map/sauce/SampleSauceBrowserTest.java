package com.coveros.training.map.sauce;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.LinkedList;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.coveros.training.SauceProperties;
import com.saucelabs.common.SauceOnDemandAuthentication;
import com.saucelabs.common.SauceOnDemandSessionIdProvider;
import com.saucelabs.junit.ConcurrentParameterized;
import com.saucelabs.junit.SauceOnDemandTestWatcher;
import com.saucelabs.saucerest.SauceREST;

/**
 * Demonstrates how to write a JUnit test that runs tests against Sauce Labs
 * using multiple browsers in parallel.
 * <p/>
 * The test also includes the {@link SauceOnDemandTestWatcher} which will invoke
 * the Sauce REST API to mark the test as passed or failed.
 *
 * @author Ross Rowe
 */
@RunWith(ConcurrentParameterized.class)
public class SampleSauceBrowserTest implements SauceOnDemandSessionIdProvider {

	@Rule
	public TestName testName = new TestName();

	@Rule
	public TestWatcher failureRule = new TestWatcher() {

		@Override
		protected void failed(Throwable e, Description description) {
			sauceRestApi.jobFailed(getSessionId());
		}

	};
	
	/**
	 * Constructs a {@link SauceOnDemandAuthentication} instance using the supplied
	 * user name/access key. To use the authentication supplied by environment
	 * variables or from an external file, use the no-arg
	 * {@link SauceOnDemandAuthentication} constructor.
	 */
	public SauceOnDemandAuthentication authentication = new SauceOnDemandAuthentication(
			SauceProperties.getString(SauceProperties.USER_NAME),
			SauceProperties.getString(SauceProperties.ACCESS_KEY));
	
	/**
	 * Represents the browser to be used as part of the test run.
	 */
	private String browser;
	/**
	 * Represents the operating system to be used as part of the test run.
	 */
	private String os;
	/**
	 * Represents the version of the browser to be used as part of the test run.
	 */
	private String version;
	/**
	 * Instance variable which contains the Sauce Job Id.
	 */
	private String sessionId;

	/**
	 * The {@link WebDriver} instance which is used to perform browser interactions
	 * with.
	 */
	private WebDriver driver;
	private SauceREST sauceRestApi;

	/**
	 * Constructs a new instance of the test. The constructor requires three string
	 * parameters, which represent the operating system, version and browser to be
	 * used when launching a Sauce VM. The order of the parameters should be the
	 * same as that of the elements within the {@link #browsersStrings()} method.
	 * 
	 * @param os
	 * @param version
	 * @param browser
	 */
	public SampleSauceBrowserTest(String os, String version, String browser) {
		super();
		this.os = os;
		this.version = version;
		this.browser = browser;
	}

	/**
	 * @return a LinkedList containing String arrays representing the browser
	 *         combinations the test should be run against. The values in the String
	 *         array are used as part of the invocation of the test constructor
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@ConcurrentParameterized.Parameters
	public static LinkedList browsersStrings() {
		LinkedList browsers = new LinkedList();

		browsers.add(new String[] { "Windows 10", "latest", "chrome" });
		browsers.add(new String[] { "Windows 10", "70.0", "firefox" });
		browsers.add(new String[] { "macOS 10.14", "12.0", "safari" });

		return browsers;
	}

	/**
	 * Constructs a new {@link RemoteWebDriver} instance which is configured to use
	 * the capabilities defined by the {@link #browser}, {@link #version} and
	 * {@link #os} instance variables, and which is configured to run against
	 * ondemand.saucelabs.com, using the username and access key populated by the
	 * {@link #authentication} instance.
	 *
	 * @throws Exception if an error occurs during the creation of the
	 *                   {@link RemoteWebDriver} instance.
	 */
	@Before
	public void setUp() throws Exception {

		MutableCapabilities sauceOptions = new MutableCapabilities();
		sauceOptions.setCapability("name", testName.getMethodName() + " test - " + os + ": " + browser + " " + version);

		MutableCapabilities browserOptions = getBaseBrowserOptions(this.browser);

		if (version != null) {
			browserOptions.setCapability(CapabilityType.BROWSER_VERSION, version);
		}
		browserOptions.setCapability(CapabilityType.PLATFORM_NAME, os);
		browserOptions.setCapability("sauce:options", sauceOptions);

		this.driver = new RemoteWebDriver(new URL("http://" + authentication.getUsername() + ":"
				+ authentication.getAccessKey() + "@ondemand.saucelabs.com:80/wd/hub"), browserOptions);

		sauceRestApi = new SauceREST(SauceProperties.getString(SauceProperties.USER_NAME),
				SauceProperties.getString(SauceProperties.ACCESS_KEY));
		this.sessionId = (((RemoteWebDriver) driver).getSessionId()).toString();

	}

	private MutableCapabilities getBaseBrowserOptions(String browserName) {
		if (browserName.equals("firefox")) {
			return new FirefoxOptions();
		} else {
			MutableCapabilities result = new DesiredCapabilities();
			result.setCapability(CapabilityType.BROWSER_NAME, browser);
			return result;
		}
	}

	/**
	 * Runs a simple test verifying the title of the amazon.com homepage.
	 * 
	 * @throws Exception
	 */
	@Test
	public void amazon() throws Exception {
		driver.get("http://www.amazon.com/");
		if (browser == "android" || browser == "iphone") {
			assertEquals("Amazon.com", driver.getTitle());
		} else {

			assertEquals("Amazon.com: Online Shopping for Electronics, Apparel, Computers, Books, DVDs & more",
					driver.getTitle());

			sauceRestApi.jobPassed(getSessionId());
		}
	}

	/**
	 * Runs a simple test verifying the search functionality of the google.com
	 * homepage.
	 * 
	 * @throws Exception
	 */
	@Test
	public void google() throws Exception {
		driver.get("http://www.google.com");
		WebElement element = driver.findElement(By.name("q"));
		element.sendKeys("Cheese!");
		element.submit();

		assertEquals("Cheese! - Google Search", driver.getTitle());

		sauceRestApi.jobPassed(sessionId);
	}

	/**
	 * Closes the {@link WebDriver} session.
	 *
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		if (driver != null) {
			driver.quit();
		}
	}

	/**
	 *
	 * @return the value of the Sauce Job id.
	 */
	@Override
	public String getSessionId() {
		return sessionId;
	}
}