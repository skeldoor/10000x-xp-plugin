package com.xpbooster;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import java.util.*;

@Slf4j
@PluginDescriptor(
	name = "Xpbooster"
)
public class Xpbooster extends Plugin
{
	@Inject
	private Client client;

	private static final String TOTAL_LEVEL_TEXT_PREFIX = "Total level:<br>";

	HashMap<Skill, Integer> realXpTracker = new HashMap<>();
	HashMap<Skill, Integer> fakeXpTracker = new HashMap<>();

	void initialiseSkills(){
		System.out.println("Initialising skills");
		for (Skill skill : Skill.values()){
			if (skill != Skill.OVERALL){
				fakeXpTracker.put(skill, client.getSkillExperience(skill));
				realXpTracker.put(skill, client.getSkillExperience(skill));
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		GameState state = event.getGameState();
		if (state == GameState.LOGGED_IN && fakeXpTracker.size() == 0) {
			initialiseSkills();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged) {
		final Skill skill = statChanged.getSkill();
		final int currentXp = statChanged.getXp();

		if (skill == Skill.OVERALL) return;

		int realpreviousxp = realXpTracker.get(skill);
		int fakepreviousxp = fakeXpTracker.get(skill);

		int realcurrentxp = currentXp;
		int realXpGained = realcurrentxp - realpreviousxp;
		int fakecurrentxp;

		//For first startup when stats are changed from 0 to your current stat, couldn't be bothered to figure out a
		//smarter (read less lazy) way of detecting this.
		if (realXpGained > 1000){
			fakecurrentxp = currentXp;
		} else {
			//normal operation
			fakecurrentxp = fakepreviousxp + (realXpGained * 10000);
		}

		realXpTracker.put(skill, realcurrentxp);
		fakeXpTracker.put(skill, fakecurrentxp);
	}

	@Subscribe
	public void onBeforeRender(BeforeRender tick)
	{
		System.out.println("real attack level: " + Experience.getLevelForXp(realXpTracker.get(Skill.ATTACK)));
		System.out.println("fake attack level: " + Experience.getLevelForXp(fakeXpTracker.get(Skill.ATTACK)));
		//Hope you enjoy this indentation
		for (int i = 7995410; i < 7995417; i++){
			Widget xp = client.getWidget(i);
			if (xp != null) {
				if (xp.getChildren() != null) {
					for (Widget child : xp.getChildren()){
						if (child != null) {
							if (child.getText() != null) {
								if (!child.getText().isEmpty())
									if (!child.getText().endsWith("0000")) {
										child.setText(child.getText() + "0000");
									 	xp.setOriginalWidth(xp.getOriginalWidth() + 30);
										//Surely one of these will revalidate it right?
										xp.revalidate();
										child.revalidate();
										xp.getChildren()[1].revalidate();
									}
								}
							}
						}
					}
				}
			}

		for (Skill skill : Skill.values()) {
			if (skill != Skill.OVERALL) {
				updateSkillLevel(skill);
			}
		}
	}

	private void updateSkillLevel(Skill skill)
	{
		int childId;
		switch (skill)
		{
			case ATTACK:
				childId = 1;
				break;
			case STRENGTH:
				childId = 2;
				break;
			case DEFENCE:
				childId = 3;
				break;
			case RANGED:
				childId = 4;
				break;
			case PRAYER:
				childId = 5;
				break;
			case MAGIC:
				childId = 6;
				break;
			case RUNECRAFT:
				childId = 7;
				break;
			case CONSTRUCTION:
				childId = 8;
				break;
			case HITPOINTS:
				childId = 9;
				break;
			case AGILITY:
				childId = 10;
				break;
			case HERBLORE:
				childId = 11;
				break;
			case THIEVING:
				childId = 12;
				break;
			case CRAFTING:
				childId = 13;
				break;
			case FLETCHING:
				childId = 14;
				break;
			case SLAYER:
				childId = 15;
				break;
			case HUNTER:
				childId = 16;
				break;
			case MINING:
				childId = 17;
				break;
			case SMITHING:
				childId = 18;
				break;
			case FISHING:
				childId = 19;
				break;
			case COOKING:
				childId = 20;
				break;
			case FIREMAKING:
				childId = 21;
				break;
			case WOODCUTTING:
				childId = 22;
				break;
			case FARMING:
				childId = 23;
				break;
			default:
				return;
		}
		//Snippet stolen from https://github.com/XrioBtw/effective-level
		Widget skillWidget = client.getWidget(WidgetID.SKILLS_GROUP_ID, childId);
		if (skillWidget == null)
		{
			return;
		}

		Widget[] skillWidgetComponents = skillWidget.getDynamicChildren();
		if (skillWidgetComponents.length >= 4)
		{
			int effectiveLevel = Experience.getLevelForXp(fakeXpTracker.get(skill));
			skillWidgetComponents[3].setText("" + effectiveLevel);
			skillWidgetComponents[4].setText("" + effectiveLevel);
		}
	}

	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent e)
	{
		//Snippet stolen from the Virtual Levels Runelite plugin
		final String eventName = e.getEventName();
		final int[] intStack = client.getIntStack();
		final int intStackSize = client.getIntStackSize();
		final String[] stringStack = client.getStringStack();
		final int stringStackSize = client.getStringStackSize();

		switch (eventName)
		{
			case "skillTabBaseLevel":
				final int skillId = intStack[intStackSize - 2];
				final Skill skill = Skill.values()[skillId];
				final int exp = fakeXpTracker.get(skill);

				// alter the local variable containing the level to show
				intStack[intStackSize - 1] = Experience.getLevelForXp(exp);
				break;
			case "skillTabMaxLevel":
				// alter max level constant
				intStack[intStackSize - 1] = Experience.MAX_VIRT_LEVEL;
				break;
			case "skillTabTotalLevel":
				int level = 0;

				for (Skill s : Skill.values()) {
					if (s == Skill.OVERALL) {
						continue;
					}

					level += Experience.getLevelForXp(fakeXpTracker.get(s));
				}

				stringStack[stringStackSize - 1] = TOTAL_LEVEL_TEXT_PREFIX + level;
				break;
		}
	}
}

