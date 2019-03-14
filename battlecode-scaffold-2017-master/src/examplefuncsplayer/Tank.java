package examplefuncsplayer;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Tank extends Soldier {

	public Tank(RobotController rc) {
		super(rc);
	}

	@Override
	protected boolean handleCombat(RobotInfo[] nearbyRobots) throws GameActionException {
		boolean result = false;

		// Get the closest enemy
		targetEnemy = getPrioritizedEnemy(nearbyRobots);
		MapLocation enemyLocation = targetEnemy.getLocation();
		Direction dirToTarget = myLocation.directionTo(enemyLocation);
		RobotType enemyType = targetEnemy.getType();

		// Lets set the enemy's location as the group's target
		rc.broadcast(calculateTargetingIndex(), BroadcastManager.zipLocation(enemyLocation));

		result = tryMove(dirToTarget);

		// Try to shoot
		if (rc.canFirePentadShot() && (enemyType == RobotType.SOLDIER || enemyType == RobotType.TANK)) {
			rc.firePentadShot(dirToTarget);
		} else if (rc.canFireTriadShot() && enemyType != RobotType.ARCHON) {
			rc.fireTriadShot(dirToTarget);
		} else if (rc.canFireSingleShot()) {
			rc.fireSingleShot(dirToTarget);
		}

		return result;
	}
}
