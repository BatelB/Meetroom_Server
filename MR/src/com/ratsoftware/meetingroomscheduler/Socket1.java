package com.ratsoftware.meetingroomscheduler;

import com.sun.org.apache.xpath.internal.SourceTree;
import com.sun.org.apache.xpath.internal.operations.Variable;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Scanner;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Socket1 {

	public static ServerSocket server = null;

	public static void main(String args[]) {

		if (args.length > 1) {
			Variables.port = Integer.parseInt(args[1]);
		}

		try {
			Class.forName(Variables.driverName);
			System.out.println("Mysql driver initialized");
		} catch (ClassNotFoundException e) {
			System.out.println("ClassNotFoundException");
			e.printStackTrace();
			return;
		}

		try {
			System.out.println("Opening socket...");
			server = new ServerSocket(Variables.port, Variables.backlog, InetAddress.getLocalHost());
			System.out.println("Server started");
			System.out.println("Port        : " + server.getLocalPort());
			System.out.println("Address      : " + server.getInetAddress());
			System.out.println("Backlog     : " + Variables.backlog);
			System.out.println("Running server...");
		} catch (IOException e) {
			System.out.println("Socket exception: ");
			e.printStackTrace();
			return;
		}

		Variables.connectedClients = new ArrayList<>();

		new Thread(new Runnable() {
			@Override
			public void run() {
				Socket socket = null;
				while (true) {
					try {
						socket = server.accept();
					} catch (IOException e) {
						System.out.println("I/O error: " + e);
					}
					ClientSocket newClient = new ClientSocket(socket,
							"user_" + Integer.toString(Variables.users_count));
					Variables.connectedClients.add(newClient);
					newClient.start();
					Variables.users_count++;
				}
			}
		}).start();

	}

}