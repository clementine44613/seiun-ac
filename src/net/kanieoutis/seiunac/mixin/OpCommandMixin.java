/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_11560
 *  net.minecraft.class_2168
 *  net.minecraft.class_3083
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package net.kanieoutis.seiunac.mixin;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.kanieoutis.seiunac.server.ServerNetworkHandler;
import net.minecraft.class_11560;
import net.minecraft.class_2168;
import net.minecraft.class_3083;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_3083.class})
public class OpCommandMixin {
    @Inject(method={"method_13465"}, at={@At(value="TAIL")})
    private static void onOp(class_2168 source, Collection<class_11560> targets, CallbackInfoReturnable<Integer> cir) {
        if (targets == null || targets.isEmpty()) {
            return;
        }
        List<String> targetNames = targets.stream().map(class_11560::comp_4423).collect(Collectors.toList());
        String actorName = source != null ? source.method_9214() : "Unknown";
        ServerNetworkHandler.notifyOperatorChange(actorName, targetNames, true);
    }
}
