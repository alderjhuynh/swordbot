package com.aura.swordbot.client.mixin;

import com.aura.swordbot.client.brain.act.base.look.SmoothLook;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class RenderLookMixin {

	@Inject(method = "render", at = @At("TAIL"))
	private void swordbot$advanceSmoothLook(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
		SmoothLook.renderFrame(Minecraft.getInstance());
	}
}