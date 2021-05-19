package s0571104;

import java.awt.Color;

import java.awt.Point;

import java.awt.geom.Path2D;

import lenz.htw.ai4g.ai.AI;

import lenz.htw.ai4g.ai.DivingAction;

import lenz.htw.ai4g.ai.Info;

import lenz.htw.ai4g.ai.PlayerAction;

public class Rocky extends AI {

	byte pearlNr, goalNr, scanRange, scanIgnore; // byte reicht als Größe
	int score, disX, disY, disTotal, goalRange, time, panicArea, refreshRate; // müssen ints sein
	double direction, pi, a, fleeLimit;
	Path2D[] obstacles;
	Point[] pearls, wonPearls;
	Point player, goal, closestObst, oldPlayerPos;
	boolean initialized, terminalTracking;

	public Rocky(Info info) {
		super(info);
		enlistForTournament(571104, 571394);
		// factor for determining seek/flee balance (<1= more flee, >1 = more seek)
		a = 1;
		// refresh rate for determining direction
		refreshRate = 10;
		// length of obstacle scan
		scanRange = 30;
		// if goal becomes within this reach, ignore obstacles
		// remove when better obstacle algorithm?
		scanIgnore = 15;
		// flee completely if obstacle within this range
		// remove when better obstacle algorithm?
		panicArea = 3;
		// limit for avoiding an obstacle
		// really useful? candidate for removal
		fleeLimit = pi * 0.5;
		// set true to display console info about the execution of the code
		terminalTracking = true;
		// for initializing level information at first update
		initialized = false;
	}

	@Override

	public Color getColor() {
		return Color.ORANGE;
	}

	@Override

	public String getName() {
		return "Rocky der Stein";
	}

	@Override

	public PlayerAction update() {

		// initialize all information only required once after generation of the level
		// = diver sees environment for the first time
		if (!initialized) {
			initialize();
		}

		initialize();
		// get current position of the player
		// = diver knows where he is located
		player.x = info.getX();
		player.y = info.getY();

		// determine the most official goal
		// = diver collects the pearl closest to him, starting with the most right
		determineGoal();

		// calculate current distance between player and goal
		// = diver determines distance to current goal
		disX = Math.abs(goal.x - player.x);
		disY = Math.abs(goal.y - player.y);
		// determine total distance to goal
		disTotal = calcDistance(disX, disY);

		// determin the most efficient direction (based on player, goal & obstacle
		// position
		// = diver searches the quickest way towards his goal without striking obstacles
		determineDirection();

		// if a pearl was taken last round
		// = diver knows which pearl he has already taken
		if (score < info.getScore()) {
			// memorize pearl as taken
			wonPearls[goalNr] = goal;
			// swim up to start with less obstructed point for next pearl
			time = -refreshRate*5;
			direction = pi / 2;
		}

		// update the score
		score = info.getScore();
		// amount of updates executed so far
		time++;

		if (terminalTracking) {
			// display data about positions and distances
			displayGameData();
		}
		return new DivingAction(info.getMaxAcceleration(), (float) (direction));
	}

	public void initialize() {
		// is this more efficient in some way than always loading math? i'm curious
		pi = Math.PI;
		// initialize enumeration of executed player updates to be 0
		time = 0;
		// initialize placeholder points to avoid errors
		goal = player = oldPlayerPos = new Point(0, 0);
		// initialize array for memorizing won pearl positions
		wonPearls = new Point[10];
		// get positions of pearls
		pearls = info.getScene().getPearl();
		// get all obstacle pixel of the rendered level
		obstacles = info.getScene().getObstacles();
		// only initialize once
		initialized = true;
	}

