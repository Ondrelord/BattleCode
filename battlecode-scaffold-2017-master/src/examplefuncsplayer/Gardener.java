package examplefuncsplayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;
import playerBot.Robot.BroadcastType;

public class Gardener extends Robot {
	static int state = 0;
	static MapLocation enemy;
	static MapLocation ally;
	static MapLocation myLocation;
	static Direction spawnDir;
	static Boolean spawnDirSet = false;

	public Gardener(RobotController rc) 
	{
		super(rc);
		enemy = rc.getInitialArchonLocations(rc.getTeam().opponent())[0];
	    ally = rc.getInitialArchonLocations(rc.getTeam())[0];
	    myLocation = rc.getLocation();
	    spawnDir = myLocation.directionTo(enemy);
	    
	}

	@Override
	public void run() 
	{
        int initMovement = 10;
        int findLocCounter = 100;
        boolean haveDeathWish = false;
        //float plantOffset = (float) (Math.PI/3);

        // The code you want your robot to perform every round should be in this loop
        while (true) 
        {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try 
            {
            	myLocation = rc.getLocation();
            	
            	
				if(rc.getHealth() < 3 && !haveDeathWish)
            	{
            		rc.broadcast(BroadcastType.SpawnGardener.getChannel(), rc.readBroadcast(BroadcastType.SpawnGardener.getChannel() + 1));
            		haveDeathWish = true;
            	}

            	trySpawn();
            	
            	switch(state)
            	{
            	case 0:
					//tryBuildLumberjack(false);
					if(rc.getRoundNum() > 50)
						state = 2;
					if(rc.canBuildRobot(RobotType.LUMBERJACK, myLocation.directionTo(enemy)))
					{
						rc.buildRobot(RobotType.LUMBERJACK, myLocation.directionTo(enemy));
						state = 1;
					}
					if(!rc.hasMoved())
						tryMove(myLocation.directionTo(enemy).rotateLeftDegrees(45));
					break;
				case 1:
					if(rc.getRoundNum() > 50)
						state = 2;
					if(rc.canBuildRobot(RobotType.SOLDIER, myLocation.directionTo(enemy)))
					{
						rc.buildRobot(RobotType.SOLDIER, myLocation.directionTo(enemy));
						state = 2;
					}
					if(!rc.hasMoved())
						tryMove(myLocation.directionTo(enemy).rotateRightDegrees(90));
					break;
				case 2:
            		System.out.println("State 0: Finding good location");
            		
					if (findLocation(myLocation) || findLocCounter-- < 0)
            			state = 3;
            		break;
            	case 3:
            		System.out.println("State 2: Planting trees and taking care of");
            		plantTrees();
            		waterTrees();
            		break;
            	default:
            		break;
            	
            	}
            	
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } 
            catch (Exception e) 
            {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }
	
	public void plantTrees()
	{
		try 
		{
			for(float i = 0; i < 1.0f; i += 1.0f/6.0f)
			{
				Direction dir = new Direction((float) (i*2*Math.PI));
	    		if (rc.canPlantTree(dir))
	    		{
	    			if(Math.abs(dir.degreesBetween(myLocation.directionTo(enemy))) > 30)
	    			{
	    				System.out.println("Planting trees");
	    				rc.plantTree(dir);
	    			}
	    			else if (!spawnDirSet)
    				{
	    				spawnDir = dir;
	    				spawnDirSet = true;
    				}
	    				
	    		}
			}
		}
		catch (GameActionException e) 
		{
			e.printStackTrace();
		}
	}
	
	public void waterTrees()
	{
		try 
		{
			TreeInfo treeToWater = null;
			for(TreeInfo tree : rc.senseNearbyTrees(3))
			{
				if (tree == null)
					return;
				if(treeToWater == null)
					treeToWater = tree;
				else if (treeToWater.getHealth() > tree.getHealth())
					treeToWater = tree;
			}
			if(rc.canWater(treeToWater.ID))
				rc.water(treeToWater.ID);
		}
		catch (GameActionException e) 
		{
			e.printStackTrace();
		}
	}
	
	public boolean findLocation(MapLocation center)
	{
		try
		{
			MapLocation path = center.add(spawnDir);
				if (rc.senseNearbyTrees(3).length == 0 && rc.onTheMap(center, 3))
					return true;
				else
				{
					for(TreeInfo tree : rc.senseNearbyTrees(3, Team.NEUTRAL))
					{
						if (tree != null)
						{
							Direction dir = randomDirection();
							if(rc.canBuildRobot(RobotType.LUMBERJACK, dir))
							{
								rc.buildRobot(RobotType.LUMBERJACK, dir);
							}
						}
					}
				}
				if (Math.random() >= 0.3)
				{
					Direction dir = null;
					for(int i = 0; i < Math.PI*2; i += Math.PI*2/6 )
					{
						if (!rc.onTheMap(center.add(new Direction(i), 2), 3))
							continue;
						
						if(dir == null)
							dir = new Direction(i);
						else if(rc.senseNearbyTrees(center.add(new Direction(i)), 4, Team.NEUTRAL).length 
								< rc.senseNearbyTrees(center.add(dir), 4, Team.NEUTRAL).length)
						{
							dir = new Direction(i);
						}
					}
					rc.setIndicatorDot(center.add(dir), 0, 255, 255);
					if(!rc.hasMoved())
						tryMove(dir);
				}
				else
				{
					Direction dir = new Direction((float)Math.random() * 2 * (float)Math.PI);
					if(!rc.hasMoved())
						tryMove(dir);
				}
		}
		catch (GameActionException e)
		{
			e.printStackTrace();
		}
		
		return false;
	}
 
	public void trySpawn()
	{
		try 
		{
			if(state < 3)
				return;
			
			if(rc.readBroadcast(BroadcastType.SpawnLumberjack.getChannel()) > 0)
				if(spawnDir != null && rc.canBuildRobot(RobotType.LUMBERJACK, spawnDir))
				{
					rc.buildRobot(RobotType.LUMBERJACK, spawnDir);
					rc.broadcast(BroadcastType.SpawnLumberjack.getChannel(), 
							rc.readBroadcast(BroadcastType.SpawnLumberjack.getChannel()) - 1);
					System.out.println("Spawning: Lumberjack");
				}
    	
			if(rc.readBroadcast(BroadcastType.SpawnScout.getChannel()) > 0)
	    		if(spawnDir != null && rc.canBuildRobot(RobotType.SCOUT, spawnDir))
	    		{
	    			rc.buildRobot(RobotType.SCOUT, spawnDir);
	    			rc.broadcast(BroadcastType.SpawnScout.getChannel(), 
	    					rc.readBroadcast(BroadcastType.SpawnScout.getChannel()) - 1);
	    			System.out.println("Spawning Scout");
	    		}
			
	    	if(rc.readBroadcast(BroadcastType.SpawnTank.getChannel()) > 0)
	    		if(spawnDir != null && rc.canBuildRobot(RobotType.TANK, spawnDir))
	    		{
	    			rc.buildRobot(RobotType.TANK, spawnDir);
	    			rc.broadcast(BroadcastType.SpawnTank.getChannel(), 
	    					rc.readBroadcast(BroadcastType.SpawnTank.getChannel()) - 1);
	    			System.out.println("Spawning: Tank");
	    		}
	    	
	    	if(rc.readBroadcast(BroadcastType.SpawnSoldier.getChannel()) > 0)
	    		if(spawnDir != null && rc.canBuildRobot(RobotType.SOLDIER, spawnDir))
	    		{
	    			rc.buildRobot(RobotType.SOLDIER, spawnDir);
	    			rc.broadcast(BroadcastType.SpawnSoldier.getChannel(), 
	    					rc.readBroadcast(BroadcastType.SpawnSoldier.getChannel()) - 1);
	    			System.out.println("Spawning: Soldier");
	    		}
	    	if(rc.getTeamBullets() > 300)
	    		if(spawnDir != null && rc.canBuildRobot(RobotType.SOLDIER, spawnDir))
	    		{
	    			rc.buildRobot(RobotType.SOLDIER, spawnDir);
	    		}
		}
		catch (GameActionException e) 
		{
			e.printStackTrace();
		}
	}
}
