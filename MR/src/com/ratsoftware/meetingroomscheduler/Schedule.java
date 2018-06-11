package com.ratsoftware.meetingroomscheduler;

public class Schedule {

	String room_id;
	String begin_time;
	String end_time;
	
	public Schedule(String room_id, String begin_time, String end_time) {
		this.room_id = room_id;
		this.begin_time=begin_time;
		this.end_time = end_time;
	}
}
