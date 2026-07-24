package starmaker.utils.json;

import asmodeuscore.AsmodeusCore;
import asmodeuscore.api.dimension.IAdvancedSpace.ClassBody;
import asmodeuscore.api.dimension.IAdvancedSpace.StarColor;
import asmodeuscore.api.dimension.IAdvancedSpace.StarType;
import asmodeuscore.api.dimension.IAdvancedSpace.TypeBody;
import asmodeuscore.core.astronomy.BodiesData;
import asmodeuscore.core.astronomy.BodiesRegistry;
import asmodeuscore.core.astronomy.dimension.world.gen.ACBiome;
import asmodeuscore.core.prefab.celestialbody.ExMoon;
import asmodeuscore.core.prefab.celestialbody.ExPlanet;
import asmodeuscore.core.utils.ACCompatibilityManager;
import asmodeuscore.core.utils.Utils;
import com.google.common.collect.Lists;
import galaxyspace.core.GSItems;
import galaxyspace.systems.SolarSystem.SolarSystemBodies;
import micdoodle8.mods.galacticraft.api.GalacticraftRegistry;
import micdoodle8.mods.galacticraft.api.galaxies.*;
import micdoodle8.mods.galacticraft.api.galaxies.CelestialBody.ScalableDistance;
import micdoodle8.mods.galacticraft.api.recipe.SpaceStationRecipe;
import micdoodle8.mods.galacticraft.api.vector.Vector3;
import micdoodle8.mods.galacticraft.api.world.AtmosphereInfo;
import micdoodle8.mods.galacticraft.api.world.EnumAtmosphericGas;
import micdoodle8.mods.galacticraft.api.world.ITeleportType;
import micdoodle8.mods.galacticraft.api.world.SpaceStationType;
import micdoodle8.mods.galacticraft.core.GCItems;
import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import micdoodle8.mods.galacticraft.core.entities.EntityEvolvedZombie;
import micdoodle8.mods.galacticraft.core.items.ItemBasic;
import micdoodle8.mods.galacticraft.planets.mars.MarsModule;
import micdoodle8.mods.galacticraft.planets.venus.VenusModule;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import starmaker.CoreConfig;
import starmaker.StarMaker;
import starmaker.dimension.TeleportTypeAsteroid;
import starmaker.dimension.WorldProviderAsteroid;
import starmaker.dimension.WorldProviderBody;
import starmaker.dimension.WorldProviderSatellite;
import starmaker.utils.MakerUtils;
import starmaker.utils.data.*;
import starmaker.utils.json.celestialimpl.*;
import starmaker.utils.json.data.*;
import starmaker.world.TeleportTypeBody;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParseFiles {

	public static ParseFiles instance = new ParseFiles();
	private static Map<String, BiomeData> listBiomes = new HashMap<String, BiomeData>();
	private static int dimID = CoreConfig.startIDs;

	private static final int LIMIT_GALAXIES = 10;
	private static final int LIMIT_SYSTEMS = 65;
	private static final int LIMIT_PLANETS = 300;
	private static final int LIMIT_MOONS = 150;
	private static final int LIMIT_ASTEROIDS = 50;
	private static final int LIMIT_SATELLITES = 20;
	private static final int LIMIT_BIOMES = 50;

	private static int count_galaxies = 0;
	private static int count_systems = 0;
	private static int count_planets = 0;
	private static int count_moons = 0;
	private static int count_asteroids = 0;
	private static int count_satellites = 0;

	public void parseBiomes(InputStream file, String name) {

		if(listBiomes.containsKey(name)) {
			StarMaker.LOG.info("Biome: %s already registered. Skipped!", name);
			return;
		}

		Reader reader = new InputStreamReader(file);
		BiomeImpl impl = MakerUtils.gson.fromJson(reader, BiomeImpl.class);

		int water = Utils.getIntColor(impl.getWaterColor().intX(), impl.getWaterColor().intY(),
				impl.getWaterColor().intZ());
		int foliage = Utils.getIntColor(impl.getFoliageColor().intX(), impl.getFoliageColor().intY(),
				impl.getFoliageColor().intZ());
		int grass = Utils.getIntColor(impl.getGrassColor().intX(), impl.getGrassColor().intY(),
				impl.getGrassColor().intZ());

		List<OreGenData> oregen = new ArrayList<OreGenData>();
		for (OreGenImpl data : impl.getOreGenList()) {
			oregen.add(new OreGenData(data.getOreBlock(), data.getReplacedBlock(), data.getBlockCount(), data.getMinY(),
					data.getMaxY(), data.getAmountPerChunk()));
		}


		List<GrassGenData> grassgen = new ArrayList<GrassGenData>();
		if (impl.getGrassGenList() != null)
			for (GrassGenImpl data : impl.getGrassGenList()) {
				if (data != null)
					grassgen.add(new GrassGenData(data.getGrassBlock(), data.getGrassCount(), data.onWater(),
							data.getGroundBlock()));
			}

		LakesGenData lakesgen = null;
		if (impl.getLakesGen() != null)
			lakesgen = new LakesGenData(impl.getLakesGen().getLiquidBlock(), impl.getLakesGen().getQuantity());

		StarMaker.LOG.info("Registered New Biome: %s", name);

		listBiomes.put(name.toLowerCase(),
				new BiomeData(name, impl.getBiomeSize())
						.setData(impl.getPersistance(), impl.getHeight(), impl.getOctaves(), impl.getIntquility())
						.setBlocks(impl.getSurfaceBlock(), impl.getSubsurfaceBlock()).setColors(water, foliage, grass)
						.setOreGenData(oregen).setGrassGenData(grassgen)
						.setLakesGenData(lakesgen).setSpawnLists(impl.getCreatureSpawnList(),
								impl.getMonsterSpawnList(), impl.getWaterCreatureSpawnList())
						.setStructureList(impl.getStructuresList()));

	}

	public void parseSystems(InputStream file, String name) {
		Reader input = new InputStreamReader(file);
		SystemImpl impl = MakerUtils.gson.fromJson(input, SystemImpl.class);

		if (count_systems++ > LIMIT_SYSTEMS) {
			StarMaker.info("Limit systems = " + LIMIT_SYSTEMS);
			return;
		}

		String galaxy = impl.getGalaxy();
		float posX = impl.getPosX();
		float posY = impl.getPosY();

		if (BodiesRegistry.getGalaxy(galaxy) == null) {

			if (count_galaxies++ > LIMIT_GALAXIES) {
				StarMaker.info("Limit galaxies = " + LIMIT_GALAXIES);
				return;
			}

			ResourceLocation icon = new ResourceLocation(CoreConfig.resourceDomain,
					"textures/gui/celestialbodies/galaxy/" + galaxy + ".png");
			BodiesRegistry.registerGalaxy(galaxy, icon);

		}

		StarsDataImpl first_star = impl.getStars().get(0);
		ResourceLocation icon = new ResourceLocation(AsmodeusCore.ASSET_PREFIX,
				"textures/gui/celestialbodies/yellow.png");
		StarType star_class = StarType.values()[first_star.getStarType()];
		StarColor star_color = null;

		if (first_star.getStarColor() >= 0)
			star_color = StarColor.values()[first_star.getStarColor() % 7];

		if (star_color != null)
			icon = new ResourceLocation(AsmodeusCore.ASSET_PREFIX,
					"textures/gui/celestialbodies/" + star_color.name().toLowerCase() + ".png");

		if (star_class == StarType.BLACKHOLE)
			icon = new ResourceLocation(StarMaker.ASSET_PREFIX, "textures/gui/celestialbodies/blackhole.png");

		SolarSystem system = BodiesRegistry.registerSolarSystem(StarMaker.ASSET_PREFIX, name,
				BodiesRegistry.getGalaxy(galaxy), new Vector3(posX, posY, 0.0F), first_star.getName(),
				first_star.getStarSize(), icon);
		GalaxyRegistry.registerSolarSystem(system);
		BodiesData data = new BodiesData(TypeBody.STAR).setStarType(star_class).setStarColor(star_color);
		if (impl.getHabitableZone() != null && impl.getHabitableZone().size() == 2) {
			data.setStarHabitableZone(impl.getHabitableZone().get(0), impl.getHabitableZone().get(1));
		} else {
			switch (star_color) {
				case BLUE:
					data.setStarHabitableZone(1.8F, 0.35F);
					break;
				case BROWN:
					data.setStarHabitableZone(0.3F, 0.05F);
					break;
				case LIGHTBLUE:
					data.setStarHabitableZone(1.5F, 0.3F);
					break;
				case ORANGE:
					data.setStarHabitableZone(0.7F, 0.15F);
					break;
				case RED:
					data.setStarHabitableZone(0.5F, 0.1F);
					break;
				case WHITE:
					data.setStarHabitableZone(2.2F, 0.35F);
					break;
				case YELLOW:
					data.setStarHabitableZone(1.0F, 0.22F);
					break;
				default:
					break;
			}
		}
		BodiesRegistry.registerBodyData(system.getMainStar(), data);

		for (int i = 1; i < impl.getStars().size(); i++) {
			if (i > 4)
				break;
			StarsDataImpl star_data = impl.getStars().get(i);

			star_class = StarType.values()[star_data.getStarType()];
			if (star_data.getStarColor() >= 0)
				star_color = StarColor.values()[star_data.getStarColor() % 7];

			if (star_color != null)
				icon = new ResourceLocation(AsmodeusCore.ASSET_PREFIX,
						"textures/gui/celestialbodies/" + star_color.name().toLowerCase() + ".png");

			float distance = star_data.getDistanceFromCenter() != null ? star_data.getDistanceFromCenter() : 0.3F * i;

			Planet star = BodiesRegistry.registerExPlanet(system, star_data.getName(), StarMaker.ASSET_PREFIX,
					distance);
			star.setRingColorRGB(0.0F, 0.0F, 0.0F);
			star.setBodyIcon(icon);
			BodiesRegistry.setOrbitData(star, star_data.getStarPhase(), star_data.getStarSize(), 1000F);
			GalaxyRegistry.registerPlanet(star);

			data = new BodiesData(TypeBody.STAR).setStarType(star_class).setStarColor(star_color);
			BodiesRegistry.registerBodyData(star, data);
		}

	}

	public void parsePlanets(InputStream file, String name) {

		Reader input = new InputStreamReader(file);
		// Reader reader = new FileReader(file);
		PlanetImpl impl = MakerUtils.gson.fromJson(input, PlanetImpl.class);

		if (!GalaxyRegistry.getRegisteredSolarSystems().containsKey(impl.getParentSystem())
				&& !impl.getParentSystem().equals("sol"))
			return;
		SolarSystem system = null;
		if (impl.getParentSystem().equals("sol")) {
			system = GalacticraftCore.solarSystemSol;
		} else
			system = GalaxyRegistry.getRegisteredSolarSystems().get(impl.getParentSystem());
		String planet_name = name;

		OrbitDataImpl orbitData = impl.getOrbitData();

		Planet planet = BodiesRegistry.registerExPlanet(system, planet_name, CoreConfig.resourceDomain,
				orbitData.getDistanceFromCenter());

		BodiesRegistry.setOrbitData(planet, orbitData.getPhase(), orbitData.getSize(), orbitData.getRelativeTime(),
				orbitData.getEccentricityX(), orbitData.getEccentricityY(), 0.0F, 0.0F);


		int id = -1;

		if (count_planets++ > LIMIT_PLANETS) {
			StarMaker.info("Ignore: " + name + ". Limit planets = " + LIMIT_PLANETS);
			return;
		}

		DimData data = new DimData(planet).setRingOnMapTexture(impl.getRingOnMapTextureName());

		if (!impl.getUnreachable()) {
			BodiesRegistry.setPlanetData(planet, impl.getAtmospherePressure(), impl.getDayLenght(), impl.getGravity(),
					impl.getSolarRadiation());
			BodiesRegistry.setProviderData(planet, WorldProviderBody.class, dimID, impl.getWorldData().getTier(),
					ACBiome.ACSpace);
			planet.setAtmosphere(new AtmosphereInfo(impl.getBreathable(), impl.getPrecipitation(),
					impl.getCorrosiveAtmo(), impl.getTemperature(), impl.getWind(), 0.0F));

			if(impl.getAtmosphereComponents() != null) {
				List<String> atmosphereComponents = impl.getAtmosphereComponents();
				List<EnumAtmosphericGas> gases = Lists.newArrayList();

				for(String component : atmosphereComponents) {
					gases.add(EnumAtmosphericGas.valueOf(component));
				}

				planet.atmosphereComponents(gases);
			}

			Vec3d skyColor = new Vec3d(impl.getSky());
			Vec3d fogColor = new Vec3d(impl.getFog());
			Vec3d cloudColor = impl.getCloud() == null ? null : new Vec3d(impl.getCloud());
			Vec3d lightColor = impl.getLight() == null ? null : new Vec3d(impl.getLight());

			List<BiomeData> biomes = new ArrayList<BiomeData>();
			for (int i = 0; i < impl.getBiomes().size(); i++) {
				if (i >= LIMIT_BIOMES)
					break;

				String biomename = impl.getBiomes().get(i);
				if (listBiomes.containsKey(biomename)) {
					biomes.add(listBiomes.get(biomename));
				}

			}

			WorldDataImpl dataImpl = impl.getWorldData();

			data.setStone(dataImpl.getStoneBlock())
					.setMapSize(dataImpl.getMapSize())
					.setSkyFogColor(skyColor, fogColor).setCloudColor(cloudColor).setLightColor(lightColor)
					.setBrightness(impl.getSunBrightness(), impl.getStarBrightness())
					.setGenCavesRavines(dataImpl.getGenCave(), dataImpl.getGenRavine(), dataImpl.getCrateProb(),
							dataImpl.getWaterBlock())
					.setBiomes(biomes).setSunSize(impl.getSunSize()).setWaterY(dataImpl.getWaterY())
					.setLanderType(dataImpl.getLanderType()).setMeteorFrequency(dataImpl.getMeteorFrequency())
					.setCloudHeight(impl.getCloudHeight()).setTemperatureMod(impl.getTemperatureModificator())
					.setFallDamageModifier(dataImpl.getFallDamageModifier())
					.setTidallyLocked(impl.getTidallyLocked())
					.setSunTexture(impl.getSunTextureName())
					.setRingOnSkyTexture(impl.getRingOnSkyTextureName())
					.setFuelUsageModificator(dataImpl.getFuelUsageModifier())
					.setCloudTexture(impl.getCloudTexture());

			id = getAvailableID();
			regDim(id, data, planet.getWorldProvider(), new TeleportTypeBody());
			StarMaker.LOG.info("Registered" + " new Planet: %s | %s | %s",
					planet.getName(), planet.getWorldProvider(), id);
		} else {
			Vec3d skyColor = new Vec3d(impl.getSky());
			Vec3d fogColor = new Vec3d(impl.getFog());
			data.setSkyFogColor(skyColor, fogColor);

			regUnreachDim(data);

			StarMaker.LOG.info("Registered unreachable new Planet: %s",
					planet.getName());
		}

		BodiesData bodies_data = new BodiesData(TypeBody.PLANET);
		BodiesRegistry.registerBodyData(planet, bodies_data);

	}

	public void parseMoons(InputStream file, String name) {

		Reader reader = new InputStreamReader(file);
		MoonImpl impl = MakerUtils.gson.fromJson(reader, MoonImpl.class);

		Planet planet = null;

		switch (impl.getParentPlanet()) {
			case "venus":
				planet = VenusModule.planetVenus;
				break;
			case "overworld":
				planet = GalacticraftCore.planetOverworld;
				break;
			case "mars":
				planet = MarsModule.planetMars;
				break;
			default:
				planet = GalaxyRegistry.getRegisteredPlanets().get(impl.getParentPlanet());
		}

		if (ACCompatibilityManager.isGalaxySpaceLoaded()) {
			switch (impl.getParentPlanet()) {
				case "mercury":
					planet = SolarSystemBodies.planetMercury;
					break;
				case "venus":
					planet = VenusModule.planetVenus;
					break;
				case "overworld":
					planet = GalacticraftCore.planetOverworld;
					break;
				case "mars":
					planet = MarsModule.planetMars;
					break;
				case "jupiter":
					planet = SolarSystemBodies.planetJupiter;
					break;
				case "saturn":
					planet = SolarSystemBodies.planetSaturn;
					break;
				case "uranus":
					planet = SolarSystemBodies.planetUranus;
					break;
				case "neptune":
					planet = SolarSystemBodies.planetNeptune;
					break;
				default:
					planet = GalaxyRegistry.getRegisteredPlanets().get(impl.getParentPlanet());
			}
		}

		if (planet == null)
			return;

		OrbitDataImpl orbitData = impl.getOrbitData();

		Moon moon = BodiesRegistry.registerExMoon(planet, name, CoreConfig.resourceDomain,
				orbitData.getDistanceFromCenter());
		BodiesRegistry.setOrbitData(moon, orbitData.getPhase(), orbitData.getSize(), orbitData.getRelativeTime());

		int id = -1;
		if (count_moons++ > LIMIT_MOONS) {
			StarMaker.info("Ignore: " + name + ". Limit moons = " + LIMIT_MOONS);
			return;
		}
		DimData data = new DimData(moon).setRingOnMapTexture(impl.getRingOnMapTextureName());

		if (!impl.getUnreachable()) {

			BodiesRegistry.setPlanetData(moon, impl.getAtmospherePressure(), impl.getDayLenght(), impl.getGravity(),
					impl.getSolarRadiation());
			BodiesRegistry.setProviderData(moon, WorldProviderBody.class, dimID, impl.getWorldData().getTier(),
					ACBiome.ACSpace);
			moon.setAtmosphere(new AtmosphereInfo(impl.getBreathable(), impl.getPrecipitation(),
					impl.getCorrosiveAtmo(), impl.getTemperature(), impl.getWind(), 0.0F));

			if(impl.getAtmosphereComponents() != null) {
				List<String> atmosphereComponents = impl.getAtmosphereComponents();
				List<EnumAtmosphericGas> gases = Lists.newArrayList();

				for(String component : atmosphereComponents) {
					gases.add(EnumAtmosphericGas.valueOf(component));
				}

				moon.atmosphereComponents(gases);
			}

			Vec3d skyColor = new Vec3d(impl.getSky());
			Vec3d fogColor = new Vec3d(impl.getFog());
			Vec3d cloudColor = impl.getCloud() == null ? null : new Vec3d(impl.getCloud());
			Vec3d lightColor = impl.getLight() == null ? null : new Vec3d(impl.getLight());

			List<BiomeData> biomes = new ArrayList<BiomeData>();
			for (int i = 0; i < impl.getBiomes().size(); i++) {
				if (i >= LIMIT_BIOMES)
					break;

				String biomename = impl.getBiomes().get(i);
				if (listBiomes.containsKey(biomename)) {
					biomes.add(listBiomes.get(biomename));
				}
			}

			WorldDataImpl dataImpl = impl.getWorldData();

			data.setStone(dataImpl.getStoneBlock())
					.setMapSize(dataImpl.getMapSize())
					.setSkyFogColor(skyColor, fogColor).setCloudColor(cloudColor).setLightColor(lightColor)
					.setBrightness(impl.getSunBrightness(), impl.getStarBrightness())
					.setGenCavesRavines(dataImpl.getGenCave(), dataImpl.getGenRavine(), dataImpl.getCrateProb(),
							dataImpl.getWaterBlock())
					.setBiomes(biomes).setSunSize(impl.getSunSize()).setWaterY(dataImpl.getWaterY())
					.setLanderType(dataImpl.getLanderType()).setMeteorFrequency(dataImpl.getMeteorFrequency())
					.setCloudHeight(impl.getCloudHeight()).setTemperatureMod(impl.getTemperatureModificator())
					.setSunTexture(impl.getSunTextureName()).setPlanetSize(impl.getPlanetSize())
					.setFallDamageModifier(dataImpl.getFallDamageModifier())
					.setTidallyLocked(impl.getTidallyLocked())
					.setRingOnSkyTexture(impl.getRingOnSkyTextureName())
					.setFuelUsageModificator(dataImpl.getFuelUsageModifier())
					.setCloudTexture(impl.getCloudTexture());

			id = getAvailableID();
			regDim(id, data, moon.getWorldProvider(), new TeleportTypeBody());

		} else {
			regUnreachDim(data);
		}

		BodiesData bodies_data = new BodiesData(TypeBody.MOON);
		BodiesRegistry.registerBodyData(moon, bodies_data);
		StarMaker.LOG.info(
				"Registered" + (impl.getUnreachable() ? " unreachable" : "")
						+ " new Moon: %s on Parent Planet: %s | %s | %s",
				moon.getName(), moon.getParentPlanet().getName(), moon.getWorldProvider(), id);

	}

	public void parseAsteroids(InputStream file, String name) {

		Reader reader = new InputStreamReader(file);
		AsteroidImpl impl = MakerUtils.gson.fromJson(reader, AsteroidImpl.class);
		int id = -1;

		if (count_asteroids++ > LIMIT_ASTEROIDS) {
			StarMaker.info("Ignore: " + name + ". Limit asteroids = " + LIMIT_ASTEROIDS);
			return;
		}


		if(impl.getParentPlanet() == null) {
			if (!GalaxyRegistry.getRegisteredSolarSystems().containsKey(impl.getParentSystem())
					&& !impl.getParentSystem().equals("sol"))
				return;

			SolarSystem system = null;
			if (impl.getParentSystem().equals("sol")) {
				system = GalacticraftCore.solarSystemSol;
			} else
				system = GalaxyRegistry.getRegisteredSolarSystems().get(impl.getParentSystem());

			OrbitDataImpl orbitData = impl.getOrbitData();
			ExPlanet asteroid = BodiesRegistry.registerExPlanet(system, name, CoreConfig.resourceDomain,
					orbitData.getDistanceFromCenter());

			BodiesRegistry.setOrbitData(asteroid, orbitData.getPhase(), 1.0F, orbitData.getRelativeTime(),
					orbitData.getEccentricityX(), orbitData.getEccentricityY(), 0.0F, 0.0F);

			asteroid.setRingColorRGB(1.1F, 0.0F, 0.0F);

			DimData data = new DimData(asteroid);

			if (!impl.getUnreachable()) {

				BodiesRegistry.setPlanetData(asteroid, 0F, 0, impl.getGravity(), impl.getSolarRadiation());
				BodiesRegistry.setProviderData(asteroid, WorldProviderAsteroid.class, dimID, impl.getTier(),
						ACBiome.ACSpace);
				asteroid.setAtmosphere(new AtmosphereInfo(false, false, false, impl.getTemperature(), impl.getWind(), 0F));

				asteroid.setRelativeDistanceFromCenter(
						new ScalableDistance(orbitData.getDistanceFromCenter() + orbitData.getSize(),
								orbitData.getDistanceFromCenter() - orbitData.getSize()));

				List<String> oregen = new ArrayList<>();
				for (OreGenImpl ore_gen : impl.getOreGenList())
					oregen.add(ore_gen.getOreBlock());

				data.setSkyFogColor(Vec3d.ZERO, Vec3d.ZERO)
						.setBrightness(impl.getSunBrightness(), impl.getStarBrightness())
						.setGenCavesRavines(false, false, 0, "").setBiomes(null).setSunSize(impl.getSunSize()).setWaterY(0)
						.setLanderType(1).setCloudHeight(0).setTemperatureMod(0)
						.setListAsteroidsOres(oregen).setAsteroidBlocks(impl.getAsteroidBlocks());

				id = getAvailableID();
				regDim(id, data, asteroid.getWorldProvider(), new TeleportTypeAsteroid());

			} else {
				regUnreachDim(data);
			}
			BodiesData bodies_data = new BodiesData(TypeBody.ASTEROID, ClassBody.ASTEROID);
			BodiesRegistry.registerBodyData(asteroid, bodies_data);
			StarMaker.LOG.info(
					"Registered" + (impl.getUnreachable() ? " unreachable" : "")
							+ " new Asteroid: %s on Parent System: %s | %s | %s",
					asteroid.getName(), asteroid.getParentSolarSystem().getName(), asteroid.getWorldProvider(), id);
		} else {


			Planet planet = null;

			switch (impl.getParentPlanet()) {
				case "venus":
					planet = VenusModule.planetVenus;
					break;
				case "overworld":
					planet = GalacticraftCore.planetOverworld;
					break;
				case "mars":
					planet = MarsModule.planetMars;
					break;
				default:
					planet = GalaxyRegistry.getRegisteredPlanets().get(impl.getParentPlanet());
			}

			if (ACCompatibilityManager.isGalaxySpaceLoaded()) {
				switch (impl.getParentPlanet()) {
					case "mercury":
						planet = SolarSystemBodies.planetMercury;
						break;
					case "venus":
						planet = VenusModule.planetVenus;
						break;
					case "overworld":
						planet = GalacticraftCore.planetOverworld;
						break;
					case "mars":
						planet = MarsModule.planetMars;
						break;
					case "jupiter":
						planet = SolarSystemBodies.planetJupiter;
						break;
					case "saturn":
						planet = SolarSystemBodies.planetSaturn;
						break;
					case "uranus":
						planet = SolarSystemBodies.planetUranus;
						break;
					case "neptune":
						planet = SolarSystemBodies.planetNeptune;
						break;
					default:
						planet = GalaxyRegistry.getRegisteredPlanets().get(impl.getParentPlanet());
				}
			}

			if (planet == null)
				return;

			OrbitDataImpl orbitData = impl.getOrbitData();

			ExMoon asteroid = BodiesRegistry.registerExMoon(planet, name, CoreConfig.resourceDomain,
					orbitData.getDistanceFromCenter());

			BodiesRegistry.setOrbitData(asteroid, orbitData.getPhase(), 1.0F, orbitData.getRelativeTime(),
					orbitData.getEccentricityX(), orbitData.getEccentricityY(), 0.0F, 0.0F);
			asteroid.setRingColorRGB(1.1F, 0.0F, 0.0F);
			DimData data = new DimData(asteroid);
			if (!impl.getUnreachable()) {

				BodiesRegistry.setPlanetData(asteroid, 0F, 0, impl.getGravity(), impl.getSolarRadiation());
				BodiesRegistry.setProviderData(asteroid, WorldProviderAsteroid.class, dimID, impl.getTier(),
						ACBiome.ACSpace);
				asteroid.setAtmosphere(new AtmosphereInfo(false, false, false, impl.getTemperature(), impl.getWind(), 0F));

				asteroid.setRelativeDistanceFromCenter(
						new ScalableDistance(orbitData.getDistanceFromCenter() + orbitData.getSize(),
								orbitData.getDistanceFromCenter() - orbitData.getSize()));

				List<String> oregen = new ArrayList<>();
				for (OreGenImpl ore_gen : impl.getOreGenList())
					oregen.add(ore_gen.getOreBlock());



				data.setSkyFogColor(Vec3d.ZERO, Vec3d.ZERO)
						.setBrightness(impl.getSunBrightness(), impl.getStarBrightness())
						.setGenCavesRavines(false, false, 0, "").setBiomes(null).setSunSize(impl.getSunSize()).setWaterY(0)
						.setLanderType(1).setCloudHeight(0).setTemperatureMod(0)
						.setListAsteroidsOres(oregen).setAsteroidBlocks(impl.getAsteroidBlocks());

				id = getAvailableID();
				regDim(id, data, asteroid.getWorldProvider(), new TeleportTypeAsteroid());

			} else {
				regUnreachDim(data);
			}
			BodiesData bodies_data = new BodiesData(TypeBody.ASTEROID, ClassBody.ASTEROID);
			BodiesRegistry.registerBodyData(asteroid, bodies_data);
			StarMaker.LOG.info(
					"Registered" + (impl.getUnreachable() ? " unreachable" : "")
							+ " new Asteroid Ring: %s on Parent Planet: %s | %s | %s",
					asteroid.getName(), asteroid.getParentPlanet().getName(), asteroid.getWorldProvider(), id);
		}

	}

	public void parseSatellites(InputStream file, String name) {

		Reader reader = new InputStreamReader(file);
		SatelliteImpl impl = MakerUtils.gson.fromJson(reader, SatelliteImpl.class);

		Planet planet = null;

		switch (impl.getParentPlanet()) {
			case "venus":
				planet = VenusModule.planetVenus;
				break;
			case "overworld":
				planet = GalacticraftCore.planetOverworld;
				break;
			case "mars":
				planet = MarsModule.planetMars;
				break;
			default:
				planet = GalaxyRegistry.getRegisteredPlanets().get(impl.getParentPlanet());
		}

		if (ACCompatibilityManager.isGalaxySpaceLoaded()) {
			switch (impl.getParentPlanet()) {
				case "mercury":
					planet = SolarSystemBodies.planetMercury;
					break;
				case "venus":
					planet = VenusModule.planetVenus;
					break;
				case "overworld":
					planet = GalacticraftCore.planetOverworld;
					break;
				case "mars":
					planet = MarsModule.planetMars;
					break;
				case "jupiter":
					planet = SolarSystemBodies.planetJupiter;
					break;
				case "saturn":
					planet = SolarSystemBodies.planetSaturn;
					break;
				case "uranus":
					planet = SolarSystemBodies.planetUranus;
					break;
				case "neptune":
					planet = SolarSystemBodies.planetNeptune;
					break;
				default:
					planet = GalaxyRegistry.getRegisteredPlanets().get(impl.getParentPlanet());
			}
		}

		if (planet == null)
			return;

		if (!planet.getReachable())
			return;

		if (count_satellites++ > LIMIT_SATELLITES) {
			StarMaker.info("Ignore: " + name + ". Limit satellites = " + LIMIT_SATELLITES);
			return;
		}

		int id = getAvailableID();
		int id_static = getAvailableID();

		OrbitDataImpl orbitData = impl.getOrbitData();

		Satellite satellite = new Satellite("spacestation." + planet.getTranslationKey().replace("planet.", ""));
		satellite.setParentBody(planet);
		satellite.setRelativeSize(0.2667F);
		satellite.setRelativeDistanceFromCenter(new CelestialBody.ScalableDistance(orbitData.getDistanceFromCenter(), orbitData.getDistanceFromCenter()));
		satellite.setRelativeOrbitTime(orbitData.getRelativeTime());

		satellite.setRingColorRGB(0.0F, 0.4F, 0.9F);
		satellite.setTierRequired(planet.getTierRequirement());
		satellite.setDimensionInfo(id, id_static, WorldProviderSatellite.class);
		satellite.setBodyIcon(new ResourceLocation("galacticraftcore:textures/gui/celestialbodies/space_station.png"));
		satellite.setBiomeInfo(ACBiome.ACSpace);

		Vec3d skyColor = new Vec3d(impl.getSky());
		Vec3d fogColor = new Vec3d(impl.getFog());
		Vec3d cloudColor = impl.getCloud() == null ? null : new Vec3d(impl.getCloud());

		DimData data = new DimData(satellite).setSkyFogColor(skyColor, fogColor).setCloudColor(cloudColor)
				.setBrightness(impl.getSunBrightness(), impl.getStarBrightness()).setSunSize(impl.getSunSize())
				.setCloudHeight(impl.getCloudHeight()).setTemperatureMod(impl.getTemperatureModificator())
				.setSunTexture(impl.getSunTextureName()).setGravity(impl.getGravity())
				.setDayLenght(impl.getDayLenght());

		GalacticraftRegistry.registerDimension(planet.getTranslationKey().replace("planet.", "") + " Space Station",
				"_" + planet.getTranslationKey().replace("planet.", "") + "_orbit", id, WorldProviderSatellite.class,
				false);
		GalacticraftRegistry.registerDimension(planet.getTranslationKey().replace("planet.", "") + " Space Station",
				"_" + planet.getTranslationKey().replace("planet.", "") + "_orbit", id_static,
				WorldProviderSatellite.class, true);
		regDim(id, data, satellite.getWorldProvider(), new TeleportTypeBody(data));

		final HashMap<Object, Integer> spaceStationRequirements = new HashMap<Object, Integer>(6, 1.0F);
		spaceStationRequirements.put("ingotTin", 32);
		spaceStationRequirements.put("ingotCopper", 64);
		spaceStationRequirements.put(new ItemStack(GCItems.basicItem, 1, ItemBasic.WAFER_ADVANCED), 1);
		spaceStationRequirements.put(Items.IRON_INGOT, 24);
		spaceStationRequirements.put(new ItemStack(GSItems.HDP, 1, 0), 10);
		spaceStationRequirements.put(new ItemStack(GSItems.BASIC, 1, 6), 10);
		GalacticraftRegistry.registerSpaceStation(
				new SpaceStationType(id, planet.getDimensionID(), new SpaceStationRecipe(spaceStationRequirements)));

		StarMaker.LOG.info("Registered new Sattelite: %s on Parent Planet: %s | %s", satellite.getName(),
				satellite.getParentPlanet().getName(), id);

	}

	private static void regDim(int dimID, DimData data, Class<? extends WorldProvider> provider,
							   ITeleportType teleport) {
		MakerUtils.bodies.put(dimID, data);
		// StarMaker.bodies.forEach((dimID, data) -> {

		if (!(data.getBody() instanceof Satellite)) {
			data.getBody().setBiomeInfo(ACBiome.ACSpace);
			data.getBody().setDimensionInfo(dimID, provider, true);
			data.getBody().addMobInfo(new Biome.SpawnListEntry(EntityEvolvedZombie.class, 8, 2, 3));
		}

		// BodiesRegistry.registerBodyData(data.getBody(), data.getData());

		if (data.getBody() instanceof Moon)
			GalaxyRegistry.registerMoon((Moon) data.getBody());

		if (data.getBody() instanceof Planet)
			GalaxyRegistry.registerPlanet((Planet) data.getBody());

		if (data.getBody() instanceof Satellite)
			GalaxyRegistry.registerSatellite((Satellite) data.getBody());

		GalacticraftRegistry.registerTeleportType(provider, teleport);

		// });
	}

	private static void regUnreachDim(DimData data) {
		MakerUtils.unreachable_bodies.put(data.getBody(), data);

		if (data.getBody() instanceof Moon)
			GalaxyRegistry.registerMoon((Moon) data.getBody());
		else
			GalaxyRegistry.registerPlanet((Planet) data.getBody());
	}

	public static IBlockState getBlock(String par1) {
		String[] meta = par1.split(":");


		Block blocks = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(meta[0] + ":" + 0));
		if (meta.length > 1)
			blocks = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(meta[0] + ":" + meta[1]));

		if (meta.length > 2)
			return blocks.getStateFromMeta(Integer.parseInt(meta[2]));

		return blocks.getDefaultState();
	}


	private static int getAvailableID() {
		return dimID--;
	}

	public void printResults() {
		StarMaker.LOG.info("Registered Galaxies: %s", count_galaxies);
		StarMaker.LOG.info("Registered Systems: %s", count_systems);
		StarMaker.LOG.info("Registered Planets: %s", count_planets);
		StarMaker.LOG.info("Registered Moons: %s", count_moons);
		StarMaker.LOG.info("Registered Asteroids: %s", count_asteroids);
		StarMaker.LOG.info("Registered Satellites: %s", count_satellites);
		StarMaker.LOG.info("Registered Biomes: %s", listBiomes.size());
		StarMaker.LOG.info("Registered Biomes: %s", listBiomes);
	}

}
