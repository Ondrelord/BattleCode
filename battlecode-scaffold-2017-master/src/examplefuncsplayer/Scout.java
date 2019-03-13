package examplefuncsplayer;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TreeInfo;

public class Scout extends Robot
{
	private MapLocation myLocation;
	
	private int state;

	public Scout(RobotController rc) 
	{
		super(rc);
		state = 0;
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
								//if(!rc.hasMoved())
									//tryMove(myLocation.directionTo(robot.getLocation()));
								/*if(myLocation.distanceTo(robot.getLocation()) < 4)
								{
									System.out.println("Shooting");
									if(rc.canFireSingleShot()) rc.fireSingleShot(myLocation.directionTo(robot.getLocation()));
								}*/
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
				//if(!rc.hasMoved())
					//tryMove(myLocation.directionTo(enemyLocation));
				for (TreeInfo tree : rc.senseNearbyTrees(2))
				{
					if(rc.canShake(tree.ID))
					{
						rc.shake(tree.ID);
						System.out.println("Tree Shaken");
					}
				}
				
				scoutMap();
				
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
	}

	public void scoutMap()
	{
		try 
		{
			if (!rc.hasMoved()) 
			{
				
				switch (state)
				{
				case 0:
					if(!tryMove(Direction.EAST))
						state = 1;
					break;
				case 1:
					if(!tryMove(Direction.NORTH))
						state = 2;
					break;
				case 2:
					if(!tryMove(Direction.WEST))
						state = 3;
					break;
				case 3:
					if(!tryMove(Direction.SOUTH))
						state = 0;
					break;
				}
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
}
