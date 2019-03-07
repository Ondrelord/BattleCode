package examplefuncsplayer;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import examplefuncsplayer.Robot.BroadcastType;

public class Archon extends Robot 
{
	private MapLocation enemyLocation;

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
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
		
	}

}
