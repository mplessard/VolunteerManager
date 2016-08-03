package ca.uqam.latece.rest.volunteer;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import ca.uqam.latece.rest.authentication.Authentication;
import ca.uqam.latece.rest.database.Database;

@Path("/v1/volunteerservice")
public class volunteerService {
	Connection c = Database.getConnection();

	@SuppressWarnings("unchecked")
	@Path("/volunteers/")
	@GET
	@Produces("application/json")
	public String volunteers(@QueryParam("access_token") String accessToken) throws SQLException, ClassNotFoundException {
		JSONArray volunteersList = new JSONArray();
		JSONObject volunteers = new JSONObject();

		if (Authentication.authenticationByToken(accessToken)) {
			ResultSet resultSet = Database.tableRequest("SELECT username, id FROM Volunteer");

			if (!resultSet.next()) {
				volunteers.put("error", "no data found");
			} else {
				resultSet.beforeFirst();

				while (resultSet.next()) {
					JSONObject volunteerAttributes = new JSONObject();
					volunteerAttributes.put("id", resultSet.getInt("id"));
					volunteerAttributes.put("username", resultSet.getString("username"));
					volunteersList.add(volunteerAttributes);
					volunteerAttributes = null;
				}
				volunteers.put("volunteers", volunteersList);
			}
		} else {
			volunteers.put("error", "The access token is invalid.");
		}
		return volunteers.toJSONString();
	}

	@Path("/volunteers/")
	@POST
	@Produces(MediaType.TEXT_HTML)
	@Consumes("application/x-www-form-urlencoded")
	public Response postVolunteers(@FormParam("username") String username, @FormParam("email") String email, @FormParam("password") String password) throws ClassNotFoundException, SQLException, NoSuchAlgorithmException {
		int responseCode = 404;
		String responseMessage = "The volunteer was not created.";

		Pattern usernamePattern = Pattern.compile("[A-Za-z0-9_]{6,25}");
		Pattern emailPattern = Pattern.compile("^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+$");

		if (username != null && username != "" && usernamePattern.matcher(username).matches()) {
			String sql = "SELECT COUNT(*) AS exist FROM Volunteer WHERE username='" + username + "'";
			ResultSet resultSet = Database.tableRequest(sql);
			resultSet.next();
			
			if(password != null && password != "" && usernamePattern.matcher(password).matches()){
				if (resultSet.getInt("exist") < 1) {
					if (email != null && email != "" && emailPattern.matcher(email).matches()) {
						resultSet = Database.tableRequest("SELECT COUNT(*) AS exist FROM Volunteer WHERE email='" + email + "'");
						resultSet.next();
						
						if (resultSet.getInt("exist") < 1) {
							String encryptedPassword  = Authentication.getEncryptedPassword(password);
							sql = "INSERT INTO volunteer(username, email, password) VALUES('" + username + "', '" + email + "', '" + encryptedPassword + "')";
							Database.operationOnTable(sql);
							
							responseCode = 200;
							responseMessage = "The volunteer has been created.";
						} else {
							responseMessage = "There's already a user with this email.";
						}
					} else {
						responseMessage = "The email is invalid.";
					}
				} else {
					responseMessage = "The username already exist.";
				}
			}else{
				responseMessage = "The password is not valid.";
			}
		} else {
			responseMessage = "The username is not valid.";
		}
		return Response.status(responseCode).entity(responseMessage).build();
	}

	@SuppressWarnings("unchecked")
	@Path("/volunteers/{id:[0-9]*}")
	@GET
	@Produces("application/json")
	public String volunteerInfo(@QueryParam("access_token") String accessToken, @PathParam("id") int id) throws ClassNotFoundException,SQLException {
		JSONObject volunteer = new JSONObject();

		if (Authentication.authenticationByToken(accessToken)) {
			ResultSet resultSet = Database.tableRequest("SELECT username, email FROM Volunteer WHERE ID=" + id);
			ResultSet resultSet2 = Database.tableRequest("SELECT id, task FROM gardenResponsibility WHERE ID_Volunteer=" + id);

			if (!resultSet.next()) {
				volunteer.put("error", "no data found.");
			} else {
				volunteer.put("id", id);
				volunteer.put("username", resultSet.getString("username"));
				volunteer.put("email", resultSet.getString("email"));
				
				if (resultSet2.next()) {
					resultSet2.beforeFirst();
					JSONObject idGardenTask = new JSONObject();
					JSONObject gardenAndTask = new JSONObject();
					
					while (resultSet2.next()) {
						//gardenAndTask.put("garden", resultSet2.getString("G.name"));
						gardenAndTask.put("task", resultSet2.getString("task"));
						idGardenTask.put(resultSet2.getInt("id"), gardenAndTask);
						gardenAndTask = new JSONObject();
					}
					volunteer.put("tasks", idGardenTask);
				}
			}
		} else {
			volunteer.put("error", "The access token is invalid.");
		}
		return volunteer.toJSONString();
	}

