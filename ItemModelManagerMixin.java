package dispenserduel.mixin;

import dispenserduel.DispenserDuelFeature;
import net.minecraft.client.render.item.ItemModelManager;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Alle Items (GUI, Hotbar, Hand, Boden) laufen durch diese Methode, hier wird nur das Aussehen getauscht. */
@Mixin(ItemModelManager.class)
public class ItemModelManagerMixin {

    @ModifyVariable(method = "clearAndUpdate", at = @At("HEAD"), argsOnly = true, require = 0)
    private ItemStack dispenserduel$swapLook(ItemStack stack) {
        return DispenserDuelFeature.mapStack(stack);
    }
}
