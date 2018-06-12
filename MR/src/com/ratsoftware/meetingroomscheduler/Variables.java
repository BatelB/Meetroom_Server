package com.ratsoftware.meetingroomscheduler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;

/**
 * Config file for socket port number, DB credentials etc..
 * 
 */
public class Variables {

	//######################### SOCKET #########################
	/**
	 *Not used
	 */
	public static String default_adress="";
	/**
	 * port number of ther socket
	 */
    public static int port = 55555;
    /**
     * the size of the socket queue
     */
    public static int backlog = 10000;
    /**
     * Our current connections
     */
    public static ArrayList<ClientSocket> connectedClients;
    /**
     * Counts the current connected users
     */
    public static int users_count = 0;
    //######################### SOCKET #########################
    
    //######################### MYSQL #########################
    /**The mysql driver for JAVA
     */
    public static String driverName = "org.gjt.mm.mysql.Driver";
    /**The location of our DB, in this case the DB running locally
     */
    public static String serverName = "localhost";
    /**The DB logical name
     */
    public static String mydatabase = "meeting_room_scheduler";
    /**The full DB path
     * 
     */
    public static String url = "jdbc:mysql://" + serverName + "/" + mydatabase; 
    /**Mysql administrator user
     * 
     */
    public static String username = "root";
    /**
     * Mysql password
     */
    public static String password = "";
    /**A connection (session) with a specific database. SQL statements are executed and results are returned within the context of a connection.
     * 
     */
    public static Connection conn;
    //######################### MYSQL #########################

}
