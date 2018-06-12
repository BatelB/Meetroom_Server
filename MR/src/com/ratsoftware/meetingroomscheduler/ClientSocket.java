package com.ratsoftware.meetingroomscheduler;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mysql.jdbc.StringUtils;
import java.time.format.DateTimeFormatter;

/**
 * 
 * @author ilyafish bbatel
 *
 */
public class ClientSocket extends Thread {

	public String uid;

	protected Socket socket;
	InputStream inputStream = null; // get data from client
	BufferedReader bufferedReader = null; // read data from inputStream
	DataOutputStream outputStream = null; // send data to client

	public ClientSocket(Socket clientSocket, String uid) {
		this.socket = clientSocket;
		this.uid = uid;
	}

	/**
	 * // Actually running the server, bringing up the socket and waiting for
	 * input // stream
	 * 
	 */
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
		// We are making the server to listen to every packet that coming to our
		// port
		while (true) {
			try {
				line = bufferedReader.readLine();
				if (line == null) {
					clear();
					return;

				} else {
					// This will help us to follow the action that executed on
					// the server, and which
					// client is communicating with us
					System.out.println("Package received from: " + uid);
					//
					// We are processing the request and checking if it of our
					// conditions #1-#14
					String response = processRequest(line);
					System.out.println("Package sent to: " + uid);
					send(response);
				}
			} catch (IOException | ParseException e) {
				e.printStackTrace();
				clear();
				return;
			}
		}
	}

	/**
	 * All scenarios we are expecting the client to send us
	 * @throws ParseException 
	 */
	public String processRequest(String param) throws ParseException {
		try {
			// JSON is the simplest key:value data structure to pass data, JSON
			// can have
			// multiple levels
			JSONObject json = new JSONObject(param);
			String action = json.getString("action");
			System.out.println("action : " + action);
			/**
			 * #1: The Log in part
			 *
			 **/
			if (action.equals("login_user")) {
				return login_user(json.getString("email"), json.getString("password"));

				/**
				 * #2: Returning the room list and their properties
				 *
				 */
			} else if (action.equals("get_rooms_list")) {
				return get_rooms_list(json.getString("email"), json.getString("password"));
				/**
				 * #3: Returning the user list and their properties
				 *
				 */
			} else if (action.equals("get_users_list")) {
				return get_users_list(json.getString("email"), json.getString("password"));
				/**
				 * // #4: Returning all scheduled meeting for specific room
				 * 
				 */
			} else if (action.equals("get_schedule_for_room")) {
				return get_schedule_for_room(json.getString("email"), json.getString("password"),
						json.getString("room_id"));
				/**
				 * // #5: First creating a schedule and the update DB with each
				 * user id from the // invitations String
				 * 
				 */
			} else if (action.equals("create_schedule")) {
				return create_schedule(json.getString("email"), json.getString("password"), json.getString("room_id"),
						json.getString("begin_time"), json.getString("end_time"), json.getString("invitations"));
				// #6: First deleting a schedule and the update DB with each
				// user id from the
				// invitations String
			} else if (action.equals("delete_schedule")) {
				return delete_schedule(json.getString("email"), json.getString("password"), json.getString("id"));

				// #7: Here we can edit the begin time the end time and the user
				// we have invited
			} else if (action.equals("edit_schedule")) {
				return edit_schedule(json.getString("email"), json.getString("password"), json.getString("schedule_id"),
						json.getString("begin_time"), json.getString("begin_time"), json.getString("invitations"));

				// #8: Returns the schedule of the current user
			} else if (action.equals("get_my_schedule")) {
				return get_my_schedule(json.getString("email"), json.getString("password"));
			}
			// #9: Returning the scheduled room per id
			else if (action.equals("get_schedule_by_id")) {
				return get_schedule_by_id(json.getString("email"), json.getString("password"), json.getString("id"));
			}
			// #10: Create user, will work only for user with admin type
			else if (action.equals("create_user")) {
				return create_user(json.getString("email"), json.getString("password"), json.getString("new_email"),
						json.getString("new_password"), json.getString("type"), json.getString("fullname"));
				// #11: Delete user, will work only for user with admin type
			} else if (action.equals("delete_user")) {
				return delete_user(json.getString("email"), json.getString("password"), json.getString("id"));
				// #12: Create user, will work only for user with admin type
			} else if (action.equals("create_room")) {
				return create_room(json.getString("email"), json.getString("password"), json.getString("number"),
						json.getString("floor"), json.getString("chairs"), json.getString("equipment"));
				// #13: Edit user, will work only for user with admin type
			} else if (action.equals("edit_room")) {
				return edit_room(json.getString("email"), json.getString("password"), json.getString("room_id"),
						json.getString("number"), json.getString("floor"), json.getString("chairs"),
						json.getString("equipment"));
				// #14: Delete user, will work only for user with admin type
			} else if (action.equals("delete_room")) {
				return delete_room(json.getString("email"), json.getString("password"), json.getString("id"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * #1: The Log in part
	 * 
	 * @param email
	 * @param password
	 * @return
	 */
	public String login_user(String email, String password) {
		Map<String, String> result = new HashMap<>();
		result.put("status", "false");
		result.put("id", "");
		result.put("type", "");
		result.put("fullname", "");
		result.put("message", "Login server error");

		password = Utils.md5(password);

		try {
			Connection connection = DriverManager.getConnection(Variables.url, Variables.username, Variables.password);

			String query = "SELECT * FROM users WHERE email='" + email + "' AND password='" + password + "'";

			Statement st = connection.createStatement();

			ResultSet rs = st.executeQuery(query);

			if (rs.next()) {
				// If status returns true so the client will load Adapter by the
				// type:AdminPage.class ,ReadWriteUserPage.class,
				// ReadOnlyUserPage.class
				result.put("status", "true");
				result.put("id", rs.getString("id"));
				result.put("type", rs.getString("type"));
				result.put("fullname", rs.getString("fullname"));
				result.put("message", "Success log in !");
			} else {
				result.put("status", "Wrong username or password");
			}

		} catch (SQLException e) {
			System.out.println("catch in ligin_user()");
			e.printStackTrace();
		}

		return new JSONObject(result).toString();
	}

	/**
	 * //* #2: Returning the room list and their properties
	 * 
	 * @param email
	 * @param password
	 * @return
	 */
	public String get_rooms_list(String email, String password) {
		String user_id = get_user_id(email, password);

		System.out.println("user_id: " + user_id);

		ArrayList<Map<String, String>> result = new ArrayList<>();

		if (user_id.equals("-1")) {
			return "bad_request";
		} else {
			try {

				Connection connection = DriverManager.getConnection(Variables.url, Variables.username,
						Variables.password);
				String query = "SELECT * FROM rooms";
				Statement st = connection.createStatement();
				ResultSet rs = st.executeQuery(query);

				while (rs.next()) {
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

	/**
	 * #3: Returning the user list and their properties
	 * 
	 * @param email
	 * @param password
	 * @return
	 */
	public String get_users_list(String email, String password) {

		String user_id = get_user_id(email, password);

		ArrayList<Map<String, String>> result = new ArrayList<>();

		if (user_id.equals("-1")) {
			return "bad_request";
		} else {
			try {

				Connection connection = DriverManager.getConnection(Variables.url, Variables.username,
						Variables.password);
				String query = "SELECT * FROM users";
				Statement st = connection.createStatement();
				ResultSet rs = st.executeQuery(query);

				while (rs.next()) {
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

	/**
	 * #4: Returning all scheduled meeting for specific room
	 * 
	 * @param email
	 * @param password
	 * @param room_id
	 * @return
	 */
	public String get_schedule_for_room(String email, String password, String room_id) {

		String user_id = get_user_id(email, password);

		ArrayList<Map<String, String>> result = new ArrayList<>();

		if (user_id.equals("-1")) {
			return "bad_request";
		} else {
			try {

				Connection connection = DriverManager.getConnection(Variables.url, Variables.username,
						Variables.password);
				String query = "SELECT * FROM schedule WHERE room_id = '" + room_id + "' ORDER BY begin_time ASC";
				Statement st = connection.createStatement();
				ResultSet rs = st.executeQuery(query);

				while (rs.next()) {
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

	/**
	 * #5: First creating a schedule and the update DB with each user id from
	 * the invitations String
	 * 
	 * @param email
	 * @param password
	 * @param room_id
	 * @param begin_time
	 * @param end_time
	 * @param invitations
	 * @return
	 * @throws ParseException 
	 */
	public String create_schedule(String email, String password, String room_id, String begin_time, String end_time,
			String invitations) throws ParseException {

		String user_id = get_user_id(email, password);

		System.out.println("Create schedule user id " + user_id);

		String result = "fail";

		if (user_id.equals("-1")) {
			return "bad_request";
		} else {
			try {

				Connection connection = DriverManager.getConnection(Variables.url, Variables.username,
						Variables.password);
				// ToDo: add logic
				String bestRoom = findBestRoom(begin_time, end_time, invitations);
				if(bestRoom == "-1")
					return "no room availble";
				else
				{
				String query = "INSERT INTO schedule (room_id, manager_id, begin_time, end_time) VALUES('" + bestRoom
						+ "', '" + user_id + "', '" + begin_time + "', '" + end_time + "')";
				System.out.println("query|" + query + "|");
				Statement st = connection.createStatement();
				st.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);

				String insert_id = "-1";
				ResultSet rs = st.getGeneratedKeys();
				if (rs.next()) {
					System.out.println("rs.next()");
					insert_id = Integer.toString(rs.getInt(1));
				} else {
					System.out.println("NOT rs.next()");
				}
				rs.close();

				System.out.println("Create schedule insert id: " + insert_id);

				if (insert_id.equals("-1"))
					return "database error";

				JSONArray json_array = new JSONArray(invitations);
				for (int i = 0; i < json_array.length(); i++) {
					JSONObject json_object = json_array.getJSONObject(i);
					String invited_id = json_object.getString("id");
					query = "INSERT INTO invitations(schedule_id, user_id) VALUES ('" + insert_id + "', '" + invited_id
							+ "')";
					st.executeUpdate(query);
				}

				result = insert_id + "," +bestRoom;
				}
			} catch (SQLException | JSONException e) {
				e.printStackTrace();
			}
		}

		return result;
	}

	private String findBestRoom(String begin_time, String end_time, String invitations) throws ParseException {

		// Get all rooms
		List<Room> rooms = getAllRooms();
		System.out.println("All rooms: " + printListOfRooms(rooms));// R1
		
		// remove busy rooms
		List<Room> availbleRooms = removeBusyRooms(rooms, begin_time, end_time);
		System.out.println("availbleRooms: " + printListOfRooms(availbleRooms));// R1
		if (availbleRooms.isEmpty())
		{
			return "-1";
		}
		else{
		// Number of users invited to meeting 
		int numInvitedUsers = getNumOfUsersInvited(invitations);
		System.out.println("numInvitedUsers: " + numInvitedUsers); // R1
		
		String bestRoom = findSmallestDelta(availbleRooms, numInvitedUsers);
		System.out.println("bestRoom: " + bestRoom);// R1
		
		return bestRoom;
		}
	}

	private String findSmallestDelta(List<Room> availbleRooms, int numInvitedUsers) {
	    
		int index=-1;
	    int minDelta= 1000;
	    int currDelta;
	    
		for(int i=0;i<availbleRooms.size();i++)
		{
			currDelta = Integer.parseInt(availbleRooms.get(i).chairs) - numInvitedUsers ;
			System.out.println("the delta for room: " + availbleRooms.get(i).id + "is: " + currDelta);// R1
			if(  currDelta >= 0)
			{
				if ( currDelta < minDelta )
				{
					minDelta = currDelta;
					index = i;
				}
			}
		}
		
		if (index == -1){
			return "-1";
		}
		else {
			System.out.println("best Room from delta: " + availbleRooms.get(index).id);// R1
			return availbleRooms.get(index).id;
		}
	}

	private int getNumOfUsersInvited(String invitations) {
		  int count = 0;
	      int i = 0;
	      String pattern = "id";
	        // Keep calling indexOf for the pattern.
	        while ((i = invitations.indexOf(pattern, i)) != -1) {
	            // Change starting index
	            i += pattern.length();
	            // Increment count.
	            count++;
	        }
	        return count;
	}

	private List<Room> removeBusyRooms(List<Room> rooms, String begin_time, String end_time) throws ParseException {
		List<Room> availbleRooms = rooms;
		List<Schedule> schedules = getAllSchedules();
		
		for(int i=0;i<schedules.size();i++)
		{
			if (overlaps(begin_time, end_time, schedules.get(i).begin_time,schedules.get(i).end_time))
			{
				String roomToRemove = schedules.get(i).room_id;
				availbleRooms = removeRoomFromList(availbleRooms,roomToRemove);
				System.out.println("removeBusyRooms - availbleRooms List Of Rooms: "+ printListOfRooms(availbleRooms)) ;// R1	
				
			}
		}
		return availbleRooms;
	}

	private String printListOfRooms (List<Room> roomsList){
		String rooms = "list: ";
		for (int i=0;i<roomsList.size();i++)
		{
			rooms = rooms + " {room id: " + roomsList.get(i).id +  " chairs: " + roomsList.get(i).chairs +"}  ";
		}
		return rooms; 
	}
	private List<Room> removeRoomFromList(List<Room> availbleRooms, String roomToRemove) {
		List<Room> newListOfrooms = availbleRooms;
		System.out.println("removeRoomFromList - room to remove "+ roomToRemove) ;// R1	
		
		for(int i=0;i<newListOfrooms.size();i++)
		{
			if(newListOfrooms.get(i).id.equals(roomToRemove))
			{
				System.out.println("removeRoomFromList - removed room "+ newListOfrooms.get(i).id) ;// R1	
				newListOfrooms.remove(i);
				
				System.out.println("removeRoomFromList - newListOfrooms: "+ printListOfRooms(newListOfrooms)+ " \n /n   ") ;// R1					
				return newListOfrooms;
	
			}
		}
		return newListOfrooms;
	}

	private boolean overlaps(String begin_time1, String end_time1, String begin_time2, String end_time2)
			throws ParseException {
		// DateTimeFormatter parser = DateTimeFormatter.ofPattern("yyyy-MM-dd
		// HH:mm:ss");
		// DateTime dt = parser.pa
		// .parseDateTime(begin_time1);
		//
		Date startdate1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(begin_time1);
		Date enddate1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(end_time1);
		Date startdate2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(begin_time2);
		Date enddate2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(end_time2);

		if ((startdate1.before(startdate2) && enddate1.after(startdate2))
				|| (startdate1.before(enddate2) && enddate1.after(enddate2))
				|| (startdate1.before(startdate2) && enddate1.after(enddate2))) {	
			System.out.println("data1: "+ begin_time1 +" - "+ end_time1 +"data2: "+ begin_time2 +" - "+  end_time2 + " overlaps = true") ;// R1
			return true;
		} else {
			System.out.println("data1: "+ begin_time1 +" - "+ end_time1 +"data2: "+ begin_time2 +" - "+  end_time2 + " overlaps = false") ;// R1
			return false;
		}
	}

	private List<Schedule> getAllSchedules() {
		List<Schedule> schedules = new ArrayList<Schedule>();
		try {
			Connection connection = DriverManager.getConnection(Variables.url, Variables.username, Variables.password);
			String query = "SELECT room_id, begin_time, end_time FROM schedule";
			Statement st1 = connection.createStatement();
			ResultSet rs1 = st1.executeQuery(query);
			while (rs1.next()) {
				String room_id = rs1.getString(1);
				String begin_time = rs1.getString(2);
				String end_time = rs1.getString(3);
				schedules.add(new Schedule(room_id, begin_time, end_time));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return schedules;
	}

	public List<Room> getAllRooms() {
		List<Room> allRooms = new ArrayList<Room>();
		try {

			Connection connection = DriverManager.getConnection(Variables.url, Variables.username, Variables.password);
			String query = "SELECT id, chairs FROM rooms";
			Statement st1 = connection.createStatement();
			ResultSet rs1 = st1.executeQuery(query);

			while (rs1.next()) {
				String id = rs1.getString(1);
				String chairs = rs1.getString(2);
				allRooms.add(new Room(id, chairs));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return allRooms;
	}

	/**
	 * #6: First deleting a schedule and the update DB with each user id from
	 * the invitations String
	 * 
	 * @param email
	 * @param password
	 * @param id
	 * @return
	 */
	public String delete_schedule(String email, String password, String id) {

		String user_id = get_user_id(email, password);

		// ArrayList<Map<String, String>> result = new ArrayList<>();

		if (user_id.equals("-1")) {
			return "bad_request";
		} else {
			try {

				Connection connection = DriverManager.getConnection(Variables.url, Variables.username,
						Variables.password);
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

	/**
	 * #7: Here we can edit the begin time the end time and the user we have
	 * invited
	 * 
	 * @param email
	 * @param password
	 * @param schedule_id
	 * @param begin_time
	 * @param end_time
	 * @param invitations
	 * @return
	 * @throws JSONException 
	 */
	public String edit_schedule(String email, String password, String schedule_id, String begin_time, String end_time,
			String invitations) throws JSONException {

		String user_id = get_user_id(email, password);

		System.out.println("Create schedule user id " + user_id);

		if (user_id.equals("-1")) {
			return "bad_request";
		} else {
			try {

				Connection connection = DriverManager.getConnection(Variables.url, Variables.username,
						Variables.password);

				String query = "UPDATE schedule SET begin_time = '" + begin_time + "', end_time = '" + end_time
						+ "' WHERE id = '" + schedule_id + "'";
				Statement st = connection.createStatement();
				st.executeUpdate(query);
				// Here we are deleting the old schedule
				query = "DELETE FROM invitations WHERE schedule_id = '" + schedule_id + "'";
				st.executeUpdate(query);
				// recreate the invitation to each user in the DB
				JSONArray json_array = new JSONArray(invitations);
				for (int i = 0; i < json_array.length(); i++) {
					JSONObject json_object = json_array.getJSONObject(i);
					String invited_id = json_object.getString("id");
					query = "INSERT INTO invitations(schedule_id, user_id) VALUES ('" + schedule_id + "', '"
							+ invited_id + "')";
					st.executeUpdate(query);
				}

				return "success";

			} catch (SQLException e) {
				e.printStackTrace();
				return "database_error";
			}
		}

	}

	/**
	 * #8: Returns the schedule of the current user
	 * 
	 * @param email
	 * @param password
	 * @return
	 */
	public String get_my_schedule(String email, String password) {

		String user_id = get_user_id(email, password);

		ArrayList<Map<String, String>> result = new ArrayList<>();

		if (user_id.equals("-1")) {
			return "bad_request";
		} else {
			try {

				Connection connection = DriverManager.getConnection(Variables.url, Variables.username,
						Variables.password);
				String query = "SELECT * FROM invitations WHERE user_id = '" + user_id + "'";
				Statement st1 = connection.createStatement();
				ResultSet rs1 = st1.executeQuery(query);

				while (rs1.next()) {
					String schedule_id = rs1.getString("schedule_id");

					query = "SELECT * FROM schedule WHERE id = '" + schedule_id + "'";
					Statement st2 = connection.createStatement();
					ResultSet rs2 = st2.executeQuery(query);

					if (rs2.next()) {
						String room_id = rs2.getString("room_id");
						String begin_time = rs2.getString("begin_time");
						String end_time = rs2.getString("end_time");

						query = "SELECT * FROM rooms WHERE id = '" + room_id + "'";
						Statement st3 = connection.createStatement();
						ResultSet rs3 = st3.executeQuery(query);

						if (rs3.next()) {
							Map<String, String> item = new HashMap<>();
							item.put("room_id", room_id);
							item.put("room_number", rs3.getString("number"));
							item.put("room_floor", rs3.getString("floor"));
							item.put("schedule_id", schedule_id);
							item.put("begin_time", begin_time);
							item.put("end_time", end_time);
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

	/**
	 * #9: Returning the scheduled room per id
	 * 
	 * @param email
	 * @param password
	 * @param id
	 * @return
	 */
	public String get_schedule_by_id(String email, String password, String id) {
		String user_id = get_user_id(email, password);

		if (user_id.equals("-1")) {
			return "bad_request";
		} else {
			try {
				Connection connection = DriverManager.getConnection(Variables.url, Variables.username,
						Variables.password);
				String query = "SELECT * FROM schedule WHERE id = '" + id + "'";
				Statement st = connection.createStatement();
				ResultSet rs = st.executeQuery(query);

				Map<String, String> result = new HashMap<>();
				if (rs.next()) {
					result.put("begin", rs.getString("begin_time"));
					result.put("end", rs.getString("end_time"));
				} else {
					return "database error";
				}

				Statement st1 = connection.createStatement();
				ResultSet rs1 = st1.executeQuery("SELECT * FROM invitations WHERE schedule_id = '" + id + "'");
				ArrayList<Map<String, String>> invitations = new ArrayList<>();

				while (rs1.next()) {
					String invited_user_id = rs1.getString("user_id");
					Statement st2 = connection.createStatement();
					ResultSet rs2 = st2.executeQuery("SELECT * FROM users WHERE id = '" + invited_user_id + "'");
					if (rs2.next()) {
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

	/**
	 * #10: Create user, will work only for user with admin type
	 * 
	 * @param email
	 * @param password
	 * @param new_email
	 * @param new_password
	 * @param type
	 * @param fullname
	 * @return
	 */
	public String create_user(String email, String password, String new_email, String new_password, String type,
			String fullname) {

		String user_id = get_user_id(email, password);

		if (user_id.equals("-1")) {
			return "bad_request";
		} else if (!check_admin(user_id)) {
			return "bad_request";
		} else {
			try {
				new_password = Utils.md5(new_password);
				Connection connection = DriverManager.getConnection(Variables.url, Variables.username,
						Variables.password);
				String query = "INSERT INTO users (email, password, fullname, entry_date, type) VALUES('" + new_email
						+ "', '" + new_password + "', '" + fullname + "', now(), '" + type + "')";
				Statement st = connection.createStatement();
				st.executeUpdate(query);
				return "success";
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return "fail";
	}

	/**
	 * #11: Delete user, will work only for user with admin type
	 * 
	 * @param email
	 * @param password
	 * @param id
	 * @return
	 */
	public String delete_user(String email, String password, String id) {

		String user_id = get_user_id(email, password);

		if (user_id.equals("-1")) {
			return "bad_request";
		} else if (!check_admin(user_id)) {
			return "bad_request";
		} else {
			try {
				Connection connection = DriverManager.getConnection(Variables.url, Variables.username,
						Variables.password);
				Statement st = connection.createStatement();
				st.executeUpdate("DELETE FROM users WHERE id = '" + id + "'");
				return "success";
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return "fail";
	}

	/**
	 * #12: Create user, will work only for user with admin type
	 * 
	 * @param email
	 * @param password
	 * @param number
	 * @param floor
	 * @param chairs
	 * @param equipment
	 * @return
	 */
	public String create_room(String email, String password, String number, String floor, String chairs,
			String equipment) {

		String user_id = get_user_id(email, password);

		if (user_id.equals("-1")) {
			return "bad_request";
		} else if (!check_admin(user_id)) {
			return "bad_request";
		} else {
			try {
				Connection connection = DriverManager.getConnection(Variables.url, Variables.username,
						Variables.password);
				String query = "INSERT INTO rooms (number, floor, chairs, equipment, entry_date) VALUES('" + number
						+ "', '" + floor + "', '" + chairs + "', '" + equipment + "', now())";
				Statement st = connection.createStatement();
				st.executeUpdate(query);
				return "success";
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return "fail";
	}

	/**
	 * #13: Edit user, will work only for user with admin type
	 * 
	 * @param email
	 * @param password
	 * @param room_id
	 * @param number
	 * @param floor
	 * @param chairs
	 * @param equipment
	 * @return
	 */
	public String edit_room(String email, String password, String room_id, String number, String floor, String chairs,
			String equipment) {

		String user_id = get_user_id(email, password);

		if (user_id.equals("-1")) {
			return "bad_request";
		} else if (!check_admin(user_id)) {
			return "bad_request";
		} else {
			try {
				Connection connection = DriverManager.getConnection(Variables.url, Variables.username,
						Variables.password);
				String query = "UPDATE rooms SET number = '" + number + "', floor = '" + floor + "', chairs = '"
						+ chairs + "', equipment = '" + equipment + "' WHERE id = '" + room_id + "'";
				Statement st = connection.createStatement();
				st.executeUpdate(query);
				return "success";
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return "fail";
	}

	/**
	 * #14: Delete user, will work only for user with admin type
	 * 
	 * @param email
	 * @param password
	 * @param id
	 * @return
	 */
	public String delete_room(String email, String password, String id) {

		String user_id = get_user_id(email, password);

		if (user_id.equals("-1")) {
			return "bad_request";
		} else if (!check_admin(user_id)) {
			return "bad_request";
		} else {
			try {
				Connection connection = DriverManager.getConnection(Variables.url, Variables.username,
						Variables.password);
				Statement st = connection.createStatement();
				st.executeUpdate("DELETE FROM rooms WHERE id = '" + id + "'");
				return "success";
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return "fail";
	}

	/**
	 * Returning the unique user id by email and password
	 * 
	 * @param email
	 * @param password
	 * @return
	 */
	public String get_user_id(String email, String password) {
		password = Utils.md5(password);
		try {
			Connection connection = DriverManager.getConnection(Variables.url, Variables.username, Variables.password);
			String query = "SELECT * FROM users WHERE email='" + email + "' AND password='" + password + "'";
			Statement st = connection.createStatement();
			ResultSet rs = st.executeQuery(query);
			if (rs.next()) {
				return rs.getString("id");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "-1";
	}

	/**
	 * checking if this user has admin permissions
	 * 
	 * @param user_id
	 * @return
	 */
	public boolean check_admin(String user_id) {
		try {
			Connection connection = DriverManager.getConnection(Variables.url, Variables.username, Variables.password);
			String query = "SELECT * FROM users WHERE id='" + user_id + "'";
			Statement st = connection.createStatement();
			ResultSet rs = st.executeQuery(query);
			if (rs.next()) {
				if (rs.getString("type").equals("admin")) {
					return true;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * The server sending its response to the client
	 * 
	 * @param data
	 */
	public void send(String data) {
		System.out.println("Sending to: " + uid);
		try {
			outputStream.writeBytes(data + "\n");
			outputStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
			clear();
		}
	}

	/**
	 * finished the connections for this uid
	 */
	public void clear() {
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

		for (int i = 0; i < Variables.connectedClients.size(); i++) {
			if (Variables.connectedClients.get(i).uid.equals(uid)) {
				Variables.connectedClients.remove(i);
			}
		}

		Variables.users_count -= 1;

	}
}