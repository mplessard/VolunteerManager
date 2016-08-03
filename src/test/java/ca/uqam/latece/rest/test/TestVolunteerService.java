package ca.uqam.latece.rest.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import ca.uqam.latece.rest.database.Database;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestVolunteerService {
	WebClient webClient;
	URL url1;
	URL url2;
	WebRequest requestSettings1;
	WebRequest requestSettings2;
	WebRequest requestSettings3;
	ArrayList<NameValuePair> parameters;
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		Database.getConnection();
		ArrayList<String> sqlOp = new ArrayList<String>();
		sqlOp.add("DELETE FROM volunteer WHERE username='TestUser'");
		sqlOp.add("INSERT INTO volunteer VALUES(1, 'JohnDoe', 'John@Doe.com', 'XohImNooBHFR0OVvjcYpJ3NgPQ1qq73WKhHvch0VQtg=', 2, 'hj5T_-WpCkLrcJc2K1i_FcWjIoRuub-khV4DAgEQpEcADi5TyZsEwhETgVqud9OE', date('2016-11-11'))");
		sqlOp.add("INSERT INTO GardenResponsibility VALUES(1, 1, 1, 'Carotte')");
		sqlOp.add("INSERT INTO GardenResponsibility VALUES(3, 1, 2, 'Brocoli')");
		
		for(String sql: sqlOp){
			Database.operationOnTable(sql);
		}
	}
	
	@Before
	public void setUp() throws Exception {
		webClient = new WebClient();
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		url1 = new URL("http://localhost:8080/VolunteerManager/api/v1/volunteerservice/volunteers");
		url2 = new URL("http://localhost:8080/VolunteerManager/api/v1/volunteerservice/volunteers/1?access_token=hj5T_-WpCkLrcJc2K1i_FcWjIoRuub-khV4DAgEQpEcADi5TyZsEwhETgVqud9OE");
		requestSettings1 = new WebRequest(url1, HttpMethod.POST);
		requestSettings2 = new WebRequest(url2, HttpMethod.DELETE);
		requestSettings3 = new WebRequest(url2, HttpMethod.POST);
		parameters = new ArrayList<NameValuePair>();
	}

	@After
	public void tearDown() throws Exception {
		webClient.close();
		webClient = null;
		url1 = null;
		url2 = null;
		requestSettings1 = null;
		requestSettings2 = null;
		requestSettings3 = null;
		parameters = null;
	}

	@Test
	public void post1Volunteer() throws FailingHttpStatusCodeException, IOException, SQLException {
		parameters.add(new NameValuePair("username", "TestUser"));
		parameters.add(new NameValuePair("email", "Test@User.com"));
		parameters.add(new NameValuePair("password", "password"));
		
		requestSettings1.setRequestParameters(parameters);
	
		HtmlPage currentPage = webClient.getPage(requestSettings1);
		int statusCode = currentPage.getWebResponse().getStatusCode();
		
		assertEquals(200, statusCode);
		assertEquals("The volunteer has been created.", currentPage.asText());
	}
	
	@Test
	public void post2volunteerAlreadyExistingUsername() throws FailingHttpStatusCodeException, IOException{
		parameters.add(new NameValuePair("username", "TestUser"));
		parameters.add(new NameValuePair("email", "Test@User.com"));
		parameters.add(new NameValuePair("password", "password"));
		
		requestSettings1.setRequestParameters(parameters);
		
		HtmlPage currentPage = webClient.getPage(requestSettings1);
		int statusCode = currentPage.getWebResponse().getStatusCode();
		
		assertEquals(404, statusCode);
		assertEquals("The username already exist.", currentPage.asText());
	}
	
	@Test
	public void post3VolunteerAlreadyExistingEmail() throws FailingHttpStatusCodeException, IOException{
		parameters.add(new NameValuePair("username", "nonExistingName"));
		parameters.add(new NameValuePair("email", "Test@User.com"));
		parameters.add(new NameValuePair("password", "password"));
		
		requestSettings1.setRequestParameters(parameters);
		
		HtmlPage currentPage = webClient.getPage(requestSettings1);
		int statusCode = currentPage.getWebResponse().getStatusCode();
		
		assertEquals(404, statusCode);
		assertEquals("There's already a user with this email.", currentPage.asText());
	}
	
	@Test
	public void post4VolunteerInvalidUsernameLessThan6() throws FailingHttpStatusCodeException, IOException{
		parameters.add(new NameValuePair("username", "12345"));
		parameters.add(new NameValuePair("email", "12345@test.com"));
		parameters.add(new NameValuePair("password", "password"));
		
		requestSettings1.setRequestParameters(parameters);
		
		HtmlPage currentPage = webClient.getPage(requestSettings1);
		int statusCode = currentPage.getWebResponse().getStatusCode();
		
		assertEquals(404, statusCode);
		assertEquals("The username is not valid.", currentPage.asText());
	}
	
	@Test
	public void post5VolunteerInvalidUsernameMoreThan25() throws FailingHttpStatusCodeException, IOException{
		parameters.add(new NameValuePair("username", "123456789012345678901234567890"));
		parameters.add(new NameValuePair("email", "12345@test.com"));
		parameters.add(new NameValuePair("password", "password"));
		
		requestSettings1.setRequestParameters(parameters);
		
		HtmlPage currentPage = webClient.getPage(requestSettings1);
		int statusCode = currentPage.getWebResponse().getStatusCode();
		
		assertEquals(404, statusCode);
		assertEquals("The username is not valid.", currentPage.asText());
	}
	
	@Test
	public void post6VolunteerInvalidUsernameSpecialChar() throws FailingHttpStatusCodeException, IOException{
		parameters.add(new NameValuePair("username", "TestUser'"));
		parameters.add(new NameValuePair("email", "12345@test.com"));
		parameters.add(new NameValuePair("password", "password"));
		
		requestSettings1.setRequestParameters(parameters);
		
		HtmlPage currentPage = webClient.getPage(requestSettings1);
		int statusCode = currentPage.getWebResponse().getStatusCode();
		
		assertEquals(404, statusCode);
		assertEquals("The username is not valid.", currentPage.asText());
		
		parameters.remove(new NameValuePair("username", "TestUser'"));
		parameters.add(new NameValuePair("username", "TestUser\\"));
		
		requestSettings1.setRequestParameters(parameters);
		currentPage = webClient.getPage(requestSettings1);
		statusCode = currentPage.getWebResponse().getStatusCode();
		
		assertEquals(404, statusCode);
		assertEquals("The username is not valid.", currentPage.asText());
		
		parameters.remove(new NameValuePair("username", "TestUser\\"));
		parameters.add(new NameValuePair("username", "TestUser!"));
		
		requestSettings1.setRequestParameters(parameters);
		currentPage = webClient.getPage(requestSettings1);
		statusCode = currentPage.getWebResponse().getStatusCode();
		
		assertEquals(404, statusCode);
		assertEquals("The username is not valid.", currentPage.asText());
		
		parameters.remove(new NameValuePair("username", "TestUser!"));
		parameters.add(new NameValuePair("username", "TestUser$"));
		
		requestSettings1.setRequestParameters(parameters);
		currentPage = webClient.getPage(requestSettings1);
		statusCode = currentPage.getWebResponse().getStatusCode();
		
		assertEquals(404, statusCode);
		assertEquals("The username is not valid.", currentPage.asText());
		
		parameters.remove(new NameValuePair("username", "TestUser$"));
		parameters.add(new NameValuePair("username", "TestUser*"));
		
		requestSettings1.setRequestParameters(parameters);
		currentPage = webClient.getPage(requestSettings1);
		statusCode = currentPage.getWebResponse().getStatusCode();
		
		assertEquals(404, statusCode);
		assertEquals("The username is not valid.", currentPage.asText());
	}
	
	@Test
	public void post7VolunteerInvalidUsername() throws FailingHttpStatusCodeException, IOException{
		parameters.add(new NameValuePair("username", ""));
		parameters.add(new NameValuePair("email", "12345@test.com"));
		parameters.add(new NameValuePair("password", "password"));
		
		requestSettings1.setRequestParameters(parameters);
		
		HtmlPage currentPage = webClient.getPage(requestSettings1);
		int statusCode = currentPage.getWebResponse().getStatusCode();
		
		assertEquals(404, statusCode);
		assertEquals("The username is not valid.", currentPage.asText());
		
		parameters.remove(new NameValuePair("username", ""));
		parameters.add(new NameValuePair("username", null));
		
		requestSettings1.setRequestParameters(parameters);
		currentPage = webClient.getPage(requestSettings1);
		statusCode = currentPage.getWebResponse().getStatusCode();
		
		assertEquals(404, statusCode);
		assertEquals("The username is not valid.", currentPage.asText());
		
	}
	
	@Test
	public void postUpdate1Volunteer() throws FailingHttpStatusCodeException, IOException {
		parameters.add(new NameValuePair("username", "blabla"));
		parameters.add(new NameValuePair("email", "12345@test.com"));
		
		requestSettings3.setRequestParameters(parameters);
		
		HtmlPage currentPage = webClient.getPage(requestSettings3);
		int statusCode = currentPage.getWebResponse().getStatusCode();
		assertEquals(200, statusCode);
		assertEquals("The volunteer has been updated.", currentPage.asText());
	}
	
	@Test
	public void testDelete1Volunteer() throws FailingHttpStatusCodeException, IOException{		
		HtmlPage currentPage = webClient.getPage(requestSettings2);
		int statusCode = currentPage.getWebResponse().getStatusCode();
		
		assertEquals(200, statusCode);
		assertEquals("The volunteer has been deleted.", currentPage.asText());
	}
}
