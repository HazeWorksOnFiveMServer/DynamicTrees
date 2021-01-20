package com.ferreusveritas.dynamictrees.models.bakedmodels;

import com.ferreusveritas.dynamictrees.blocks.branches.BranchBlock;
import com.ferreusveritas.dynamictrees.client.ModelUtils;
import com.ferreusveritas.dynamictrees.init.DTConfigs;
import com.ferreusveritas.dynamictrees.models.ICustomDamageModel;
import com.ferreusveritas.dynamictrees.util.Connections;
import com.ferreusveritas.dynamictrees.util.CoordUtils;
import com.ferreusveritas.dynamictrees.util.CoordUtils.Surround;
import com.google.common.collect.Maps;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@OnlyIn(Dist.CLIENT)
public class ThickBranchBlockBakedModel extends BasicBranchBlockBakedModel implements ICustomDamageModel {

	protected final ResourceLocation thickRingsResLoc;

	private IBakedModel[] trunksBark = new IBakedModel[16];//The trunk will always feature bark on it's sides
	private IBakedModel[] trunksTopBark = new IBakedModel[16];//The trunk will feature bark on it's top when there's more tree on it's surface
	private IBakedModel[] trunksTopRings = new IBakedModel[16];//The trunk will feature rings on it's top when there's not any tree on it's surface(cut)
	private IBakedModel[] trunksBotRings = new IBakedModel[16];//The trunk will always feature rings on it's bottom surface(or nothing)

	public ThickBranchBlockBakedModel (ResourceLocation modelResLoc, ResourceLocation barkResLoc, ResourceLocation ringsResLoc, ResourceLocation thickRingsResLoc) {
		super(modelResLoc, barkResLoc, ringsResLoc);
		this.thickRingsResLoc = thickRingsResLoc;
	}

	@Override
	public void setupModels() {
		super.setupModels();

		TextureAtlasSprite ringsTexture = ModelUtils.getTexture(this.ringsResLoc);
		TextureAtlasSprite thickRingsTexture = ModelUtils.getTexture(this.thickRingsResLoc);

		for (int i = 0; i < 16; i++) {
			int radius = i + 9;
			trunksBark[i] = bakeTrunkBark(radius, this.barkTexture, true);
			trunksTopBark[i] = bakeTrunkBark(radius, this.barkTexture, false);
			trunksTopRings[i] = bakeTrunkRings(radius, /*DTConfigs.fancyThickRings.get() ? thickRingsTexture : */ringsTexture, Direction.UP);
			trunksBotRings[i] = bakeTrunkRings(radius, /*DTConfigs.fancyThickRings.get() ? thickRingsTexture : */ringsTexture, Direction.DOWN);
		}
	}

	public IBakedModel bakeTrunkBark(int radius, TextureAtlasSprite bark, boolean side) {

		SimpleBakedModel.Builder builder = new SimpleBakedModel.Builder(this.blockModel.customData, ItemOverrideList.EMPTY).setTexture(bark);
		AxisAlignedBB wholeVolume = new AxisAlignedBB(8 - radius, 0, 8 - radius, 8 + radius, 16, 8 + radius);

		final Direction[] run = side ? CoordUtils.HORIZONTALS : new Direction[] { Direction.UP, Direction.DOWN };
		ArrayList<Vector3i> offsets = new ArrayList<>();

		for (Surround dir: Surround.values()) {
			offsets.add(dir.getOffset());//8 surrounding component pieces
		}
		offsets.add(new Vector3i(0, 0, 0));//Center

		for (Direction face: run) {
			final Vector3i dirVector = face.getDirectionVec();

			for (Vector3i offset : offsets) {
				if (face.getAxis() == Axis.Y || new Vector3d(dirVector.getX(), dirVector.getY(), dirVector.getZ()).add(new Vector3d(offset.getX(), offset.getY(), offset.getZ())).lengthSquared() > 2.25) { //This means that the dir and face share a common direction
					Vector3d scaledOffset = new Vector3d(offset.getX() * 16, offset.getY() * 16, offset.getZ() * 16);//Scale the dimensions to match standard minecraft texels
					AxisAlignedBB partBoundary = new AxisAlignedBB(0, 0, 0, 16, 16, 16).offset(scaledOffset).intersect(wholeVolume);

					Vector3f limits[] = ModelUtils.AABBLimits(partBoundary);

					Map<Direction, BlockPartFace> mapFacesIn = Maps.newEnumMap(Direction.class);

					BlockFaceUV uvface = new BlockFaceUV(ModelUtils.modUV(ModelUtils.getUVs(partBoundary, face)), getFaceAngle(Axis.Y, face));
					mapFacesIn.put(face, new BlockPartFace(null, -1, null, uvface));

					BlockPart part = new BlockPart(limits[0], limits[1], mapFacesIn, null, true);
					builder.addFaceQuad(face, ModelUtils.makeBakedQuad(part, part.mapFaces.get(face), bark, face, ModelRotation.X0_Y0, this.modelResLoc));
				}

			}
		}

		return builder.build();
	}

	public IBakedModel bakeTrunkRings(int radius, TextureAtlasSprite ring, Direction face) {
		return bakeTrunkRings(radius, ring, face, DTConfigs.fancyThickRings.get());
	}