	public void determineGoal() {

		// old method also considered the x value when choosing

		// compare value initialized as smallest possible x value
		// int max = -800;
		// for each pearls in the lvl (0-9)
		// for (pearlNr = 0; pearlNr < pearls.length; pearlNr++) {
		// if checked pearl is more right than last most right one & not yet taken
		// if (pearls[pearlNr].x > max && pearls[pearlNr] != wonPearls[pearlNr]) {
		// last most right pearl is overwritten with the new most right one
		// max = pearls[pearlNr].x;
		// update the goal to be the pearl currently having the highest x value
		// goal = pearls[pearlNr];
		// remember pearl number to mark it as won later
		// goalNr = pearlNr;
		// }
		// }

		// for saving the distance from player to each checked pearl
		int pearlDistance;
		// initialize the compare value to start at maximum possible distance
		int closest = 1600;
		// for each pearl in the lvl (0-9)
		for (pearlNr = 0; pearlNr < pearls.length; pearlNr++) {
			// calclate player distance to the current pearl
			pearlDistance = calcDistance(player.x, player.y, pearls[pearlNr].x, pearls[pearlNr].y);
			// if distance is to compared pearl is smaller than last closest pearl & it has
			// not been taken yet
			if (pearlDistance <= closest && pearls[pearlNr] != wonPearls[pearlNr]) {
				// the pearl with lowest distance becomes new compare value
				closest = pearlDistance;
				// pearl becomes new goal due to lower distance
				goal = pearls[pearlNr];
				// mark this pearl to be able to win it later
				goalNr = pearlNr;
			}
		}
		// start with the chosen range again for the next update
		closest = 1600;
	}

	public void determineDirection() {
		// if time positive (no event happening) and only as often as refreshRate allows
		if (time % refreshRate == 0 && time >= 0) {
			// swim to goal
			direction = a * seek(player, goal);
			// if avoidObstacles return 0, no obstacle was sighted within range
			if (avoidObstacles() == 0)
				// reset factor to avoid inadequate number for direction
				direction /= a;
			// if an obstacle was sighted within the chosen panic range
			else if (avoidObstacles() == 5) {
				panicMode();
			} else {
				// if obstacle is found, add the flee direction from this obstacle
				direction += avoidObstacles();
				// correct direction of value too excessive
				// if (direction < seek(player.x, player.y, closestObst.x, closestObst.y)
				// -fleeLimit)
				// direction = seek(player.x, player.y, closestObst.x, closestObst.y) -
				// fleeLimit;
				// else if (Math.abs(direction) > seek(player.x, player.y, closestObst.x,
				// closestObst.y) + fleeLimit)
				// direction = seek(player.x, player.y, closestObst.x, closestObst.y) +
				// fleeLimit;
			}

		}
		
		/*

		if (time % (refreshRate * 3) == 0) {
			// if player hasn't changed position some time (stuck)
			if (calcDistance(player.x, player.y, oldPlayerPos.x, oldPlayerPos.y) == 0) {
				// flee next updates to get away from there
				time -= refreshRate * 3;
				// if there's an obstacle
				if (avoidObstacles() != 0)
					panicMode();
			}
			// memorize the oldPlayerPosition
			oldPlayerPos.x = player.x;
			oldPlayerPos.y = player.y;
		}*/

		// if direction exceeds limit of pi
		if (direction < -pi)
			direction += (2 * pi);
		else if (direction > pi)
			direction -= (2 * pi);
	}

