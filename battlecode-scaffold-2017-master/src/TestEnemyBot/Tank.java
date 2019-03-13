package TestEnemyBot;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Tank extends Robot
{

	public Tank(RobotController rc) 
	{
		super(rc);
	}

	@Override
	public void run() 
	{
		while(true)
		{
			try {
                MapLocation myLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

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
	                if (myLocation.distanceTo(enemyLocation) < 4 && rc.canFireTriadShot()) 
	                {
	                    // ...Then fire a bullet in the direction of the enemy.
	                    rc.fireTriadShot(toEnemy);
	                } 
	                else if(rc.canFireSingleShot())
	                {
	                	rc.fireSingleShot(toEnemy);
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
                System.out.println("Tank Exception");
                e.printStackTrace();
            }
		}
	}

}
