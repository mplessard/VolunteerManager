package ca.uqam.latece.rest.garden;

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

@Path("/v1/gardenservice")
public class GardenService {
	Connection c = Database.getConnection();
	
	@SuppressWarnings("unchecked")
	@Path("/gardens/")
	@GET
	@Produces("application/json")
	public String gardens(@QueryParam("access_token") String accessToken) throws ClassNotFoundException, SQLException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, ParseException{
		JSONArray gardensList = new JSONArray();
		JSONObject gardens = new JSONObject();
		
		if(Authentication.authenticationByToken(accessToken)){
			ResultSet resultSet = Database.tableRequest("SELECT name FROM Garden");
			
			if(!resultSet.next()){
				gardens.put("error", "no data found");
			}else{
				resultSet.beforeFirst();
				
				while(resultSet.next()){
					gardensList.add(resultSet.getString("name"));
				}
				gardens.put("name", gardensList);
			}
		}else{
			gardens.put("error", "Access denied: The access token is invalid.");
		}
		return gardens.toJSONString();
	}

	@Path("/gardens/")
	@POST
	@Consumes("application/x-www-form-urlencoded")
	@Produces(MediaType.TEXT_HTML)
	public Response postGardens(@QueryParam("access_token") String accessToken, @FormParam("name") String name, @FormParam("category") int category, @FormParam("address") String address ) throws SQLException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, ParseException{
		int responseCode = 404;
		String responseMessage = "The garden was not created.";
		
		if(Authentication.authenticationByToken(accessToken)){
			JSONObject decryptedToken = Authentication.getTokenDecrypted(accessToken);

			if((Integer)decryptedToken.get("role") == 2){
				Pattern namePattern = Pattern.compile("[A-Za-z0-9_\\s]+");
				Pattern addressPattern = Pattern.compile("[0-9]+\\s[A-Za-z0-9_\\s]+");
				
				if(name != null && name != "" && namePattern.matcher(name).matches()){
					String sql = "SELECT COUNT(*) AS exist FROM garden WHERE name='" + name + "'";
					ResultSet resultSet = Database.tableRequest(sql);
					resultSet.next();
					
					if(resultSet.getInt("exist") == 0){
						sql = "SELECT id FROM gardenCategory";
						resultSet = Database.tableRequest(sql);
						boolean exist = false;
						
						while(resultSet.next() && exist == false){
							int x = resultSet.getInt("id");
							
							if(x == category){
								exist = true;
							}
						}
						
						if(exist){
							if(address != null && address != "" && addressPattern.matcher(address).matches()){
								sql = "INSERT INTO garden(name, ID_category, address) VALUES('" + name + "', '" + category + "', '" + address + "')";
								Database.operationOnTable(sql);
								
								responseCode = 200;
								responseMessage = "The garden has been created.";
							}else{
								responseMessage = "The address is not valid.";
							}
						}else{
							responseMessage = "The category is not valid.";
						}
					}else{
						responseMessage = "A garden with this name already exist.";
					}
				}else{
					responseMessage = "The name is not valid.";
				}
			}else{
				responseCode = 401;
				responseMessage = "Access denied: you are not authorized to add a garden.";
			}
		}else{
			responseCode = 401;
			responseMessage = "Access denied: the access token is invalid.";
		}
		return Response.status(responseCode).entity(responseMessage).build();
	}
	
	@SuppressWarnings("unchecked")
	@Path("/gardens/{id:[0-9]*}")
	@GET
	@Produces("application/json")
	public String gardenInfo(@QueryParam("access_token") String accessToken, @PathParam("id") int id) throws ClassNotFoundException, SQLException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, ParseException{	
		JSONObject garden = new JSONObject();

		ResultSet resultSet = Database.tableRequest("SELECT G.name, GC.name, address, G.id FROM Garden AS G, GardenCategory AS GC WHERE ID_Category = GC.id AND G.id =" + id);
		ResultSet resultSet2 = Database.tableRequest("SELECT GR.id, username, task FROM gardenResponsibility AS GR, volunteer AS V WHERE V.id = GR.ID_Volunteer AND ID_Garden =" + id);
		
		if (Authentication.authenticationByToken(accessToken)) {
			if(!resultSet.next()){
				garden.put("error", "no data found.");
			}else{
				garden.put("id", id);
				garden.put("name", resultSet.getString("G.name"));
				garden.put("category", resultSet.getString("GC.name"));
				garden.put("address", resultSet.getString("address"));
				if(resultSet2.next()){
					resultSet2.beforeFirst();
					JSONObject idVolunteer = new JSONObject();
					JSONObject volunteersAndTask = new JSONObject();
					while(resultSet2.next()){
						volunteersAndTask.put("username", resultSet2.getString("username"));
						volunteersAndTask.put("task", resultSet2.getString("task"));
						idVolunteer.put(resultSet2.getInt("GR.id"), volunteersAndTask);
						volunteersAndTask = new JSONObject();
					}
					garden.put("volunteers", idVolunteer);
				}
			}
			}else{
				garden.put("error", "Access denied: You are not authorized to add garden.");
			}
		return garden.toJSONString();
	}
	
