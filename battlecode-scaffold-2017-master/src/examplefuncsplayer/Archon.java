package examplefuncsplayer;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Archon extends Robot 
{
	private MapLocation enemyLocation;
	public static final int SOLDIER_FIELDS_CLEANUP_PERIOD = 5;
	
	public static final int SOLDIER_TARGETS_CLEANUP_PERIOD = 50;
	
	public static final int ENEMY_LOCATIONS_CLEANUP_PERIOD = 2;
	
	public static final int ARCHON_PROTECTOR_GROUPS = 1;
	
	public Archon(RobotController rc) 
	{
		super(rc);
		enemyLocation = rc.getInitialArchonLocations(rc.getTeam().opponent())[0];
	}

	@Override
	public void run() 
	{
        // The code you want your robot to perform every round should be in this loop
        while (true) 
        {
        	// Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try 
            {
            	// We decrease soldier fields each 6 seconds
            	if (rc.getRoundNum() % SOLDIER_FIELDS_CLEANUP_PERIOD == 0) {
            		annulateSoldierFields();
            	}
            	
            	if (rc.getRoundNum() % ENEMY_LOCATIONS_CLEANUP_PERIOD == 0)
            		cleanupEnemyLocations();
            	
            	//if (rc.getRoundNum() % SOLDIER_TARGETS_CLEANUP_PERIOD == 0)
            		//cleanupSoldierTargets();
            	
            	// If someone is broadcasting, save their locations
            	saveBroadcastingEnemiesLocations();
            	
            	// Check for nearby enemies, alert all of the soldiers if in danger.
            	//alertSoldiersIfInDanger();
            	
            	// Give orders to soldier groups
            	//giveSoldierOrders();
            	
            	int mustHaveGardeners = rc.readBroadcast(BroadcastType.SpawnGardener.getChannel());
            	
                // Generate a random direction
                Direction dir = randomDirection();

                // Randomly attempt to build a gardener in this direction
                
                if ( Math.random() < .01) 
                	mustHaveGardeners++;
                
            	if (rc.canHireGardener(dir) && mustHaveGardeners > 0)
            	{
            		mustHaveGardeners--;
            		rc.broadcast(BroadcastType.SpawnGardener.getChannel(), mustHaveGardeners);
                    rc.hireGardener(dir);
            	}


                // Move randomly
                tryMove(randomDirection());

                // Broadcast archon's location for other robots on the team to know
                MapLocation myLocation = rc.getLocation();
                rc.broadcast(0,(int)myLocation.x);
                rc.broadcast(1,(int)myLocation.y);
                
                if(rc.getRoundNum() % 100 == 1)
                {
                	rc.broadcastFloat(BroadcastType.AttackLocationX.getChannel(), enemyLocation.x);
                	rc.broadcastFloat(BroadcastType.AttackLocationY.getChannel(), enemyLocation.y);
                }
                
                if(rc.getRoundNum() == 1)
                {
                	rc.broadcastFloat(BroadcastType.EnemyArchonLocationX.getChannel(), enemyLocation.x);
                	rc.broadcastFloat(BroadcastType.EnemyArchonLocationY.getChannel(), enemyLocation.y);
                	
                	rc.broadcast(BroadcastType.SpawnGardener.getChannel(), 3);
                	rc.broadcast(BroadcastType.SpawnLumberjack.getChannel(), 1);
                	rc.broadcast(BroadcastType.SpawnScout.getChannel(), 1);
                }
                
                if(rc.getRoundNum() % 10 == 1)
                	rc.broadcast(BroadcastType.SpawnSoldier.getChannel(), 
                			rc.readBroadcast(BroadcastType.SpawnSoldier.getChannel()) + 1);
                
                if(rc.getTeamBullets() > 500)
                	rc.donate(rc.getVictoryPointCost());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) 
            {
                System.out.println("Archon exception");
                e.printStackTrace();
            }
        }
		
	}

	private void cleanupSoldierTargets() throws GameActionException {
		for (int i = BroadcastType.SoldierTargetingStart.getChannel();
				i < BroadcastType.SoldierTargetingEnd.getChannel();
				i++) {
						
			rc.broadcast(i, 0);
		}
		
	}

	private void giveSoldierOrders() throws GameActionException {
		int count = ARCHON_PROTECTOR_GROUPS;
		
		// For now only allocate protectors, let the rest do what it wants
		// -> the first group should always go to their archon's position
		for (int i = BroadcastType.SoldierTargetingStart.getChannel();
				i < BroadcastType.SoldierTargetingEnd.getChannel() && count > 0;
				i++, count--) {
						
			rc.broadcast(i, BroadcastManager.zipLocation(rc.getLocation()));
		}		
	}

	private void alertSoldiersIfInDanger() throws GameActionException {
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		if (nearbyRobots.length > 0) {
			// Give all of the soldiers my location
			for (int i = BroadcastType.SoldierTargetingStart.getChannel(); i < BroadcastType.SoldierTargetingEnd.getChannel(); i++) {
				//int number = rc.readBroadcast(i);
				rc.broadcast(i, BroadcastManager.zipLocation(rc.getLocation()));
			}			
		}		
	}

	private void saveBroadcastingEnemiesLocations() throws GameActionException {
		MapLocation[] broadcastingRobots = rc.senseBroadcastingRobotLocations();
		int index = BroadcastType.BroadcastLocationsStart.getChannel();
		
		for (MapLocation loc : broadcastingRobots) {
			rc.broadcast(index, BroadcastManager.zipLocation(loc));
			
			index++;
			if (index > BroadcastType.BroadcastLocationsEnd.getChannel())
				return; // We have wrote what we could
		}
	}

	private void cleanupEnemyLocations() throws GameActionException {
		for (int i =  BroadcastType.EnemyLocationsStart.getChannel(); i <  BroadcastType.EnemyLocationsEnd.getChannel(); i++) {
			//int number = rc.readBroadcast(i);
			rc.broadcast(i, 0);
		}
		
		for (int i =  BroadcastType.BroadcastLocationsStart.getChannel(); i <  BroadcastType.BroadcastLocationsEnd.getChannel(); i++) {
			//int number = rc.readBroadcast(i);
			rc.broadcast(i, 0);
		}
		
	}

	private void annulateSoldierFields() throws GameActionException {
		int numberGroups = 0;
		
		for (int i = BroadcastType.SoldierFieldsStart.getChannel(); i <  BroadcastType.SoldierFieldsEnd.getChannel(); i++) {
			int number = rc.readBroadcast(i);
			if (number > 0)
				numberGroups++;
			
			rc.broadcast(i, 0);
		}	
		
		System.out.println("There are: " + numberGroups + " groups.");
	}

}
