package examplefuncsplayer;

import java.util.Random;

import battlecode.common.BulletInfo;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TreeInfo;

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
		AllyArchonLocationX(0), AllyArchonLocationY(1), EnemyArchonLocationX(2), EnemyArchonLocationY(
				3), AttackLocationX(4), AttackLocationY(5), RegroupLocationX(6), RegroupLocationY(7),

		// Spawning Broadcast
		SpawnGardener(10), SpawnLumberjack(11), SpawnSoldier(12), SpawnTank(13), SpawnScout(14), SpawnSoldierRush(15),

		// Soldiers
		SoldierFieldsStart(501), SoldierFieldsEnd(600), SoldierAdvertisingField(500), SoldierTargetingStart(
				601), SoldierTargetingEnd(700),

		// Enemy locations

		EnemyLocationsStart(401), EnemyLocationsEnd(449), BroadcastLocationsStart(450), BroadcastLocationsEnd(
				499), EnemyArchonLocationSingle(400);

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
			rc.broadcast(BroadcastType.EnemyArchonLocationSingle.getChannel(),
					BroadcastManager.zipLocation(rb.getLocation()));
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
	 * @param dir
	 *            The intended direction of movement
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
	 * @param bullet
	 *            The bullet in question
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

	protected MapLocation getBroadcastingEnemyLocation() throws GameActionException {
		Random rnd = new Random();
		int i = rnd.nextInt(
				BroadcastType.BroadcastLocationsEnd.getChannel() - BroadcastType.BroadcastLocationsStart.getChannel());

		int num = rc.readBroadcast(i);

		if (num != 0) {
			return BroadcastManager.unzipLocation(num);
		} else {
			return null;
		}
	}

	/**
	 * Attempts to move in a given direction, while avoiding small obstacles
	 * direction in the path.
	 *
	 * @param dir
	 *            The intended direction of movement
	 * @param degreeOffset
	 *            Spacing between checked directions (degrees)
	 * @param checksPerSide
	 *            Number of extra directions checked on each side, if intended
	 *            direction was unavailable
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

	/*
	 * Calls one lumberjack to the location. One lumberjack will be send per
	 * request.
	 * 
	 * @param loc The location where you need a lumberjack.
	 * 
	 * @param inNecessary Is this request urgent or it is only hit where trees can
	 * be found.
	 */
	protected void CallLumberjacks(MapLocation loc, boolean isNecessary) throws GameActionException {
		int startIndex;
		if (isNecessary)
			startIndex = 1000;
		else
			startIndex = 1100;

		for (int i = startIndex; i < startIndex + 100; i++) {

			int intLoc = rc.readBroadcastInt(i);
			if (intLoc == 0) {
				rc.broadcast(i, LocToInt(loc));
				return;
			}
		}
		Random rnd = new Random();
		int index = startIndex + rnd.nextInt(100);
		rc.broadcast(index, LocToInt(loc));

		return;
	}

	protected int LocToInt(MapLocation loc) {
		return (((int) loc.x) << 16) + (int) loc.y;
	}

	protected MapLocation IntToLoc(int loc) {
		return new MapLocation(loc >>> 16, (loc << 16) >>> 16);
	}

	protected boolean dodge(MapLocation myLocation) throws GameActionException {
		// Initialize the target location, we do not move by default
		MapLocation targetLoc = myLocation;

		// Sense radius using body size, speed and speed of fastest bullet (tank)
		float MAX_SENSE_RANGE = rc.getType().bodyRadius + rc.getType().strideRadius + RobotType.TANK.bulletSpeed;

		// Get the nearby bullets
		BulletInfo[] nearbyBullets = rc.senseNearbyBullets(MAX_SENSE_RANGE);
		if (nearbyBullets.length > 0) {
			for (BulletInfo bullet : nearbyBullets) {
				Direction bulletDir = bullet.dir;

				// Calculate next bullet location
				MapLocation p1 = bullet.location;
				MapLocation p2 = p1.add(bulletDir, bullet.speed);

				float xDiff = p2.x - p1.x;
				float yDiff = p2.y - p1.y;

				// Calculate smallest vector between intended loc and bullet trajectory
				float distance = (float) (Math
						.abs((yDiff * targetLoc.x) - (xDiff * targetLoc.y) + (p2.x * p1.y) - (p2.y * p1.x))
						/ Math.sqrt((yDiff * yDiff) + (xDiff * xDiff)));

				Direction dir;
				if (bulletDir.degreesBetween(p1.directionTo(targetLoc)) > 0) {
					dir = bulletDir.rotateLeftDegrees(90);
				} else {
					dir = bulletDir.rotateRightDegrees(90);
				}

				distance = Math.max(0, rc.getType().bodyRadius - distance);

				targetLoc = targetLoc.add(dir, distance);
			}
		}

		// Avoid lumberjacks
		// If we are in his strike radius, we move away from him
		RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		if (nearbyEnemyRobots.length > 0) {
			for (RobotInfo enemyRobot : nearbyEnemyRobots) {
				if (enemyRobot.getType() == RobotType.LUMBERJACK && MapLocation.doCirclesCollide(targetLoc,
						rc.getType().bodyRadius, enemyRobot.location, GameConstants.LUMBERJACK_STRIKE_RADIUS)) {
					float distance = rc.getType().bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS
							- targetLoc.distanceTo(enemyRobot.location);
					targetLoc = targetLoc.add(targetLoc.directionTo(enemyRobot.location).opposite(), distance * 2);
				}
			}
		}

		// Not dodging
		/*if (targetLoc.equals(myLocation)) {
			// Check if there is a tree blocking the view of a target enemy
			TreeInfo[] nearbyTrees = rc.senseNearbyTrees(MAX_SENSE_RANGE);
			if (nearbyTrees.length > 0) {
				TreeInfo closestTree = nearbyTrees[0];
				MapLocation closestTreeLocation = closestTree.getLocation();

				// If tree blocks the projectiles, move to the closer end
				if (intersects(myLocation, targetEnemy.getLocation(), closestTree.getLocation(),
						closestTree.getRadius())) {

					Direction toLeft = myLocation.directionTo(closestTreeLocation).rotateLeftDegrees(90);
					Direction toRight = myLocation.directionTo(closestTreeLocation).rotateRightDegrees(90);

					if (closestTreeLocation.add(toRight).distanceTo(myLocation) < closestTreeLocation.add(toLeft)
							.distanceTo(myLocation)) {
						// Move to the right
						return tryMove(myLocation.add(toRight, rc.getType().strideRadius), 10, 17);

					} else {
						return tryMove(myLocation.add(toLeft, rc.getType().strideRadius), 10, 17);
					}
				}
			}
		}*/

		// Only move if there's actually stuff to dodge.
		if (!targetLoc.equals(myLocation)) {
			return tryMove(targetLoc, 10, 17);
		}

		return false;
	}
	
	protected void buyBulletsToWin() throws GameActionException {
		if (rc.getTeamBullets() / rc.getVictoryPointCost() >= GameConstants.VICTORY_POINTS_TO_WIN) {
			rc.donate(rc.getTeamBullets());
		}
	}

}