	@Path("/gardens/{id:[0-9]*}")
	@POST
	@Consumes("application/x-www-form-urlencoded")
	@Produces(MediaType.TEXT_HTML)
	public Response updateGarden(@QueryParam("access_token") String accessToken,@PathParam("id") String id, @FormParam("name") String name, @FormParam("category") int category, @FormParam("address") String address) throws SQLException, ClassNotFoundException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, ParseException{
		int responseCode = 404;
		String responseMessage = "Garden with the id " + id + " is not in the Database.";
		
		if (Authentication.authenticationByToken(accessToken)) {
			JSONObject decryptedToken = Authentication.getTokenDecrypted(accessToken);
			
			if(decryptedToken.get("role") == "2"){
				Pattern namePattern = Pattern.compile("[A-Za-z0-9_\\s]+");
				Pattern addressPattern = Pattern.compile("[A-Za-z0-9_\\s]+");
				
				if(name != null && name != "" && namePattern.matcher(name).matches()){
					String sql = "SELECT id FROM gardenCategory";
					ResultSet resultSet = Database.tableRequest(sql);
					boolean exist = false;
					
					while(resultSet.next() && exist == false){					
						if(resultSet.getInt("id") == category){
							exist = true;
						}
					}
					if(exist){
						if(address != null && address != "" && addressPattern.matcher(address).matches()){
							sql = "UPDATE garden SET name=" + name + "' AND category='" + category + "' AND address='" + address + "'";
							Database.operationOnTable(sql);
							
							responseCode = 200;
							responseMessage = "The garden has been updated.";
						}else{
							responseMessage = "The address is not valid.";
						}
					}else{
						responseMessage = "The category is not valid.";
					}
				}else{
					responseMessage = "The name is not valid.";
				}
			}else{
				responseMessage = "Access denied: you are not authorized to modify a garden.";
			}
		} else {
			responseCode = 401;
			responseMessage = "Access denied: the access token is invalid.";
		}
		return Response.status(responseCode).entity(responseMessage).build();
	}
	
	@Path("/gardens/{id:[0-9]*}")
	@DELETE
	@Produces(MediaType.TEXT_HTML)
	public Response deleteGarden(@QueryParam("access_token") String accessToken, @PathParam("id") String id) throws ClassNotFoundException, SQLException{
		int responseCode = 404;
		String responseMessage = "Garden with the id " + id + " is not in the Database.";
		
		if (Authentication.authenticationByToken(accessToken)){
			ResultSet resultSet = Database.tableRequest("SELECT COUNT(*) FROM Garden WHERE ID =" + id);
			if(resultSet.next()){
				Database.operationOnTable("DELETE FROM Garden WHERE id = " + id);
				
				responseCode = 200;
				responseMessage = "The garden has been deleted.";
			}
		} else {
			responseCode = 401;
			responseMessage = "Access denied: the access token is invalid.";
		}
		return Response.status(responseCode).entity(responseMessage).build();
	}
	
