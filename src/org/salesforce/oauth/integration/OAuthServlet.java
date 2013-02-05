package org.salesforce.oauth.integration;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;

import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet parameters
 */
@WebServlet(name = "OAuthServlet", urlPatterns = { "/OAuthServlet/*",
		"/OAuthServlet" }, initParams = {
		// clientId is 'Consumer Key' in the Remote Access UI
		@WebInitParam(name = "clientId", value = "3MVG9Y6d_Btp4xp5hntckvnA5QVKsxlc4RUx9CbJndYCQQS4oO7jHAVspS0WdeCXBJlMXO1e9hwQSCjCBB71H"),
		// clientSecret is 'Consumer Secret' in the Remote Access UI
		@WebInitParam(name = "clientSecret", value = "4518803906379506686"),
		// This must be identical to 'Callback URL' in the Remote Access UI
		@WebInitParam(name = "redirectUri", value = "https://localhost:8443/Services/OAuthServlet/callback"),
		@WebInitParam(name = "environment", value = "https://login.salesforce.com"), })
public class OAuthServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory
			.getLogger(OAuthServlet.class);

	private static final String ACCESS_TOKEN = "ACCESS_TOKEN";
	private static final String INSTANCE_URL = "INSTANCE_URL";

	private String clientId = null;
	private String clientSecret = null;
	private String redirectUri = null;
	private String environment = null;
	private String authUrl = null;
	private String tokenUrl = null;
	private String authorizationCode = null;

	public void init() throws ServletException {
		clientId = this.getInitParameter("clientId");
		clientSecret = this.getInitParameter("clientSecret");
		redirectUri = this.getInitParameter("redirectUri");
		environment = this.getInitParameter("environment");

		try {
			authUrl = environment
					+ "/services/oauth2/authorize?response_type=code&client_id="
					+ clientId + "&redirect_uri="
					+ URLEncoder.encode(redirectUri, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOG.error("Error while creating AuthURL: {} ", e.getMessage());
			throw new ServletException("Error while creating AuthURL:", e);
		}
		tokenUrl = environment + "/services/oauth2/token";
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String accessToken = (String) request.getSession().getAttribute(
				ACCESS_TOKEN);

		if (null == accessToken) {
			String instanceUrl = null;
			if (request.getRequestURI().endsWith("OAuthServlet")) {

				// Send the user to authorize
				response.sendRedirect(authUrl);
				return;
			} else {
				String code = request.getParameter("code");
				authorizationCode = code;
				LOG.info("Auth successful, got Authorization code: {} ", code);

				HttpClient httpclient = new HttpClient();
				PostMethod post = new PostMethod(tokenUrl);
				post.addParameter("code", code);
				post.addParameter("grant_type", "authorization_code");
				post.addParameter("client_id", clientId);
				post.addParameter("client_secret", clientSecret);
				post.addParameter("redirect_uri", redirectUri);
				try {
					httpclient.executeMethod(post);
					try {
						JSONObject authResponse = new JSONObject(
								new JSONTokener(new InputStreamReader(
										post.getResponseBodyAsStream())));
						accessToken = authResponse.getString("access_token");
						instanceUrl = authResponse.getString("instance_url");
						LOG.info("Auth Response: {} ", authResponse.toString(2));
					} catch (JSONException e) {
						LOG.error(
								"Error while getting JSONObject from AuthResponse: {} ",
								e.getMessage());
						throw new ServletException(
								"Error while getting JSONObject from AuthResponse: {} ",
								e);
					}
				} catch (Exception e) {
					LOG.error("Error while Oauth with Salesforce: {} ",
							e.getMessage());
					throw new ServletException(
							"Error while Oauth with Salesforce:  ", e);
				} finally {
					post.releaseConnection();
				}
			}

			// Set a session attribute so that other servlets can get the access
			// token
			request.getSession().setAttribute(ACCESS_TOKEN, accessToken);

			// We also get the instance URL from the OAuth response, so set it
			// in the session too
			request.getSession().setAttribute(INSTANCE_URL, instanceUrl);
			request.getSession().setAttribute("AuthorizationCode",
					authorizationCode);
		}

		response.sendRedirect(request.getContextPath() + "/display");
	}
}