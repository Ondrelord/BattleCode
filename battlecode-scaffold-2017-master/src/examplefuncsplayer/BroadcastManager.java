package examplefuncsplayer;

import battlecode.common.MapLocation;

public class BroadcastManager {

	/*public static final int SOLDIER_FIELDS_START = 501;
	
	public static final int SOLDIER_FIELDS_END = 600;
	
	public static final int SOLDIER_ADVERTISING_FIELD = 500;
	
	public static final int SOLDIER_TARGETING_START = 601;
	
	public static final int SOLDIER_TARGETING_END = 700;
	
	// Enemy locations
	
	public static final int ENEMY_LOCATIONS_START = 401;
	
	public static final int ENEMY_LOCATIONS_END = 449;
	
	public static final int ENEMY_LOCATIONS_BROADCAST_START = 450;
	
	public static final int ENEMY_LOCATIONS_BROADCAST_END = 499;*/
		
	// Cantor's pairing function
	// https://en.wikipedia.org/wiki/Pairing_function
	public static int zipLocation(MapLocation location) {
		int x = (int) (location.x * 10);
		int y = (int) (location.y * 10);
		
		return (int) (0.5 * (x + y) * 
						(x + y + 1) 
						+ y + 1);
	}
	
	// Cantor's unpairing function
	// https://en.wikipedia.org/wiki/Pairing_function
	public static MapLocation unzipLocation(int number) {
		if (number == 0)
			return null;
		
		int w = (int) Math.floor((Math.sqrt(8 * number + 1) - 1) / 2);
		int t = ((int) Math.pow(w, 2) + w) / 2;
		int y = number - t;
		int x = w - y;
		
		return new MapLocation(x/10f, y/10f);				
	}
}
