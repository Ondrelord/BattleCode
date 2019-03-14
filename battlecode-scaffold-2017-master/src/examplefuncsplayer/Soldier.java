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

	public static final int DEFAULT_RANDOM_MOVEMENT_TICK = 5;

	public static final int ADVANCED_SHOOTING_STARTING_ROUND = 100;

	protected int groupIndex = -1;

	protected RobotInfo targetEnemy = null;

	protected MapLocation groupTarget = null;

	protected MapLocation myLocation = null;

	protected int randomMovementTick = 0;

	protected Direction randomDirection;

	public Soldier(RobotController rc) {
		super(rc);
	}

	@Override
	public void run() {
		Team enemyTeam = rc.getTeam().opponent();

		while (true) {
			try {
				myLocation = rc.getLocation();

				// Try to buy bullets to win
				buyBulletsToWin();

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
					// First, avoid gardeners, if possible
					if (!rc.hasMoved()) {
						avoidGardeners();
					}
					
					if (groupTarget != null) {
						// Check if I am on the target allready
						if (groupTarget.distanceTo(myLocation) < 5) {
							selectNewGroupTarget();
						} else {
							Direction towardsTarget = myLocation.directionTo(groupTarget);
							if (!rc.hasMoved()) {
								tryMove(towardsTarget, 10, 17);
							}
						}
					} else {
						selectNewGroupTarget();
					}
				}



				// If I didn't move, then just explore randomly
				if (!rc.hasMoved()) {
					doRandomMove();
				}

				// Check for nearby trees and report them back to lumberjack
				TreeInfo[] trees = rc.senseNearbyTrees(-1);
				if (trees.length > 0) {
					processTrees(trees);
				}

				// Decrease random movement tick;
				randomMovementTick--;

				// Clock.yield() makes the robot wait until the next turn, then it will perform
				// this loop again
				Clock.yield();

			} catch (Exception e) {
				System.out.println("Soldier Exception");
				e.printStackTrace();
			}
		}
	}

	private void processTrees(TreeInfo[] trees) throws GameActionException {
		TreeInfo closestTree = trees[0];
		CallLumberjacks(closestTree.getLocation(), false);

		// Try to shake a closest tree
		if (rc.canShake(closestTree.getID()) && closestTree.getContainedBullets() > 0) {
			rc.shake(closestTree.getID());
		}

	}

	protected void doRandomMove() throws GameActionException {
		if (randomMovementTick <= 0) {
			randomDirection = randomDirection();
			randomMovementTick = DEFAULT_RANDOM_MOVEMENT_TICK;
		}

		tryMove(randomDirection);
	}

	protected void avoidGardeners() throws GameActionException {
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots(3, rc.getTeam());
		for (RobotInfo robot : nearbyRobots) {
			if (robot.getType() == RobotType.GARDENER) {
				// Found a gardener, move away from him

				float distance = rc.getType().bodyRadius + myLocation.distanceTo(robot.getLocation());
				MapLocation targetLoc = myLocation.add(myLocation.directionTo(robot.getLocation()).opposite(),
						distance * 2);
				
				if (groupTarget != null) {
					targetLoc = targetLoc.add(myLocation.directionTo(groupTarget));
				}

				tryMove(targetLoc, 10, 17);

			}
		}
	}

	protected void selectNewGroupTarget() throws GameActionException {
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
		if (rnd.nextFloat() < 0.04f) {
			// We will choose a target from broadcasting enemies
			// Again, for now it will again be just the first enemy
			enemyLocation = getBroadcastingEnemyLocation();

			if (enemyLocation != null) {
				rc.broadcast(calculateTargetingIndex(), BroadcastManager.zipLocation(enemyLocation));
			}
		}

		// If both null wait

	}

	protected void handleGrouping() throws GameActionException {
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

	protected void updateGroupTarget() throws GameActionException {
		// Update the group's target if I have a group
		if (groupIndex != -1) {
			int number = rc.readBroadcast(calculateTargetingIndex());

			groupTarget = BroadcastManager.unzipLocation(number);
		}

	}

	protected MapLocation getSeenEnemyLocation() throws GameActionException {
		for (int i = BroadcastType.EnemyLocationsStart.getChannel(); i < BroadcastType.EnemyLocationsEnd
				.getChannel(); i++) {
			int num = rc.readBroadcast(i);

			if (num != 0) {
				return BroadcastManager.unzipLocation(num);
			}
		}

		return null;
	}

	protected int calculateTargetingIndex() {
		return BroadcastType.SoldierTargetingStart.getChannel()
				+ (groupIndex - BroadcastType.SoldierFieldsStart.getChannel());
	}

	protected boolean handleCombat(RobotInfo[] nearbyRobots) throws GameActionException {
		boolean result = false;

		// Get the closest enemy
		targetEnemy = getPrioritizedEnemy(nearbyRobots);
		MapLocation enemyLocation = targetEnemy.getLocation();
		Direction dirToTarget = myLocation.directionTo(enemyLocation);
		RobotType enemyType = targetEnemy.getType();

		// Lets set the enemy's location as the group's target
		rc.broadcast(calculateTargetingIndex(), BroadcastManager.zipLocation(enemyLocation));

		if (!dodge(myLocation)) {
			result = tryMove(dirToTarget);
		}

		if (rc.getRoundNum() > ADVANCED_SHOOTING_STARTING_ROUND) {

			// Try to shoot
			if (rc.canFirePentadShot() && (enemyType == RobotType.SOLDIER || enemyType == RobotType.TANK)) {
				rc.firePentadShot(dirToTarget);
			} else if (rc.canFireTriadShot() && enemyType != RobotType.ARCHON) {
				rc.fireTriadShot(dirToTarget);
			} else if (rc.canFireSingleShot()) {
				rc.fireSingleShot(dirToTarget);
			}
		} else {
			if (rc.canFireSingleShot()) {
				rc.fireSingleShot(dirToTarget);
			}
		}

		return result;
	}

	protected RobotInfo getPrioritizedEnemy(RobotInfo[] nearestRobots) {
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

	protected int getSoldierTargetPriority(RobotType targetType) {
		if (targetType == null)
			return -1;

		switch (targetType) {
		case ARCHON:
			return 1;
		case GARDENER:
			return 6;
		case LUMBERJACK:
			return 3;
		case SOLDIER:
			return 5;
		case SCOUT:
			return 2;
		case TANK:
			return 4;
		}
		return -1;

	}

	protected boolean intersects(MapLocation pointA, MapLocation pointB, MapLocation center, double radius) {
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

	protected void increaseSoldierField() throws GameActionException {
		if (groupIndex == -1)
			return;

		int number = rc.readBroadcast(groupIndex);
		if (number <= GROUP_SIZE)
			rc.broadcast(groupIndex, number + 1);

	}

	protected void advertise() throws GameActionException {
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

	protected void lookForAGroup() throws GameActionException {
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

	protected int findEmptyGroupIndex() throws GameActionException {
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
