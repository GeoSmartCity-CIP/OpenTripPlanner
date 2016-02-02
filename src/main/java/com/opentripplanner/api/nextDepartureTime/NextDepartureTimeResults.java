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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;


@XmlRootElement
public class NextDepartureTimeResults {

    private String error;
    private Map<String,String> requestParameters;
    private Collection<NextDepartureTimeResult> results;
    
    public NextDepartureTimeResults() {}
    
    public NextDepartureTimeResults(String error) {
        this.error = error;
    }
    
    public NextDepartureTimeResults(Collection<NextDepartureTimeResult> results) {
        this.results = results;
    }

    @XmlElement(required=false)
    public String getError() {
        return error;
    }

    
    public void setError(String error) {
        this.error = error;
    }

    public Map<String, String> getRequestParameters() {
		return requestParameters;
	}

	public void setRequestParameters(Map<String, String> requestParameters) {
		this.requestParameters = requestParameters;
	}

	@XmlElementWrapper(name="results")
    @XmlElement(name="result")
    @JsonProperty(value="results")
    public Collection<NextDepartureTimeResult> getResults() {
        return results;
    }

    
    public void setResults(Collection<NextDepartureTimeResult> results) {
        this.results = results;
    }
    
    public void addResult(NextDepartureTimeResult result) {
        if (results == null)
            results = new ArrayList<NextDepartureTimeResult>();
        results.add(result);
    }

    @XmlElement(name="count")
    public int getCount() {
        return results != null ? results.size() : 0;
    }
    
}
