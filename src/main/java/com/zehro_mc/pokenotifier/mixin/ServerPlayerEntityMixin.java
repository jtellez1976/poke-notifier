package com.zehro_mc.pokenotifier.mixin;

import com.zehro_mc.pokenotifier.util.PlayerRankUtil;
import com.zehro_mc.pokenotifier.util.PlayerRankManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void addPrestigePrefixToDisplayName(CallbackInfoReturnable<Text> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        // Nos aseguramos de que solo aplicamos esto a jugadores en el servidor.
        if (player instanceof ServerPlayerEntity) {
            Text originalName = cir.getReturnValue();
            int rank = PlayerRankManager.getRank(player.getUuid());
            cir.setReturnValue(PlayerRankUtil.getDecoratedPlayerName(rank, originalName));
        }
    }
}