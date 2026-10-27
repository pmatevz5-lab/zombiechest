package com.example.zombiechest;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;


public class ZombieChestMod implements ModInitializer {
	/** 2% chance per zombie village. */
	public static final double CHANCE = 0.02;
	/** Netherite blocks in the chest. */
	public static final int NETHERITE_BLOCKS = 3;

	@Override
	public void onInitialize() {
		ServerChunkEvents.CHUNK_LOAD.register((world, chunk, newlyGenerated) -> {
			if (!newlyGenerated) return; // only when the chunk is first generated -> chest never respawns
			for (StructureStart start : world.getStructureAccessor()
					.getStructureStarts(chunk.getPos(), s -> true)) {
				tryPlace(world, chunk.getPos(), start);
			}
		});
	}

	private static void tryPlace(ServerWorld world, ChunkPos currentChunk, StructureStart start) {
		if (!start.hasChildren()) return;

		// Only zombie villages: their pool pieces reference ".../zombie/..." templates.
		PoolStructurePiece house = null;
		for (StructurePiece p : start.getChildren()) {
			if (p instanceof PoolStructurePiece pp
					&& pp.getPoolElement().toString().contains("zombie")
					&& pp.getPoolElement().toString().contains("houses")) {
				house = pp;
				break; // deterministic: first zombie house piece
			}
		}
		if (house == null) return;

		// One deterministic 2% roll per village structure (same result no matter which chunk asks).
		Random rng = Random.create(world.getSeed() ^ start.getPos().toLong() * 341873128712L);
		if (rng.nextDouble() >= CHANCE) return;

		var box = house.getBoundingBox();
		int x = box.getCenter().getX();
		int z = box.getCenter().getZ();
		if (!new ChunkPos(new BlockPos(x, 0, z)).equals(currentChunk)) return; // place it only once

		// Find an interior floor: air block sitting on a solid block, scanning up from the bottom.
		for (int y = box.getMinY() + 1; y < box.getMaxY(); y++) {
			BlockPos pos = new BlockPos(x, y, z);
			if (world.getBlockState(pos).isAir()
					&& world.getBlockState(pos.down()).isSolidBlock(world, pos.down())
					&& world.getBlockState(pos.up()).isAir()) {
				world.setBlockState(pos, Blocks.CHEST.getDefaultState());
				if (world.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
					chest.setStack(rng.nextInt(27), new ItemStack(Items.NETHERITE_BLOCK, NETHERITE_BLOCKS));
				}
				return;
			}
		}
	}
}