	public double avoidObstacles() {
		// chosen length of the search ray (see field section)
		byte s = scanRange;
		// direction for fleeing from obstacle
		// initialized as 0 in case no obstacle is found (seek+0);
		double fleeDirection = 0;
		// for memorizing the closest obstacle found in scan
		closestObst = new Point();
		// for memorizing the distance values of currently closest obstacle ((s+s/2))
		int cDis = s;
		// compare value for nearest found obstacle
		int cn = s;
		// for each obstacle in the scene
		for (Path2D o : obstacles) {
			// only if distance to goal is larger than the chosen ignore range (see field
			// section)
			if (disTotal > scanIgnore) {
				// for each obstacle from 1 distance to chosen scan range
				for (int n = 0; n <= s; n++) {
					// search left and right
					for (int x = -1; x <= 1; x++) {
						// search up and down
						for (int y = -1; y <= 1; y++) {
							// check current range in each direction (like a circle)
							if (o.contains(player.x + n * x, player.y + n * y)) {
								// if the found obstacle is closer than last found closest obstacle (or scan
								// range)
								if (n <= cn) {
									// now check more specific for distance;
									// is distance to checked obstacle is smaller than to the last closest
									if (calcDistance(player.x, player.y, player.x + n * x, player.y + n * y) < cDis) {
										// only if in a radius of 90 degrees to each side
										if (seek(player.x, player.y, player.x + n * x, player.y + n * y) > direction
												- pi / 2
												|| seek(player.x, player.y, player.x + n * x,
														player.y + n * y) < direction + pi / 2) {
											// memorize position of currently closest obstacl
											closestObst.x = player.x + n * x;
											closestObst.y = player.y + n * y;
											// mark closeness value as new closest one
											cn = n;
											// memorize distance to the currently closest obstacle
											cDis = calcDistance(player.x, player.y, player.x + n * x, player.y + n * y);
											// marks that (at least 1) obstacle was found (see further down)
											fleeDirection = 10;
										}
									}
								}
							}
						}
					}
				}
			}
		}

		// if at least 1 obstacle was found (as to not screw up direction if there
		// wasn't)
		if (fleeDirection == 10) {
			if (terminalTracking) {
				System.out.println("Closeness to obstacle: " + cDis);
			}
			// take direction for diving directly towards closest obstacle
			fleeDirection = seek(player, closestObst);
			// from there, turn right for 90 degrees divided by closeness (more distance to
			// obstacle = less avoiding)
			if (goal.x < player.x) {
				// flee to the left
				fleeDirection += pi / cn;
			} else {
				// flee to the right
				fleeDirection -= pi / cn;
			}
			// divide by factor
			fleeDirection /= a;
		}

		if (cn <= panicArea) {
			fleeDirection = 5;
		}
		// returns 0 = no obstacle found, only seek
		// returns 5 = panic mode; flee from goal for some time
		// returns other = add fleeing in a curve relative to distance and position of
		// obstacle
		return fleeDirection;
	}

	public void panicMode() {
		if (terminalTracking)
			System.out.println("Panic mode active!");
		if (time >= 0) {
			// in case the player is in the obstacle (even possible?)
			if (calcDistance(player, closestObst) == 0) {
				direction = flee(player, goal);
			} else {
				time = -refreshRate * 3;
				direction = (flee(player, closestObst)) / a * 3 + (seek(player, goal) * a) / 4;
			}
		}
	}

	public void displayGameData() {
		if (terminalTracking == true) {
			// tracking of player position
			System.out.println("Player position: (" + player.x + "/" + player.y + ")");
			// tracking of the currently searched pearl
			System.out.println("Pearl " + (score + 1) + ": (" + goal.x + "/" + goal.y + ")");
			// tracking of the distance to the goal
			System.out.println("Distance: (" + disX + "/" + disY + "), total (average): " + disTotal);
			// tracking of the player direction
			System.out.println("Direction: " + direction + "(Pi) Degrees: " + (int) ((Math.toDegrees(direction))) + "°");
			// tracking of time
			System.out.println("Time: " + time + " (negative if panic mode or just won pearl)");
		}
	}

	public double seek(double x1, double y1, double x2, double y2) {
		return Math.atan2((y2 - y1), (x2 - x1));
	}

	public double seek(Point a, Point b) {
		return Math.atan2((b.y - a.y), (b.x - a.x));
	}

	public double flee(double x1, double y1, double x2, double y2) {
		return Math.atan2((y1 - y2), (x1 - x2));
	}

	public double flee(Point a, Point b) {
		return Math.atan2((a.y - b.y), (a.x - b.x));
	}

	// calculate distance with two sets of x/y-coordinates
	public int calcDistance(int x1, int y1, int x2, int y2) {
		return (Math.abs(x2 - x1) + Math.abs(y2 - y1) / 2);
	}

	// calculate distance with a x & y distance
	public int calcDistance(int disX, int disY) {
		return (disX + disY) / 2;
	}

	// calculate distance between 2 points
	public int calcDistance(Point p1, Point p2) {
		return (Math.abs(p2.x - p1.x) + Math.abs(p2.y - p1.y) / 2);
	}
}
