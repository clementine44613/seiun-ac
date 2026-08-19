/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
 *  net.minecraft.class_310
 *  net.minecraft.class_5375
 *  net.minecraft.class_8710
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.kanieoutis.seiunac.mixin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.kanieoutis.seiunac.SeiunAC;
import net.kanieoutis.seiunac.client.ModChecker;
import net.kanieoutis.seiunac.network.AntiCheatPackets;
import net.minecraft.class_310;
import net.minecraft.class_5375;
import net.minecraft.class_8710;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_5375.class})
public class PackScreenMixin {
    @Unique
    private List<String> SeiunAC$initialPackSnapshot = new ArrayList<String>();

    @Inject(at={@At(value="RETURN")}, method={"method_25426"})
    private void onPackScreenInit(CallbackInfo ci) {
        this.SeiunAC$initialPackSnapshot = ModChecker.getActiveResourcePacks();
    }

    @Inject(at={@At(value="TAIL")}, method={"method_25419"})
    private void onPackScreenApplyAndClose(CallbackInfo ci) {
        class_310 client = class_310.method_1551();
        if (client == null || client.method_1542() || client.method_1562() == null) {
            return;
        }
        List<String> currentPackSnapshot = ModChecker.getActiveResourcePacks();
        if (currentPackSnapshot.isEmpty() && this.SeiunAC$initialPackSnapshot.isEmpty()) {
            return;
        }
        HashSet<String> initialPacks = new HashSet<String>(this.SeiunAC$initialPackSnapshot);
        HashSet<String> currentPacks = new HashSet<String>(currentPackSnapshot);
        if (initialPacks.equals(currentPacks)) {
            return;
        }
        ArrayList<String> added = new ArrayList<String>();
        ArrayList<String> removed = new ArrayList<String>();
        for (String pack : currentPackSnapshot) {
            if (initialPacks.contains(pack)) continue;
            added.add(pack);
        }
        for (String pack : this.SeiunAC$initialPackSnapshot) {
            if (currentPacks.contains(pack)) continue;
            removed.add(pack);
        }
        if (added.isEmpty() && removed.isEmpty()) {
            return;
        }
        AntiCheatPackets.ResourcePackChangePayload payload = new AntiCheatPackets.ResourcePackChangePayload(Instant.now().toString(), added, removed);
        if (ClientPlayNetworking.canSend(AntiCheatPackets.ResourcePackChangePayload.ID)) {
            ClientPlayNetworking.send((class_8710)payload);
            SeiunAC.LOGGER.info("\u2192 Sent resource pack change payload (added: {}, removed: {})", (Object)added.size(), (Object)removed.size());
        } else {
            SeiunAC.LOGGER.debug("Server cannot receive resource pack change payloads");
        }
    }
}
