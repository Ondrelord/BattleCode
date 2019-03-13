package examplefuncsplayer;

import battlecode.common.BulletInfo;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public abstract class Robot {

	protected RobotController rc;

	public Robot(RobotController rc) {
		this.rc = rc;
	}

	// Runs the Robot
	public abstract void run();

	// --------------------------

	public static enum BroadcastType {
		// Locations
		AllyArchonLocationX(0),
		AllyArchonLocationY(1),
		EnemyArchonLocationX(2),
		EnemyArchonLocationY(3),
		AttackLocationX(4), 
		AttackLocationY(5),
		RegroupLocationX(6),
		RegroupLocationY(7),

		// Spawning Broadcast
		SpawnGardener(10),
		SpawnLumberjack(11),
		SpawnSoldier(12),
		SpawnTank(13),
		SpawnScout(14),
		SpawnSoldierRush(15),

		// Soldiers
		SoldierFieldsStart(501),
		SoldierFieldsEnd(600),
		SoldierAdvertisingField(500),
		SoldierTargetingStart(601),
		SoldierTargetingEnd(700),

		// Enemy locations

		EnemyLocationsStart(401),
		EnemyLocationsEnd(449),
		BroadcastLocationsStart(450),
		BroadcastLocationsEnd(499),
		EnemyArchonLocationSingle(400);

		private final int channel;

		private BroadcastType(int channel) {
			this.channel = channel;
		}

		public int getChannel() {
			return channel;
		}
	}

	protected void checkForArchon(RobotInfo rb) throws GameActionException {
		if (rb.getType() == RobotType.ARCHON) {
			rc.broadcast(BroadcastType.EnemyArchonLocationSingle.getChannel(), BroadcastManager.zipLocation(rb.getLocation()));
		}
		
	}
	
	/**
	 * Returns a random Direction
	 * 
	 * @return a random Direction
	 */
	protected static Direction randomDirection() {
		return new Direction((float) Math.random() * 2 * (float) Math.PI);
	}

	/**
	 * Attempts to move in a given direction, while avoiding small obstacles
	 * directly in the path.
	 *
	 * @param dir The intended direction of movement
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	protected boolean tryMove(Direction dir) throws GameActionException {
		return tryMove(dir, 20, 3);
	}

	/**
	 * A slightly more complicated example function, this returns true if the given
	 * bullet is on a collision course with the current robot. Doesn't take into
	 * account objects between the bullet and this robot.
	 *
	 * @param bullet The bullet in question
	 * @return True if the line of the bullet's path intersects with this robot's
	 *         current position.
	 */
	protected boolean willCollideWithMe(BulletInfo bullet) {
		MapLocation myLocation = rc.getLocation();

		// Get relevant bullet information
		Direction propagationDirection = bullet.dir;
		MapLocation bulletLocation = bullet.location;

		// Calculate bullet relations to this robot
		Direction directionToRobot = bulletLocation.directionTo(myLocation);
		float distToRobot = bulletLocation.distanceTo(myLocation);
		float theta = propagationDirection.radiansBetween(directionToRobot);

		// If theta > 90 degrees, then the bullet is traveling away from us and we can
		// break early
		if (Math.abs(theta) > Math.PI / 2) {
			return false;
		}

		// distToRobot is our hypotenuse, theta is our angle, and we want to know this
		// length of the opposite leg.
		// This is the distance of a line that goes from myLocation and intersects
		// perpendicularly with propagationDirection.
		// This corresponds to the smallest radius circle centered at our location that
		// would intersect with the
		// line that is the path of the bullet.
		float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

		return (perpendicularDist <= rc.getType().bodyRadius);
	}

	protected void broadcastAllNearbyEnemies() throws GameActionException {
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

		if (nearbyRobots.length > 0) {
			int lastLocationIndex = 0;
			for (int i = 0; i < nearbyRobots.length; i++) {
				// Check if it isn't an archon and share it's location if it is.
				checkForArchon(nearbyRobots[i]);
				
				
				// Find empty location
				lastLocationIndex = findEmptyEnemiesLocation(lastLocationIndex);

				if (lastLocationIndex == -1) {
					// Locations are full, do nothing
					return;
				}

				rc.broadcast(lastLocationIndex, BroadcastManager.zipLocation(nearbyRobots[i].getLocation()));
			}
		}
	}

	private int findEmptyEnemiesLocation(int lastLocationIndex) throws GameActionException {
		int index = lastLocationIndex;
		if (lastLocationIndex == 0) {
			index = (int) BroadcastType.EnemyLocationsStart.getChannel();
		}

		for (; index < BroadcastType.EnemyLocationsEnd.getChannel(); index++) {
			int location = rc.readBroadcast(index);
			if (location == 0) { // empty
				return index;
			}
		}

		return -1;
	}

	/**
	 * Attempts to move in a given direction, while avoiding small obstacles
	 * direction in the path.
	 *
	 * @param dir           The intended direction of movement
	 * @param degreeOffset  Spacing between checked directions (degrees)
	 * @param checksPerSide Number of extra directions checked on each side, if
	 *                      intended direction was unavailable
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	protected boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

		// First, try intended direction
		if (rc.canMove(dir)) {
			rc.move(dir);
			return true;
		}

		// Now try a bunch of similar angles
		boolean moved = false;
		int currentCheck = 1;

		while (currentCheck <= checksPerSide) {
			// Try the offset of the left side
			if (rc.canMove(dir.rotateLeftDegrees(degreeOffset * currentCheck))) {
				rc.move(dir.rotateLeftDegrees(degreeOffset * currentCheck));
				return true;
			}
			// Try the offset on the right side
			if (rc.canMove(dir.rotateRightDegrees(degreeOffset * currentCheck))) {
				rc.move(dir.rotateRightDegrees(degreeOffset * currentCheck));
				return true;
			}
			// No move performed, try slightly further
			currentCheck++;
		}

		// A move never happened, so return false.
		return false;
	}

	protected boolean tryMove(MapLocation loc, float degreeOffset, int checksPerSide) throws GameActionException {

		// First, try intended direction
		if (rc.canMove(loc)) {
			rc.move(loc);
			return true;
		}

		Direction dir = rc.getLocation().directionTo(loc);
		float distance = rc.getLocation().distanceTo(loc);

		// Now try a bunch of similar angles
		int currentCheck = 1;

		while (currentCheck <= checksPerSide) {
			// Try the offset of the left side
			if (rc.canMove(dir.rotateLeftDegrees(degreeOffset * currentCheck), distance)) {
				rc.move(dir.rotateLeftDegrees(degreeOffset * currentCheck), distance);
				return true;
			}
			// Try the offset on the right side
			if (rc.canMove(dir.rotateRightDegrees(degreeOffset * currentCheck), distance)) {
				rc.move(dir.rotateRightDegrees(degreeOffset * currentCheck), distance);
				return true;
			}
			// No move performed, try slightly further
			currentCheck++;
		}

		// A move never happened, so return false.
		return false;
	}
}
