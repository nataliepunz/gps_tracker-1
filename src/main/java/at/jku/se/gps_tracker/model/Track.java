package at.jku.se.gps_tracker.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import at.jku.se.gps_tracker.data.TrackParsingOperations;

public class Track extends AbstractTrack {

	private LocalDate date;
	private LocalTime startTime;
	private List<TrackPoint> trackPoints;
	private String parentDirectory;
	private TrackParsingOperations conn;

	public Track(String parentDirectory, String name, LocalDate date, LocalTime startTime, double distance, Duration duration, double elevation, List<TrackPoint> trackPoints) {
		super(name,distance,duration,elevation);
		this.date = date;
		this.startTime = startTime;
		this.trackPoints = trackPoints;
		this.parentDirectory=parentDirectory;
	}
	
	public Track(String parentDirectory, String name, LocalDate date, LocalTime startTime, double distance, Duration duration, int averageBPM, int maximumBPM, double elevation, List<TrackPoint> trackPoints) {
		super(name,distance,duration,averageBPM,maximumBPM,elevation);
		this.date = date;
		this.startTime = startTime;
		this.trackPoints = trackPoints;
		this.parentDirectory=parentDirectory;
	}
	
	public Track(TrackParsingOperations conn, String parentDirectory, String name, LocalDate date, LocalTime startTime, double distance, Duration duration, int averageBPM, int maximumBPM, double elevation) {
		super(name,distance,duration,averageBPM,maximumBPM,elevation);
		this.conn=conn;
		this.date = date;
		this.startTime = startTime;
		this.parentDirectory=parentDirectory;
	}

	public Track() {

	}

	public LocalDate getDate() {
		return date;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public List<TrackPoint> getTrackPoints() {
		if(trackPoints==null && conn!=null) {
			this.trackPoints = conn.getTrackPoints(this);
		}
		return this.trackPoints;
	}

	@Override
	public String toString() {
		return this.getName();
	}

	public String getParentDirectory() {
		return this.parentDirectory;
	}

}

