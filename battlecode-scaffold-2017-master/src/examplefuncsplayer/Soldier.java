package examplefuncsplayer;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;

public class Soldier extends Robot
{

	public Soldier(RobotController rc) 
	{
		super(rc);
	}

	@Override
	public void run() 
	{
		Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                MapLocation myLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

                // If there are some...
                if (robots.length > 0) 
                {
	            	MapLocation enemyLocation = robots[0].getLocation();
	            	Direction toEnemy = myLocation.directionTo(enemyLocation);
	            	
	            	rc.broadcastFloat(BroadcastType.AttackLocationX.getChannel(), enemyLocation.x);
	            	rc.broadcastFloat(BroadcastType.AttackLocationY.getChannel(), enemyLocation.y);
	            	
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
                	MapLocation enemyLocation = new MapLocation(rc.readBroadcastFloat(BroadcastType.AttackLocationX.getChannel()),
                													rc.readBroadcastFloat(BroadcastType.AttackLocationY.getChannel()));
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

}
