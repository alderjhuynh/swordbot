package com.aura.swordbot.client.render;

import com.aura.swordbot.client.brain.FightController;
import com.aura.swordbot.client.brain.info.selfStats;
import com.aura.swordbot.client.brain.neurons.Brain;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * HUD overlay that makes the bot's focus obvious to a human onlooker: a status
 * line (fight toggle + brain state), plus a world-projected marker over the
 * locked target (with name / HP / distance) and a clamped edge indicator when
 * the target is off-screen.
 */
public final class TargetHud {

	public static final Identifier ID = Identifier.fromNamespaceAndPath("swordbot", "target_hud");

	private static final int STATUS_ON = 0xFF55FF55;
	private static final int STATUS_OFF = 0xFFFFFFFF;
	private static final int MARKER_FILL = 0x40FF3333;
	private static final int MARKER_EDGE = 0xFFFF2222;
	private static final int LABEL = 0xFFFFDDDD;
	private static final int EDGE = 0xFFFFCC00;

	private TargetHud() {}

	public static void init() {
		HudElementRegistry.addLast(ID, TargetHud::render);
	}

	private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) return;

		int width = graphics.guiWidth();
		int height = graphics.guiHeight();

		boolean on = FightController.enabled;
		graphics.text(client.font, "Fight: " + (on ? "ON" : "OFF"), 4, 4, on ? STATUS_ON : STATUS_OFF);

		String stats = Brain.state + "  cd " + String.format("%.0f%%", selfStats.attackCooldown * 100.0f)
				+ "  " + (selfStats.sprinting ? "sprint" : "walk");
		graphics.text(client.font, stats, 4, 14, 0xFFAAAAAA);

		if (!on) return;
		LivingEntity target = FightController.getTarget();

		drawTarget(graphics, client, target, width, height);
	}

	private static void drawTarget(GuiGraphicsExtractor graphics, Minecraft client, LivingEntity target, int width, int height) {
		if (target == null || !target.isAlive()) return;

		// project the top of the target's hitbox to screen space (camera space, -Z forward)
		Vec3 camPos = client.gameRenderer.mainCamera().position();
		float targetY = (float) target.getBoundingBox().maxY;
		Vector3f rel = new Vector3f(
				(float) (target.position().x - camPos.x),
				targetY - (float) camPos.y,
				(float) (target.position().z - camPos.z)
		);
		rel.rotate(client.gameRenderer.mainCamera().rotation().conjugate());

		// mirror behind-camera points to the front hemisphere so they still project
		if (rel.z >= 0) {
			rel.mul(-1.0f);
		}

		float f = (float) (1.0 / Math.tan(Math.toRadians(client.gameRenderer.mainCamera().getFov()) / 2.0));
		float nx = rel.x / -rel.z * f;
		float ny = rel.y / -rel.z * f;
		int sx = (int) Math.round((nx * 0.5 + 0.5) * width);
		int sy = (int) Math.round((1.0 - (ny * 0.5 + 0.5)) * height);

		int margin = 22;
		if (sx < margin || sx > width - margin || sy < margin || sy > height - margin) {
			drawEdge(graphics, clamp(sx, margin, width - margin), clamp(sy, margin, height - margin), width, height);
		} else {
			drawMarker(graphics, client, sx, sy, target);
		}
	}

	private static void drawMarker(GuiGraphicsExtractor graphics, Minecraft client, int sx, int sy, LivingEntity target) {
		int size = 7;
		int x0 = sx - size;
		int y0 = sy - size;
		int x1 = sx + size;
		int y1 = sy + size;
		int edge = 1;

		graphics.fill(x0, y0, x1, y1, MARKER_FILL);
		graphics.fill(x0, y0, x1, y0 + edge, MARKER_EDGE);
		graphics.fill(x0, y1 - edge, x1, y1, MARKER_EDGE);
		graphics.fill(x0, y0, x0 + edge, y1, MARKER_EDGE);
		graphics.fill(x1 - edge, y0, x1, y1, MARKER_EDGE);

		double dist = Math.sqrt(target.distanceToSqr(client.gameRenderer.mainCamera().position()));
		String label = target.getName().getString()
				+ "  " + String.format("%.1f", target.getHealth()) + "hp"
				+ "  " + String.format("%.1fm", dist);
		graphics.text(client.font, label, x1 + 5, y1 - 4, LABEL);
	}

	private static void drawEdge(GuiGraphicsExtractor graphics, int sx, int sy, int width, int height) {
		int size = 4;
		graphics.fill(sx - size, sy - size, sx + size, sy + size, EDGE);
		graphics.fill(sx - size, sy - size, sx + size, sy - size + 1, 0xFFFFFFFF);
		graphics.fill(sx - size, sy + size - 1, sx + size, sy + size, 0xFFFFFFFF);

		String arrow = sx < width / 2 ? "<-" : "->";
		graphics.text(Minecraft.getInstance().font, arrow, sx - size - 4, sy - 4, EDGE);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}