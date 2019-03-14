package TestEnemyBot2;
import java.util.ArrayList;
import java.util.List;

import battlecode.common.*;

public strictfp class RobotPlayer {
    //static RobotController rc;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

    	// The robot we will be trying to run
    	Robot robot = null;
    	
        switch (rc.getType()) {
            case ARCHON:
                robot = new Archon(rc);
                break;
            case GARDENER:
                robot = new Gardener(rc);
                break;
            case SOLDIER:
                robot = new Soldier(rc);
                break;
            case LUMBERJACK:
                robot = new Lumberjack(rc);
                break;
			case SCOUT:
				robot = new Scout(rc);
				break;
			case TANK:
				robot = new Tank(rc);
				break;
			default:
				break;
        }
        
        // Run the robot
        robot.run();
	}
}
