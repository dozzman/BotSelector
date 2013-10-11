package net.bot;

import static net.bot.util.SimRegisterConstants.FOOD_SPECKS;
import static net.bot.util.SimRegisterConstants.INITIAL_BOT_ENTITIES;

import java.util.ArrayList;
import java.util.List;

import net.bot.entities.AbstractEntityBot;
import net.bot.entities.Entity;
import net.bot.entities.Entity.State;
import net.bot.entities.EntityBot;
import net.bot.entities.EntityDiseasedBot;
import net.bot.entities.EntityFoodSpeck;
import net.bot.event.handler.DisplayEventHandler;
import net.bot.event.handler.EntityEventHandler;
import net.bot.event.listener.IDisplayEventListener;
import net.bot.event.listener.IEntityEventListener;
import net.bot.input.KeyboardInput;

import org.lwjgl.util.vector.Vector2f;

public class MainModel {
	
	private List<AbstractEntityBot> mBotEntityList, mBotsToAdd, mBotsToRemove;
	private List<EntityFoodSpeck> mFoodEntityList, mFoodToAdd, mFoodToRemove;
	
	public MainModel() {
		
		DisplayEventHandler.addListener(new IDisplayEventListener() {
			@Override
			public void onUpdate(double delta) {
				updateEntities(delta);
				drawEntities();
			}
		});
		
		// 
		
		mBotEntityList = new ArrayList<AbstractEntityBot>();
		for (int i = 0; i < INITIAL_BOT_ENTITIES; i++) {
			mBotEntityList.add(new EntityBot());
		}
		
		mFoodEntityList = new ArrayList<EntityFoodSpeck>();
		for (int i = 0; i < FOOD_SPECKS; i++) {
			mFoodEntityList.add(new EntityFoodSpeck());
		}
		
		mBotsToAdd = new ArrayList<AbstractEntityBot>();
		mBotsToRemove = new ArrayList<AbstractEntityBot>();

		mFoodToAdd = new ArrayList<EntityFoodSpeck>();		
		mFoodToRemove = new ArrayList<EntityFoodSpeck>();
			EntityEventHandler.addListener(new IEntityEventListener() {
			@Override
			public void onFoodDestroyed(EntityFoodSpeck speck) {
				mFoodEntityList.add(speck);
			}
			@Override
			public void onFoodCreated(EntityFoodSpeck speck) {
				mFoodEntityList.add(speck);
			}
			@Override
			public void onBotDestroyed(AbstractEntityBot bot) {
				mBotEntityList.add(bot);
			}
			@Override
			public void onBotCreated(AbstractEntityBot bot) {
				mBotEntityList.add(bot);
			}
		});
		
/*		DisplayEventHandler.addListener(new IDisplayEventListener() {
			@Override
			public void onUpdate(double delta) {
				// Complete our add/remove operations
				
				// Food
				// Special operation when removing, in that we need to replace lost food
				for (int i = 0; i < mFoodEntityToRemove.size(); i++) {
					mFoodEntityList.remove(mFoodEntityToRemove.get(i));
					EntityEventHandler.foodCreated(new EntityFoodSpeck());
				}
				mFoodEntityToRemove.clear();
				mFoodEntityList.addAll(mFoodEntityToAdd);
				mFoodEntityToAdd.clear();
				
				// Bots
//				mBotEntityList.removeAll(mBotEntityToRemove);
				for (EntityBot bot : mBotEntityToRemove) {
					mBotEntityList.remove(bot);
				}
				mBotEntityToRemove.clear();
				mBotEntityList.addAll(mBotEntityToAdd);
				mBotEntityToAdd.clear();
			}
		}); */
	}
	
