package ca.uqam.latece.rest.database;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import com.mysql.jdbc.Connection;

public class Database {
	private String username = "root";
	private String password = "root";
	private String dbms = "mysql";
	private String serverName = "localhost";
	private String portNumber = "3306";
	private String databaseName = "VolunteerDB";
	private static Connection connection = null;

	public Database(){
		try{
			connection = this.connectToDatabase();
		}catch(Exception e){
			System.out.println("Error: " + e);
		}
	}

	private Connection connectToDatabase(){
		Connection connection = null;
		try{
			Class.forName("com.mysql.jdbc.Driver");
			
			Properties connectionProps = new Properties();
			
			connectionProps.put("user", this.username);
			connectionProps.put("password", this.password);
			
			if(this.dbms.equals("mysql")){
				connection = (Connection) DriverManager.getConnection(
						"jdbc:" + this.dbms + "://" +
						this.serverName + ":" + 
						this.portNumber + "/" +
						this.databaseName,
						connectionProps);
			}
		}catch(Exception e){
			System.out.println("Error: " + e);
		}
		return connection;
	}
	
	public static void databaseRequest(String sql) throws SQLException{
		Statement statement = null;
		
		statement = connection.createStatement();
		statement.executeUpdate(sql);
		System.out.println("Requete executée!");
		
		if(statement != null){
			statement.close();
		}
	}
	 
	public static ResultSet tableRequest(String sql) throws SQLException{
		Statement statement = null;
		
		statement = connection.createStatement();
		ResultSet resultSet = statement.executeQuery(sql);
		
		return resultSet;
	}
	
	public static Connection getConnection() {
		if(connection == null){
			new Database();
		}
		return connection;
	}
	
	public static void operationOnTable(String sql) throws SQLException{
		Statement statement = connection.createStatement();
		
		statement.executeUpdate(sql);
	}
}
