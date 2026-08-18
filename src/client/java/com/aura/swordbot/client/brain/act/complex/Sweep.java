package com.aura.swordbot.client.brain.act.complex;

import com.aura.swordbot.client.brain.act.base.attack.AttackOnce;
import com.aura.swordbot.client.brain.act.base.move.MoveDirection;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyDown;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyUp;
import com.aura.swordbot.client.util.TickScheduler;
import net.minecraft.client.Minecraft;

public final class Sweep {

	private Sweep() {}

	/** Attack without sprint reset — deliberate sweep. Forces hit-select situations. */
	public static void execute(Minecraft client) {
		if (client.player == null) return;
		FaceTarget.instant(client);
		TickScheduler.schedule(1, () -> {
			MoveKeyDown.execute(client, MoveDirection.FORWARD);
			Attack.commit(client, () -> AttackOnce.execute(client));
		});
	}

	/** Back sweep (S-sweep): hold S and sweep with deflection timing. Crit-trade counter. */
	public static void back(Minecraft client) {
		if (client.player == null) return;
		FaceTarget.instant(client);
		TickScheduler.schedule(1, () -> {
			MoveKeyUp.execute(client, MoveDirection.FORWARD);
			MoveKeyDown.execute(client, MoveDirection.BACK);
			Attack.commit(client, () -> AttackOnce.execute(client));
			MoveKeyUp.execute(client, MoveDirection.BACK);
			MoveKeyDown.execute(client, MoveDirection.FORWARD);
		});
	}
}