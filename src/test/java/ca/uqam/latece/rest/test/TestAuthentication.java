package ca.uqam.latece.rest.test;

import static org.junit.Assert.*;

import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import ca.uqam.latece.rest.authentication.Authentication;

public class TestAuthentication {
	WebClient webClient;
	URL url;
	WebRequest requestSettings;

	@Before
	public void setUp() throws Exception {
		webClient = new WebClient();
		url = new URL("http://localhost:8080/VolunteerManager2/api/authentication");
		requestSettings = new WebRequest(url, HttpMethod.POST);
	}

	@After
	public void tearDown() throws Exception {
		webClient.close();
		webClient = null;
		url = null;
		requestSettings = null;
	}

	@Test
	public void successfullAuthentication() throws Exception {
		//put credentials that exist.
		ArrayList<NameValuePair> parameters = new ArrayList<NameValuePair>();
		parameters.add(new NameValuePair("username", "JohnDoe"));
		parameters.add(new NameValuePair("password", "pw"));
		requestSettings.setRequestParameters(parameters);
	
		HtmlPage currentPage = webClient.getPage(requestSettings);
		int statusCode = webClient.getPage(requestSettings).getWebResponse().getStatusCode();

		assertEquals(200, statusCode);
		assertEquals("hj5T_-WpCkLrcJc2K1i_FcWjIoRuub-khV4DAgEQpEcADi5TyZsEwhETgVqud9OE", currentPage.asText());		
	}
	
	@Test
	public void unsuccessfullAuthentication() throws Exception {
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		
		//put credentials that doesn't exist.
		ArrayList<NameValuePair> parameters = new ArrayList<NameValuePair>();
		parameters.add(new NameValuePair("username", "Test"));
		parameters.add(new NameValuePair("password", "pw"));
		requestSettings.setRequestParameters(parameters);
		
		int statusCode = webClient.getPage(requestSettings).getWebResponse().getStatusCode();

		assertEquals(statusCode, 401);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testDecryptToken() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, ParseException{
		JSONObject expected = new JSONObject();
		expected.put("username", "JohnDoe");
		expected.put("role", 2);
		expected.put("id", 1);
		
		JSONObject test = Authentication.getTokenDecrypted("hj5T_-WpCkLrcJc2K1i_FcWjIoRuub-khV4DAgEQpEcADi5TyZsEwhETgVqud9OE");
		
		assertEquals(test.toString(), expected.toString());
	}
}
