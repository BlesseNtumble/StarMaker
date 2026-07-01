package starmaker.dimension;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import asmodeuscore.core.astronomy.dimension.world.worldengine.WE_ChunkProviderSpace;
import asmodeuscore.core.astronomy.dimension.world.worldengine.WE_WorldProviderSpace;
import asmodeuscore.core.astronomy.dimension.world.worldengine.biome.WE_BaseBiome;
import asmodeuscore.core.astronomy.sky.CustomCloudRender;
import asmodeuscore.core.client.entity.particle.ParticleRainCustom;
import asmodeuscore.core.utils.worldengine.WE_Biome;
import asmodeuscore.core.utils.worldengine.WE_ChunkProvider;
import asmodeuscore.core.utils.worldengine.standardcustomgen.WE_BiomeLayer;
import asmodeuscore.core.utils.worldengine.standardcustomgen.WE_CaveGen;
import asmodeuscore.core.utils.worldengine.standardcustomgen.WE_GrassGen;
import asmodeuscore.core.utils.worldengine.standardcustomgen.WE_LakeGen;
import asmodeuscore.core.utils.worldengine.standardcustomgen.WE_OreGen;
import asmodeuscore.core.utils.worldengine.standardcustomgen.WE_RavineGen;
import asmodeuscore.core.utils.worldengine.standardcustomgen.WE_TerrainGenerator;
import micdoodle8.mods.galacticraft.api.galaxies.CelestialBody;
import micdoodle8.mods.galacticraft.api.vector.Vector3;
import micdoodle8.mods.galacticraft.api.world.IWeatherProvider;
import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import micdoodle8.mods.galacticraft.core.client.CloudRenderer;
import micdoodle8.mods.galacticraft.core.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import starmaker.api.ILanderTypeProvider;
import starmaker.dimension.sky.SkyProviderBody;
import starmaker.dimension.sky.WeatherProviderBody;
import starmaker.utils.MakerUtils;
import starmaker.utils.data.BiomeData;
import starmaker.utils.data.DimData;
import starmaker.utils.data.GrassGenData;
import starmaker.utils.data.OreGenData;
import starmaker.utils.json.ParseFiles;
import starmaker.utils.json.data.EntitySpawnImpl;
import starmaker.utils.json.data.StructuresDataImpl;
import starmaker.world.gen.NBTStructureConfiguration;
import starmaker.world.gen.NBTStructureGenerator;

public class WorldProviderBody extends WE_WorldProviderSpace implements IWeatherProvider, ILanderTypeProvider {

	protected DimData getDimData()
	{
		return MakerUtils.bodies.get(this.getDimension());
	}

	@Override
	public CelestialBody getCelestialBody() {
		if(getDimData() != null)
		{
			//StarMaker.debug(StarMaker.bodies.get(this.getDimension()).getBody().getName());
			return getDimData().getBody();
		}

		return GalacticraftCore.moonMoon;
	}

	@Override
	public double getFuelUsageMultiplier() {
		return getDimData().getFuelUsageModificator();
	}

	@Override
	public float getFallDamageModifier() {
		return getDimData().getFallDamageModifier();
	}

	@Override
	public int getDungeonSpacing() {
		return 0;
	}

	@Override
	public double getMeteorFrequency() {

		return this.getDimData().getMeteorFrequency();//(3 - (getSkyColor().x + getSkyColor().y + getSkyColor().z)) * 10;
	}

	@Override
	public ResourceLocation getDungeonChestType() {
		return null;
	}

