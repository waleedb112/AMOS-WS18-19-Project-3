package com.jwt.service;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.sql.Array;
import java.sql.Connection;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.jwt.DataBaseConnection.Config;
import com.jwt.DataBaseConnection.DatabaseProvider;
import com.jwt.dao.EventDao;
import com.jwt.dao.EventDaoImplementation;
import com.jwt.dao.EventTypeDao;
import com.jwt.dao.EventTypeDaoImplementation;
import com.jwt.dao.RouteDao;
import com.jwt.dao.RouteDaoImplementation;
import com.jwt.dao.UserDao;
import com.jwt.dao.UserDaoImplementation;
import com.jwt.model.Address;
import com.jwt.model.BasicUser;
import com.jwt.model.Event;
import com.jwt.model.EventType;
import com.jwt.service.mail.Mailer;

/**
 * This class provides all web services.
 */
/**
 * @author User
 *
 */
@Path("/services")
public class Services {

	@Context
	private ServletContext context;

	/**
	 * Gives greetings to an Request. Testable with
	 * http://localhost:8080/RESTfulWebserver/services/Tester
	 * 
	 * @param name path ending of tester
	 * @return Response with status 200 and a JSONObject with greetings.
	 * @throws JSONException
	 */
	@GET
	@Path("/{name}")
	public Response getMsg(@PathParam("name") String name) throws JSONException {

		JSONObject output = new JSONObject("{Welcome : " + name + "}");
		System.out.println("...TestRequest from " + name);
		return Response.status(200).entity(output.toString()).build();
	}

