package examplefuncsplayer;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Archon extends Robot 
{
	private MapLocation enemyLocation;
	public static final int SOLDIER_FIELDS_DECREASE_PERIOD = 5;

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
            	if (rc.getRoundNum() % SOLDIER_FIELDS_DECREASE_PERIOD == 0) {
            		annulateSoldierFields();
            	}

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
                	rc.broadcastFloat(3, enemyLocation.x);
                	rc.broadcastFloat(4, enemyLocation.y);
                }
                if(rc.getRoundNum() == 1)
                {
                	rc.broadcast(BroadcastType.SpawnGardener.getChannel(), 3);
                	rc.broadcast(BroadcastType.SpawnLumberjack.getChannel(), 1);
                }
                
                rc.broadcast(BroadcastType.SpawnSoldier.getChannel(), 
                		rc.readBroadcast(BroadcastType.SpawnSoldier.getChannel()) + 1);

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) 
            {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
		
	}

	private void annulateSoldierFields() throws GameActionException {
		for (int i = BroadcastManager.SOLDIER_FIELDS_START; i < BroadcastManager.SOLDIER_FIELDS_END; i++) {
			//int number = rc.readBroadcast(i);
			rc.broadcast(i, 0);
		}	
	}

}
