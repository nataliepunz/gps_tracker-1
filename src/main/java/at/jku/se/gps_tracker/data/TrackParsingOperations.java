package at.jku.se.gps_tracker.data;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import at.jku.se.gps_tracker.controller.ErrorPopUpController;
import at.jku.se.gps_tracker.model.Track;
import at.jku.se.gps_tracker.model.TrackPoint;

public class TrackParsingOperations implements ErrorPopUpController {

	private Connection conn;
	private String directory;
	
	public TrackParsingOperations(String directory) {
		this.directory=directory;
	}
	
	public void establishConnection(String dataBaseLocation) {
		boolean setUpNecessary = new File(dataBaseLocation).exists();
		String url = "jdbc:sqlite:"+dataBaseLocation;
		try {
			conn = DriverManager.getConnection(url);
			conn.setAutoCommit(false);
			if(!setUpNecessary) {
				setUpTables();
			}
		} catch (SQLException e) {  
			showErrorPopUp("ERROR! COULD NOT CONNECT TO DATABASE! "+e.getMessage());
		}
	}
	
	private void setUpTables() {		
		try(Statement stmt = conn.createStatement()){
			stmt.execute("CREATE TABLE tracks (name TEXT NOT NULL, folder TEXT NOT NULL, date TEXT NOT NULL, time TEXT NOT NULL, distance REAL NOT NULL, duration REAL NOT NULL, pace REAL NOT NULL, speed REAL NOT NULL, averageBPM INTEGER NOT NULL, maximumBPM INTEGER NOT NULL, elevation REAL NOT NULL, PRIMARY KEY(name,folder));");
            conn.commit();
        } catch (SQLException e) {  
        	showErrorPopUp("ERROR! COULD NOT CREATE TABLES! "+e.getMessage());
            try {
				conn.rollback();
			} catch (SQLException e1) {
				showErrorPopUp("ERROR! COULD NOT ROLLBACK! "+e.getMessage());
			}
        }  
	}
	
	public void addTrackToDataBase(String file, Track track) {
		try(PreparedStatement stmt = conn.prepareStatement("INSERT INTO tracks VALUES(?,?,?,?,?,?,?,?,?,?,?)")){
			stmt.setString(1, track.getName());
			stmt.setString(2, new File(file).getParentFile().getName());
			stmt.setString(3, track.getDate().toString());
			stmt.setString(4, track.getStartTime().toString());
			stmt.setDouble(5, track.getDistance());
			stmt.setDouble(6, track.getDuration().toSeconds());
			stmt.setDouble(7, track.getPace().toSeconds());
			stmt.setDouble(8, track.getSpeed());
			stmt.setInt(9, track.getAverageBPM());
			stmt.setInt(10, track.getMaximumBPM());
			stmt.setDouble(11, track.getElevation());
			stmt.execute();
			
			conn.commit();
		} catch (SQLException e) {
			showErrorPopUp("ERROR! COULD NOT ADD TRACK! "+e.getMessage());
			try {
				conn.rollback();
			} catch (SQLException e1) {
				showErrorPopUp("ERROR! COULD NOT ROLLBACK! "+e.getMessage());
			}
		}
	}
	
	public List<Track> getTracks(String currentDirectory){
		List<Track> trackHelpList = new ArrayList<>();
		try(PreparedStatement stmt = conn.prepareStatement("SELECT * FROM tracks WHERE folder=?")){
			stmt.setString(1, currentDirectory);
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
				trackHelpList.add(new Track(this, rs.getString("folder"), rs.getString("name"), LocalDate.parse(rs.getString("date")), LocalTime.parse(rs.getString("time")), rs.getDouble("distance"), Duration.ofSeconds((long) rs.getDouble("duration")), Duration.ofSeconds((long) rs.getDouble("pace")), rs.getDouble("speed"), rs.getInt("averageBPM"), rs.getInt("maximumBPM"), rs.getDouble("elevation")));
			}
		} catch (SQLException e) {
			showErrorPopUp("ERROR! COULD NOT GET TRACKS! "+e.getMessage());
		}
		return trackHelpList;
	}
	
	public List<TrackPoint> getTrackPoints(Track track){
		return new TrackParser().getTrackPoints(FilenameUtils.concat(FilenameUtils.concat(directory, track.getParentDirectory()),track.getName()));
	}
	
	public void removeTracks(List<File> files, String currentDirectoryFolder){
		HashSet<String> driveFiles = new HashSet<>();
		files.forEach(f -> driveFiles.add(FilenameUtils.getName(f.getAbsolutePath())));
		
		HashSet<String> dataBaseFiles = new HashSet<>();
		try(PreparedStatement stmt = conn.prepareStatement("SELECT name FROM tracks WHERE folder=?")){
			stmt.setString(1, currentDirectoryFolder);
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
				dataBaseFiles.add(rs.getString("name"));
			}
		} catch (SQLException e) {
			showErrorPopUp("ERROR! COULD NOT GET TRACKS TO CHECK! "+e.getMessage());
		}
		
		dataBaseFiles.removeAll(driveFiles);
		
		try(PreparedStatement stmt = conn.prepareStatement("DELETE FROM tracks WHERE name=? AND folder=?")){
			for(String s : dataBaseFiles) {
				stmt.setString(1, s);
				stmt.setString(2, currentDirectoryFolder);
				stmt.execute();
			}
		} catch (SQLException e) {
			showErrorPopUp("ERROR! COULD NOT REMOVE TRACK! "+e.getMessage());
			try {
				conn.rollback();
			} catch (SQLException e1) {
				showErrorPopUp("ERROR! COULD NOT ROLLBACK! "+e.getMessage());
			}
		}		
	}

	public void addTracks(List<File> files, String currentDirectoryFolder) {
		HashMap<String, File> mapping = new HashMap<>();
		HashSet<String> driveFiles = new HashSet<>();
		files.forEach(f -> {
			driveFiles.add(FilenameUtils.getName(f.getAbsolutePath()));
			mapping.put(FilenameUtils.getName(f.getAbsolutePath()), f);
		});
		
		HashSet<String> dataBaseFiles = new HashSet<>();
		try(PreparedStatement stmt = conn.prepareStatement("SELECT name FROM tracks WHERE folder=?")){
			stmt.setString(1, currentDirectoryFolder);
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
				dataBaseFiles.add(rs.getString("name"));
			}
		} catch (SQLException e) {
			showErrorPopUp("ERROR! COULD NOT GET TRACKS TO CHECK! "+e.getMessage());
		}
		
		driveFiles.removeAll(dataBaseFiles);
		
		TrackParser parser = new TrackParser();
		parser.createParsers();
		for(String s : driveFiles) {
			addTrackToDataBase(mapping.get(s).getAbsolutePath(), parser.getTrack(mapping.get(s).getAbsolutePath()));
		}
		
	}
	
	public void setDirectory(String directory) {
		this.directory=directory;
	}
}
