package dispenserduel.mixin;

import dispenserduel.DispenserDuelFeature;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Aendert den angezeigten Namen (z. B. im Tooltip) passend zum Fake-Aussehen. */
@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "getName", at = @At("RETURN"), cancellable = true, require = 0)
    private void dispenserduel$swapName(CallbackInfoReturnable<Text> cir) {
        cir.setReturnValue(DispenserDuelFeature.mapName((ItemStack) (Object) this, cir.getReturnValue()));
    }
}
