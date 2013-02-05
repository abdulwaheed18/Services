package org.salesforce.oauth.integration;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(urlPatterns = { "/display" })
public class TestAPI extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final String ACCESS_TOKEN = "ACCESS_TOKEN";
	private static final String INSTANCE_URL = "INSTANCE_URL";

	private static final Logger LOG = LoggerFactory.getLogger(TestAPI.class);

	private void showAccounts(String instanceUrl, String accessToken,
			PrintWriter writer) throws ServletException, IOException {
		LOG.info("Displaying List of Accounts:");
		HttpClient httpclient = new HttpClient();
		GetMethod get = new GetMethod(instanceUrl
				+ "/services/data/v20.0/query");

		// set the token in the header
		get.setRequestHeader("Authorization", "OAuth " + accessToken);

		// set the SOQL as a query param
		NameValuePair[] params = new NameValuePair[1];

		params[0] = new NameValuePair("q",
				"SELECT Name,Id from Account LIMIT 100");
		get.setQueryString(params);
		try {
			httpclient.executeMethod(get);
			if (get.getStatusCode() == HttpStatus.SC_OK) {
				// Now lets use the standard java json classes to work with the
				// results
				try {
					JSONObject response = new JSONObject(
							new JSONTokener(new InputStreamReader(
									get.getResponseBodyAsStream())));
					LOG.debug("Query response: {}, \n Total records: {} ",
							response.toString(2),
							response.getString("totalSize"));
					JSONArray results = response.getJSONArray("records");
					for (int i = 0; i < results.length(); i++) {
						LOG.debug("Id: {}, Name: {}", results.getJSONObject(i)
								.getString("Id"), results.getJSONObject(i)
								.getString("Name"));
						writer.println("<br>");
						writer.println(i + 1 + ". "
								+ results.getJSONObject(i).getString("Name"));
					}
					writer.println("<br>");
				} catch (JSONException e) {
					LOG.error(
							"Error while getting JSONObject from the records {} ",
							e.getMessage());
					throw new ServletException(
							"Error while getting JSONObject from the records: ",
							e);
				}
			}
		} catch (Exception e) {
			LOG.error("Error while displaying list of accounts {} ",
					e.getMessage());
			throw new ServletException(
					"Error while displaying list of accounts: ", e);
		} finally {
			get.releaseConnection();
		}
	}

	private String createAccount(String name, String instanceUrl,
			String accessToken, PrintWriter writer) throws ServletException,
			IOException {
		LOG.info("Creating account: {}",name);
		String accountId = null;
		HttpClient httpclient = new HttpClient();
		JSONObject account = new JSONObject();
		try {
			account.put("Name", name);
		} catch (JSONException e) {
			LOG.error("Error while getting JSONObject {} ", e.getMessage());
			throw new ServletException("Error while getting JSONObject: ", e);
		}
		PostMethod post = new PostMethod(instanceUrl
				+ "/services/data/v20.0/sobjects/Account/");
		post.setRequestHeader("Authorization", "OAuth " + accessToken);
		post.setRequestEntity(new StringRequestEntity(account.toString(),
				"application/json", null));
		try {
			httpclient.executeMethod(post);
			if (post.getStatusCode() == HttpStatus.SC_CREATED) {
				try {
					JSONObject response = new JSONObject(new JSONTokener(
							new InputStreamReader(
									post.getResponseBodyAsStream())));
					if (response.getBoolean("success")) {
						accountId = response.getString("id");
						writer.println(name
								+ " account created Successfully. <br>");
					}
				} catch (JSONException e) {
					LOG.error("Error while getting JSONObject {} ",
							e.getMessage());
					throw new ServletException(
							"Error while getting JSONObject: ", e);
				}
			}
		} catch (Exception e) {
			LOG.error("Error while creating account for {} :", name,
					e.getMessage());
			throw new ServletException("Error while creating account:", e);
		} finally {
			post.releaseConnection();
		}
		LOG.info("Account[{}] Created Successfully.",name);
		return accountId;
	}

	private void updateAccount(String accountId, String newName, String city,
			String instanceUrl, String accessToken, PrintWriter writer)
			throws ServletException, IOException {
		LOG.info("Updating account[{}]", accountId);
		HttpClient httpclient = new HttpClient();

		JSONObject update = new JSONObject();

		try {
			update.put("Name", newName);
			update.put("BillingCity", city);
		} catch (JSONException e) {
			LOG.error("Error while getting JSONObject {} ", e.getMessage());
			throw new ServletException("Error while getting JSONObject: ", e);
		}

		PostMethod patch = new PostMethod(instanceUrl
				+ "/services/data/v20.0/sobjects/Account/" + accountId) {
			@Override
			public String getName() {
				return "PATCH";
			}
		};

		patch.setRequestHeader("Authorization", "OAuth " + accessToken);
		patch.setRequestEntity(new StringRequestEntity(update.toString(),
				"application/json", null));

		try {
			httpclient.executeMethod(patch);
			writer.println("updated account: " + newName);
		} catch (Exception e) {
			LOG.error("Error while updating account for {} :", newName,
					e.getMessage());
			throw new ServletException("Error while updating account:", e);
		} finally {
			patch.releaseConnection();
		}
		LOG.info("Account[{}] updated Successfully.",newName);
	}

	private void deleteAccount(String accountId, String instanceUrl,
			String accessToken, PrintWriter writer) throws IOException,
			ServletException {
		LOG.info("Deleting account: {}", accountId);
		HttpClient httpclient = new HttpClient();
		DeleteMethod delete = new DeleteMethod(instanceUrl
				+ "/services/data/v20.0/sobjects/Account/" + accountId);

		delete.setRequestHeader("Authorization", "OAuth " + accessToken);
		try {
			httpclient.executeMethod(delete);
			writer.println("Account deleted Successfully.");
		} catch (Exception e) {
			LOG.error("Error while deleting accountId:{} ", accountId,
					e.getMessage());
			throw new ServletException("Error while deleting accountId:", e);
		} finally {
			delete.releaseConnection();
		}
		LOG.info("Account[{}] deleted Successfully.", accountId);
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		PrintWriter writer = response.getWriter();
		writer.println("<html>");
		writer.println("<head>");
		writer.println("<title>OAuth Response</title>");
		writer.println("</head>");
		writer.println("<body bgcolor=#5F9EA0>");

		String accessToken = (String) request.getSession().getAttribute(
				ACCESS_TOKEN);
		String instanceUrl = (String) request.getSession().getAttribute(
				INSTANCE_URL);
		String authorizationCode = (String) request.getSession().getAttribute(
				"AuthorizationCode");

		LOG.debug("instance URL: {} ", instanceUrl);
		LOG.debug("Access Token: {} ", accessToken);
		LOG.debug("Authorization code: {} ", authorizationCode);

		if (accessToken == null) {
			writer.write("Error - no access token");
			return;
		}

		writer.println("<h2><b><center>Congratulation..!!!! OAuth process completed Successfully</center></b><h2><br>");

		writer.println("<h4>List of existing Accounts: <h4>");
		showAccounts(instanceUrl, accessToken, writer);

		writer.println("<h4>Creating a new account on name : GSLAB <h4>");
		String accountId = createAccount("GSLAB", instanceUrl, accessToken,
				writer);
		LOG.debug("Account ID of new created account GSLAB: {}", accountId);

		writer.println("<h4>List of Accounts after creating GSLAB : <h4>");
		showAccounts(instanceUrl, accessToken, writer);

		writer.println("<h4>Updating account GSLAB to GREAT SOFTWARE <h4>");
		updateAccount(accountId, "GREAT SOFTWARE", "PUNE", instanceUrl,
				accessToken, writer);

		writer.println("<h4>List of Accounts after updating GSLAB to GREAT SOFTWARE : <h4>");
		showAccounts(instanceUrl, accessToken, writer);

		writer.println("<h4>Deleting account: GREAT SOFTWARE <h4>");
		deleteAccount(accountId, instanceUrl, accessToken, writer);

		writer.println("<h4>List of Accounts after deleting GREAT SOFTWARE : <h4>");
		showAccounts(instanceUrl, accessToken, writer);

		writer.println("</body>");
		writer.println("</html>");
	}
}