	@Path("/volunteers/{id:[0-9]*}")
	@POST
	@Consumes("application/x-www-form-urlencoded")
	@Produces(MediaType.TEXT_HTML)
	public Response updateVolunteer(@QueryParam("access_token") String accessToken, @PathParam("id") String id, @FormParam("username") String username, @FormParam("email") String email) throws SQLException,ClassNotFoundException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, ParseException {
		int responseCode = 404;
		String responseMessage = "Volunteer with the id " + id + " is not in the Database.";
		if (Authentication.authenticationByToken(accessToken)){
			JSONObject decryptedToken = Authentication.getTokenDecrypted(accessToken);
			
			if((int)(long)decryptedToken.get("role") == 2 || decryptedToken.get("username").equals(username)){
			
				String sql = "SELECT username FROM Volunteer WHERE ID=" + id;
				ResultSet resultSet = Database.tableRequest(sql);
				Pattern usernamePattern = Pattern.compile("[A-Za-z0-9_]{6,25}");
				Pattern emailPattern = Pattern.compile("^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+$");
	
				if (resultSet.next()) {
					if (username != null && username != "" && usernamePattern.matcher(username).matches()) {
						sql = "SELECT COUNT(*) AS exist FROM Volunteer WHERE username='" + username + "' AND id NOT IN(" + id + ")";
						resultSet = Database.tableRequest(sql);
						resultSet.next();
	
						if (resultSet.getInt("exist") < 1) {
							if (email != null && email != "" && emailPattern.matcher(email).matches()) {
								resultSet = Database
										.tableRequest("SELECT COUNT(*) AS exist FROM Volunteer WHERE email='" + email + "' AND id NOT IN (" + id + ")");
								resultSet.next();
	
								if (resultSet.getInt("exist") < 1) {
									Database.operationOnTable("UPDATE volunteer SET username='" + username + "', email='" + email + "' WHERE id =" + id);
	
									responseCode = 200;
									responseMessage = "The volunteer has been updated.";
								} else {
									responseMessage = "There's already a user with this email.";
								}
							} else {
								responseMessage = "The email is invalid.";
							}
						} else {
							responseMessage = "The username already exist.";
						}
					} else {
						responseMessage = "The username is not valid.";
					}
				}
			}else{
				responseMessage = "Access denied: You are not authorized to modify other volunteer.";
			}
		} else {
			responseCode = 401;
			responseMessage = "Access denied: the access token is invalid.";
		}
		return Response.status(responseCode).entity(responseMessage).build();
	}

	@Path("/volunteers/{id:[0-9]*}")
	@DELETE
	@Produces(MediaType.TEXT_HTML)
	public Response deleteVolunteer(@QueryParam("access_token") String accessToken, @PathParam("id") int id) throws ClassNotFoundException,SQLException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, ParseException {
		int responseCode = 404;
		String responseMessage = "Volunteer with the id " + id + " is not in the Database.";

		if (Authentication.authenticationByToken(accessToken)) {
			JSONObject decryptedToken = Authentication.getTokenDecrypted(accessToken);
			
			if((int)(long)decryptedToken.get("role") == 2 || decryptedToken.get("id").equals(id)){
				ResultSet resultSet = Database.tableRequest("SELECT COUNT(*) AS exist FROM Volunteer WHERE ID=" + id);
				resultSet.next();
				if (resultSet.getInt("exist") > 0) {
					Database.operationOnTable("DELETE FROM volunteer WHERE id=" + id);
	
					responseCode = 200;
					responseMessage = "The volunteer has been deleted.";
				}
			}else{
				responseMessage = "Access denied: You are not authorized to delete other volunteer.";
			}
		} else {
			responseCode = 401;
			responseMessage = "Access denied: the access token is invalid.";
		}
		return Response.status(responseCode).entity(responseMessage).build();
	}
	
	@Path("/volunteers/{id:[0-9]*}/tasks")
	@POST
	@Produces(MediaType.TEXT_HTML)
	@Consumes("application/x-www-form-urlencoded")
	public Response addTaskToVolunteer(@QueryParam("access_token") String accessToken, @PathParam("id") int id, @FormParam("gardenid") int gardenID, @FormParam("task") String task) throws SQLException,ClassNotFoundException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, ParseException {
		int responseCode = 404;
		String responseMessage = "Volunteer with the id " + id + " is not in the Database.";
		if (Authentication.authenticationByToken(accessToken)){
			JSONObject decryptedToken = Authentication.getTokenDecrypted(accessToken);
			
			if((int)(long)decryptedToken.get("role") == 2 || (int)decryptedToken.get("id") == id){
				//TODO vérifie si id garden est valide
				
				Database.operationOnTable("INSERT INTO GardenResponsibility(ID_Volunteer, ID_Garden, task) VALUES(" + id + ", " + gardenID + ", '" + task + "')");
	
				responseCode = 200;
				responseMessage = "The task has been added.";
					
			}else{
				responseMessage = "Access denied: You are not authorized to modify other volunteer.";
			}
		} else {
			responseCode = 401;
			responseMessage = "Access denied: the access token is invalid.";
		}
		return Response.status(responseCode).entity(responseMessage).build();
	}
	