	@Override
	public List<Block> getSurfaceBlocks() {
		return null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Vector3 getFogColor() {
		float f = 1.0F - this.getStarBrightness(1.0F);
		return new Vector3(getDimData().getFogColor().x / 255.0F * f, getDimData().getFogColor().y / 255.0F * f, getDimData().getFogColor().z / 255.0F * f);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Vector3 getSkyColor() {
		float f = 1.0F - this.getStarBrightness(1.0F);
		return new Vector3(getDimData().getSkyColor().x / 255.0F * f, getDimData().getSkyColor().y / 255.0F * f, getDimData().getSkyColor().z / 255.0F * f);
	}

    @Override
    @SideOnly(Side.CLIENT)
    public float getStarBrightness(float par1)
    {
    	float f = this.world.getCelestialAngle(par1);
        float f1 = 1.0F - (MathHelper.cos(f * ((float)Math.PI * 2F)) * 2.0F + 0.25F);
        f1 = MathHelper.clamp(f1, 0.0F, 1.0F);
        return f1 * f1 * 0.5F + getDimData().getStarBrightness();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public float getSunBrightness(float par1) {
       float f1 = this.world.getCelestialAngle(1.0F);
       float f2 = 1.0F - (MathHelper.cos(f1 * 3.1415927F * 2.0F) * 2.0F + 0.2F);
       f2 = MathHelper.clamp(f2, 0.0F, 1.0F);

       f2 = 1.0F - f2;

       return f2 * getDimData().getSunBrightness();
    }

	@Override
	public boolean hasSunset() {
		return false;
	}

	@Override
	public Class<? extends IChunkGenerator> getChunkProviderClass() {
		return WE_ChunkProviderSpace.class;
	}

	@Override
	public DimensionType getDimensionType() {
		return WorldUtil.getDimensionTypeById(this.getDimension());
	}

	@Override
	public boolean enableAdvancedThermalLevel() {
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IRenderHandler getCloudRenderer() {

		if(getDimData().getCloudColor() == null)
			return new CloudRenderer();

		if(super.getCloudRenderer() == null) {

			float[] f = {1.0F};
			CustomCloudRender cloud = new CustomCloudRender(f) {
				
				@Override
				public ResourceLocation getCloudTexture() {
					if(getDimData().getCloudTexture() != null)
						return getDimData().getCloudTexture();

					return default_clouds;
				}
				
				@Override
				public Vec3d getCloudColor(float renderPartialTicks) {
					float f = this.mc.world.provider.getSunBrightness(1.0F);
					
					return new Vec3d(getDimData().getCloudColor().x * f, getDimData().getCloudColor().y * f, getDimData().getCloudColor().z * f);
				
					//return getDimData().getCloudColor();
				}
	
				@Override
				public float getCloudMovementSpeed(WorldClient world) {
					return 1F;
				}
			};
			this.setCloudRenderer(cloud);
		}
		return super.getCloudRenderer();
		
		
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public float getCloudHeight() {
		
		return  getDimData().getCloudColor() != null ? getDimData().getCloudHeight() : 0.0F;
	}

	 
	@Override
    @SideOnly(Side.CLIENT)
    public IRenderHandler getWeatherRenderer()
    {
        return new WeatherProviderBody(getDimData());
    }
	
	@Override
    @SideOnly(Side.CLIENT)
    public Particle getParticle(WorldClient world, double x, double y, double z)
    {
		if(getDimData().getBody().atmosphere.thermalLevel() < 0.0) return null;
		
        return new ParticleRainCustom(world, x, y + 0.1D, z, 0.0D, 0.0D, 0.0D, EnumParticleTypes.SMOKE_NORMAL.getParticleID(), 1.0F, new Vector3(0F, 0.4F, 1.0F));
    }
	
	@Override
	@SideOnly(Side.CLIENT)	
    public IRenderHandler getSkyRenderer()
    {
    	if (super.getSkyRenderer() == null)
		{
			this.setSkyRenderer(new SkyProviderBody(getDimData()));
		}
    	
		return super.getSkyRenderer();
    }
    

	@Override
	public void onChunkProvider(int cX, int cZ, ChunkPrimer primer) {
	
	}

	@Override
	public void onPopulate(int cX, int cZ) {
	}

	@Override
	public void recreateStructures(Chunk chunkIn, int x, int z) {
	}
	
	@Override
	public WorldSleepResult canSleepAt(net.minecraft.entity.player.EntityPlayer player, BlockPos pos)
    {		
        return (this.canRespawnHere() && this.getDimData().getBody().atmosphere.isBreathable()) ? WorldSleepResult.ALLOW : WorldSleepResult.BED_EXPLODES;
    }

	@Override
	public void genSettings(WE_ChunkProvider cp) {
		cp.createChunkGen_List .clear(); 
		cp.createChunkGen_InXZ_List .clear(); 
		cp.createChunkGen_InXYZ_List.clear(); 
		cp.decorateChunkGen_List .clear(); 
		cp.biomesList.clear();		
		
		((WE_ChunkProviderSpace)cp).worldGenerators.clear();			
		((WE_ChunkProviderSpace)cp).CRATER_PROB = getDimData().getCrateProb();
			
		
		WE_Biome.setBiomeMap(cp, 1.4D, 4, getDimData().getMapSize(), 1.0D);		
			
		WE_TerrainGenerator terrainGenerator = new WE_TerrainGenerator(); 
		terrainGenerator.worldStoneBlock = ParseFiles.getBlock(getDimData().getStoneBlock()); 
		terrainGenerator.worldSeaGen = !getDimData().getWaterBlock().isEmpty();
		
		if(terrainGenerator.worldSeaGen) {
			terrainGenerator.worldSeaGenBlock = ParseFiles.getBlock(getDimData().getWaterBlock());
			terrainGenerator.worldSeaGenMaxY = getDimData().getWaterY();
		}
		cp.createChunkGen_List.add(terrainGenerator);
		
		if(getDimData().getGenCaves()) {
			WE_CaveGen cg = new WE_CaveGen(); 
			cg.replaceBlocksList.clear(); 
			cg.addReplacingBlock(terrainGenerator.worldStoneBlock); 
			cg.lavaMaxY = 15;
			cp.createChunkGen_List.add(cg); 
		}
		
		if(getDimData().getGenRavines()) {
			WE_RavineGen rg = new WE_RavineGen();
			rg.replaceBlocksList.clear();
			rg.addReplacingBlock(terrainGenerator.worldStoneBlock);
			rg.lavaBlock = Blocks.LAVA.getDefaultState();
			rg.lavaMaxY = 15;		
			cp.createChunkGen_List.add(rg);
		}
			
		if(getDimData().getBiomes() != null)
			for(BiomeData biome : getDimData().getBiomes()) {
	
				WE_BiomeLayer layer = new WE_BiomeLayer();		
				layer.add(ParseFiles.getBlock(biome.getSubsurfaceBlock()), terrainGenerator.worldStoneBlock, -256, 0, -4, -2, true);
				layer.add(ParseFiles.getBlock(biome.getSurfaceBlock()), ParseFiles.getBlock(biome.getSubsurfaceBlock()), -256, 0, -256, 0, false);
				//layer.add(Blocks.BEDROCK.getDefaultState(), 0, 0, 1, 2, true);
			
				WE_Biome b = new WE_BaseBiome(biome.getBiomeSize(), biome.getPersistance(), biome.getOctaves(), biome.getHeight(), biome.getIntquility(), layer) {
					
					@Override
					public void decorateBiome(World world, Random rand, int x, int z)
					{
					}
					
				}.setSize(280.0D, 1.5D).setColors(biome.getGrassColor(), biome.getWaterColor(), biome.getFoliageColor());
				
				if(!biome.getCreatureSpawnList().isEmpty()) {
					for(EntitySpawnImpl entities : biome.getCreatureSpawnList())
					{
						EntityEntry entry = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(entities.getEntity()));
						//FIXME: ADD LOG
						if(entry == null) continue;
						Class<? extends Entity> entityClass = entry.getEntityClass();
						b.getSpawnableList(EnumCreatureType.CREATURE).add(new Biome.SpawnListEntry((Class<? extends EntityLiving>) entityClass, entities.getWeight(), entities.getMinCount(), entities.getMaxCount()));
					}
				}
				
				if(!biome.getMonsterSpawnList().isEmpty()) {
					for(EntitySpawnImpl entities : biome.getMonsterSpawnList())
					{
						EntityEntry entry = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(entities.getEntity()));
						//FIXME: ADD LOG
						if(entry == null) continue;
						Class<? extends Entity> entityClass = entry.getEntityClass();
						b.getSpawnableList(EnumCreatureType.MONSTER).add(new Biome.SpawnListEntry((Class<? extends EntityLiving>) entityClass, entities.getWeight(), entities.getMinCount(), entities.getMaxCount()));
					}
				}
				
				if(!biome.getWaterCreatureSpawnList().isEmpty()) {
					for(EntitySpawnImpl entities : biome.getWaterCreatureSpawnList())
					{
						EntityEntry entry = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(entities.getEntity()));
						//FIXME: ADD LOG
						if(entry == null) continue;
						Class<? extends Entity> entityClass = entry.getEntityClass();
						b.getSpawnableList(EnumCreatureType.WATER_CREATURE).add(new Biome.SpawnListEntry((Class<? extends EntityLiving>) entityClass, entities.getWeight(), entities.getMinCount(), entities.getMaxCount()));
					}
				}
				
				if(!biome.getOreGenData().isEmpty()) {
					WE_OreGen standardOres = new WE_OreGen();
				
					for(OreGenData oregen : biome.getOreGenData())
					{
						standardOres.add(ParseFiles.getBlock(oregen.getOre()), ParseFiles.getBlock(oregen.getReplaced()), oregen.getBlockCount(), oregen.getMinY(), oregen.getMaxY(), oregen.getAmountPerChunk());
					}				
					b.decorateChunkGen_List.add(standardOres);
				}
				
				if(biome.getLakesGenData() != null) {
					WE_LakeGen lakes = new WE_LakeGen();
					lakes.lakeBlock = ParseFiles.getBlock(biome.getLakesGenData().getLiquidBlock());
					lakes.iceGen = false;
					lakes.chunksForLake = biome.getLakesGenData().getQuantity();
					b.decorateChunkGen_List.add(lakes);				
				}

				if(!biome.getStructureList().isEmpty()) {

					for(StructuresDataImpl data : biome.getStructureList()) {
						NBTStructureConfiguration config = new NBTStructureConfiguration(data);
						b.decorateChunkGen_List.add(new NBTStructureGenerator(config));
					}
				}

				if(!biome.getGrassGenData().isEmpty()) {
					
					WE_GrassGen grassGen = new WE_GrassGen();
					for(GrassGenData data : biome.getGrassGenData())
						grassGen.add(ParseFiles.getBlock(data.getGrass()), data.getBlockCount(), data.onWater(), ParseFiles.getBlock(data.getGround()));
					
					b.decorateChunkGen_List.add(grassGen);
				}
					
				WE_Biome.addBiomeToGeneration(cp, b);
							
			}
	}

	@Override
	public void weatherSounds(int j, Minecraft mc, World world, BlockPos blockpos, double xx, double yy, double zz,	Random random) {
		if(getDimData().getBody().atmosphere.thermalLevel() < 0.0) return;
		
		if ((int) yy >= blockpos.getY() + 1 && world.getPrecipitationHeight(blockpos).getY() > blockpos.getY()) {
			mc.world.playSound(xx, yy, zz, SoundEvents.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, 0.8F, 0.6F + random.nextFloat() * 0.2F, false);
		} else {
			mc.world.playSound(xx, yy, zz, SoundEvents.WEATHER_RAIN, SoundCategory.WEATHER, 0.8F, 0.8F + random.nextFloat() * 0.06F + random.nextFloat() * 0.06F, false);
		}
	}

	@Override
	public int getSoundInterval(float rainStrength) {
		if(getDimData().getBody().atmosphere.thermalLevel() < 0.0) return 1;
		
		int result = 80 - (int)(rainStrength * 88F);
        return result > 0 ? result : 1;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void getLightmapColors(float partialTicks, float sunBrightness, float skyLight, float blockLight, float[] colors) 
	{
		EntityPlayer player = FMLClientHandler.instance().getClientPlayerEntity();
		
		if (player != null)
		{
			int phase = this.getMoonPhase(this.getWorldTime());
			
			if(getDimData().getLightColor() != null && sunBrightness > 0.2f && !this.world.isRaining() && skyLight > 0)
			{
				float skyRed = colors[0];
				float skyGreen = colors[1];
				float skyBlue = colors[2];

				float modR = (float) getDimData().getLightColor().x / 255F;
				float modG = (float) getDimData().getLightColor().y / 255F;
				float modB = (float) getDimData().getLightColor().z / 255F;


				skyRed = skyRed * (1.0f - sunBrightness) + (skyRed * modR) * sunBrightness;

				skyGreen = skyGreen * (1.0f - sunBrightness) + (skyGreen * modG) * sunBrightness;

				skyBlue = skyBlue * (1.0f - sunBrightness) + (skyBlue * modB) * sunBrightness;

				colors[0] = skyRed;
				colors[1] = skyGreen;
				colors[2] = skyBlue;
			}				
		}
	}

	@Override
	public int getLanderType() {		
		return getDimData().getLanderType();
	}

	@Override
	public boolean isColorWorld() { return true; }

	@Override
	protected float getThermalValueMod() {
		return getDimData().getTemperatureMod();
	}

	@Override
	public float calculateCelestialAngle(long par1, float par3) {
		if(getDimData().getTidallyLocked())
			return ((float)super.getDayLength() / 24000F);

		return super.calculateCelestialAngle(par1, par3);
	}

	@Override
	public long getDayLength() {
		if(getDimData().getTidallyLocked())
			return 0L;

		return super.getDayLength();
	}
}
