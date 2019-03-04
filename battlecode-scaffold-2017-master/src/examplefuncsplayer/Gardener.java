package examplefuncsplayer;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

public class Gardener extends Robot{

	public Gardener(RobotController rc) {
		super(rc);
	}

	@Override
	public void run() {
		int state = 0;
        MapLocation base = null;
        float plantOffset = (float) (Math.PI/3);

        // The code you want your robot to perform every round should be in this loop
        while (true) 
        {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try 
            {
            	switch(state)
            	{
            	case 0:
            		System.out.println("State 0: Finding good location");
            		base = findLocation(rc.getLocation());
            		if(base != null)
            			state = 1;
            		else
            		{
            			System.out.println("Location not found");
            			if(!rc.hasMoved())
            				tryMove(randomDirection());
            		}
            		break;
            	case 1:
            		System.out.println("State 1: Moving to location");
            		if (base == null)
            			break;
            		if (rc.canMove(base))
            			rc.move(base);
            		if (rc.getLocation() == base)
            			state = 2;
            		break;
            	case 2:
            		System.out.println("State 2: Planting trees and taking care of");
            		plantTrees();
            		waterTrees();
            		break;
            	default:
            		break;
            	
            	}
            	
            	
            	/*
                // Generate a random direction
                Direction dir = randomDirection();
                
                if (rc.canPlantTree(dir))
                {
                	rc.plantTree(new Direction(plantOffset));
                }
                
                
                // Randomly attempt to build a soldier or lumberjack in this direction
                if (rc.canBuildRobot(RobotType.SOLDIER, dir) && Math.random() < .01) 
                {
                    rc.buildRobot(RobotType.SOLDIER, dir);
                } 
                else if (rc.canBuildRobot(RobotType.LUMBERJACK, dir) && Math.random() < .01 && rc.isBuildReady()) 
                {
                    rc.buildRobot(RobotType.LUMBERJACK, dir);
                }

                // Move randomly
                
                if(rc.isCircleOccupiedExceptByThisRobot(rc.getLocation(), 3))
                	tryMove(randomDirection());
            	*/
            	
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
	
	private void plantTrees()
	{
		try 
		{
			for(float i = 0; i < 1.0f; i += 1.0f/6.0f)
			{
				Direction dir = new Direction((float) (i*2*Math.PI));
	    		if (rc.canPlantTree(dir))
	    		{
	    			System.out.println("Planting trees");
					rc.plantTree(dir);
	    		}
			}
		}
		catch (GameActionException e) 
		{
			e.printStackTrace();
		}
	}
	
	private void waterTrees()
	{
		try 
		{
			TreeInfo treeToWater = null;
			for(TreeInfo tree : rc.senseNearbyTrees(3))
			{
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
	
	private MapLocation findLocation(MapLocation center)
	{
		try
		{
			MapLocation path = null;
			while (true)
			{
				if (!rc.isCircleOccupiedExceptByThisRobot(center, 5))
					return center;
				for(int i = 0; i < 1; i += 1.0f/6.0f)
				{			
					MapLocation loc = center.add((float)(i*2*Math.PI),6);
					MapLocation enemy = rc.getInitialArchonLocations(rc.getTeam() == Team.A ? Team.A : Team.B)[0];
					rc.setIndicatorDot(loc, 0, 255, 0);
					if(!rc.isLocationOccupied(loc))
					{
						if (path == null)
							path = loc;
						else if (path.distanceTo(enemy) > loc.distanceTo(enemy))
						{
							path = loc;
						}
					}
				}
				tryMove(center.directionTo(path));
			}
		}
		catch (GameActionException e)
		{
			e.printStackTrace();
		}
		
		
		
		
		/*List<MapLocation> locations = new ArrayList<MapLocation>();
		MapLocation loc, goal = center;
		locations.add(center);
		boolean foundOne = false;
		
		try 
		{
			while(locations.size() > 0)
			{
				foundOne = false;
				loc = locations.get(0);
				locations.remove(0);
				rc.setIndicatorDot(loc, 0, 0, 255);
				
				if (rc.isCircleOccupiedExceptByThisRobot(loc, 5) || !rc.onTheMap(loc, 3) )
				{
					System.out.println("start: " + loc.x + " " + loc.y);
					
					
					MapLocation newLoc;
					for(float i = 1.0f/6.0f; i < 1; i += 1.0f/6.0f)
					{
						newLoc = loc.add((float) (i * Math.PI * 2));
						try
						{
							System.out.println("try: " + newLoc.x + " " + newLoc.y);
							if(rc.onTheMap(newLoc, 3) && !rc.isLocationOccupied(newLoc))
							{
								foundOne = true;
								locations.add(newLoc);
								 
								if ()
									goal = newLoc;
							}
						}
						catch (GameActionException e) 
						{
							System.out.println("Circle not on Map");
							e.printStackTrace();
							if (!foundOne)
								return null;
							
						}
					}
				}
				else
					return loc;
			}
		} 
		catch (GameActionException e) 
		{
			System.out.println("Find Location Exception");
			e.printStackTrace();
			if (!foundOne)
			{
				System.out.println("goal");
				rc.setIndicatorDot(goal, 0, 255, 0);
				try {
					tryMove(center.directionTo(goal));
				} catch (GameActionException e1) {
					e1.printStackTrace();
				}
			}
		}
		*/
		return null;
	}
 
	
}
