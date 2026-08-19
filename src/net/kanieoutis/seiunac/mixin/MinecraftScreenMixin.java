/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_437
 *  net.minecraft.class_5375
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.kanieoutis.seiunac.mixin;

import net.kanieoutis.seiunac.SeiunAC;
import net.kanieoutis.seiunac.client.ClientVerificationState;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_437;
import net.minecraft.class_5375;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_310.class})
public class MinecraftScreenMixin {
    @Inject(method={"method_1507"}, at={@At(value="HEAD")}, cancellable=true)
    private void SeiunAC$blockPackScreen(class_437 screen, CallbackInfo ci) {
        this.SeiunAC$blockPackScreenInternal(screen, ci);
    }

    @Inject(method={"method_29970"}, at={@At(value="HEAD")}, cancellable=true)
    private void SeiunAC$blockPackScreenAndShow(class_437 screen, CallbackInfo ci) {
        this.SeiunAC$blockPackScreenInternal(screen, ci);
    }

    @Unique
    private void SeiunAC$blockPackScreenInternal(class_437 screen, CallbackInfo ci) {
        if (!(screen instanceof class_5375)) {
            return;
        }
        if (!ClientVerificationState.isBlockPackChangeEnabled()) {
            return;
        }
        class_310 client = class_310.method_1551();
        SeiunAC.LOGGER.warn("Player attempted to open the resource pack screen while server-side blocking is enabled");
        if (client.field_1724 != null) {
            client.field_1724.method_7353((class_2561)class_2561.method_43470((String)"\u00a7c\u00a7lSeiun AC\n\n\u00a77Resource pack changes are not allowed on this server!"), false);
        }
        ci.cancel();
    }
}