	public void updateEntities(double delta) {
		
		// Check for age in food specks
		for (EntityFoodSpeck speck : mFoodEntityList) { 
			speck.update();
		}
		
		// Sort out collisions
		for (int i = 0; i < mBotEntityList.size(); i++) {
			AbstractEntityBot bot = mBotEntityList.get(i);
			bot.update();
			
			// TODO remove after disease testing
			if (!bot.isDiseased()) {
				EntityDiseasedBot newBot = new EntityDiseasedBot(bot);
				mBotsToAdd.add(newBot);
				mBotsToRemove.add(bot);
			}
			for (int j = 0; j < mBotEntityList.size(); j++) {
				if (j > i) {
					collideOrConsume(bot, mBotEntityList.get(j));
				}
				if (j != i) {
					// Add forces for acceleration
					bot.addForce(mBotEntityList.get(j));
				}
			}
			for (EntityFoodSpeck speck : mFoodEntityList) {
				// Check for collision here
				collideOrConsume(bot, speck);
				bot.addForce(speck);
			}
		}
		
		for (AbstractEntityBot bot : mBotEntityList) {
			if (bot.getState() != State.ALIVE) {
				mBotsToRemove.add(bot);
			}
		}
		mBotEntityList.removeAll(mBotsToRemove);
		mBotsToRemove.clear();
		mBotEntityList.addAll(mBotsToAdd);
		mBotsToAdd.clear();
		
		for (EntityFoodSpeck food : mFoodEntityList) {
			if (food.getState() == State.CONSUMED) {
				mFoodToRemove.add(food);
				mFoodToAdd.add(new EntityFoodSpeck());
			}
		}
		mFoodEntityList.removeAll(mFoodToRemove);
		mFoodToRemove.clear();
		mFoodEntityList.addAll(mFoodToAdd);
		mFoodToAdd.clear();
		
	}
	
	private void collideOrConsume(AbstractEntityBot bot, Entity entity) {
		Vector2f compare = new Vector2f();
		Vector2f.sub(bot.getPosition(), entity.getPosition(), compare);
		if (compare.length() <= bot.getSize() + entity.getSize()) {
			// Collision!!
			if (bot.getSize() == entity.getSize()) {
				// Same size or same colour, so bounce off
				Vector2f newBot = new Vector2f();
				Vector2f newEntity = new Vector2f();
				float massA = bot.getSize(), massB = entity.getSize();
				
				Vector2f velA = new Vector2f(bot.getVelocity().x, bot.getVelocity().y);
				Vector2f velB = new Vector2f(entity.getVelocity().x, entity.getVelocity().y);
				
				velA.scale((float)((massA - massB)/(massA+massB)));
				velB.scale((float)((massB * 2)/(massA+massB)));
				Vector2f.add(velA, velB, newBot);
				
				velA = new Vector2f(bot.getVelocity().x, bot.getVelocity().y);
				velB = new Vector2f(entity.getVelocity().x, entity.getVelocity().y);
				
				velB.scale((float)((massB - massA)/(massA+massB)));
				velA.scale((float)((massA * 2)/(massA+massB)));
				Vector2f.add(velA, velB, newEntity);
				
				bot.setVelocity(newBot);
				entity.setVelocity(newEntity);
				
			} else if (bot.getColor().equals(entity.getColor())) {
				return;
			} else if (bot.getSize() < entity.getSize()) {
				entity.consume(bot);
			} else if (bot.getSize() > entity.getSize()) {
				bot.consume(entity);
			}
//			else if (bot.getSize() < entity.getSize()) {
//				entity.consume(bot);
//			} else if (bot.getColor().equals(entity.getColor())) {
//				// Do nothing :-)
//				return;
//			} else {
//				bot.consume(entity);
//			}
		}
	}

	public void drawEntities() {
		for (AbstractEntityBot bot : mBotEntityList) {
			bot.draw();
		}
		for (EntityFoodSpeck speck : mFoodEntityList) {
			speck.draw();
		}
	}
	
	public static void main(String[] args) {
		new MainModel();
		new KeyboardInput();
		MainDisplay display = new MainDisplay();
		display.run();
	}

}