package com.opentripplanner.api.nextDepartureTime;

public class LineAndTime {
    
    private String line_number;
    private String next_departure_time;
    
    public LineAndTime(String line_number,String next_departure_time) {
    	this.line_number = line_number;
    	this.next_departure_time = next_departure_time;
    }
    
	public String getLine_number() {
		return line_number;
	}
	public void setLine_number(String line_number) {
		this.line_number = line_number;
	}
	public String getNext_departure_time() {
		return next_departure_time;
	}
	public void setNext_departure_time(String next_departure_time) {
		this.next_departure_time = next_departure_time;
	}
    
}