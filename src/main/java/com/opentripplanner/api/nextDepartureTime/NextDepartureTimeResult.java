/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package com.opentripplanner.api.nextDepartureTime;

import java.util.LinkedList;
import java.util.List;

public class NextDepartureTimeResult {
    
    private double lat;
    private double lng;
    private String stopName;
    private List<LineAndTime> lineAndTime;
    
    public NextDepartureTimeResult() {}

    public NextDepartureTimeResult(double lat, double lng, String stopName) {
        this.lat = lat;
        this.lng = lng;
        this.stopName = stopName;
        lineAndTime = new LinkedList<LineAndTime>();
    }

    public double getLat() {
        return lat;
    }
    
    public void setLat(double lat) {
        this.lat = lat;
    }
    
    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

	public List<LineAndTime> getLineAndTime() {
		return lineAndTime;
	}

	public void setLineAndTime(List<LineAndTime> lineAndTime) {
		this.lineAndTime = lineAndTime;
	}

	public String getStopName() {
		return stopName;
	}

	public void setStopName(String stopName) {
		this.stopName = stopName;
	}
    
    

}
