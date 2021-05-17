package s0571104;

import java.awt.Color;

import java.awt.Point;

import java.awt.geom.Path2D;

import lenz.htw.ai4g.ai.AI;

import lenz.htw.ai4g.ai.DivingAction;

import lenz.htw.ai4g.ai.Info;

import lenz.htw.ai4g.ai.PlayerAction;

public class Schnelli extends AI {

	Path2D[] obstacles;
	Point[] pearls, wonPearls;
	Point goal;
	int playerX, playerY, disX, disY, disTotal;
	double direction;
	double pi = Math.PI;
	int pearlNr, goalNr, score, time;

	public Schnelli(Info info) {
		super(info);
		enlistForTournament(571104, 571394);
		wonPearls = new Point[10];
		time = 0;
	}

	@Override

	public Color getColor() {
		return Color.MAGENTA;
	}

	@Override

	public String getName() {
		return "Schnelli";
	}

	@Override

	public PlayerAction update() {
		if (score != info.getScore()) // falls letzte Runde eine Perle gesammelt wurde
			wonPearls[goalNr] = goal; // Perle als bereits genommen merken

		// geladene Szenen-Infos
		pearls = info.getScene().getPearl(); // Startpositionen zufällig
		obstacles = info.getScene().getObstacles(); // Path2D des Meeresbodens
		score = info.getScore(); // Tracking des Scores

		// Perle mit größtem X-Wert (also immer die rechteste) ermitteln
		int max = -800; // niedrigstmöglicher Wert für X
		goal = new Point(0, 0);
		for (pearlNr = 0; pearlNr < pearls.length; pearlNr++) { // für alle Perlen im Level
			// falls die geprüfte Perle weiter rechts als die letzte ist & noch nicht
			// genommen
			if (pearls[pearlNr].x > max && pearls[pearlNr] != wonPearls[pearlNr]) {
				max = pearls[pearlNr].x; // neuer Richtwert = das bisher größte X
				goal = pearls[pearlNr]; // neues Ziel: Perle mit bisher größtem X (am Rechtesten)
				goalNr = pearlNr; // Merke Nummer der aktuellen Perle zum späteren "Vergessen"
			}
		}

		// Positionen von Taucher & verfolger Perle + jeweilige Distanz
		playerX = info.getX();
		playerY = info.getY();
		// wir hatten für die Suchalgorithmen zu anfangs mit disX & disY gearbeit
		disX = Math.abs(goal.x - playerX);
		disY = Math.abs(goal.y - playerY);
		disTotal = disX + disY;
		// disTotal = (int) Math.sqrt((disX * disX) + (disY * disY)); // Zieldiagonale

		if (time > 0 && time % 10 == 0) // refresh rate der Richtungsorientierung
		{
			// in Richtung Ziel
			direction = seek(playerX, playerY, goal.x, goal.y);
			// außer ein obstacle ist in sicht
			avoidObstacles();
		}

		// Tracking der Taucher-Position
		// System.out.println("Player: (" + playerX + "/" + playerY + ")");
		// Tracking der aktuell gesuchten Perle
		// System.out.println("Pearl " + (score + 1) + ": (" + goal.x + "/" + goal.y +
		// *")");
		// Tracking der Distanz System.out.println("Distance: (" + disX + "/" + disY +
		// "), insgesamt: " + disTotal); // Tracking der Richtung
		// System.out.println("Richtung: " + direction + "(Pi) Grad: "
		// +(int)((Math.toDegrees(direction))) + "°");
		time++;
		return new DivingAction(info.getMaxAcceleration(), (float) (direction));
	}

	public double seek(double x1, double y1, double x2, double y2) {
		return Math.atan2((y2 - y1), (x2 - x1));
	}

	public double flee(double x1, double y1, double x2, double y2) {
		return Math.atan2((y1 - y2), (x1 - x2));
	}

	public void avoidObstacles() {

		double k = 1; // Faktor (Start 1, da 1x seek)
		int t = 10; // Scan-Bereich
		int m; // im Loop:
		for (Path2D o : obstacles) { // für alle obstacles

			if (disTotal > t * 5) { // Toleranzbereich, falls nahe am Ziel
				for (int n = -t; n <= t; n++) {
					m = Math.abs(n); // absoluter Abstand (vgl. mit bspw. -5 x)
					for (int r = -1; r <= +1; r++) {
						if (o.contains(playerX + r, playerY + r)) {
							direction += 10*flee(playerX, playerY, playerX + r, playerY - r);
							k+=10;
							time = -15;
						} else if (o.contains(playerX + n, playerY + n)) // von unten links nach oben rechts
						{
							direction += (t / m) * flee(playerX, playerY, playerX + n, playerY + n);
							// t durch m erzeugt stärkeres flee, desto näher das obstacle ist
							k += (t / m); // zu Gewichtung hinzufügen
						}

						if (o.contains(playerX + n, playerY - n))// von oben links nach unten rechts
						{
							direction += (t / m) * flee(playerX, playerY, playerX + n, playerY - n);
							k += (t / m);
						}

						if (o.contains(playerX + n, playerY)) // von links nach rechts
						{
							direction += (t / m) * flee(playerX, playerY, playerX + n, playerY);
							k += (t / m);
						}

						if (o.contains(playerX, playerY + n)) // von unten nach oben
						{
							direction += (t / m) * flee(playerX, playerY, playerX, playerY + n);
							k += (t / m);

						}
					}

					direction /= k; //
					k = 1; // Faktor zurücksetzen
					// obstacles[0].getPathIterator(null); // für interessierte und angehende profis
				}
			}

		}
	}
}
