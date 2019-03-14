package examplefuncsplayer;


import battlecode.common.BulletInfo;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

import battlecode.common.Team;
import battlecode.common.TreeInfo;

public class Lumberjack extends Robot{

	public Lumberjack(RobotController rc) {
		super(rc);
	}

	State state = new FindTreeS(this);
	ChopState chopS = new ChopState(this);
	FindTreeS findTreeS = new FindTreeS(this);
	GoToState goBackS = new GoToState(this);
	WalkState walkS = new WalkState(this);
    CallState callS = new CallState(this);

	
	 

	 private MapLocation AnswerToCall(int startIndex) throws GameActionException
	 {
		 for (int i = startIndex; i<startIndex+100;i++)
		 {
			 int intLoc=rc.readBroadcastInt(i);
			 if (intLoc!=0)
			{
				 MapLocation loc = IntToLoc(rc.readBroadcast(i));	
				 rc.broadcast(i, 0);
			}
		 } 
		 return null;
	 }
	
	
	
	
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
    		 silence--;
    		 BulletInfo[] bullets = bot.rc.senseNearbyBullets();
    		 for(int i=0;i<bullets.length;i++)
    		 {
    			 if (bot.willCollideWithMe(bullets[i]))
    			 {
    				BulletInfo b = bullets[i];
    				Direction left =b.getDir().rotateLeftDegrees(90);
    				Direction right =b.getDir().rotateRightDegrees(90);
    				
    				if (bot.tryMove(left))
    					return;
    				if (bot.tryMove(right))
    					return;
    				break;
    			 }
    		 }
    		 		 
    		 if (inDanger()) 
    			 return;
    		 
    		  MapLocation duty = AnswerToCall(1000);
    		 if (duty!=null)
    		 {
    		  	bot.rc.setIndicatorDot(duty,0,0,0);
    			bot.state=bot.callS;
    			bot.callS.SetLoc(duty);
    			return;
    		 }
    		 
    		 if (bot.rc.getLocation().equals(lastLoc))
    			 holdCounter++;
    		 else
    			 lastLoc = bot.rc.getLocation();
    		 if (holdCounter>20 && this!= bot.chopS)
    		 {
    			 Direction dir =Robot.randomDirection();
    			 if (bot.rc.canMove(dir))
    			 {
    				 bot.state=bot.findTreeS;
    				 bot.findTreeS.deadTreeCount=0;
    				 holdCounter=0;
    				 bot.rc.move(dir);
    				 return;
    			 } 			
    			 
    		 }
    		 SubStep();		 
    	 }
    	 
    	 
    	 public State(Lumberjack bot)
    	 {
    		 this.bot=bot;
    		 lastLoc = bot.rc.getLocation();
    	 }
    	 
    	 
    	 private boolean inDanger() throws GameActionException
    	 {
    		 Team enemy = bot.rc.getTeam().opponent();
    		    RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

                if(robots.length > 0 && !bot.rc.hasAttacked()) {
                    // Use strike() to hit all nearby robots!
                    bot.rc.strike();
                    if (silence<=0)
                    {
                    	silence=50;
                    	bot.broadcastAllNearbyEnemies();
                    }
                    return true;
                } else {
                    // No close robots, so search for robots within sight radius
                    robots = rc.senseNearbyRobots(-1,enemy);

                    // If there is a robot, move towards it
                    if(robots.length > 0) {
                	   if (silence<=0)
                       {
                       	silence=50;
                       	bot.broadcastAllNearbyEnemies();
                       }
                        MapLocation myLocation = rc.getLocation();
                        MapLocation enemyLocation = robots[0].getLocation();
                        Direction toEnemy = myLocation.directionTo(enemyLocation);
                        
                        return tryMove(toEnemy);
                    }
                }
                return false;
    	 }
    	    	 
    	 protected abstract void SubStep() throws GameActionException;
    	 protected Lumberjack bot;
    	 private MapLocation lastLoc;
    	 private int holdCounter = 0;
    	 private int silence=0;
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
    			 bot.rc.setIndicatorDot(tree.getLocation(),50,50,50);
    			 tree = TryChop(tree);
    		 }
    		 else
    		 {
    			 bot.state = bot.findTreeS;
    		 }
    	 }
    	 
    	 public TreeInfo tree;

     }
     
     class GoToState extends State
     {

		public GoToState(Lumberjack bot) {
			super(bot);
			MapLocation botLoc= bot.rc.getLocation();
			loc = botLoc.add(0);			
		}
    	
		@Override
		protected void SubStep() throws GameActionException
		{
			bot.rc.setIndicatorDot(loc,255,255,255);
			if (bot.rc.getLocation().distanceSquaredTo(loc)>15)
			{					
				if (tryMove(rc.getLocation().directionTo(loc)))
					return;

			}
			else
			{
				bot.findTreeS.deadTreeCount=0;
				bot.state = bot.findTreeS;
			}
			
		}
		protected MapLocation loc;
     }
     
     class CallState extends GoToState
     {
    	 public CallState(Lumberjack bot)
    	 {
    		 super(bot);
    	 }
    	 @Override
 		protected void SubStep() throws GameActionException
 		{
    		 super.SubStep();
 		}
    	 public void SetLoc(MapLocation loc)
    	 {
    		 this.loc=loc;
    	 }
     }
     
     class WalkState extends State
     {
		public WalkState(Lumberjack bot) {
			super(bot);
		}
		
		@Override
		protected void SubStep() throws GameActionException
		{					
			MapLocation loc = bot.getBroadcastingEnemyLocation();
			if (loc!=null)
			{
				bot.state=bot.callS;
				bot.callS.loc=loc;
				return;
			}
				
				
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
        			
                	if (trees.length>3)
                	{
                		MapLocation loc = bot.rc.getLocation();
                		CallLumberjacks(loc, false);	
                	}
            		              		
        			
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
    		
    		
    		if (treePlace == null)
    			treePlace = bot.AnswerToCall(1100);
    		
    		
    		if ( treePlace!=null)
    		{

    			
    			if (treePlace.distanceSquaredTo(bot.rc.getLocation())>=16)
    				{
    					if (bot.rc.canMove(treePlace))
    					{
    						bot.rc.move(treePlace);
    						return;
    					}
    				}
    			treePlace = null;
    		}
    		
            bot.state= bot.walkS;
            bot.walkS.StepCounter=0;
    	 }
    	 
    	 MapLocation treePlace;
    	 
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