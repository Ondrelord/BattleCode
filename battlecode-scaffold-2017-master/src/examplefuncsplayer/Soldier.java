package examplefuncsplayer;

import java.util.Random;

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
	
	private MapLocation groupTarget = null;
	
	private MapLocation myLocation = null;
	
	public Soldier(RobotController rc) {
		super(rc);		
	}
	

	@Override
	public void run() {
		Team enemyTeam = rc.getTeam().opponent();
		
        while (true) {
        	try {
        		myLocation = rc.getLocation();
        		        		
                // We increase soldier fields each 5 seconds
            	if (rc.getRoundNum() % SOLDIER_FIELDS_INCREASE_PERIOD == 0){
            		increaseSoldierField();
            	}
                
            	handleGrouping();
            	updateGroupTarget();
            	
            	// DEBUG:
            	if (groupTarget != null) {
            		if (rc.getTeam() == Team.A) {
            			rc.setIndicatorDot(groupTarget, 255, 0, 0);
            		} else {
            			rc.setIndicatorDot(groupTarget, 0, 255, 0);
            		}            		
            	}
            	
                // Check for nearby robots and handle combat ...
                // else go towards the group target or select one for the group if none available
                RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, enemyTeam);

                // If there are some...
                if (nearbyRobots.length > 0) 
                {
                	broadcastAllNearbyEnemies();
                	handleCombat(nearbyRobots);
                }
                else
                {
                	if (groupTarget != null) {
                		// Check if I am on the target allready
                		if (groupTarget.distanceTo(myLocation) < 5) {
                			selectNewGroupTarget();
                		} else {
                			Direction towardsTarget = myLocation.directionTo(groupTarget);
                        	if(!rc.hasMoved())
                        		tryMove(towardsTarget);                			
                		}               		
                	}
                	else {                		
                		selectNewGroupTarget();               	
                	}               	
                }

                // If I didn't move, then just explore randomly
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

	
	private void selectNewGroupTarget() throws GameActionException {
		// Choose some target for the group from the enemies list or from the broadcast
		// For now lets choose visible enemy, if none exist the one that is broadcasting

		// We will choose a target from seen enemies
		// For now just the first seen enemy
		MapLocation enemyLocation = getSeenEnemyLocation();
		
		if (enemyLocation != null) {
			rc.broadcast(calculateTargetingIndex(), 
				BroadcastManager.zipLocation(enemyLocation));
			return;
		}
		
		
		// Rarely we will try to follow some broadcasting target
		Random rnd = new Random();
		if (rnd.nextFloat() < 0.01f)
		{
			// We will choose a target from broadcasting enemies
			// Again, for now it will again be just the first enemy
			enemyLocation = getBroadcastingEnemyLocation();
			
			if (enemyLocation != null) { 
				rc.broadcast(calculateTargetingIndex(), 
					BroadcastManager.zipLocation(enemyLocation));
			}
		}           		 
		
		// If both null wait
		
	}


	private void handleGrouping() throws GameActionException {
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
	}
	
	private void updateGroupTarget() throws GameActionException {
        // Update the group's target if I have a group
        if (groupIndex != -1) {
         	int number = rc.readBroadcast(calculateTargetingIndex());
        	
       		groupTarget = BroadcastManager.unzipLocation(number);                	
        }
		
	}


	private MapLocation getSeenEnemyLocation() throws GameActionException {
		for (int i = BroadcastManager.ENEMY_LOCATIONS_START; 
				i < BroadcastManager.ENEMY_LOCATIONS_END;
				i++) {
			int num = rc.readBroadcast(i);
			
			if (num != 0) {
				return BroadcastManager.unzipLocation(num);
			} 
		}
		
		return null;
	}
	
	private MapLocation getBroadcastingEnemyLocation() throws GameActionException {
		Random rnd = new Random();
		int i =rnd.nextInt(BroadcastManager.ENEMY_LOCATIONS_BROADCAST_END - BroadcastManager.ENEMY_LOCATIONS_BROADCAST_START);
		
		int num = rc.readBroadcast(i);
		
		if (num != 0) {
			return BroadcastManager.unzipLocation(num);
		} else {
			return null;
		}
	}


	private int calculateTargetingIndex() {
		return BroadcastManager.SOLDIER_TARGETING_START + (groupIndex - BroadcastManager.SOLDIER_FIELDS_START);
	}


	private void handleCombat(RobotInfo[] nearbyRobots) throws GameActionException {
		MapLocation enemyLocation = nearbyRobots[0].getLocation();
    	
		// Lets set the enemy's location as the group's target
		rc.broadcast(calculateTargetingIndex(), BroadcastManager.zipLocation(enemyLocation));
		
		Direction toEnemy = myLocation.directionTo(enemyLocation);
    	
    	if(!rc.hasMoved())
    		tryMove(toEnemy);
    	
        // And we have enough bullets, and haven't attacked yet this turn...
        if (rc.canFireSingleShot()) 
        {
            // ...Then fire a bullet in the direction of the enemy.
            rc.fireSingleShot(rc.getLocation().directionTo(nearbyRobots[0].location));
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
		if (rc.getRoundNum() % Archon.SOLDIER_FIELDS_CLEANUP_PERIOD == 0
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
		
		// We look for a group only if we know that the number specifing group count 
		// will be the correct number of members.
		if (rc.getRoundNum() % Archon.SOLDIER_FIELDS_CLEANUP_PERIOD == 0
				&& rc.getRoundNum() % SOLDIER_FIELDS_INCREASE_PERIOD == 0) {
			return;
		}		
		
		int advertisingGroup = rc.readBroadcast(BroadcastManager.SOLDIER_ADVERTISING_FIELD);
		if (advertisingGroup == 0) {
			advertise();		
		} else {
			// I will add myself to the advertising group, if not full
			int groupCount = rc.readBroadcast(advertisingGroup);
			if (groupCount < GROUP_SIZE) {
				rc.broadcast(advertisingGroup, groupCount+1);
				groupIndex = advertisingGroup;
				
				System.out.println("Joining group with index: " + groupIndex);
			}
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
