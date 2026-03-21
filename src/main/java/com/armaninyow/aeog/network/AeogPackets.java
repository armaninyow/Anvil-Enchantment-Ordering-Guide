package com.armaninyow.aeog.network;

import com.armaninyow.aeog.AnvilEnchantmentOrderingGuide;
import com.armaninyow.aeog.engine.EnchantData;
import com.armaninyow.aeog.engine.MergeInstruction;
import com.armaninyow.aeog.engine.OptimizationEngine;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class AeogPackets {

	// ── C2S: player requests calculation ─────────────────────────────────────

	public record CalcRequestPayload(
		String itemName,
		List<int[]> enchants, // [enchantId, level]
		boolean modeLevels    // true = LEVELS, false = WORK
	) implements CustomPayload {
		public static final Id<CalcRequestPayload> ID =
			new Id<>(Identifier.of(AnvilEnchantmentOrderingGuide.MOD_ID, "calc_request"));

		public static final PacketCodec<RegistryByteBuf, CalcRequestPayload> CODEC = PacketCodec.of(
			(v, buf) -> {
				buf.writeString(v.itemName);
				buf.writeInt(v.enchants.size());
				for (int[] pair : v.enchants) { buf.writeInt(pair[0]); buf.writeInt(pair[1]); }
				buf.writeBoolean(v.modeLevels);
			},
			buf -> {
				String item = buf.readString();
				int count = buf.readInt();
				List<int[]> enchants = new ArrayList<>(count);
				for (int i = 0; i < count; i++) enchants.add(new int[]{buf.readInt(), buf.readInt()});
				return new CalcRequestPayload(item, enchants, buf.readBoolean());
			}
		);

		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	// ── S2C: engine result ───────────────────────────────────────────────────

	public record EngineResultPayload(
		String itemName,
		List<MergeInstruction> instructions
	) implements CustomPayload {
		public static final Id<EngineResultPayload> ID =
			new Id<>(Identifier.of(AnvilEnchantmentOrderingGuide.MOD_ID, "engine_result"));

		public static final PacketCodec<RegistryByteBuf, EngineResultPayload> CODEC = PacketCodec.of(
			(v, buf) -> {
				buf.writeString(v.itemName);
				buf.writeInt(v.instructions.size());
				for (MergeInstruction instr : v.instructions) {
					writeNode(buf, instr.left());
					writeNode(buf, instr.right());
					buf.writeInt(instr.mergeCost());
					buf.writeInt(instr.mergeCostXp());
					buf.writeInt(instr.priorWorkPenalty());
				}
			},
			buf -> {
				String item = buf.readString();
				int count = buf.readInt();
				List<MergeInstruction> instructions = new ArrayList<>(count);
				for (int i = 0; i < count; i++) {
					MergeInstruction.NodeItem left  = readNode(buf);
					MergeInstruction.NodeItem right = readNode(buf);
					int cost = buf.readInt();
					int xp   = buf.readInt();
					int pw   = buf.readInt();
					instructions.add(new MergeInstruction(left, right, cost, xp, pw));
				}
				return new EngineResultPayload(item, instructions);
			}
		);

		private static void writeNode(RegistryByteBuf buf, MergeInstruction.NodeItem node) {
			buf.writeString(node.id());
			buf.writeInt(node.enchants().size());
			for (String[] e : node.enchants()) { buf.writeString(e[0]); buf.writeString(e[1]); }
			buf.writeInt(node.work());
			buf.writeInt(node.level());
		}

		private static MergeInstruction.NodeItem readNode(RegistryByteBuf buf) {
			String id = buf.readString();
			int eCount = buf.readInt();
			List<String[]> enchants = new ArrayList<>(eCount);
			for (int i = 0; i < eCount; i++) enchants.add(new String[]{buf.readString(), buf.readString()});
			int work  = buf.readInt();
			int level = buf.readInt();
			return new MergeInstruction.NodeItem(id, enchants, work, level);
		}

		@Override public Id<? extends CustomPayload> getId() { return ID; }
	}

	// ── Registration ──────────────────────────────────────────────────────────

	private static final OptimizationEngine ENGINE = new OptimizationEngine();

	public static void registerServerPackets() {
		PayloadTypeRegistry.playC2S().register(CalcRequestPayload.ID, CalcRequestPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(EngineResultPayload.ID, EngineResultPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(CalcRequestPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			String itemName     = payload.itemName();
			List<int[]> enchants = payload.enchants();
			OptimizationEngine.Mode mode = payload.modeLevels()
				? OptimizationEngine.Mode.LEVELS
				: OptimizationEngine.Mode.WORK;

			// Run engine on server thread (already on server thread via Fabric)
			List<MergeInstruction> instructions;
			try {
				instructions = ENGINE.process(itemName, enchants, mode);
			} catch (Exception e) {
				AnvilEnchantmentOrderingGuide.LOGGER.error("[AEOG] Engine error: {}", e.getMessage(), e);
				return;
			}

			ServerPlayNetworking.send(player, new EngineResultPayload(itemName, instructions));
		});
	}

	private AeogPackets() {}
}