	/**
	 * Checks users authentication.
	 * 
	 * @param urlReq with user name and password
	 * @return Response with status 200 and massage for valid user or status 400
	 *         with InvalidRequestBody-message.
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws JSONException
	 * @throws UnsupportedEncodingException
	 */
	@POST
	@Path("/checkUser")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response checkUser(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {

		JSONObject JSONreq = new JSONObject(urlReq);

		if (JSONreq.has("email") && JSONreq.has("password")) {
			String email = JSONreq.getString("email");
			String password = JSONreq.getString("password");

			System.out.println("...userLoginRequest from " + email);

			// check users info in DB
			DatabaseProvider provider = DatabaseProvider.getInstance(context);
			ResultSet rs = provider.querySelectDB("SELECT * FROM user_reg WHERE email = ? AND password = ?", email,
					password);

			JSONObject response = new JSONObject();
			if (!rs.next()) {
				response.put("login", "wrongCredentials");
				return Response.status(200).entity(response.toString()).build();
			}

			response.put("login", "successfulLogin");

			return Response.status(200).entity(response.toString()).build();
		} else {
			return Response.status(400).entity("InvalidRequestBody").build();
		}
	}

	/**
	 * TODO Adds new user to database.
	 * 
	 * @param urlReq with user name, password, email, ...
	 * @return Response with status 200 and a message or status 400 with
	 *         InvalidRequestBody-message.
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws JSONException
	 * @throws UnsupportedEncodingException
	 */
	@POST
	@Path("/userRegistration")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response userRegistration(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {

		JSONObject JSONreq = new JSONObject(urlReq);

		if (JSONreq.has("name") && JSONreq.has("password") && JSONreq.has("email")) {
			try {
				JSONObject response = new JSONObject();

				// get data
				String username = JSONreq.getString("name");
				String password = JSONreq.getString("password");
				String email = JSONreq.getString("email");
				// TODO: more data... city, birth?

				System.out.println("...userRegistrationRequest from " + username);

				// check if user already exists

				DatabaseProvider db = DatabaseProvider.getInstance(context);
				ResultSet result = db.querySelectDB("SELECT * FROM user_reg WHERE email = '" + email + "'");
				while (result.next()) {
					response.put("userRegistration", "emailExistsAlready");
					return Response.status(200).entity(response.toString()).build();
				}

				// insert into DB
				db.queryInsertDB("INSERT INTO user_reg (password,email) VALUES (?,?)", password, email);

				// send registration mail
				Mailer mailer = new Mailer(context);
				boolean messageSent = mailer.sendRegistrationMail(email, username);

				if (!messageSent) {
					response.put("userRegistraion", "invalidMail");
					return Response.status(400).entity(response.toString()).build();
				}

				response.put("userRegistration", "successfullRegistration");
				return Response.status(200).entity(response.toString()).build();

			} catch (Exception e) {
				System.out.println("Wrong JSONFormat:" + e.toString());
			}
		}
		System.out.println("InvalidRequestbody");
		return Response.status(400).entity("InvalidRequestBody").build();
	}

	// Create New Event Type

	@POST
	@Path("/createEventType")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createEventType(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {

		JSONObject JSONreq = new JSONObject(urlReq);
		System.out.println(".. Create New Event Type");

		if (JSONreq.has("name")) {
			try {

				String eventname = JSONreq.getString("name");
				String eventdescription = JSONreq.getString("description");

				System.out.println("...New Event Type Created:" + eventname);

				try {
					DatabaseProvider provider = DatabaseProvider.getInstance(context);
					PreparedStatement st = provider.getConnection()
							.prepareStatement("INSERT INTO EVENT_TYPE (name,description) VALUES (?,?)");
					st.setString(1, eventname);
					st.setString(2, eventdescription);
					st.executeUpdate();
					st.closeOnCompletion();

				} catch (Exception ex) {
					ex.printStackTrace();
				}

				JSONObject response = new JSONObject();

				response.put("eventTypeCreation", "successfullCreation");

				return Response.status(200).entity(response.toString()).build();

			} catch (Exception e) {
				System.out.println("Wrong JSONFormat:" + e.toString());
			}
		}
		System.out.println("InvalidRequestbody");
		return Response.status(400).entity("InvalidRequestBody").build();
	}

	/**
	 * TODO
	 * 
	 * @param urlReq
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws JSONException
	 * @throws UnsupportedEncodingException
	 */
	@POST
	@Path("/createEvent")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createEvent(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {
		DatabaseProvider.getInstance(context);
		JSONObject JSONreq = new JSONObject(urlReq);
		System.out.println("...createEventRequest");

		if (JSONreq.has("event") && JSONreq.has("address")) {
			try {

				Event newEvent = new Event(JSONreq.getJSONObject("event"));
				Address address = new Address(JSONreq.getJSONObject("address"));

				EventDao eventDao = new EventDaoImplementation();
				eventDao.createEvent(newEvent, address);

				JSONObject response = new JSONObject();

				response.put("eventCreation", "successfullCreation");
				System.out.println("...newEventCreated:" + newEvent.getName());
				return Response.status(200).entity(response.toString()).build();

			} catch (Exception e) {
				System.out.println("Wrong JSONFormat:" + e.toString());
			}
		}
		System.out.println("InvalidRequestbody");
		return Response.status(400).entity("InvalidRequestBody").build();
	}

	/**
	 * Sets options for headers.
	 */
	@POST
	@Path("/addUserBasic")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addUserBasic(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {

		JSONObject JSONreq = new JSONObject(urlReq);
		System.out.println("...addInfoBasicUserRequest");
		if (JSONreq.has("first_name") && JSONreq.has("last_name") && JSONreq.has("dob") && JSONreq.has("country")
				&& JSONreq.has("state") && JSONreq.has("city") && JSONreq.has("street") && JSONreq.has("postcode")
				&& JSONreq.has("housenumber") && JSONreq.has("gender")) {
			try {
				String fname = JSONreq.getString("first_name");
				String lname = JSONreq.getString("last_name");
				String dob = JSONreq.getString("dob");
				String country = JSONreq.getString("country");
				String state = JSONreq.getString("state");
				String city = JSONreq.getString("city");
				String street = JSONreq.getString("street");
				Integer postcode = JSONreq.getInt("postcode");
				String housenumber = JSONreq.getString("housenumber");
				String genderCol = JSONreq.getString("gender");

				System.out.println("...newInfoBasicUserAdded:");
				try {
					DatabaseProvider provider = DatabaseProvider.getInstance(context);
					Connection conn = provider.getConnection();
					PreparedStatement s1 = conn.prepareStatement("INSERT INTO GENDER (gender) VALUES (?)");

					PreparedStatement s2 = conn.prepareStatement(
							"INSERT INTO ADDRESS (country, state, city, street, postcode, housenumber) VALUES (?,?,?,?,?,?)");

					PreparedStatement s3 = conn.prepareStatement(
							"INSERT INTO BASIC_USER (id_user, first_name,last_name,dob, id_gender, id_address) VALUES (?,?,?,?::date,?,?)");

					s1.setString(1, genderCol);
					s1.executeUpdate();
					s1.closeOnCompletion();

					s2.setString(1, country);
					s2.setString(2, state);
					s2.setString(3, city);
					s2.setString(4, street);
					s2.setInt(5, postcode);
					s2.setString(6, housenumber);
					s2.executeUpdate();
					s2.closeOnCompletion();

					String selectSQL = "SELECT ID_USER FROM USER_REG ORDER BY ID_USER DESC LIMIT 1";
					PreparedStatement preparedStatement = conn.prepareStatement(selectSQL);
					ResultSet rs = preparedStatement.executeQuery();
					int result;
					while (rs.next()) {
						String userId = rs.getString("ID_USER");
						result = Integer.parseInt(userId);
						s3.setInt(1, result);
					}

					s3.setString(2, fname);
					s3.setString(3, lname);
					s3.setString(4, dob);

					String selectSQL2 = "SELECT ID_GENDER FROM GENDER ORDER BY ID_GENDER DESC LIMIT 1";
					PreparedStatement preparedStatement2 = conn.prepareStatement(selectSQL2);
					ResultSet rs2 = preparedStatement2.executeQuery();
					int result2;
					while (rs2.next()) {
						String genderId = rs2.getString("ID_GENDER");
						result2 = Integer.parseInt(genderId);
						s3.setInt(5, result2);
					}

					String selectSQL3 = "SELECT ID_ADDRESS FROM ADDRESS ORDER BY ID_ADDRESS DESC LIMIT 1";
					PreparedStatement preparedStatement3 = conn.prepareStatement(selectSQL3);
					ResultSet rs3 = preparedStatement3.executeQuery();
					int result3;
					while (rs3.next()) {
						String addressId = rs3.getString("ID_ADDRESS");
						result3 = Integer.parseInt(addressId);
						s3.setInt(6, result3);
					}

					s3.executeUpdate();
					s3.closeOnCompletion();

				} catch (Exception ex) {
					ex.printStackTrace();
				}
				JSONObject response = new JSONObject();
				response.put("addUserBasic", "successfullCreation");
				return Response.status(200).entity(response.toString()).build();
			} catch (Exception e) {
				System.out.println("Wrong JSONFormat:" + e.toString());
			}
		}
		System.out.println("InvalidRequestbody");
		return Response.status(400).entity("InvalidRequestBody").build();
	}

	/**
	 * Sets options for headers.
	 */

	@OPTIONS
	public Response optionsOptions() {
		return Response.ok().header("Allow-Control-Allow-Methods", "POST,GET,OPTIONS")
				.header("Access-Control-Allow-Origin", "*").build();
	}

	@GET
	@Path("/testMail")
	public Response sendTestMail() throws JSONException {

		JSONObject output = new JSONObject("{Test: send Mail}");
		System.out.println("...sendTestMail Request ");

		Mailer mailer = new Mailer(context);
		// send test registration mail

		// mailer.sendRegistrationMail("anika.apel@gmx.de", "test");

		return Response.status(200).entity(output.toString()).build();
	}

	@GET
	@Path("/getEvents")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getAllEvents() throws JSONException {

		JSONObject jobj1 = new JSONObject();

		System.out.println("...getAllEvetns");
		try {
			DatabaseProvider provider = DatabaseProvider.getInstance(context);

			// PreparedStatement st = conn.prepareStatement("SELECT
			// name,description,date,time FROM EVENT");

			ResultSet result = provider.querySelectDB("SELECT id_event,name,description,date,time FROM EVENT");
			System.out.println("this" + result.getClass().getName());

			JSONArray jArray = new JSONArray();
			while (result.next()) {
				String id_json = result.getString("id_event");
				String name_json = result.getString("name");
				String desc_json = result.getString("description");
				String date_json = result.getString("date");
				String time_json = result.getString("time");
				System.out.println(result);

				JSONObject jobj = new JSONObject();
				jobj.put("id_event", id_json);
				jobj.put("name", name_json);
				jobj.put("description", desc_json);
				jobj.put("date", date_json);
				jobj.put("time", time_json);
				jArray.put(jobj);

			}

			jobj1.put("event", jArray);

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		JSONObject response = new JSONObject();

		response.put("eventCreation", jobj1);

		return Response.status(200).entity(response.toString()).build();

	}

	@GET
	@Path("/getEventById/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getEventById(@PathParam("id") int id) throws JSONException {

		JSONObject jobj = new JSONObject();

		System.out.println("...Get Event By ID ");
		try {
			DatabaseProvider provider = DatabaseProvider.getInstance(context);

			Statement statement = provider.getConnection().createStatement();
			ResultSet result = statement.executeQuery("SELECT * FROM EVENT WHERE id_event=" + id);
			System.out.println("...Get Event By ID ");

			System.out.println("this" + result.getClass().getName());

			if (result.next()) {
				String id_json, name_json, desc_json, date_json, time_json;
				id_json = result.getString("id_event");
				name_json = result.getString("name");
				desc_json = result.getString("description");
				date_json = result.getString("date");
				time_json = result.getString("time");
				System.out.println(result);

				jobj.put("id_event", id_json);
				jobj.put("name", name_json);
				jobj.put("description", desc_json);
				jobj.put("date", date_json);
				jobj.put("time", time_json);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		JSONObject response = new JSONObject();

		response.put("eventCreation", jobj);

		return Response.status(200).entity(response.toString()).build();

	}

	@POST
	@Path("/updateEvent")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateEvent(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {

		JSONObject JSONreq = new JSONObject(urlReq);
		System.out.println("...updateEventRequest");

		if (JSONreq.has("id_event") && JSONreq.has("name") && JSONreq.has("description") && JSONreq.has("date")
				&& JSONreq.has("time")) {
			try {

				int eventid = (int) JSONreq.get("id_event");
				String eventname = JSONreq.getString("name");
				String eventdescription = JSONreq.getString("description");
				String eventdate = JSONreq.getString("date");
				String eventtime = JSONreq.getString("time");

				System.out.println("...updateEvent:" + eventname + "...updateEvent Id:" + eventid
						+ "...updateEvent Date:" + eventdate + "...updateEvent tinme:" + eventtime);

				try {

					DatabaseProvider provider = DatabaseProvider.getInstance(context);
					PreparedStatement statement = provider.getConnection().prepareStatement(
							"UPDATE EVENT SET name =?, description =?, date=?::date, time=?::time WHERE id_event="
									+ eventid);

					statement.setString(1, eventname);
					statement.setString(2, eventdescription);
					statement.setString(3, eventdate);
					statement.setString(4, eventtime);
					statement.executeUpdate();
					statement.closeOnCompletion();

				} catch (Exception ex) {
					ex.printStackTrace();
				}

				JSONObject response = new JSONObject();

				response.put("eventUpdate", "successfullUpdation");

				return Response.status(200).entity(response.toString()).build();

			} catch (Exception e) {
				System.out.println("Wrong JSONFormat:" + e.toString());
			}
		}
		System.out.println("InvalidRequestbody");
		return Response.status(400).entity("InvalidRequestBody").build();
	}

	@POST
	@Path("/changePassword")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response changePassword(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {

		JSONObject JSONreq = new JSONObject(urlReq);

		if (JSONreq.has("email") && JSONreq.has("oldPassword") && JSONreq.has("newPassword")
				&& JSONreq.has("repeatNewPassword")
//				&& (JSONreq.getString("newPassword").equals(JSONreq.getString("repeatNewPassword")))
		) {
			try {

				String email = JSONreq.getString("email");
				String oldPassword = JSONreq.getString("oldPassword");
				String newPassword = JSONreq.getString("newPassword");
				String repeatNewPassword = JSONreq.getString("repeatNewPassword");

				System.out.println("...changePassword:" + email);

				try {

//					Statement statement = conn.createStatement();
//					
//					ResultSet result = statement.executeQuery("UPDATE USER_REG SET password =? WHERE email='"+ email +"'" + "AND password='" + oldPassword + "'", newPassword);

					DatabaseProvider provider = DatabaseProvider.getInstance(context);
					PreparedStatement statement = provider.getConnection()
							.prepareStatement("UPDATE USER_REG SET password =? WHERE email='" + email + "'"
									+ "AND password='" + oldPassword + "'");

					statement.setString(1, newPassword);
					int count = statement.executeUpdate();
					statement.closeOnCompletion();
					JSONObject response = new JSONObject();
					if (count > 0) {

						response.put("passwordUpdated", "successfullUpdation");

						return Response.status(200).entity(response.toString()).build();

					} else {
						response.put("passwordUpdated", "notUpdated");

						return Response.status(200).entity(response.toString()).build();
					}

				} catch (Exception ex) {
					ex.printStackTrace();
				}

//				JSONObject response = new JSONObject();
//				response.put("passwordUpdated", "successfullUpdation");
//
//				return Response.status(200).entity(response.toString()).build();

			} catch (Exception e) {
				System.out.println("Wrong JSONFormat:" + e.toString());
			}
		}
		System.out.println("InvalidRequestbody");
		return Response.status(400).entity("InvalidRequestBody").build();
	}

	@POST
	@Path("/deleteEvent")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response deleteEvent(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {

		JSONObject JSONreq = new JSONObject(urlReq);
		System.out.println("...Delete Event Request");

		if (JSONreq.has("id_event")) {
			try {

				int eventid = (int) JSONreq.get("id_event");

				System.out.println("...Delete Event ID" + eventid);

				try {

					DatabaseProvider provider = DatabaseProvider.getInstance(context);
					PreparedStatement statement = provider.getConnection()
							.prepareStatement("DELETE FROM EVENT WHERE id_event=" + eventid);

					statement.executeUpdate();
					statement.closeOnCompletion();

				} catch (Exception ex) {
					ex.printStackTrace();
				}

				JSONObject response = new JSONObject();

				response.put("Event Deletion", "successfullDeletoion");

				return Response.status(200).entity(response.toString()).build();

			} catch (Exception e) {
				System.out.println("Wrong JSONFormat:" + e.toString());
			}
		}
		System.out.println("InvalidRequestbody");
		return Response.status(400).entity("InvalidRequestBody").build();
	}

	/**
	 * TODO
	 * 
	 * @param urlReq
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws JSONException
	 * @throws UnsupportedEncodingException
	 */
	@POST
	@Path("/createRoute")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createRoute(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {

		JSONObject JSONreq = new JSONObject(urlReq);
		System.out.println("...createRoute");
		try {
			DatabaseProvider.getInstance(context);

			NewRouteRequest newRoute = new NewRouteRequest(JSONreq);
			RouteDao dao = new RouteDaoImplementation();
			dao.createRoute(newRoute.getStartAddress(), newRoute.getEndAddress(), newRoute.getRoute());

			JSONObject response = new JSONObject();

			response.put("routeCreation", "successfullCreation");
			return Response.status(200).entity(response.toString()).build();
		} catch (Exception ex) {
			System.out.println("InvalidRequestbody");
			return Response.status(400).entity("InvalidRequestBody").build();
		}
	}

	@POST
	@Path("/searchUser")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response searchUserByMail(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {
		JSONObject JSONreq = new JSONObject(urlReq);
		JSONObject allUser = new JSONObject();
		String input;

		System.out.println("...Get User By Mail or Name");
		try {

			UserDaoImplementation dao = new UserDaoImplementation();

			int userId;
			if (JSONreq.has("id") && JSONreq.has("input")) {
				userId = JSONreq.getInt("id");
				input = JSONreq.getString("input");
			} else {
				return Response.status(500).entity("InvalidRequestBody".toString()).build();
			}

			List<BasicUser> searchResults = dao.searchUser(input, userId);

			if (searchResults != null && searchResults.isEmpty()) {
				allUser.put("foundUser", "unsuccessful");
			} else {
				allUser.put("foundUser", "successful");
				allUser.put("user", BasicUser.serializeUserList(searchResults));
			}

			return Response.status(200).entity(allUser.toString()).build();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return Response.status(500).entity("InvalidRequestBody".toString()).build();
	}

	@POST
	@Path("/editUserInfo")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response editUserInfo(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {

		JSONObject JSONreq = new JSONObject(urlReq);

		if (JSONreq.has("last_name") && JSONreq.has("country") && JSONreq.has("state") && JSONreq.has("city")
				&& JSONreq.has("street") && JSONreq.has("postcode") && JSONreq.has("housenumber")) {
			try {

				String lname = JSONreq.getString("last_name");
				String country = JSONreq.getString("country");
				String state = JSONreq.getString("state");
				String city = JSONreq.getString("city");
				String street = JSONreq.getString("street");
				Integer postcode = JSONreq.getInt("postcode");
				String housenumber = JSONreq.getString("housenumber");

				try {

					DatabaseProvider provider = DatabaseProvider.getInstance(context);

					PreparedStatement s1 = provider.getConnection().prepareStatement(
							"UPDATE ADDRESS SET country =?, state =?, city =?, street =?, postcode =?, housenumber =? WHERE id_address = (SELECT max(id_address) FROM ADDRESS)");

					PreparedStatement s2 = provider.getConnection().prepareStatement(
							"UPDATE BASIC_USER SET last_name =? WHERE id_user = (SELECT max(id_user) FROM BASIC_USER)");

					s1.setString(1, country);
					s1.setString(2, state);
					s1.setString(3, city);
					s1.setString(4, street);
					s1.setInt(5, postcode);
					s1.setString(6, housenumber);
					s1.executeUpdate();
					s1.closeOnCompletion();

					s2.setString(1, lname);
					s2.executeUpdate();
					s2.closeOnCompletion();

				} catch (Exception ex) {
					ex.printStackTrace();
				}

			} catch (Exception e) {
				System.out.println("Wrong JSONFormat:" + e.toString());
			}
		}
		System.out.println("...EditedInfoGotInserted");
		return Response.status(400).entity("InvalidRequestBody").build();
	}

	@POST
	@Path("/addFriend")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addFriend(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {

		JSONObject JSONreq = new JSONObject(urlReq);
		System.out.println("...addFriendRequest");

		// TODO not tested yet
		if (JSONreq.has("idUser") && JSONreq.has("idFollower")) {

			JSONObject response = new JSONObject();
			DatabaseProvider db = DatabaseProvider.getInstance(context);
			int idUser = JSONreq.getInt("idUser");
			int idFollower = JSONreq.getInt("idFollower");
			try {
				db.queryInsertDB("INSERT into FRIENDSHIP VALUES (?,?)", idUser, idFollower);
				response.put("friendship", "successful");
			} catch (Exception ex) {
				ex.printStackTrace();
				response.put("friendship", "internalProblems");
			}
			return Response.status(200).entity(response.toString()).build();
		}
		return Response.status(400).entity("InvalidRequestBody").build();
	}

	@GET
	@Path("/get_event_type")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getEventType() throws JSONException {
		JSONObject response = new JSONObject();
		System.out.println("Get Event Type... ");
		try {
			// Setting the DB context in case its not set
			DatabaseProvider.getInstance(context);
			EventTypeDao eventType = new EventTypeDaoImplementation();
			List<EventType> eventTypeList = eventType.getEventTypes();
			response.put("result", EventType.serializeEventTypeList(eventTypeList));
			System.out.println("Response: " + response.toString());
			return Response.status(200).entity(response.toString()).build();
		} catch (Exception ex) {
			ex.printStackTrace();

			response.put("error_code", "failed_to_get_event_type");
			response.put("description", "Failed to get event types");
			System.out.println("Response: " + response.toString());
			return Response.status(500).entity(response.toString()).build();
		}
	}

	/**
	 * Get UserID for user with given mail
	 * 
	 * @param mail
	 * @return userID
	 * @throws JSONException
	 */
	@GET
	@Path("/getUserID/{mail}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getUserID(@PathParam("mail") String mail) throws JSONException {

		JSONObject jobj = new JSONObject();

		System.out.println("...Get UserID By Email ");
		try {
			DatabaseProvider provider = DatabaseProvider.getInstance(context);
			ResultSet result = provider.querySelectDB("SELECT id_user FROM user_reg WHERE email = ?", mail);

			if (result.next()) {
				jobj.put("user_id", result.getString("id_user"));
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		JSONObject response = new JSONObject();

		response.put("getUserID", jobj);

		return Response.status(200).entity(response.toString()).build();

	}

	/**
	 * Returns list with every friend
	 * 
	 * @param userId
	 * @return
	 * @throws JSONException
	 */
	@GET
	@Path("/getFriends/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getFriends(@PathParam("id") int userId) throws JSONException {

		JSONObject response = new JSONObject();
		System.out.println("Get Friends... ");
		try {
			// Setting the DB context in case its not set
			DatabaseProvider.getInstance(context);
			UserDao userD = new UserDaoImplementation();
			List<BasicUser> friends = userD.getFriends(userId);
			response.put("friends", BasicUser.serializeUserList(friends));
			System.out.println("Response: " + response.toString());
			return Response.status(200).entity(response.toString()).build();
		} catch (Exception ex) {
			ex.printStackTrace();
			response.put("error_code", "failed_to_get_friends");
			response.put("description", "Failed to get Friends");
			System.out.println("Response: " + response.toString());
			return Response.status(500).entity(response.toString()).build();
		}
	}

}
