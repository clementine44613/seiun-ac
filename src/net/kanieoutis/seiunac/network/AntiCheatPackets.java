/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2960
 *  net.minecraft.class_8710
 *  net.minecraft.class_8710$class_9154
 *  net.minecraft.class_9129
 *  net.minecraft.class_9135
 *  net.minecraft.class_9139
 */
package net.kanieoutis.seiunac.network;

import java.util.List;
import net.minecraft.class_2960;
import net.minecraft.class_8710;
import net.minecraft.class_9129;
import net.minecraft.class_9135;
import net.minecraft.class_9139;

public class AntiCheatPackets {

    public record ServerResponsePayload(boolean allowed, String reason) implements class_8710
    {
        public static final class_8710.class_9154<ServerResponsePayload> ID = new class_8710.class_9154(class_2960.method_60655((String)"SeiunAC", (String)"server_response"));
        public static final class_9139<class_9129, ServerResponsePayload> CODEC = class_9139.method_56435((class_9139)class_9135.field_48547, ServerResponsePayload::allowed, (class_9139)class_9135.field_48554, ServerResponsePayload::reason, ServerResponsePayload::new);

        public class_8710.class_9154<? extends class_8710> method_56479() {
            return ID;
        }
    }

    public record ResourcePackChangePayload(String timestamp, List<String> addedPacks, List<String> removedPacks) implements class_8710
    {
        public static final class_8710.class_9154<ResourcePackChangePayload> ID = new class_8710.class_9154(class_2960.method_60655((String)"SeiunAC", (String)"resource_pack_change"));
        public static final class_9139<class_9129, ResourcePackChangePayload> CODEC = class_9139.method_56436((class_9139)class_9135.field_48554, ResourcePackChangePayload::timestamp, (class_9139)class_9135.field_48554.method_56433(class_9135.method_56363()), ResourcePackChangePayload::addedPacks, (class_9139)class_9135.field_48554.method_56433(class_9135.method_56363()), ResourcePackChangePayload::removedPacks, ResourcePackChangePayload::new);

        public class_8710.class_9154<? extends class_8710> method_56479() {
            return ID;
        }
    }

    public record ClientModListPayload(String antiCheatVersion, String minecraftVersion, List<String> modsWithHashes, List<String> resourcePacks) implements class_8710
    {
        public static final class_8710.class_9154<ClientModListPayload> ID = new class_8710.class_9154(class_2960.method_60655((String)"SeiunAC", (String)"mod_list"));
        public static final class_9139<class_9129, ClientModListPayload> CODEC = class_9139.method_56905((class_9139)class_9135.field_48554, ClientModListPayload::antiCheatVersion, (class_9139)class_9135.field_48554, ClientModListPayload::minecraftVersion, (class_9139)class_9135.field_48554.method_56433(class_9135.method_56363()), ClientModListPayload::modsWithHashes, (class_9139)class_9135.field_48554.method_56433(class_9135.method_56363()), ClientModListPayload::resourcePacks, ClientModListPayload::new);

        public class_8710.class_9154<? extends class_8710> method_56479() {
            return ID;
        }
    }

    public record VerificationSettingsPayload(boolean blockPackChange) implements class_8710
    {
        public static final class_8710.class_9154<VerificationSettingsPayload> ID = new class_8710.class_9154(class_2960.method_60655((String)"SeiunAC", (String)"verification_settings"));
        public static final class_9139<class_9129, VerificationSettingsPayload> CODEC = class_9139.method_56434((class_9139)class_9135.field_48547, VerificationSettingsPayload::blockPackChange, VerificationSettingsPayload::new);

        public class_8710.class_9154<? extends class_8710> method_56479() {
            return ID;
        }
    }

    public record RequestModListPayload() implements class_8710
    {
        public static final class_8710.class_9154<RequestModListPayload> ID = new class_8710.class_9154(class_2960.method_60655((String)"SeiunAC", (String)"request_mod_list"));
        public static final class_9139<class_9129, RequestModListPayload> CODEC = class_9139.method_56431((Object)new RequestModListPayload());

        public class_8710.class_9154<? extends class_8710> method_56479() {
            return ID;
        }
    }
}