	@Path("/volunteers/{id:[0-9]*}/tasks/{taskid:[0-9]*}")
	@DELETE
	@Produces(MediaType.TEXT_HTML)
	public Response deleteTaskToVolunteer(@QueryParam("access_token") String accessToken, @PathParam("id") int id, @PathParam("taskid") int taskID) throws SQLException,ClassNotFoundException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, ParseException {
		int responseCode = 404;
		String responseMessage = "Volunteer with the id " + id + " is not in the Database.";
		if (Authentication.authenticationByToken(accessToken)){
			JSONObject decryptedToken = Authentication.getTokenDecrypted(accessToken);
			
			if((int)(long)decryptedToken.get("role") == 2 || (int)decryptedToken.get("id") == id){				
				Database.operationOnTable("DELETE FROM GardenResponsibility WHERE id=" + taskID);
	
				responseCode = 200;
				responseMessage = "The task has been deleted.";
					
			}else{
				responseMessage = "Access denied: You are not authorized to modify other volunteer.";
			}
		} else {
			responseCode = 401;
			responseMessage = "Access denied: the access token is invalid.";
		}
		return Response.status(responseCode).entity(responseMessage).build();
	}
	
	@Path("/volunteers/{id:[0-9]*}/tasks/{taskid:[0-9]*}")
	@POST
	@Produces(MediaType.TEXT_HTML)
	public Response editTaskToVolunteer(@QueryParam("access_token") String accessToken, @PathParam("id") int id, @PathParam("taskid") int taskID, @FormParam("taskdesc") String taskDesc) throws SQLException,ClassNotFoundException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, ParseException {
		int responseCode = 404;
		String responseMessage = "Volunteer with the id " + id + " is not in the Database.";
		if (Authentication.authenticationByToken(accessToken)){
			JSONObject decryptedToken = Authentication.getTokenDecrypted(accessToken);
			
			if((int)(long)decryptedToken.get("role") == 2 || (int)decryptedToken.get("id") == id){		
				Database.operationOnTable("UPDATE GardenResponsibility SET task='" + taskDesc + "' WHERE id=" + taskID);
	
				responseCode = 200;
				responseMessage = "The task has been updated.";
					
			}else{
				responseMessage = "Access denied: You are not authorized to modify other volunteer.";
			}
		} else {
			responseCode = 401;
			responseMessage = "Access denied: the access token is invalid.";
		}
		return Response.status(responseCode).entity(responseMessage).build();
	}
	
	@SuppressWarnings("unchecked")
	@Path("/volunteers/{id:[0-9]*}/tasks/{taskid:[0-9]*}")
	@GET
	@Produces("application/json")
	public Response getTaskToVolunteer(@QueryParam("access_token") String accessToken, @PathParam("id") int id, @PathParam("taskid") int taskID) throws SQLException,ClassNotFoundException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, ParseException {
		int responseCode = 404;
		JSONObject task = new JSONObject();
		
		if (Authentication.authenticationByToken(accessToken)){
			JSONObject decryptedToken = Authentication.getTokenDecrypted(accessToken);
			
			if((int)(long)decryptedToken.get("role") == 2 || (int)(long)decryptedToken.get("id") == id){		
				
				ResultSet resultSet = Database.tableRequest("SELECT id, ID_Volunteer, task, ID_Garden FROM gardenResponsibility WHERE id=" + taskID + " AND ID_Volunteer=" + id);

				if (!resultSet.next()) {
					task.put("error", "no data found.");
					responseCode = 200;
				} else {
					task.put("taskID", resultSet.getInt("id"));
					task.put("volunteerID", resultSet.getInt("ID_Volunteer"));
					task.put("task", resultSet.getString("task"));
					task.put("gardenID", resultSet.getInt("ID_garden"));
					
					responseCode = 200;
				}	
			}else{
				responseCode = 200;
				task.put("error", "Access denied: You are not authorized to modify other volunteer's task.");
			}
		} else {
			responseCode = 401;
			task.put("error", "Access denied: the access token is invalid.");
		}
		return Response.status(responseCode).entity(task.toJSONString()).build();
	}
}
