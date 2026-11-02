package net.nullcoil.nccu.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.nullcoil.nccu.InventoryHelper;
import net.nullcoil.nccu.config.NCControlConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

	@Shadow
	private boolean wasFallFlying;

	@Unique
	private boolean lastJumpPressed = false;

	@Unique
	private boolean lastFallFlying = false;

	@Inject(method = "aiStep", at = @At("HEAD"))
	private void onAiStepHead(CallbackInfo ci) {
		if (!NCControlConfig.elytraSwap) return;

		LocalPlayer self = (LocalPlayer) (Object) this;
		Minecraft client = Minecraft.getInstance();

		InventoryHelper.tickPendingReequip(client);

		boolean flying = self.isFallFlying();
		if (!lastFallFlying && flying) {
			InventoryHelper.glidingStartCooldown = 5;
		}
		lastFallFlying = flying;

		if (InventoryHelper.stopGlidingCooldown > 0) InventoryHelper.stopGlidingCooldown--;
		if (InventoryHelper.glidingStartCooldown > 0) InventoryHelper.glidingStartCooldown--;

		boolean jump = self.input.keyPresses.jump();
		boolean jumpPressed = jump && !lastJumpPressed;

		// Space while gliding: stop, then chestplate.
		if (flying && jumpPressed
				&& InventoryHelper.stopGlidingCooldown == 0
				&& InventoryHelper.glidingStartCooldown == 0) {
			self.stopFallFlying();
			if (!InventoryHelper.swapElytraToBestChestplate(client)) {
				InventoryHelper.cancelGlideByReequip(client);
			}
			InventoryHelper.stopGlidingCooldown = 10;
		}

		// Landed or walking. Skip the few ticks after an equip so a jump off a ledge does not immediately undo it.
		if (self.onGround()
				&& InventoryHelper.glidingStartCooldown == 0
				&& InventoryHelper.stopGlidingCooldown == 0) {
			InventoryHelper.swapElytraToBestChestplate(client);
		}

		lastJumpPressed = jump;
	}

	@Inject(method = "aiStep", at = @At("TAIL"))
	private void nccu$holdToSprint(CallbackInfo ci) {
		if (NCControlConfig.sprintMode != NCControlConfig.SprintMode.HOLD) return;

		LocalPlayer self = (LocalPlayer) (Object) this;
		if (!self.input.keyPresses.sprint() && self.isSprinting()) {
			self.setSprinting(false);
		}
	}
}