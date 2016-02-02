package com.opentripplanner.api.nextDepartureTime;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.index.model.StopTimesInPattern;
import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.routing.edgetype.PatternEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * OTP simple built-in geocoder.
 * Client geocoder modules usually read XML, but GeocoderBuiltin reads JSON.
 */
/**
 * @author A2LL0148
 *
 */
@Path("/routers/{routerId}/nextDepartureTime")
@Produces(MediaType.APPLICATION_JSON)
public class NextDepartureTimeResource {
	
	private static final Logger LOG = LoggerFactory.getLogger(NextDepartureTimeResource.class);
	
	private static final double MAX_STOP_SEARCH_RADIUS = 5000;

	
	/**
	 *  longitude of the current position coordinate.
	 */
	@QueryParam("lon") Double lon;
	
	/**
	 * latitude of the current position coordinate.
	 */
	@QueryParam("lat") Double lat;
	
	/**
	 * research buffer dimension in meters. Default 500 mt.
	 */
	@QueryParam("buffer") Double buffer;
	
	/**
	 * research starting time. Defaults to server current time.
	 */
	@QueryParam("time") Long time;
	
	/**
	 * research starting time offset in minutes. Defaults to 30 mins.
	 */
	@QueryParam("timeOffset") Integer timeOffset;
	
	/**
	 * public transport line to check. If not specified, get all of them.
	 */
	@QueryParam("lineNumber") String lineNumber;

	
	@Context
    protected OTPServer otpServer;

	@PathParam("routerId") 
    public String routerId;
	
	private Graph graph;
	private SpatialIndex edgeTree;
    private SpatialIndex transitStopTree;
    private SpatialIndex verticesTree;
	
    /**
     *  This service retrieves all the stops within a specified range from the given coordinate. 
     *  Then for every stop found it returns the next departure time of each line avaliable at that stop.
     *  
     *  The parameters that should be specified in the url get request are:
     *  lan and lon to define the starting point of the research.
     *  buffer to define the research area.
     *  Time and TimeOffset to define the starting time for the research and some delay which may be needed to reach the stop. Time defaults to current time, offset defaults to 30 mins.
     *  If line number is passed the service will only check the given line.
     *  
     * 
     * @param otpServer
     * @param uriInfo
     * @return
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON + "; charset=UTF-8"})
    public NextDepartureTimeResults getNextDepartureTime(@Context OTPServer otpServer, @Context UriInfo uriInfo) {
    	LOG.info("GetNextDepartureTime elaboration..."); 

    	this.otpServer = otpServer;
    	Router router = otpServer.getRouter(routerId);
    	initializeTrees(router.graph,true);
    	
    	setDefaults();
  
        List<Vertex> closestVertexList = getLocalPatternDepartVertex();
        NextDepartureTimeResults response = getResponse(closestVertexList);
				
		LOG.info("GetNextDepartureTime elaboration ended..."); 
		return response;
    }
    
    
    /**
     *  Set parameters to default values if they were not passed through the request.
     */
    private void setDefaults() {
    	LOG.info("setDefaults elaboration..."); 
    	
    	if(buffer == null) {
    		buffer = 500d;
    	}
    	if(time == null) {
    		time = System.currentTimeMillis() / 1000;
    	}
    	if(timeOffset == null) {
    		timeOffset = 30;
    	}
    	
    	LOG.info("setDefaults elaboration ended..."); 
    }
    
    /**
     * Fills the map containing the request parameters, to be later added to the response.
     * 
     * @return
     */
    private Map<String,String> createNextDepartureParameters() {
    	LOG.info("createNextDepartureParameters elaboration..."); 
    	
    	Map<String,String> requestParameters = new HashMap<String,String>();
    	
    	if(lon != null) {
    		requestParameters.put("lon",lon.toString());
    	}
    	
    	if(lat != null) {
    		requestParameters.put("lat",lat.toString());
    	}
    	
    	if(buffer != null) {
    		requestParameters.put("buffer",buffer.toString());
    	}
    	
    	if(time != null) {
    		requestParameters.put("time",time.toString());
    	}
    	
    	if(timeOffset != null) {
    		requestParameters.put("timeOffset",timeOffset.toString());
    	}
    	
    	if(lineNumber != null) {
    		requestParameters.put("lineNumber",lineNumber.toString());
    	}
    	
    	LOG.info("createNextDepartureParameters elaboration ended..."); 
    	
    	return requestParameters;
    }
    
    /**
     * Initialize trees.
     * 
     * @param graph
     * @param hashGrid
     */
    public void initializeTrees(Graph graph, boolean hashGrid) {
    	LOG.info("initializeTrees elaboration..."); 
    	
        this.graph = graph;
        if (hashGrid) {
            edgeTree = new HashGridSpatialIndex<>();
            transitStopTree = new HashGridSpatialIndex<>();
            verticesTree = new HashGridSpatialIndex<>();
        } else {
            edgeTree = new STRtree();
            transitStopTree = new STRtree();
            verticesTree = new STRtree();
        }
        postSetup();
        if (!hashGrid) {
            ((STRtree) edgeTree).build();
            ((STRtree) transitStopTree).build();
        }
        
        LOG.info("initializeTrees elaboration ended..."); 
    }
    
