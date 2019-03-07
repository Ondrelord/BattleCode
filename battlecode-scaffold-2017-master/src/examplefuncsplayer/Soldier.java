package examplefuncsplayer;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;

public class Soldier extends Robot{

	public static final int GROUP_SIZE = 4;
	
	public static final int SOLDIER_FIELDS_INCREASE_PERIOD = 6; 
	
	private int groupIndex = -1;
	
	public Soldier(RobotController rc) {
		super(rc);		
	}
	

	@Override
	public void run() {
		Team enemyTeam = rc.getTeam().opponent();
		
        while (true) {
        	try {
                // We increase soldier fields each 5 seconds
            	if (rc.getRoundNum() % SOLDIER_FIELDS_INCREASE_PERIOD == 0){
            		increaseSoldierField();
            	}
                
                // Look for a group if I don't have one.
                if (groupIndex == -1) {
                	lookForAGroup();
                } 
                else {
                	// Check if our group is full and stop advertising if we are advertising
                	int groupCount = rc.readBroadcast(groupIndex);
                	int advertisingGroup = rc.readBroadcast(BroadcastManager.SOLDIER_ADVERTISING_FIELD);
                	
                	if (groupCount == GROUP_SIZE) {
                		// Check if we are advertising and stop it.
                		
                		if (advertisingGroup == groupIndex) {
                			rc.broadcast(BroadcastManager.SOLDIER_ADVERTISING_FIELD, 0);
                			System.out.println("Stopped advertising.");
                		}
                	}                	
                	// We should be advertising if we are almost empty or wait for our turn to advertise
                	else if (groupCount == 1) {
                		if (advertisingGroup == 0) {
                			advertise();
                		}
                		// Else we will wait
                	}
                }
                
                // Update the group's target
                
                MapLocation myLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemyTeam);

                // If there are some...
                if (robots.length > 0) 
                {
	            	MapLocation enemyLocation = robots[0].getLocation();
	            	Direction toEnemy = myLocation.directionTo(enemyLocation);
	            	
	            	rc.broadcastFloat(3, enemyLocation.x);
	            	rc.broadcastFloat(4, enemyLocation.y);
	            	
	            	if(!rc.hasMoved())
	            		tryMove(toEnemy);
	            	
	                // And we have enough bullets, and haven't attacked yet this turn...
	                if (rc.canFireSingleShot()) 
	                {
	                    // ...Then fire a bullet in the direction of the enemy.
	                    rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
	                }
                }
                else
                {
                	MapLocation enemyLocation = new MapLocation(rc.readBroadcastFloat(3), rc.readBroadcastFloat(4));
                	Direction toEnemy = myLocation.directionTo(enemyLocation);
                	
                	if(!rc.hasMoved())
                		tryMove(toEnemy);
                }

                // Move randomly
                if(!rc.hasMoved())
                	tryMove(randomDirection());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
		
	}

	
	private void increaseSoldierField() throws GameActionException {
		if (groupIndex == -1)
			return;
		
		int number = rc.readBroadcast(groupIndex);
		if (number <= GROUP_SIZE)
				rc.broadcast(groupIndex, number + 1);
					
	}

	private void advertise() throws GameActionException {
		// Advertise only if it is not the turn when the Archon annuls the fields
		// Or the turn when we increment the fields
		if (rc.getRoundNum() % Archon.SOLDIER_FIELDS_DECREASE_PERIOD == 0
				&& rc.getRoundNum() % SOLDIER_FIELDS_INCREASE_PERIOD == 0) {
			return;
		}		
		
		// No one is advertising, I will create my own group
		groupIndex = findEmptyGroupIndex();
		
		if (groupIndex == -1) {
			System.out.println("Cannot find empty group index.");
			return;
		} // FAIL
		
		System.out.println("Creating new group with index: " + groupIndex);
		
		// Start advertising
		rc.broadcast(BroadcastManager.SOLDIER_ADVERTISING_FIELD, groupIndex);
		rc.broadcast(groupIndex, 1);
	}


	private void lookForAGroup() throws GameActionException {
		// I don't have a group and I will try to get one, or start advertising my own
		System.out.println("Looking for a group.");
		
		int advertisingGroup = rc.readBroadcast(BroadcastManager.SOLDIER_ADVERTISING_FIELD);
		if (advertisingGroup == 0) {
			advertise();		
		} else {
			// I will add myself to the advertising group
			int groupCount = rc.readBroadcast(advertisingGroup);
			rc.broadcast(advertisingGroup, groupCount+1);
			groupIndex = advertisingGroup;
			
			System.out.println("Joining group with index: " + groupIndex);
		}
	}


	private int findEmptyGroupIndex() throws GameActionException {
		for (int i = BroadcastManager.SOLDIER_FIELDS_START; i < BroadcastManager.SOLDIER_FIELDS_END; i++) {
			int number = rc.readBroadcastInt(i);
			
			System.out.println(i + ": " + number);
			
			if (number == 0) {
				return i;
			}
		}	
		
		return -1;
	}

}
