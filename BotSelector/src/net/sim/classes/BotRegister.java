package net.sim.classes;

import java.util.ArrayList;

/**
 * A model for storing bots in.
 * 
 * @author mrh2
 * 
 */
public class BotRegister {

	private ArrayList<Bot> botList, toAdd, toRemove;
	private int selected;

	public BotRegister() {
		botList = new ArrayList<Bot>();
		toAdd = new ArrayList<Bot>();
		toRemove = new ArrayList<Bot>();
		selected = -1;
	}

	/**
	 * To be called as a new bot is constructed, and never otherwise. Else, the
	 * bot will be duplicated in its updates and movement.
	 * 
	 * @param newBot
	 */
	public void registerBot(Bot newBot) {
		toAdd.add(newBot);
	}

	public void update() {
		try {
			if (toAdd.size() > 0) {
				botList.addAll(toAdd);
				toAdd.clear();
				toAdd.clear();
			}
		} catch (Exception e) {
			System.out.println("toAdd size: " + toAdd.size());
		}
		for (int i = 0; i < botList.size() - 1; i++) {
			for (int j = i + 1; j < botList.size(); j++) {
				if ((Math.pow(botList.get(i).getX() - botList.get(j).getX(), 2) + Math
						.pow(botList.get(i).getY() - botList.get(j).getY(), 2)) < Math
						.pow(botList.get(i).getSize()
								+ botList.get(j).getSize(), 2)) {
					double sizeA = botList.get(i).getSize();
					double sizeB = botList.get(j).getSize();
					if (sizeA > sizeB) {
						toRemove.add(botList.get(j));
						botList.get(i).increaseSize(sizeB);
					} else if (sizeA < sizeB) {
						toRemove.add(botList.get(i));
						botList.get(j).increaseSize(sizeA);
					} else {
						double angle1 = botList.get(i).getTheta();
						double angle2 = botList.get(j).getTheta();
						double collisionAngle = Math.PI
								- ((angle1 + angle2) / 2);
						if (i != selected)
							botList.get(i)
									.setTheta(2 * collisionAngle + angle1);
						if (j != selected)
							botList.get(j)
									.setTheta(2 * collisionAngle + angle2);
					}
				}
			}
		}
		try {
			if (toRemove.size() > 0) {
				botList.removeAll(toRemove);
				toRemove.clear();
			}
		} catch (Exception e) {
			System.out.println("Error removing bots. toRemove size: "
					+ toRemove.size());
		}
		for (Bot bot : botList) {
			bot.update();
			bot.draw();
		}
	}

	public Bot getBot(int p) {
		return botList.get(p);
	}

	public void selectBot(int i) {
		if (i < botList.size()) {
			botList.get(i).select();
			selected = i;
		}

	}

	public void setSelectedMovingForward(boolean b) {
		botList.get(selected).setMovingForward(b);
	}

	public void setSelectedRotatingClockwise(boolean b) {
		botList.get(selected).setRotatingClockwise(b);
	}

	public void setSelectedRotatingAntiClockwise(boolean b) {
		botList.get(selected).setRotatingAntiClockwise(b);
	}

	public void pickupBotNearest(int x, int y) {
		int nearestBotIndex = 0;
		double nearestDistance = 500000;
		for (Bot bot : botList) {
			double currentXDistSquared = Math.pow(bot.getX() - x, 2);
			double currentYDistSquared = Math.pow(bot.getY() - y, 2);
			if (currentXDistSquared + currentYDistSquared < nearestDistance) {
				nearestDistance = currentXDistSquared + currentYDistSquared;
				nearestBotIndex = botList.indexOf(bot);
			}
		}
		if (selected >= 0)
			botList.get(selected).deselect();
		selected = nearestBotIndex;
		botList.get(selected).select();
		botList.get(selected).lock();
		botList.get(selected).setX(x);
		botList.get(selected).setY(y);
	}

	public void release() {
		botList.get(selected).unlock();
	}

	public void dragSelected(int x, int y) {
		botList.get(selected).setX(x);
		botList.get(selected).setY(y);
	}

}
