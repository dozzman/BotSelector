package net.bot.entities;

import net.bot.event.handler.EntityEventHandler;
import static net.bot.util.RandomUtil.rand;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector2f;

public class EntityBot extends Entity {
	
	private static final int MAX_SPAWN_SPEED = 7;
	private static final int MIN_SPAWN_SPEED = 2;
	private static final float SPEED_MULTIPLIER = 1000F; // = 0.007 max, 0.002 min
	private static final float MAX_SPEED = 0.003F;
	
//	private static final int FRAMES_BEFORE_FOOD_DECREMENT = 60;
//	private static final float FOOD_DECREMENT = 0.005F;
	
	private static final float OFFSPRING_PROPORTION = 0.3F;
	private static final float OFFSPRING_MIN_FOOD = 0.2F;
	private static final float OFFSPRING_MAX_FOOD = 50F;
	
	private static final float MAXIMUM_FORCE_DISTANCE = 0.3F;
	private static final float G = 0.0003F;
	
	private Vector2f mResolvedForce;
	
	private boolean isDiseased;
	
	public EntityBot() {
		super();
		setColor(new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));
		setPosition(new Vector2f(rand.nextFloat(), rand.nextFloat()));
		
		// Any neater way to do this?
		float xVel = (float) ((rand.nextInt(MAX_SPAWN_SPEED-MIN_SPAWN_SPEED) + MIN_SPAWN_SPEED)/SPEED_MULTIPLIER);
		float yVel = (float) ((rand.nextInt(MAX_SPAWN_SPEED-MIN_SPAWN_SPEED) + MIN_SPAWN_SPEED)/SPEED_MULTIPLIER);
		if (rand.nextBoolean()) xVel *= -1;
		if (rand.nextBoolean()) yVel *= -1;
		
		setFoodLevel(rand.nextFloat());
		setVelocity(new Vector2f(xVel, yVel));
		setSize(foodToSize(getFoodLevel()));
		mResolvedForce = new Vector2f(0,0);
//		mFramesAlive = 0;
		