    /**
     * 
     */
    @SuppressWarnings("rawtypes")
    private void postSetup() {
    	LOG.info("postSetup elaboration..."); 
    	
        for (Vertex gv : graph.getVertices()) {
            Vertex v = gv;
            /*
             * We add all edges with geometry, skipping transit, filtering them out after. We do not
             * index transit edges as we do not need them and some GTFS do not have shape data, so
             * long straight lines between 2 faraway stations will wreck performance on a hash grid
             * spatial index.
             * 
             * If one need to store transit edges in the index, we could improve the hash grid
             * rasterizing splitting long segments.
             */
            for (Edge e : gv.getOutgoing()) {
                if (e instanceof PatternEdge)
                    continue;
                LineString geometry = e.getGeometry();
                if (geometry == null) {
                    continue;
                }
                Envelope env = geometry.getEnvelopeInternal();
                if (edgeTree instanceof HashGridSpatialIndex)
                    ((HashGridSpatialIndex)edgeTree).insert(geometry, e);
                else
                    edgeTree.insert(env, e);
            }
            if (v instanceof TransitStop) {
                Envelope env = new Envelope(v.getCoordinate());
                transitStopTree.insert(env, v);
            }
            Envelope env = new Envelope(v.getCoordinate());
            verticesTree.insert(env, v);
        }
        LOG.info("postSetup elaboration ended..."); 
    }

    
    /**
     * Returns all the park and ride vertices within a certain radius distance to the specified location. 
     */
    public List<Vertex> getLocalPatternDepartVertex() {
    	
    	LOG.info("getLocalPatternDepartVertex elaboration..."); 

        if (buffer > MAX_STOP_SEARCH_RADIUS){
        	buffer = MAX_STOP_SEARCH_RADIUS;
        }
        Coordinate coord = new Coordinate(lon, lat);
        
        
        Envelope env = new Envelope(coord);
	    env.expandBy(SphericalDistanceLibrary.metersToDegrees(buffer));
	    List<Vertex> nearby = verticesTree.query(env);
	    List<Vertex> results = new ArrayList<Vertex>();
	    for (Vertex v : nearby) {
		    if(v instanceof PatternDepartVertex) {
		    	if (SphericalDistanceLibrary.distance(v.getCoordinate(), coord) <= buffer) {
		    		results.add(v);
	            }
	      	}
	    }
	    
	    LOG.info("getLocalPatternDepartVertex elaboration ended..."); 
	    
	    return results;
    }
	 
    /**
     * Finds lines departure times given a list of vertices.
     * 
     * @param results
     * @return
     */
    public NextDepartureTimeResults getResponse(List<Vertex> results) {
    	
    	LOG.info("getResponse elaboration..."); 
    	 
    	GregorianCalendar calendar = new GregorianCalendar(graph.getTimeZone());
    	
	    NextDepartureTimeResults nextDepartureTimeResults = new NextDepartureTimeResults();
	    nextDepartureTimeResults.setRequestParameters(createNextDepartureParameters());
	    
	    for(Vertex v : results) {
	    	PatternDepartVertex pdv = (PatternDepartVertex) v;

	    	//check if vertex has already been taken into account.
	    	if(nextDepartureTimeResults.getResults() != null) {
	    		boolean found = false;
		    	for(NextDepartureTimeResult insertedRecord: nextDepartureTimeResults.getResults()) {
		    		if(pdv.getLat() == insertedRecord.getLat() &&
		    				pdv.getLon() == insertedRecord.getLng() &&
    						pdv.getName() == insertedRecord.getStopName()) {
		    			//discard record
		    			found=true;	
		    		}
		    	}
		    	if(found) {
		    		continue;
		    	}
	    	}
	    	
	    	NextDepartureTimeResult nextDepartureTimeResult = new NextDepartureTimeResult(pdv.getLat(),pdv.getLon(),pdv.getName());
	    		
			List<StopTimesInPattern> list = null;
			//convert time parameter into a date before searching for stopTimes
			calendar.setTime(new Date(time*1000));	
			String date = createDate(calendar);
			
			try {
				list = this.graph.index.getStopTimesForStop(pdv.getStop(),ServiceDate.parseString(date));
			} catch (ParseException e) {
				nextDepartureTimeResults.setError("Error converting time to date");
				return nextDepartureTimeResults;
			}
			
			
			Map<String,String> lineAndTime = new HashMap<String,String>();
			for(StopTimesInPattern stopTimes : list) {	
				String line = stopTimes.pattern.desc;
				
				//if line number parameter is not null check only the selected line.
				if(lineNumber != null && lineNumber != line) {
					continue;
				}
				
				for(TripTimeShort tts : stopTimes.times) {
					Long departureTime = tts.serviceDay + tts.scheduledDeparture;
					if(departureTime > time + timeOffset*60) {
						calendar.setTime(new Date(departureTime*1000));
						lineAndTime.put(line, calendar.getTime().toString());
						break;
					}
				}	
			}
			nextDepartureTimeResult.setLineAndTime(lineAndTime);
			
			if(lineAndTime.size() != 0) {
				nextDepartureTimeResults.addResult(nextDepartureTimeResult);
			}
	    }


        
        LOG.info("getResponse elaboration ended..."); 
        return nextDepartureTimeResults;
    }
    
    /**
     * Converts a calendar into a date in the format yyyyMMdd
     * 
     * @param calendar
     * @return
     */
    private String createDate(GregorianCalendar calendar) {
    	
    	LOG.info("createDate elaboration..."); 
    	
    	StringBuilder sb = new StringBuilder();

    	sb.append(calendar.get(Calendar.YEAR));

    	int realMonth = calendar.get(Calendar.MONTH)+1;
    	if(realMonth < 10) {
    		sb.append(0);
    	}
    	sb.append(realMonth);
    	
    	if(calendar.get(Calendar.DAY_OF_MONTH) < 10) {
    		sb.append(0);
    	}
    	sb.append(calendar.get(Calendar.DAY_OF_MONTH));
    	
    	LOG.info("createDate elaboration ended..."); 
    	 
    	return sb.toString();
    }
}
