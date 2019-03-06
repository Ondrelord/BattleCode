package examplefuncsplayer;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

public class Lumberjack extends Robot{

	public Lumberjack(RobotController rc) {
		super(rc);
	}

	State state = new FindTreeS(this);
	ChopState chopS = new ChopState(this);
	FindTreeS findTreeS = new FindTreeS(this);
	GoBackState goBackS = new GoBackState(this);
	WalkState walkS = new WalkState(this);
    
    
    
    
    
     void moveToTree(TreeInfo t) throws GameActionException 
    {
    	if (t!=null)
		{
			rc.canMove(t.getLocation());
			rc.move(t.getLocation());
		}
    }
    
     TreeInfo TryChop(TreeInfo t) throws GameActionException
    {
    	if (rc.canChop(t.ID))
		{
			rc.chop(t.ID);
			if (t.health<=0)
				return null;
			return t;
		}
    	return null;
    }
	
     abstract class State
     {
    	 public void Step() throws GameActionException
    	 {
    		 if (inDanger()) 
    			 return;
    		 SubStep();		 
    	 }
    	 
    	 
    	 public State(Lumberjack bot)
    	 {
    		 this.bot=bot;
    	 }
    	 private boolean inDanger() throws GameActionException
    	 {
    		 Team enemy = bot.rc.getTeam().opponent();
    		    RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius+GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

                if(robots.length > 0 && !bot.rc.hasAttacked()) {
                    // Use strike() to hit all nearby robots!
                    bot.rc.strike();
                } else {
                    // No close robots, so search for robots within sight radius
                    robots = rc.senseNearbyRobots(-1,enemy);

                    // If there is a robot, move towards it
                    if(robots.length > 0) {
                        MapLocation myLocation = rc.getLocation();
                        MapLocation enemyLocation = robots[0].getLocation();
                        Direction toEnemy = myLocation.directionTo(enemyLocation);

                        tryMove(toEnemy);
                        return true;
                    }
                }
                return false;
    	 }
    	    	 
    	 protected abstract void SubStep() throws GameActionException;
    	 protected Lumberjack bot;
     }
     
     class ChopState extends State 
     {
    	 public ChopState(Lumberjack bot) {
			super(bot);
		}
		
    	 @Override
    	 protected void SubStep() throws GameActionException
    	 {
    		 if (tree!= null && tree.health>0)
    		 {
    			 tree = TryChop(tree);
    		 }
    		 else
    		 {
    			 bot.state = bot.findTreeS;
    		 }
    	 }
    	 
    	 public TreeInfo tree;

     }
     
     class GoBackState extends State
     {

		public GoBackState(Lumberjack bot) {
			super(bot);
			MapLocation botLoc= bot.rc.getLocation();
			initLoc = botLoc.add(0);
			
			
		}
    	
		@Override
		protected void SubStep() throws GameActionException
		{
			if (bot.rc.getLocation().distanceSquaredTo(initLoc)>16)
			{
				if (bot.rc.canMove(initLoc))
					{
						bot.rc.move(initLoc);
						return;
					}
			}
			else
			{
				bot.findTreeS.deadTreeCount=0;
				bot.state = bot.findTreeS;
			}
			
		}
		private MapLocation initLoc;
     }
     
     class WalkState extends State
     {

		public WalkState(Lumberjack bot) {
			super(bot);
		}
		
		@Override
		protected void SubStep() throws GameActionException
		{
			if (StepCounter==0)
				dir=randomDirection();
			if (!bot.rc.canMove(dir) || StepCounter>5)
			{
				bot.state = bot.findTreeS;
				return;
			}
			bot.rc.move(dir);
			StepCounter++;			
		}
    	 public int StepCounter=0;
    	 private Direction dir;
     }
     
     
     class FindTreeS extends State
     {
    	 public FindTreeS(Lumberjack bot)
    	 {
    		 super(bot);
    	 }
    	 
    	 TreeInfo selectMinTree(TreeInfo[] trees)
    	    {
    	    	int index=0;
    	    	float minDistance = Integer.MAX_VALUE;
    	    	int minIndex=0;
    	    	for (index = 0; index< trees.length; index++)
    	    	{
    	    		float distance = trees[index].getLocation().distanceTo(rc.getLocation());
    	    		if (distance<minDistance)
    	    		{
    	    			minDistance = distance;
    	    			minIndex=index;
    	    			if (minDistance <=2)
    	    				return trees[minIndex];
    	    		}
    	    	}
    	    	return trees[minIndex];
    	    }
    	 
    	 private boolean SearchTree(TreeInfo[] trees) throws GameActionException
    	 {		
        		if (trees.length>0)
        		{
        			TreeInfo closestT = selectMinTree(trees);
        			if (rc.canChop(closestT.ID))
        				{
        	    		 	deadTreeCount+=1;
        					bot.state= bot.chopS;
        					bot.chopS.tree= closestT;
        					return true;
        				}
        			else if (rc.canMove(closestT.getLocation()))
        			{
        				moveToTree(closestT);
        				return true;
        			}
        				
        		}
        		return false;
    	 }
    	 
    	 protected void SubStep() throws GameActionException
    	 {
    		 if (deadTreeCount>4)
    		 {
    			 state = bot.goBackS;
    			 return;
    		 }
    		 
		 	Team enemy = rc.getTeam().opponent();
        	
        	TreeInfo[] enemyTrees = rc.senseNearbyTrees(-1, enemy);
        	if (SearchTree(enemyTrees))
        		return;
        	
        	
        	TreeInfo[] neutralTrees = rc.senseNearbyTrees(-1,Team.NEUTRAL);
    		if (SearchTree(neutralTrees))
    			return;
    		        		
            bot.state= bot.walkS;
            bot.walkS.StepCounter=0;
    	 }
    	 
    	 public int deadTreeCount = 0;
     }
     
	@Override
	public void run() {
		  System.out.println("I'm a lumberjack!");

	        // The code you want your robot to perform every round should be in this loop
	        while (true) {

	            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
	            try {	              
	                    	
	                  state.Step();

	                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
	                Clock.yield();

	            } catch (Exception e) {
	                System.out.println("Lumberjack Exception");
	                e.printStackTrace();
	            }
	        }
		
	}

}
