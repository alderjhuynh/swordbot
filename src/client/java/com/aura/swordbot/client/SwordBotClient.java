package com.aura.swordbot.client;

import com.aura.swordbot.client.brain.FightController;
import com.aura.swordbot.client.brain.info.selfStats;
import com.aura.swordbot.client.brain.info.targetStats;
import com.aura.swordbot.client.brain.info.WorldStats;
import com.aura.swordbot.client.brain.neurons.Brain;
import com.aura.swordbot.client.render.TargetHud;
import com.aura.swordbot.client.telemetry.Telemetry;
import com.aura.swordbot.client.util.TickScheduler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class SwordBotClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		FightController.init();
		TargetHud.init();
		Telemetry.init();

		ClientTickEvents.END_CLIENT_TICK.register(
				client -> TickScheduler.tick()
		);
		ClientTickEvents.END_CLIENT_TICK.register(
				targetStats::tick
		);
		ClientTickEvents.END_CLIENT_TICK.register(
				selfStats::tick
		);
		ClientTickEvents.END_CLIENT_TICK.register(
				WorldStats::tick
		);
		ClientTickEvents.END_CLIENT_TICK.register(
				FightController::tick
		);
		ClientTickEvents.END_CLIENT_TICK.register(
				Brain::tick
		);
	}
}