		isDiseased = false;
		
	}
	
	public EntityBot(Color color, Vector2f position, Vector2f velocity, float foodLevel) {
		super();
		setColor(color);
		setPosition(position);
		setVelocity(velocity);
		setFoodLevel(foodLevel);
		setSize(foodToSize(foodLevel));
		
		mResolvedForce = new Vector2f(0,0);
		isDiseased = false;
	}

	@Override
	public void update() {
		
		// New position
		Vector2f.add(getPosition(), getVelocity(), getPosition());
		Vector2f.add(getVelocity(), (Vector2f) mResolvedForce.scale((float) (1.0/getSize())), getVelocity());
		// Bounce off walls
		if (getPosition().x + getVelocity().x < 0 || getPosition().x + getVelocity().x > 1) {
			getVelocity().x *= -1;
		}
		if (getPosition().y + getVelocity().y < 0 || getPosition().y + getVelocity().y > 1) {
			getVelocity().y *= -1;
		}
		// Too old?
//		if (++mFramesAlive % FRAMES_BEFORE_FOOD_DECREMENT == 0) {
//			getFoodLevel() -= FOOD_DECREMENT;
//		}
//		if (getFoodLevel() < 0) {
//			mState = State.STARVED;
//		}
		
		// The bigger the bot, the more likely it is to spawn offspring
		if (rand.nextFloat() < chanceOfSpawn(getFoodLevel(), OFFSPRING_MIN_FOOD, OFFSPRING_MAX_FOOD)) {
			spawnClone();
		}
		
		float l;
		if ((l = getVelocity().length()) > MAX_SPEED) {
			getVelocity().set((getVelocity().x/l)*MAX_SPEED, (getVelocity().y/l)*MAX_SPEED);
		}
		
		setSize(foodToSize(getFoodLevel()));
		mResolvedForce = new Vector2f(0,0);
	}


	private float chanceOfSpawn(float currentFoodLevel, float minFoodLevel,
			float maxFoodLevel) {
		return (currentFoodLevel-minFoodLevel)/(maxFoodLevel-minFoodLevel);
	}

	@Override
	public void draw() {
		glPushMatrix();
		
		double angle = 0;
		if (getVelocity().x == 0) {
			angle = getVelocity().y < 0 ? 180 : 0;
		} else if (getVelocity().x > 0) {
			angle = 90 - Math.toDegrees(Math.atan(getVelocity().y / getVelocity().x));
		} else {
			angle = -90 + Math.toDegrees(Math.atan(getVelocity().y / -getVelocity().x));
		}
		
		glTranslatef(getPosition().x, getPosition().y, 0);
		glRotated(angle, 0D, 0D, -1D);
		
		
		float size = getSize();
		glBegin(GL_TRIANGLES);
		glColor3f(getColor().getRed()/256F, getColor().getGreen()/256F, getColor().getBlue()/256F);
		glVertex3f(0, size, 0);
		glVertex3f(size, -size, 0);
		glVertex3f(-size, -size, 0);
		glEnd();
		
		glPopMatrix();
		
	}
	
	public void consume(Entity food) {
		if (food.getState() != State.CONSUMED) {
			food.setState(State.CONSUMED); // Set food to eaten
			setFoodLevel(getFoodLevel() + food.getFoodLevel()); // Eat food
			
			// Update momentum using masses, instead of assuming same mass
			getVelocity().scale(getSize());
			food.getVelocity().scale(food.getSize());
			Vector2f.add(getVelocity(), food.getVelocity(), getVelocity());
			getVelocity().scale((float) (1.0/(getSize() + food.getSize())));
			
		}
	}
	
	private float foodToSize(float food) {
		// y = mx + c
		return (float) (0.01 + food * 0.025);
	}
	
	private void spawnClone() {
		
		// We need colour, position, velocity and food level.
		float offspringFood = getFoodLevel() * OFFSPRING_PROPORTION;
		// Get a new velocity that's 3/4 to 5/4 times the parent velocity
		Vector2f offspringVelocity = new Vector2f(
				getVelocity().x * (rand.nextFloat() * 0.5f + 0.75f),
				getVelocity().y * (rand.nextFloat() * 0.5f + 0.75f)
				);
		Vector2f offspringPosition = new Vector2f(getPosition().x, getPosition().y);
		Color offspringColor = new Color(getColor().getRed(), getColor().getGreen(), getColor().getBlue());
		
		// Alter parent's lost food
		setFoodLevel(getFoodLevel() - offspringFood);
		
		// Now we have to alter the velocity of the parent.
		// We use the equation v1 = u1 + (m2/m1)(u1-v2)
		Vector2f v1 = new Vector2f();
		float m1 = foodToSize(offspringFood);
		float m2 = foodToSize(getFoodLevel());
		
		Vector2f.sub(getVelocity(), offspringVelocity, v1); // u1-v2
		v1.scale(m2/m1); //(m2/m1)(u1-v2)
		Vector2f.add(getVelocity(), v1, getVelocity());// + u1
		
		int skipFrames = 30;
		// Now move the offspring somewhere away from the parent.
		Vector2f.add(offspringPosition, new Vector2f(skipFrames*offspringVelocity.x, skipFrames*offspringVelocity.y), offspringPosition);
		// Note: this may well trap the offspring in a wall.
		
		// Create the offspring.
		EntityBot offspring = new EntityBot(offspringColor, offspringPosition, offspringVelocity, offspringFood);
		EntityEventHandler.botCreated(offspring);
		
	}
	
	/**
	 * Given an entity and a constant G, calculates and adds force acting upon the bot.
	 * Also negates the force if the other entity is larger.
	 * @param entity
	 */
	public void addForce(Entity entity) {
		// Check distance
		Vector2f displacement = new Vector2f(
				entity.getPosition().x - getPosition().x, 
				entity.getPosition().y - getPosition().y);
		float length = displacement.length();
		if (length > MAXIMUM_FORCE_DISTANCE) {
			return;
		}
		
		// Find magnitude of direction vector
		double force = (G * (getSize() * entity.getSize()))/(length*length*length);
		Vector2f resolved = new Vector2f(
				(float)(force * displacement.x), 
				(float)(force * displacement.y));
		
		// Run from larger entity or same species.
		if (entity.getSize() > getSize() || entity.getColor().equals(getColor())) {
			resolved.negate();
		}
		Vector2f.add(mResolvedForce, resolved, mResolvedForce);
	}
	
	public boolean isDiseased() {
		return isDiseased;
	}
	
	public void setDiseased(boolean isDiseased) {
		this.isDiseased = isDiseased;
	}
}