	@Path("gardens/tasks")
	@POST
	@Consumes("application/x-www-form-urlencoded")
	public Response postTasks(@QueryParam("access_token") String accessToken, @FormParam("username") String username, @FormParam("garden") String garden, @FormParam("task") String task) throws SQLException, ClassNotFoundException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, ParseException{
		int responseCode = 404;
		String responseMessage = "The task was not add to the garden";
		
		if(Authentication.authenticationByToken(accessToken)){
			JSONObject decryptedToken = Authentication.getTokenDecrypted(accessToken);
			
			if((Integer) decryptedToken.get("role") == 2 || decryptedToken.get("username").equals(username)){
				if(username != null && username != ""){
					String sql = "SELECT id FROM Volunteer WHERE username='" + username + "'";
					ResultSet resultSet = Database.tableRequest(sql);
					
					if(resultSet.next()){
						int volunteerID = resultSet.getInt("id");
						
						if(garden != null && garden != ""){
							sql = "SELECT id FROM Garden WHERE name='" + garden + "'";
							resultSet = Database.tableRequest(sql);
							
							if(resultSet.next()){
								int gardenID = resultSet.getInt("id");
								sql = "INSERT INTO GardenResponsibility(ID_Volunteer, ID_Garden, task) VALUES(" + volunteerID + "," + gardenID + ",'" + task + "')";
								Database.operationOnTable(sql);
								
								responseCode = 200;
								responseMessage = "The task has been created.";
							}else{
								responseMessage = "The garden doesn't exist.";
							}
						}else{
							responseMessage = "The garden is not valid.";
						}
					}else{
						responseMessage = "The username doesn't exist.";
					}
				}else{
					responseMessage = "The username is not valid.";
				}
			}else{
				responseMessage = "Access denied you are not authorized to add a task for someone else.";
			}
		}else{
			responseMessage = "Access denied your token is invalid.";
		}
		return Response.status(responseCode).entity(responseMessage).build();
	}
	
	@Path("gardens/tasks/{id:[0-9]*}")
	@DELETE
	@Produces(MediaType.TEXT_HTML)
	public Response deleteTasks(@QueryParam("access_token") String accessToken, @PathParam("id") int id) throws SQLException, ClassNotFoundException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, ParseException{
		int responseCode = 404;
		String responseMessage = "The task was not deleted.";
		
		if(Authentication.authenticationByToken(accessToken)){
			JSONObject decryptedToken = Authentication.getTokenDecrypted(accessToken);
			String sql = "SELECT * FROM GardenResponsibility WHERE id =" + id;
			ResultSet resultSet = Database.tableRequest(sql);
			
			if(resultSet.next()){
				int volunteerID = resultSet.getInt("ID_Volunteer");
				
				if((Integer) decryptedToken.get("role") == 2 || decryptedToken.get("id").equals(volunteerID)){
					sql = "DELETE FROM GardenResponsibility WHERE id=" + id;
					Database.operationOnTable(sql);
					
					responseCode = 200;
					responseMessage = "The task has been deleted.";
				}else{
					responseMessage = "Access denied you are not authorized to delete a task for someone else.";
				}
			}else{
				responseMessage = "The task doesn't exist.";
			}
		}else{
			responseMessage = "Access denied your token is invalid.";
		}
		return Response.status(responseCode).entity(responseMessage).build();
	}
	
	@Path("gardens/tasks/{id:[0-9]*}")
	@POST
	@Consumes("application/x-www-form-urlencoded")
	@Produces(MediaType.TEXT_HTML)
	public Response updateTasks(@QueryParam("access_token") String accessToken, @PathParam("id") int id, @FormParam("garden") String garden, @FormParam("task") String task) throws SQLException, ClassNotFoundException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, ParseException{
		int responseCode = 404;
		String responseMessage = "The task was not updated.";
		
		if(Authentication.authenticationByToken(accessToken)){
			JSONObject decryptedToken = Authentication.getTokenDecrypted(accessToken);
			String sql = "SELECT * FROM GardenResponsibility WHERE id =" + id;
			ResultSet resultSet = Database.tableRequest(sql);
			
			if(resultSet.next()){
				int volunteerID = resultSet.getInt("ID_Volunteer");
				
				if((Integer)decryptedToken.get("role") == 2 || decryptedToken.get("id").equals(volunteerID)){
					garden = garden.replaceAll("'","''");
					
					if(garden != null && garden != ""){
						sql = "SELECT id FROM Garden WHERE name='" + garden + "'";
						resultSet = Database.tableRequest(sql);
						
						if(resultSet.next()){
							int gardenID = resultSet.getInt("id");
							sql = "UPDATE GardenResponsibility SET ID_Garden=" + gardenID + ", task='" + task + "' WHERE id=" + id;
							
							Database.operationOnTable(sql);
						
							responseCode = 200;
							responseMessage = "The task has been updated.";
						}else{
							responseMessage = "There's no garden with this name.";
						}
					}else{
						responseMessage = "The garden's name is invalid.";
					}
				}else{
				responseMessage = "Access denied you are not authorized to delete a task for someone else.";
				}
			}else{
				responseMessage = "The task doesn't exist.";
			}
		}else{
			responseMessage = "Access denied your token is invalid.";
		}
		return Response.status(responseCode).entity(responseMessage).build();
	}
}