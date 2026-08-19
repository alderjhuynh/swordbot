package com.aura.swordbot.client.brain.act.complex;

import com.aura.swordbot.client.brain.act.base.look.SmoothLook;
import com.aura.swordbot.client.brain.act.base.move.MoveDirection;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyUp;
import com.aura.swordbot.client.brain.neurons.Brain;
import com.aura.swordbot.client.brain.neurons.FightConfig;
import com.aura.swordbot.client.util.TickScheduler;
import net.minecraft.client.Minecraft;

public final class TurnBack {

	private static int until = Integer.MIN_VALUE;

	private TurnBack() {}

	/** True while a turn-back burst is still in progress. */
	public static boolean active() {
		return Brain.tickCount < until;
	}

	public static int activeUntil() {
		return until;
	}

	/**
	 * Turn-back movement (§8.5): face away from the target (yaw +180°) so the
	 * escape loop sprints out faster than a plain S-hold — "S on steroids". The
	 * burst ends by snapping the crosshair back onto the target.
	 */
	public static void execute(Minecraft client) {
		if (client.player == null) return;
		SmoothLook.look(client, client.player.getYRot() + 180f, client.player.getXRot(), 0);
		until = Brain.tickCount + FightConfig.TURN_BACK_TICKS;
		TickScheduler.schedule(FightConfig.TURN_BACK_TICKS, () -> {
			MoveKeyUp.execute(client, MoveDirection.FORWARD);
			FaceTarget.instant(client);
		});
	}
}