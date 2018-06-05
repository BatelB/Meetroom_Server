package com.ratsoftware.meetingroomscheduler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;

public class Variables {

	//######################### SOCKET #########################
	public static String default_adress="";
    public static int port = 55555;
    public static int backlog = 10000;
    public static ArrayList<ClientSocket> connectedClients;
    public static int users_count = 0;
    //######################### SOCKET #########################
    
    //######################### MYSQL #########################
    public static String driverName = "org.gjt.mm.mysql.Driver";
    public static String serverName = "localhost";
    public static String mydatabase = "meeting_room_scheduler";
    public static String url = "jdbc:mysql://" + serverName + "/" + mydatabase; 
    public static String username = "root";
    public static String password = "";
    public static Connection conn;
    //######################### MYSQL #########################

}