	public IBakedModel bakeTrunkRings(int radius, TextureAtlasSprite ring, Direction face, boolean fancy) {
		SimpleBakedModel.Builder builder = new SimpleBakedModel.Builder(this.blockModel.customData, ItemOverrideList.EMPTY).setTexture(ring);
		AxisAlignedBB wholeVolume = new AxisAlignedBB(8 - radius, 0, 8 - radius, 8 + radius, 16, 8 + radius);
		int wholeVolumeWidth = fancy ? 48 : radius * 2;

		ArrayList<Vector3i> offsets = new ArrayList<>();

		for (Surround dir: Surround.values()) {
			offsets.add(dir.getOffset()); // 8 surrounding component pieces
		}
		offsets.add(new Vector3i(0, 0, 0)); // Center

		for (Vector3i offset : offsets) {
			Vector3d scaledOffset = new Vector3d(offset.getX() * 16, offset.getY() * 16, offset.getZ() * 16); // Scale the dimensions to match standard minecraft texels
			AxisAlignedBB partBoundary = new AxisAlignedBB(0, 0, 0, 16, 16, 16).offset(scaledOffset).intersect(wholeVolume);

			Vector3f posFrom = new Vector3f((float) partBoundary.minX, (float) partBoundary.minY, (float) partBoundary.minZ);
			Vector3f posTo = new Vector3f((float) partBoundary.maxX, (float) partBoundary.maxY, (float) partBoundary.maxZ);

			Map<Direction, BlockPartFace> mapFacesIn = Maps.newEnumMap(Direction.class);
			float textureOffsetX = fancy ? (float) (-16f) : (float) wholeVolume.minX;
			float textureOffsetZ = fancy ? (float) (-16f) : (float) wholeVolume.minZ;

			float minX = ((float) ((partBoundary.minX - textureOffsetX) / wholeVolumeWidth)) * 16f;
			float maxX = ((float) ((partBoundary.maxX - textureOffsetX) / wholeVolumeWidth)) * 16f;
			float minZ = ((float) ((partBoundary.minZ - textureOffsetZ) / wholeVolumeWidth)) * 16f;
			float maxZ = ((float) ((partBoundary.maxZ - textureOffsetZ) / wholeVolumeWidth)) * 16f;

			if (face == Direction.DOWN) {
				minZ = ((float) ((partBoundary.maxZ - textureOffsetZ) / wholeVolumeWidth)) * 16f;
				maxZ = ((float) ((partBoundary.minZ - textureOffsetZ) / wholeVolumeWidth)) * 16f;
			}

			float[] uvs = new float[]{ minX, minZ, maxX, maxZ };

			BlockFaceUV uvface = new BlockFaceUV(uvs, getFaceAngle(Axis.Y, face));
			mapFacesIn.put(face, new BlockPartFace(null, -1, null, uvface));

			BlockPart part = new BlockPart(posFrom, posTo, mapFacesIn, null, true);
			builder.addFaceQuad(face, ModelUtils.makeBakedQuad(part, part.mapFaces.get(face), ring, face, ModelRotation.X0_Y0, this.modelResLoc));
		}

		return builder.build();
	}

	@Nonnull
	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random rand, @Nonnull IModelData extraData) {
		if (state == null || side != null) return Collections.emptyList();

		int coreRadius = this.getRadius(state);

		if (coreRadius <= BranchBlock.RADMAX_NORMAL)
			return super.getQuads(state, side, rand, extraData);

		coreRadius = MathHelper.clamp(coreRadius, 9, 24);

		List<BakedQuad> quads = new ArrayList<>(30);

		int[] connections = new int[] {0,0,0,0,0,0};
		if (extraData instanceof Connections){
			connections = ((Connections) extraData).getAllRadii();
		}

		for (Direction face : Direction.values()) {
			quads.addAll(this.trunksBark[coreRadius - 9].getQuads(state, face, rand, extraData));

			if (connections[0] < 1) {
				quads.addAll(this.trunksBotRings[coreRadius - 9].getQuads(state, face, rand, extraData));
			}
			if (connections[1] < 1) {
				quads.addAll(this.trunksTopRings[coreRadius - 9].getQuads(state, face, rand, extraData));
			} else if (connections[1] < coreRadius && face == Direction.UP) {
				quads.addAll(this.trunksTopBark[coreRadius - 9].getQuads(state, face, rand, extraData));
			}
		}

		return quads;
	}

    @Override
    public List<BakedQuad> getCustomDamageQuads(BlockState blockState, Direction side, long randSeed) {
        int coreRadius = getRadius(blockState);
        Random rand = new Random();
        rand.setSeed(randSeed);
        if (coreRadius <= BranchBlock.RADMAX_NORMAL) {
            return super.getQuads(blockState, side, rand);
        }

        coreRadius = MathHelper.clamp(coreRadius, 9, 24);

        List<BakedQuad> quadsList = new LinkedList<>();

        quadsList.addAll(trunksBark[coreRadius - 9].getQuads(blockState, side, rand));
        quadsList.addAll(trunksTopBark[coreRadius - 9].getQuads(blockState, side, rand));

        return quadsList;
    }

}
