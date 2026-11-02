package net.nullcoil.nccu.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.nullcoil.nccu.InventoryHelper;
import net.nullcoil.nccu.config.NCControlConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {

	@Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
	private void nccu$onTryToStartFallFlying(CallbackInfoReturnable<Boolean> cir) {
		if (!NCControlConfig.elytraSwap) return;

		Player self = (Player) (Object) this;
		if (!(self instanceof LocalPlayer)) return;

		// Jump is still held after a cancel. Do not equip or start again.
		if (InventoryHelper.stopGlidingCooldown > 0) {
			cir.setReturnValue(false);
			return;
		}

		if (self.onGround() || self.isInWater() || self.isInShallowWater()
				|| self.isInPowderSnow || self.isInLava()) {
			return;
		}

		InventoryHelper.swapChestplateWithBestElytra(Minecraft.getInstance());
	}
}