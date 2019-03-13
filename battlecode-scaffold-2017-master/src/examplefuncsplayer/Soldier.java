package examplefuncsplayer;

import java.util.Random;

import battlecode.common.BulletInfo;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;
import examplefuncsplayer.Robot.BroadcastType;

public class Soldier extends Robot {

	public static final int GROUP_SIZE = 4;

	public static final int SOLDIER_FIELDS_INCREASE_PERIOD = 6;

	private int groupIndex = -1;

	private RobotInfo targetEnemy = null;

	private MapLocation groupTarget = null;

	private MapLocation myLocation = null;

	public Soldier(RobotController rc) {
		super(rc);
	}

	@Override
	public void run() {
		Team enemyTeam = rc.getTeam().opponent();

		while (true) {
			try {
				myLocation = rc.getLocation();

				// We increase soldier fields each 5 seconds
				if (rc.getRoundNum() % SOLDIER_FIELDS_INCREASE_PERIOD == 0) {
					increaseSoldierField();
				}

				handleGrouping();
				updateGroupTarget();

				// DEBUG:
				if (groupTarget != null) {
					if (rc.getTeam() == Team.A) {
						rc.setIndicatorDot(groupTarget, 255, 0, 0);
					} else {
						rc.setIndicatorDot(groupTarget, 0, 255, 0);
					}
				}

				// Check for nearby robots and handle combat ...
				// else go towards the group target or select one for the group if none
				// available
				RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, enemyTeam);

				// If there are some...
				if (nearbyRobots.length > 0) {
					broadcastAllNearbyEnemies();

					// Handle combat
					handleCombat(nearbyRobots);

				} else {
					if (groupTarget != null) {
						// Check if I am on the target allready
						if (groupTarget.distanceTo(myLocation) < 5) {
							selectNewGroupTarget();
						} else {
							Direction towardsTarget = myLocation.directionTo(groupTarget);
							if (!rc.hasMoved())
								tryMove(towardsTarget, 10, 17);
						}
					} else {
						selectNewGroupTarget();
					}
				}

				// If I didn't move, then just explore randomly
				if (!rc.hasMoved())
					tryMove(randomDirection());

				// Clock.yield() makes the robot wait until the next turn, then it will perform
				// this loop again
				Clock.yield();

			} catch (Exception e) {
				System.out.println("Soldier Exception");
				e.printStackTrace();
			}
		}

	}

	private void selectNewGroupTarget() throws GameActionException {
		// Choose some target for the group from the enemies list or from the broadcast
		// For now lets choose visible enemy, if none exist the one that is broadcasting

		// We will choose a target from seen enemies
		// For now just the first seen enemy
		MapLocation enemyLocation = getSeenEnemyLocation();

		if (enemyLocation != null) {
			rc.broadcast(calculateTargetingIndex(), BroadcastManager.zipLocation(enemyLocation));
			return;
		}

		// Rarely we will try to follow some broadcasting target
		Random rnd = new Random();
		if (rnd.nextFloat() < 0.01f) {
			// We will choose a target from broadcasting enemies
			// Again, for now it will again be just the first enemy
			enemyLocation = getBroadcastingEnemyLocation();

			if (enemyLocation != null) {
				rc.broadcast(calculateTargetingIndex(), BroadcastManager.zipLocation(enemyLocation));
			}
		}

		// If both null wait

	}

	private void handleGrouping() throws GameActionException {
		// Look for a group if I don't have one.
		if (groupIndex == -1) {
			lookForAGroup();
		} else {
			// Check if our group is full and stop advertising if we are advertising
			int groupCount = rc.readBroadcast(groupIndex);
			int advertisingGroup = rc.readBroadcast(BroadcastType.SoldierAdvertisingField.getChannel());

			if (groupCount == GROUP_SIZE) {
				// Check if we are advertising and stop it.

				if (advertisingGroup == groupIndex) {
					rc.broadcast(BroadcastType.SoldierAdvertisingField.getChannel(), 0);
					System.out.println("Stopped advertising.");
				}
			}
			// We should be advertising if we are almost empty or wait for our turn to
			// advertise
			else if (groupCount == 1) {
				if (advertisingGroup == 0) {
					advertise();
				}
				// Else we will wait
			}
		}
	}

	private void updateGroupTarget() throws GameActionException {
		// Update the group's target if I have a group
		if (groupIndex != -1) {
			int number = rc.readBroadcast(calculateTargetingIndex());

			groupTarget = BroadcastManager.unzipLocation(number);
		}

	}

	private MapLocation getSeenEnemyLocation() throws GameActionException {
		for (int i = BroadcastType.EnemyLocationsStart.getChannel(); i < BroadcastType.EnemyLocationsEnd
				.getChannel(); i++) {
			int num = rc.readBroadcast(i);

			if (num != 0) {
				return BroadcastManager.unzipLocation(num);
			}
		}

		return null;
	}

	
	private int calculateTargetingIndex() {
		return BroadcastType.SoldierTargetingStart.getChannel()
				+ (groupIndex - BroadcastType.SoldierFieldsStart.getChannel());
	}

	private boolean handleCombat(RobotInfo[] nearbyRobots) throws GameActionException {
		boolean result = false;

		// Get the closest enemy
		targetEnemy = getPrioritizedEnemy(nearbyRobots);
		MapLocation enemyLocation = targetEnemy.getLocation();
		Direction dirToTarget = myLocation.directionTo(enemyLocation);
		RobotType enemyType = targetEnemy.getType();

		// Lets set the enemy's location as the group's target
		rc.broadcast(calculateTargetingIndex(), BroadcastManager.zipLocation(enemyLocation));

		// TODO: Check for trees here
		if (!dodge()) {
			result = tryMove(dirToTarget);
		}

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

	private RobotInfo getPrioritizedEnemy(RobotInfo[] nearestRobots) {
		RobotInfo target = null;

		RobotType targetType = null;
		float targetDist = rc.getType().sensorRadius + 1;
		int targetPriority = getSoldierTargetPriority(targetType);
		// Find target with most priority
		for (RobotInfo rb : nearestRobots) {
			RobotType rbType = rb.type;
			int rbPriority = getSoldierTargetPriority(rbType);
			if (rbPriority == -1)
				continue; // Ignore unit
			if (rbPriority > targetPriority
					|| (rbPriority == targetPriority && myLocation.distanceTo(rb.location) < targetDist)) {
				target = rb;
				targetType = target.type;
				targetDist = target.getLocation().distanceTo(myLocation);
				targetPriority = getSoldierTargetPriority(targetType);
			}
		}

		return target;
	}

	private int getSoldierTargetPriority(RobotType targetType) {
		if (targetType == null)
			return -1;

		switch (targetType) {
		case ARCHON:
			return 1;
		case GARDENER:
			return 6;
		case LUMBERJACK:
			return 4;
		case SOLDIER:
			return 5;
		case SCOUT:
			return 2;
		case TANK:
			return 3;
		}
		return -1;

	}

	private boolean dodge() throws GameActionException {
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
		if (targetLoc.equals(myLocation)) {
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
		}

		// Only move if there's actually stuff to dodge.
		if (!targetLoc.equals(myLocation)) {
			return tryMove(targetLoc, 10, 17);
		}

		return false;
	}

	public static boolean intersects(MapLocation pointA, MapLocation pointB, MapLocation center, double radius) {
		double baX = pointB.x - pointA.x;
		double baY = pointB.y - pointA.y;
		double caX = center.x - pointA.x;
		double caY = center.y - pointA.y;

		double a = baX * baX + baY * baY;
		double bBy2 = baX * caX + baY * caY;
		double c = caX * caX + caY * caY - radius * radius;

		double pBy2 = bBy2 / a;
		double q = c / a;

		double disc = pBy2 * pBy2 - q;
		if (disc < 0) {
			return false;
		}

		return true;
	}

	private void increaseSoldierField() throws GameActionException {
		if (groupIndex == -1)
			return;

		int number = rc.readBroadcast(groupIndex);
		if (number <= GROUP_SIZE)
			rc.broadcast(groupIndex, number + 1);

	}

	private void advertise() throws GameActionException {
		// Advertise only if it is not the turn when the Archon annuls the fields
		// Or the turn when we increment the fields
		if (rc.getRoundNum() % Archon.SOLDIER_FIELDS_CLEANUP_PERIOD == 0
				&& rc.getRoundNum() % SOLDIER_FIELDS_INCREASE_PERIOD == 0) {
			return;
		}

		// No one is advertising, I will create my own group
		groupIndex = findEmptyGroupIndex();

		if (groupIndex == -1) {
			System.out.println("Cannot find empty group index.");
			return;
		} // FAIL

		System.out.println("Creating new group with index: " + groupIndex);

		// Start advertising
		rc.broadcast(BroadcastType.SoldierAdvertisingField.getChannel(), groupIndex);
		rc.broadcast(groupIndex, 1);
	}

	private void lookForAGroup() throws GameActionException {
		// I don't have a group and I will try to get one, or start advertising my own
		System.out.println("Looking for a group.");

		// We look for a group only if we know that the number specifing group count
		// will be the correct number of members.
		if (rc.getRoundNum() % Archon.SOLDIER_FIELDS_CLEANUP_PERIOD == 0
				&& rc.getRoundNum() % SOLDIER_FIELDS_INCREASE_PERIOD == 0) {
			return;
		}

		int advertisingGroup = rc.readBroadcast(BroadcastType.SoldierAdvertisingField.getChannel());
		if (advertisingGroup == 0) {
			advertise();
		} else {
			// I will add myself to the advertising group, if not full
			int groupCount = rc.readBroadcast(advertisingGroup);
			if (groupCount < GROUP_SIZE) {
				rc.broadcast(advertisingGroup, groupCount + 1);
				groupIndex = advertisingGroup;

				System.out.println("Joining group with index: " + groupIndex);
			}
		}
	}

	private int findEmptyGroupIndex() throws GameActionException {
		for (int i = BroadcastType.SoldierFieldsStart.getChannel(); i < BroadcastType.SoldierFieldsEnd
				.getChannel(); i++) {
			int number = rc.readBroadcastInt(i);

			System.out.println(i + ": " + number);

			if (number == 0) {
				return i;
			}
		}

		return -1;
	}

}
