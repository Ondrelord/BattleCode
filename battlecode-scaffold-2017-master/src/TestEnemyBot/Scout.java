package TestEnemyBot;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Scout extends Robot
{
	private MapLocation myLocation;

	public Scout(RobotController rc) 
	{
		super(rc);
	}

	@Override
	public void run() 
	{
		while (true)
		{
			try 
			{
			
				myLocation = rc.getLocation();
				MapLocation enemyLocation = new MapLocation(rc.readBroadcastFloat(BroadcastType.EnemyArchonLocationX.getChannel()),
																	rc.readBroadcastFloat(BroadcastType.EnemyArchonLocationY.getChannel()));

				for (RobotInfo robot : rc.senseNearbyRobots())
				{
					if (robot != null)
					{
						if (robot.getTeam() == rc.getTeam().opponent())
						{
							switch(robot.getType())
							{
							case GARDENER:
								System.out.println("Found enemy Gardener");
								if(!rc.hasMoved())
									tryMove(myLocation.directionTo(robot.getLocation()));
								if(myLocation.distanceTo(robot.getLocation()) < 4)
								{
									System.out.println("Shooting");
									if(rc.canFireSingleShot()) rc.fireSingleShot(myLocation.directionTo(robot.getLocation()));
								}
								break;
							case ARCHON:
								rc.broadcastFloat(BroadcastType.EnemyArchonLocationX.getChannel(), robot.getLocation().x);
								rc.broadcastFloat(BroadcastType.EnemyArchonLocationY.getChannel(), robot.getLocation().y);
								break;
							default:
								break;
							}
						}
					}
				}
				if(!rc.hasMoved())
					tryMove(myLocation.directionTo(enemyLocation));
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
		
	}

}
