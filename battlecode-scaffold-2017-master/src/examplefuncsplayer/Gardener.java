package examplefuncsplayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

public class Gardener extends Robot {
	static int state = 0;
	static MapLocation enemy;
	static MapLocation ally;
	static MapLocation myLocation;
	static Direction spawnDir;
	static Boolean spawnDirSet = false;

	private int soldierCount = 0;

	static boolean canMove;
	static MapLocation eastBreakpoint;
	static MapLocation westBreakpoint;

	public Gardener(RobotController rc) {
		super(rc);
		enemy = rc.getInitialArchonLocations(rc.getTeam().opponent())[0];
		ally = rc.getInitialArchonLocations(rc.getTeam())[0];
		myLocation = rc.getLocation();
		spawnDir = Direction.EAST;

		canMove = false;
		eastBreakpoint = null;
		westBreakpoint = null;

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

				//if (!rc.isBuildReady())
					//continue;

				if (trySoldierRush())
					continue;

				trySpawn();

				switch (state) {
				case 0:
					// if (--initMovement > 0)
					// tryMove(myLocation.directionTo(enemy));
					// else
					state = 1;
					break;
				case 1:
					System.out.println("State 0: Finding good location");
					if (findLocation()) {
						eastBreakpoint = myLocation;
						westBreakpoint = myLocation.add(Direction.WEST, 7 * 1.5f);
						state = 2;
					}
					break;
				case 2:
					System.out.println("State 2: Planting trees and taking care of");
					rc.setIndicatorLine(westBreakpoint, eastBreakpoint, 0, 255, 0);
					plantTrees();
					roam();
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
				System.out.println("Spawning: Rush soldier " + (soldierRushCount - 1) + " left.");
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
			float spacing = 1.25f;

			if (!rc.isCircleOccupiedExceptByThisRobot(myLocation.add(Direction.NORTH), spacing)
					&& rc.canPlantTree(Direction.NORTH) && myLocation.y - eastBreakpoint.y >= 0) {
				System.out.println("Planting NORTH");
				rc.plantTree(Direction.NORTH);
			} else if (!rc.isCircleOccupiedExceptByThisRobot(myLocation.add(Direction.SOUTH), spacing)
					&& rc.canPlantTree(Direction.SOUTH) && myLocation.y - eastBreakpoint.y <= 0) {
				System.out.println("Planting SOUTH");
				rc.plantTree(Direction.SOUTH);
			}
			/*
			 * else { System.out.println("Moving to new position");
			 * if(rc.isCircleOccupiedExceptByThisRobot(myLocation.add(Direction.SOUTH),
			 * spacing) &&
			 * rc.isCircleOccupiedExceptByThisRobot(myLocation.add(Direction.NORTH),
			 * spacing)) { if(canMove) { if (myLocation.x >= eastBreakpoint.x) { canMove =
			 * !canMove; //return; }
			 * 
			 * if(!tryMove(myLocation.directionTo(eastBreakpoint))) {
			 * if(!rc.onTheMap(myLocation.add(Direction.EAST))) {
			 * westBreakpoint.add(Direction.WEST, Math.abs(eastBreakpoint.x -
			 * myLocation.x)); eastBreakpoint = myLocation; } canMove = !canMove; } } else {
			 * if (myLocation.x <= westBreakpoint.x) { canMove = !canMove; //return; }
			 * 
			 * if(!tryMove(myLocation.directionTo(westBreakpoint))) {
			 * if(!rc.onTheMap(myLocation.add(Direction.WEST))) {
			 * eastBreakpoint.add(Direction.EAST, Math.abs(westBreakpoint.x -
			 * myLocation.x)); westBreakpoint = myLocation; } canMove = !canMove; } } } }
			 */
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public void roam() {
		try {
			float spacing = 1.25f;

			System.out.println("Moving to new position");
			if (rc.isCircleOccupiedExceptByThisRobot(myLocation.add(Direction.SOUTH), spacing)
					&& rc.isCircleOccupiedExceptByThisRobot(myLocation.add(Direction.NORTH), spacing)) {
				if (canMove) {
					if (myLocation.x >= eastBreakpoint.x) {
						canMove = !canMove;
						// return;
					}

					if (!tryMove(myLocation.directionTo(eastBreakpoint))) {
						if (!rc.onTheMap(myLocation.add(Direction.EAST))) {
							westBreakpoint.add(Direction.WEST, Math.abs(eastBreakpoint.x - myLocation.x));
							eastBreakpoint = myLocation;
						}
						canMove = !canMove;
					}
				} else {
					if (myLocation.x <= westBreakpoint.x) {
						canMove = !canMove;
						// return;
					}

					if (!tryMove(myLocation.directionTo(westBreakpoint))) {
						if (!rc.onTheMap(myLocation.add(Direction.WEST))) {
							eastBreakpoint.add(Direction.EAST, Math.abs(westBreakpoint.x - myLocation.x));
							westBreakpoint = myLocation;
						}
						canMove = !canMove;
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
			for (TreeInfo tree : rc.senseNearbyTrees(2)) {
				if (tree == null)
					return;
				if (treeToWater == null)
					treeToWater = tree;
				else if (treeToWater.getHealth() > tree.getHealth())
					treeToWater = tree;
			}
			if (treeToWater != null && rc.canWater(treeToWater.ID)) {
				System.out.println("Watering trees");
				rc.water(treeToWater.ID);
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	public boolean findLocation() {
		try {
			for (RobotInfo robot : rc.senseNearbyRobots()) {
				if (robot.getType() == RobotType.GARDENER) {
					if (Math.abs(myLocation.y - robot.getLocation().y) < 4) {
						System.out.println("I'm too close to another Gardener");
						if (myLocation.y - robot.getLocation().y < 0)
							tryMove(Direction.SOUTH);
						else
							tryMove(Direction.NORTH);
						return false;
					}
				}
			}

			/*
			 * if(!rc.onTheMap(myLocation, 3) || ((int) myLocation.y) % 2 == 0) {
			 * if(rc.onTheMap(myLocation.add(Direction.SOUTH), 3)) tryMove(Direction.SOUTH);
			 * else if (rc.onTheMap(myLocation.add(Direction.NORTH), 3))
			 * tryMove(Direction.NORTH); }
			 */
			// else
			return true;
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void trySpawn() {
		try {
			if (state < 2)
				return;

			if (Math.abs(myLocation.x - eastBreakpoint.x) < 1.5f)
				spawnDir = Math.abs(myLocation.directionTo(enemy).degreesBetween(Direction.WEST)) > 90 ? Direction.EAST
						: null;
			else if (Math.abs(myLocation.x - westBreakpoint.x) < 1.5f)
				spawnDir = Math.abs(myLocation.directionTo(enemy).degreesBetween(Direction.WEST)) > 90 ? null
						: Direction.WEST;
			else if (Math.random() > 0.5f)
				spawnDir = Direction.NORTH;
			else
				spawnDir = Direction.SOUTH;

			if (spawnDir == null)
				return;

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

			if (rc.readBroadcast(BroadcastType.SpawnSoldier.getChannel()) > 0)
				if (spawnDir != null && rc.canBuildRobot(RobotType.SOLDIER, spawnDir)) {
					rc.buildRobot(RobotType.SOLDIER, spawnDir);
					rc.broadcast(BroadcastType.SpawnSoldier.getChannel(),
							rc.readBroadcast(BroadcastType.SpawnSoldier.getChannel()) - 1);
					System.out.println("Spawning: Soldier");
				}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}
}
