/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.ParseResults
 *  net.minecraft.class_2168
 *  net.minecraft.class_2170
 *  net.minecraft.class_3222
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.kanieoutis.seiunac.mixin;

import com.mojang.brigadier.ParseResults;
import java.util.Locale;
import net.kanieoutis.seiunac.server.ServerNetworkHandler;
import net.minecraft.class_2168;
import net.minecraft.class_2170;
import net.minecraft.class_3222;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_2170.class})
public class CommandManagerMixin {
    @Inject(method={"method_9249"}, at={@At(value="TAIL")})
    private void onExecute(ParseResults<class_2168> parseResults, String command, CallbackInfo ci) {
        if (parseResults == null || command == null || command.isBlank()) {
            return;
        }
        class_2168 source = (class_2168)parseResults.getContext().getSource();
        if (source == null || !(source.method_9228() instanceof class_3222)) {
            return;
        }
        String normalized = command.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1).trim();
        }
        if (normalized.isEmpty()) {
            return;
        }
        String commandName = normalized.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        ServerNetworkHandler.notifyCommandDispatch(source.method_9214(), commandName, command.trim());
    }
}
