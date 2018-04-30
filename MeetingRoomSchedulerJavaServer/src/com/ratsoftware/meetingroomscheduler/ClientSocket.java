package com.ratsoftware.meetingroomscheduler;

import com.sun.org.apache.xpath.internal.operations.Variable;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ClientSocket  extends Thread {

    public String uid;

    protected Socket socket;
    InputStream inputStream = null;       //get data from client
    BufferedReader bufferedReader = null; //read data from inputStream
    DataOutputStream outputStream = null; //send data to client


    public ClientSocket(Socket clientSocket, String uid) {
        this.socket = clientSocket;
        this.uid = uid;
    }

    public void run() {

        try {
            inputStream = socket.getInputStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            outputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            clear();
            return;
        }

        String line;
        while (true) {
            try {
                line = bufferedReader.readLine();
                if (line == null) {
                    clear();
                    return;
                } else {
                    System.out.println("Package received from: " + uid); 
                    String response = processRequest(line);
                    System.out.println("Package sent to: " + uid); 
                    send(response);
                }
            } catch (IOException e) {
                e.printStackTrace();
                clear();
                return;
            }
        }
    }

    public String processRequest(String param){ 
    	try{
	    	JSONObject json = new JSONObject(param);
	    	String action = json.getString("action");
	    	System.out.println("action : " + action);
	    	
	    	if(action.equals("login_user")){
	    		return login_user(json.getString("email"), json.getString("password"));
	    	}else if(action.equals("get_rooms_list")){
	    		return get_rooms_list(json.getString("email"), json.getString("password"));
	    	}else if(action.equals("get_users_list")){
	    		return get_users_list(json.getString("email"), json.getString("password"));
	    	}else if(action.equals("get_schedule_for_room")){
	    		return get_schedule_for_room(json.getString("email"), json.getString("password"), json.getString("room_id"));
	    	}else if(action.equals("create_schedule")){
	    		return create_schedule(json.getString("email"), json.getString("password"), json.getString("room_id"), json.getString("begin_time"), json.getString("end_time"), json.getString("invitations"));
	    	}else if(action.equals("delete_schedule")){
	    		return delete_schedule(json.getString("email"), json.getString("password"), json.getString("id"));
	    	}else if(action.equals("edit_schedule")){
	    		return edit_schedule(json.getString("email"), json.getString("password"), json.getString("schedule_id"), json.getString("begin_time"), json.getString("begin_time"), json.getString("invitations"));
	    	}else if(action.equals("get_schedule_by_id")){
	    		return get_schedule_by_id(json.getString("email"), json.getString("password"), json.getString("id"));
	    	}else if(action.equals("get_my_schedule")){
	    		return get_my_schedule(json.getString("email"), json.getString("password"));
	    	}else if(action.equals("create_user")){
	    		return create_user(json.getString("email"), json.getString("password"), json.getString("new_email"), json.getString("new_password"), json.getString("type"), json.getString("fullname"));
	    	}else if(action.equals("delete_user")){
	    		return delete_user(json.getString("email"), json.getString("password"), json.getString("id"));
	    	}else if(action.equals("create_room")){
	    		return create_room(json.getString("email"), json.getString("password"), json.getString("number"),  json.getString("floor"), json.getString("chairs"), json.getString("equipment"));
	    	}else if(action.equals("edit_room")){
	    		return edit_room(json.getString("email"), json.getString("password"), json.getString("room_id"), json.getString("number"),  json.getString("floor"), json.getString("chairs"), json.getString("equipment"));
	    	}else if(action.equals("delete_room")){
	    		return delete_room(json.getString("email"), json.getString("password"), json.getString("id"));
	    	}
    	}catch (JSONException e) {
            e.printStackTrace();
        }
    	return "";
    }
    
    public String login_user(String email, String password){
    	
    	Map<String, String> result = new HashMap<>();
    
    	result.put("status", "false");
    	result.put("id", "");
    	result.put("type", "");
    	result.put("fullname", "");
    	result.put("message", "Login server error");
    	 
    	password = Utils.md5(password);

    	try {
	          Connection connection = DriverManager.getConnection(Variables.url, Variables.username, Variables.password);
		      
	          String query = "SELECT * FROM users WHERE email='" + email + "' AND password='" + password +"'";

	          Statement st = connection.createStatement();

	          ResultSet rs = st.executeQuery(query);
	          
		       if(rs.next()){
		            result.put("status", "true");
			      	result.put("id",  rs.getString("id"));
			      	result.put("type",  rs.getString("type"));
			      	result.put("fullname",  rs.getString("fullname"));
			      	result.put("message", "Success log in !");
			   }else{ 
			    	result.put("status", "Wrong username or password");
			   }
		        
	     } catch (SQLException e) {
	    	    System.out.println("catch in ligin_user()");
				e.printStackTrace();
		 }
    	
    	return new JSONObject(result).toString();
    }
    
 
    
    public String get_rooms_list(String email, String password){
    	
	 	String user_id = get_user_id(email, password);
	 	
	 	 System.out.println("user_id: " + user_id);
	 	 
	 	ArrayList<Map<String, String>> result = new ArrayList<>();

	 	
	 	if(user_id.equals("-1")){
	 		return "bad_request";
	 	}else{
	 		try {
	 			
		 		Connection connection = DriverManager.getConnection(Variables.url, Variables.username, Variables.password);
		        String query = "SELECT * FROM rooms";
		        Statement st = connection.createStatement();
		        ResultSet rs = st.executeQuery(query);
		         
			    while(rs.next()){
			        Map<String, String> item = new HashMap<>();
			        item.put("id", rs.getString("id"));
			        item.put("number", rs.getString("number"));
			        item.put("floor", rs.getString("floor"));
			        item.put("chairs", rs.getString("chairs"));
			        item.put("equipment", rs.getString("equipment"));  
			        result.add(item);
				}
		    
		 	} catch (SQLException e) {
				e.printStackTrace();
		 	}
	 	}
	 		       	
    	return new JSONArray(result).toString();
    }
    
 public String get_users_list(String email, String password){
    	
	 	String user_id = get_user_id(email, password);
	 	
	 	ArrayList<Map<String, String>> result = new ArrayList<>();

	 	if(user_id.equals("-1")){
	 		return "bad_request";
	 	}else{
	 		try {
	 			
		 		Connection connection = DriverManager.getConnection(Variables.url, Variables.username, Variables.password);
		        String query = "SELECT * FROM users";
		        Statement st = connection.createStatement();
		        ResultSet rs = st.executeQuery(query);
		         
			    while(rs.next()){
			        Map<String, String> item = new HashMap<>();
			        item.put("id", rs.getString("id"));
			        item.put("entry_date", rs.getString("entry_date"));
			        item.put("fullname", rs.getString("fullname"));
			        item.put("email", rs.getString("email"));
			        item.put("type", rs.getString("type"));  
			        result.add(item);
				}
		    
		 	} catch (SQLException e) {
				e.printStackTrace();
		 	}
	 	}
	 		       	
    	return new JSONArray(result).toString();
    }

   public String get_schedule_for_room(String email, String password, String room_id){
 	
	 	String user_id = get_user_id(email, password);

	 	ArrayList<Map<String, String>> result = new ArrayList<>();
	 	
	 	if(user_id.equals("-1")){
	 		return "bad_request";
	 	}else{
	 		try {
	 			
		 		Connection connection = DriverManager.getConnection(Variables.url, Variables.username, Variables.password);
		        String query = "SELECT * FROM schedule WHERE room_id = '" + room_id + "' ORDER BY begin_time ASC";
		        Statement st = connection.createStatement();
		        ResultSet rs = st.executeQuery(query);
		         
			    while(rs.next()){
			        Map<String, String> item = new HashMap<>();
			        item.put("id", rs.getString("id"));
			        item.put("manager_id", rs.getString("manager_id"));
			        item.put("begin", rs.getString("begin_time"));
			        item.put("end", rs.getString("end_time")); 
			        result.add(item);
				}
		    
		 	} catch (SQLException e) {
				e.printStackTrace();
		 	}
	 	}
	 		       	
 	return new JSONArray(result).toString();
 }

   public String create_schedule(String email, String password, String room_id, String begin_time, String end_time, String invitations){
	 	
	 	String user_id = get_user_id(email, password);
	 	
	 	System.out.println("Create schedule user id " + user_id);
	 	
	 	String result = "fail";
	 	
	 	if(user_id.equals("-1")){
	 		return "bad_request";
	 	}else{
	 		try {
	 			
		 		Connection connection = DriverManager.getConnection(Variables.url, Variables.username, Variables.password);
		        String query = "INSERT INTO schedule (room_id, manager_id, begin_time, end_time) VALUES('" + room_id + "', '" + user_id + "', '" + begin_time + "', '" + end_time + "')";
		        System.out.println("query|" + query + "|");
		        Statement st = connection.createStatement();
		        st.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
		        
		        String insert_id = "-1";
		        ResultSet rs = st.getGeneratedKeys();
		        if (rs.next()){
		        	 System.out.println("rs.next()");
		            insert_id=Integer.toString(rs.getInt(1));
		        }else{
		        	System.out.println("NOT rs.next()");
		        }
		        rs.close();
		        
		        System.out.println("Create schedule insert id: " + insert_id);
		        
		        if(insert_id.equals("-1"))
		        	return "database error";
		        
		        JSONArray json_array = new JSONArray(invitations);
		        for(int i=0; i<json_array.length(); i++){
		        	JSONObject json_object = json_array.getJSONObject(i);
		        	String invited_id = json_object.getString("id");
		        	query = "INSERT INTO invitations(schedule_id, user_id) VALUES ('" + insert_id + "', '" + invited_id + "')";
				    st.executeUpdate(query);
		        }
		        
		        result = insert_id;
			  
		    
		 	} catch (SQLException e) {
				e.printStackTrace();
		 	}
	 	}
	 		       	
 	return result;
 }
   
   public String edit_schedule(String email, String password, String schedule_id, String begin_time, String end_time, String invitations){
	 	
	 	String user_id = get_user_id(email, password);
	 	
	 	System.out.println("Create schedule user id " + user_id);
	 	
	 	if(user_id.equals("-1")){
	 		return "bad_request";
	 	}else{
	 		try {
	 				
		 		Connection connection = DriverManager.getConnection(Variables.url, Variables.username, Variables.password);
		
		 		String query = "UPDATE schedule SET begin_time = '" + begin_time + "', end_time = '" + end_time + "' WHERE id = '" + schedule_id + "'";
		        Statement st = connection.createStatement();
		        st.executeUpdate(query);
		        
		        query = "DELETE FROM invitations WHERE schedule_id = '" + schedule_id + "'";
		        st.executeUpdate(query);
		        
		        JSONArray json_array = new JSONArray(invitations);
		        for(int i=0; i<json_array.length(); i++){
		        	JSONObject json_object = json_array.getJSONObject(i);
		        	String invited_id = json_object.getString("id");
		        	query = "INSERT INTO invitations(schedule_id, user_id) VALUES ('" + schedule_id + "', '" + invited_id + "')";
				    st.executeUpdate(query);
		        }
		        
		       return "success";
		    
		 	} catch (SQLException e) {
		 		e.printStackTrace();
		 		return "database_error";
		 	}
	 	}
	 		       	
 }
   
   public String get_schedule_by_id(String email, String password, String id){
	   
	    String user_id = get_user_id(email, password);

	 	if(user_id.equals("-1")){
	 		return "bad_request";
	 	}else{
	 		try {
		 		Connection connection = DriverManager.getConnection(Variables.url, Variables.username, Variables.password);
		        String query = "SELECT * FROM schedule WHERE id = '" + id + "'";
		        Statement st = connection.createStatement();
		        ResultSet rs = st.executeQuery(query);
		        
		        Map<String, String> result = new HashMap<>();
		        if(rs.next()){
		        	result.put("begin", rs.getString("begin_time"));
		        	result.put("end", rs.getString("end_time"));
		        }else{
		        	return "database error";
		        }

		        Statement st1 = connection.createStatement();
		    	ResultSet rs1 = st1.executeQuery("SELECT * FROM invitations WHERE schedule_id = '" + id + "'");
		    	ArrayList<Map<String, String>> invitations = new ArrayList<>();

			    while(rs1.next()){
			    	String invited_user_id = rs1.getString("user_id");
			    	Statement st2 = connection.createStatement();
			    	ResultSet rs2 = st2.executeQuery("SELECT * FROM users WHERE id = '" + invited_user_id + "'"); 	
			    	if(rs2.next()){
			    		Map<String, String> item = new HashMap<>();
			    		item.put("user_id", rs2.getString("id"));
			    		item.put("fullname", rs2.getString("fullname"));
			    		invitations.add(item);
			    	}
				}
			    
			    result.put("invitations", new JSONArray(invitations).toString());
			    return new JSONObject(result).toString();
			    
		 	} catch (SQLException e) {
		 		e.printStackTrace();
		 		return "database_error";
		 	}
	 	}
	 		       	
   }
   
   
 public String get_my_schedule(String email, String password){
    	
	 	String user_id = get_user_id(email, password);
	 	
	 	ArrayList<Map<String, String>> result = new ArrayList<>();

	 	if(user_id.equals("-1")){
	 		return "bad_request";
	 	}else{ 
	 		try {
	 			
		 		Connection connection = DriverManager.getConnection(Variables.url, Variables.username, Variables.password);
		        String query = "SELECT * FROM invitations WHERE user_id = '" + user_id + "'";
		        Statement st1 = connection.createStatement();
		        ResultSet rs1 = st1.executeQuery(query);
		         
			    while(rs1.next()){
				    String schedule_id = rs1.getString("schedule_id");
				    query = "SELECT * FROM schedule WHERE id = '" + schedule_id + "'";
				    Statement st2 = connection.createStatement();
				    ResultSet rs2 = st2.executeQuery(query);
			        
			        if(rs2.next()){
				        String room_id = rs2.getString("room_id");
				        query = "SELECT * FROM rooms WHERE id = '" + room_id + "'";
				        Statement st3 = connection.createStatement();
				        ResultSet rs3 = st3.executeQuery(query);
				        
				        if(rs3.next()){
					        Map<String, String> item = new HashMap<>();
					        item.put("room_id", room_id);
					        item.put("room_number", rs3.getString("number"));
					        item.put("room_floor", rs3.getString("floor"));
					        item.put("schedule_id", schedule_id);
					        item.put("begin_time", "begin_time");
					        item.put("end_time", "end_time");
					        result.add(item);
				        }
			        }
				}
		    
		 	} catch (SQLException e) {
				e.printStackTrace();
		 	}
	 	}
	 		       	
    	return new JSONArray(result).toString();
    }
    
 public String delete_schedule(String email, String password, String id){
 	
	 	String user_id = get_user_id(email, password);
	 	
	 	ArrayList<Map<String, String>> result = new ArrayList<>();

	 	if(user_id.equals("-1")){
	 		return "bad_request";
	 	}else{
	 		try {
	 			
		 		Connection connection = DriverManager.getConnection(Variables.url, Variables.username, Variables.password);
		        String query = "DELETE FROM schedule WHERE id = '" + id + "'";
		        Statement st = connection.createStatement();
		        st.executeUpdate(query);
		       
		        query = "DELETE FROM invitations WHERE schedule_id = '" + id + "'";
		        st.executeUpdate(query);
		        
			    return "success";
		    
		 	} catch (SQLException e) {
				e.printStackTrace();
				return "database_error";
		 	}
	 	}
	 		       	
 }
 
 public String create_user(String email, String password, String new_email, String new_password, String type, String fullname){
	 	
	 	String user_id = get_user_id(email, password);
	 	
	 	if(user_id.equals("-1")){
	 		return "bad_request";
	 	}else if(!check_admin(user_id)){
	 		return "bad_request";
 		}else{
	 		try {	 			
	 			new_password = Utils.md5(new_password);
	 			Connection connection = DriverManager.getConnection(Variables.url, Variables.username, Variables.password);
		        String query = "INSERT INTO users (email, password, fullname, entry_date, type) VALUES('" + new_email + "', '" + new_password + "', '" + fullname + "', now(), '" + type + "')";
		        Statement st = connection.createStatement();
		        st.executeUpdate(query);
		        return "success";
		 	} catch (SQLException e) {
				e.printStackTrace();
		 	}
	 	}
	 		       	
	return "fail";
}
 
 public String delete_user(String email, String password, String id){
	 	
	 	String user_id = get_user_id(email, password);
	 	
	 	if(user_id.equals("-1")){
	 		return "bad_request";
	 	}else if(!check_admin(user_id)){
	 		return "bad_request";
		}else{
	 		try {	 			
	 			Connection connection = DriverManager.getConnection(Variables.url, Variables.username, Variables.password);
		        Statement st = connection.createStatement();
		        st.executeUpdate("DELETE FROM users WHERE id = '" + id + "'");
		        return "success";
		 	} catch (SQLException e) {
				e.printStackTrace();
		 	}
	 	}
	 		       	
	return "fail";
}


 public String create_room(String email, String password, String number, String floor, String chairs, String equipment){
	 	
	 	String user_id = get_user_id(email, password);
	 	
	 	if(user_id.equals("-1")){
	 		return "bad_request";
	 	}else if(!check_admin(user_id)){
	 		return "bad_request";
 		}else{
	 		try {	 			
	 			Connection connection = DriverManager.getConnection(Variables.url, Variables.username, Variables.password);
		        String query = "INSERT INTO rooms (number, floor, chairs, equipment, entry_date) VALUES('" + number + "', '" + floor + "', '" + chairs + "', '" + equipment + "', now())";
		        Statement st = connection.createStatement();
		        st.executeUpdate(query);
		        return "success";
		 	} catch (SQLException e) {
				e.printStackTrace();
		 	}
	 	}
	 		       	
	return "fail";
}
 
 public String edit_room(String email, String password, String room_id, String number, String floor, String chairs, String equipment){
	 	
	 	String user_id = get_user_id(email, password);
	 	
	 	if(user_id.equals("-1")){
	 		return "bad_request";
	 	}else if(!check_admin(user_id)){
	 		return "bad_request";
		}else{
	 		try {	 			
	 			Connection connection = DriverManager.getConnection(Variables.url, Variables.username, Variables.password);
		        String query = "UPDATE rooms SET number = '" + number + "', floor = '" + floor + "', chairs = '" + chairs + "', equipment = '" + equipment + "' WHERE id = '" + room_id + "'";
	 			Statement st = connection.createStatement();
		        st.executeUpdate(query);
		        return "success";
		 	} catch (SQLException e) {
				e.printStackTrace();
		 	}
	 	}
	 		       	
	return "fail";
}

 
 public String delete_room(String email, String password, String id){
	 	
	 	String user_id = get_user_id(email, password);
	 	
	 	if(user_id.equals("-1")){
	 		return "bad_request";
	 	}else if(!check_admin(user_id)){
	 		return "bad_request";
		}else{
	 		try {	 			
	 			Connection connection = DriverManager.getConnection(Variables.url, Variables.username, Variables.password);
		        Statement st = connection.createStatement();
		        st.executeUpdate("DELETE FROM rooms WHERE id = '" + id + "'");
		        return "success";
		 	} catch (SQLException e) {
				e.printStackTrace();
		 	}
	 	}
	 		       	
	return "fail";
}


 
    public String get_user_id(String email, String password){
    	password = Utils.md5(password);
    	try {
	          Connection connection = DriverManager.getConnection(Variables.url, Variables.username, Variables.password); 
	          String query = "SELECT * FROM users WHERE email='" + email + "' AND password='" + password +"'";
	          Statement st = connection.createStatement();
	          ResultSet rs = st.executeQuery(query);
		      if(rs.next()){
			      	return rs.getString("id");  
			  } 
	     } catch (SQLException e) {
				e.printStackTrace();
		 }	
    	 return "-1";
    }
    
    public boolean check_admin(String user_id){
    	try {
	          Connection connection = DriverManager.getConnection(Variables.url, Variables.username, Variables.password); 
	          String query = "SELECT * FROM users WHERE id='" + user_id + "'";
	          Statement st = connection.createStatement();
	          ResultSet rs = st.executeQuery(query);
		      if(rs.next()){
			      	if(rs.getString("type").equals("admin")){
			      		return true;
			      	}
			  } 
	     } catch (SQLException e) {
				e.printStackTrace();
		 }	
    	 return false;
    }
    

    public void send(String data){
        System.out.println("Sending to: " + uid);
        try {
            outputStream.writeBytes(data + "\n");
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            clear();
        }
    }

    public void clear(){ 
        try {
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(int i=0; i< Variables.connectedClients.size(); i++) {
            if(Variables.connectedClients.get(i).uid.equals(uid)) {
                Variables.connectedClients.remove(i);
            }
        }

        Variables.users_count -= 1;

    }
}