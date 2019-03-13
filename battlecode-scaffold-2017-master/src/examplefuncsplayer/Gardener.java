package examplefuncsplayer;

import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

public class Gardener extends Robot {
	private int state = 0;
	private MapLocation enemy;
	private MapLocation ally;
	private MapLocation myLocation;
	private Direction spawnDir;
	private Boolean spawnDirSet = false;

	public Gardener(RobotController rc) {
		super(rc);
		enemy = rc.getInitialArchonLocations(rc.getTeam().opponent())[0];
		ally = rc.getInitialArchonLocations(rc.getTeam())[0];
		myLocation = rc.getLocation();
		spawnDir = myLocation.directionTo(enemy);

	}

	@Override
	public void run() {
		int initMovement = 10;
		// float plantOffset = (float) (Math.PI/3);

		// The code you want your robot to perform every round should be in this loop
		while (true) {
			// Try/catch blocks stop unhandled exceptions, which cause your robot to explode
			try {
				myLocation = rc.getLocation();

				if (!rc.isBuildReady())
					continue;
				
				if (trySoldierRush())
					continue;
				
				trySpawn();

				switch (state) {
				case 0:
					if (--initMovement > 0)
						if (!rc.hasMoved())
							tryMove(myLocation.directionTo(enemy));
						else
							state = 1;
				case 1:
					System.out.println("State 0: Finding good location");
					if (findLocation(myLocation))
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

				// Clock.yield() makes the robot wait until the next turn, then it will perform
				// this loop again
				Clock.yield();

			} catch (Exception e) {
				System.out.println("Gardener Exception");
				e.printStackTrace();
			}
		}
	}

	private boolean trySoldierRush() throws GameActionException {
		int soldierRushCount = rc.readBroadcastInt(BroadcastType.SpawnSoldierRush.getChannel());
		System.out.println("Soldier rush count: " + soldierRushCount);
		
		if (soldierRushCount > 0) {
			// In a random direction
			Random rnd = new Random();
			float i = rnd.nextFloat();
			Direction dir = new Direction((float) (i * 2 * Math.PI));
			
			System.out.println("Trying to spawn soldier.");
			
			if (tryToBuildUnit(RobotType.SOLDIER)) {
				rc.broadcast(BroadcastType.SpawnSoldierRush.getChannel(), soldierRushCount - 1);
				System.out.println("Spawning: Rush soldier " + (soldierRushCount-1) + " left.");
				return true;
			}			
		}
		
		return false;
	}
	
	private boolean tryToBuildUnit(RobotType toBuild) throws GameActionException {
		Direction test = Direction.getNorth();
		for (int deltaDegree = (int) (Math.random()
				* 360), count = 0; count < 36; deltaDegree += 10, deltaDegree %= 360, count++) {
			if (rc.canBuildRobot(toBuild, test.rotateLeftDegrees(deltaDegree))) {
				rc.buildRobot(toBuild, test.rotateLeftDegrees(deltaDegree));
				return true;
			}
		}
		return false;
	}

	public void plantTrees() {
		try {
			for (float i = 0; i < 1.0f; i += 1.0f / 6.0f) {
				Direction dir = new Direction((float) (i * 2 * Math.PI));
				if (rc.canPlantTree(dir)) {
					if (Math.abs(dir.degreesBetween(myLocation.directionTo(enemy))) > 30) {
						System.out.println("Planting trees");
						rc.plantTree(dir);
					} else if (!spawnDirSet) {
						spawnDir = dir;
						spawnDirSet = true;
					}

				}
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public void waterTrees() {
		try {
			TreeInfo treeToWater = null;
			for (TreeInfo tree : rc.senseNearbyTrees(3)) {
				if (tree == null)
					return;
				if (treeToWater == null)
					treeToWater = tree;
				else if (treeToWater.getHealth() > tree.getHealth())
					treeToWater = tree;
			}
			if (treeToWater != null && rc.canWater(treeToWater.ID))
				rc.water(treeToWater.ID);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public boolean findLocation(MapLocation center) {
		try {
			MapLocation path = center;
			if (!rc.isCircleOccupiedExceptByThisRobot(center, 3) && rc.onTheMap(center, 3))
				return true;
			else {
				for (TreeInfo tree : rc.senseNearbyTrees(3, Team.NEUTRAL)) {
					if (tree != null) {
						Direction dir = randomDirection();
						if (rc.canBuildRobot(RobotType.LUMBERJACK, dir)) {
							rc.buildRobot(RobotType.LUMBERJACK, dir);
						}
					}
				}
			}
			if (Math.random() >= 0.3) {
				for (float i = 0; i < 1; i += Math.random() / 2.0f) {
					MapLocation loc = center.add((float) (i * 2 * Math.PI));
					if (!rc.isLocationOccupied(loc)) {
						float point = ally.distanceTo(enemy) - ((float) (rc.getRoundNum()))
								/ ((float) (rc.getRoundLimit())) * ally.distanceTo(enemy);

						if (Math.abs(path.distanceTo(enemy) - point) > Math.abs(loc.distanceTo(enemy) - point)) {
							path = loc;
						}
					}
				}
				if (!rc.hasMoved())
					tryMove(center.directionTo(path));
			} else {
				rc.setIndicatorLine(center, path, 255, 0, 0);
				Direction dir = new Direction((float) Math.random() * 2 * (float) Math.PI);
				if (!rc.hasMoved())
					tryMove(dir);
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}

		return false;
	}

	public void trySpawn() {
		try {
			if (state < 2)
				return;

			if (rc.readBroadcastInt(BroadcastType.SpawnSoldier.getChannel()) > 0) {
				if (spawnDir != null && rc.canBuildRobot(RobotType.SOLDIER, spawnDir)) {
					rc.buildRobot(RobotType.SOLDIER, spawnDir);
					rc.broadcast(BroadcastType.SpawnSoldier.getChannel(),
							rc.readBroadcast(BroadcastType.SpawnSoldier.getChannel()) - 1);
					System.out.println("Spawning: Soldier");
				}
			}

			if (rc.readBroadcast(BroadcastType.SpawnLumberjack.getChannel()) > 0)
				if (spawnDir != null && rc.canBuildRobot(RobotType.LUMBERJACK, spawnDir)) {
					rc.buildRobot(RobotType.LUMBERJACK, spawnDir);
					rc.broadcast(BroadcastType.SpawnLumberjack.getChannel(),
							rc.readBroadcast(BroadcastType.SpawnLumberjack.getChannel()) - 1);
					System.out.println("Spawning: Lumberjack");
				}

			if (rc.readBroadcast(BroadcastType.SpawnScout.getChannel()) > 0)
				if (spawnDir != null && rc.canBuildRobot(RobotType.SCOUT, spawnDir)) {
					rc.buildRobot(RobotType.SCOUT, spawnDir);
					rc.broadcast(BroadcastType.SpawnScout.getChannel(),
							rc.readBroadcast(BroadcastType.SpawnScout.getChannel()) - 1);
					System.out.println("Spawning Scout");
				}

			if (rc.readBroadcast(BroadcastType.SpawnTank.getChannel()) > 0)
				if (spawnDir != null && rc.canBuildRobot(RobotType.TANK, spawnDir)) {
					rc.buildRobot(RobotType.TANK, spawnDir);
					rc.broadcast(BroadcastType.SpawnTank.getChannel(),
							rc.readBroadcast(BroadcastType.SpawnTank.getChannel()) - 1);
					System.out.println("Spawning: Tank");
				}

		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}
}
