package starmaker.dimension.sky;

import asmodeuscore.api.dimension.IAdvancedSpace;
import asmodeuscore.core.astronomy.BodiesData;
import asmodeuscore.core.astronomy.BodiesRegistry;
import asmodeuscore.core.astronomy.gui.screen.NewGuiCelestialSelection;
import asmodeuscore.core.astronomy.sky.SkyProviderBase;
import micdoodle8.mods.galacticraft.api.galaxies.CelestialBody;
import micdoodle8.mods.galacticraft.api.galaxies.GalaxyRegistry;
import micdoodle8.mods.galacticraft.api.galaxies.IChildBody;
import micdoodle8.mods.galacticraft.api.galaxies.Moon;
import micdoodle8.mods.galacticraft.api.galaxies.Planet;
import micdoodle8.mods.galacticraft.api.galaxies.Satellite;
import micdoodle8.mods.galacticraft.api.galaxies.SolarSystem;
import micdoodle8.mods.galacticraft.api.galaxies.Star;
import micdoodle8.mods.galacticraft.api.vector.Vector3;
import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import starmaker.utils.MakerUtils;
import starmaker.utils.data.DimData;

import static starmaker.utils.TextureSizeHelper.getTextureSizeAsFloat;

public class SkyProviderBody extends SkyProviderBase {
  private static final Logger log = LogManager.getLogger(SkyProviderBody.class);
  private final DimData data;
  
  private BodiesData body_data;
  
  public SkyProviderBody(DimData data) {
    this.data = data;
    this.body_data = BodiesRegistry.getData(data.getBody());
  }

  protected void rendererSky(Tessellator tessellator, BufferBuilder buffer, float size, float ticks) {
    //if (this.data.getBody() instanceof Satellite) {
    //  Satellite sat = (Satellite)this.data.getBody();
    //  renderImage(sat.getParentPlanet().getBodyIcon(), 0.0F, 0.0F, 0.0F, 180.0F, 1.0F, 0.4F);
    //}
    if (this.data.getBody() instanceof Moon) {
      Planet parent = ((Moon) this.data.getBody()).getParentPlanet();
      float s = (((Moon) this.data.getBody()).getRelativeDistanceFromCenter()).scaledDistance;
      s = getMaxDistance() - s * 0.5F * this.data.getPlanetSize();
      float speed = ((Moon) this.data.getBody()).getRelativeOrbitTime();
      float x = this.mc.world.getCelestialAngle(ticks) * -360.0F / 10.0F;
      float y = this.mc.world.getCelestialAngle(ticks) * 360.0F + 120.0F;
      renderImage(parent.getBodyIcon(), x, y, 0.0F, s, 1.0F);
      DimData parentData = (DimData) MakerUtils.bodies.get(Integer.valueOf(parent.getDimensionID()));
      GL11.glPushMatrix();
      float f = 0.9F;
      if (parentData == null)
        parentData = (DimData) MakerUtils.unreachable_bodies.get(parent);

      // Jack's Edits
      if (enableRenderPlanet()) {
        ResourceLocation RingTexture = null;
        boolean ringAlt = false;
        boolean ringHaumea = false;
        if (parentData != null && parentData.getRingOnMapTexture() != null) {
          RingTexture = parentData.getRingOnMapTexture();
          if (RingTexture.getResourcePath().contains("_alternative")) {
            ringAlt = true;
          }
        }
        if (Loader.isModLoaded("galaxyspace")) {
          if (parent.getName().equals("saturn")) {
            RingTexture = new ResourceLocation("galaxyspace", "textures/gui/celestialbodies/sol/saturn_rings.png");
            ringAlt = true;
          }
          if (parent.getName().equals("uranus")) {
            RingTexture = new ResourceLocation("galaxyspace", "textures/gui/celestialbodies/sol/uranus_rings.png");
            ringAlt = true;
          }
          if (parent.getName().equals("haumea")) {
            RingTexture = new ResourceLocation("galaxyspace", "textures/gui/celestialbodies/sol/haumea_rings.png");
            ringHaumea = true;
          }
        }
        if (RingTexture != null) {
          float[] ringTextureSize = getTextureSizeAsFloat(RingTexture);
          float[] planetTextureSize = getTextureSizeAsFloat(parent.getBodyIcon());
          float tempSize = 2.669F;
          if (ringAlt) {
            tempSize = ringTextureSize[0] / planetTextureSize[0];
          }
          if (ringHaumea) {
            tempSize = (ringTextureSize[0] / planetTextureSize[0]) / 10.0F;
          }
          renderImage(RingTexture, x, y, 0.0F, tempSize * s, 1.0F);
        }
      }
      // End

      if (parentData != null && parentData.getSkyColor() != null)
        renderAtmo(tessellator, x, y, s - 0.4F, new Vec3d((parentData.getSkyColor()).x / 255.0D * f, (parentData.getSkyColor()).y / 255.0D * f, (parentData.getSkyColor()).z / 255.0D * f));

      GL11.glPopMatrix();
    }
    if (getStarData().getStarType() == IAdvancedSpace.StarType.BLACKHOLE) {
      GlStateManager.depthMask(false);
      GlStateManager.pushMatrix();
      GlStateManager.rotate(getCelestialAngle(getDayLength()) + 40.0F, 0.0F, 0.0F, 1.0F);
      GlStateManager.pushMatrix();
      GlStateManager.translate(0.0F, 120.0F, 0.0F);
      GlStateManager.rotate(180.0F, -1.0F, 1.0F, 0.0F);
      GlStateManager.rotate(getCelestialAngle(getDayLength() / 50L), 0.0F, 1.0F, 0.0F);
      float f10 = size + 40.0F;
      (FMLClientHandler.instance().getClient()).renderEngine.bindTexture(NewGuiCelestialSelection.vortexTexture);
      buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
      buffer.pos(-f10, -100.0D, f10).tex(0.0D, 1.0D).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
      buffer.pos(f10, -100.0D, f10).tex(1.0D, 1.0D).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
      buffer.pos(f10, -100.0D, -f10).tex(1.0D, 0.0D).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
      buffer.pos(-f10, -100.0D, -f10).tex(0.0D, 0.0D).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
      tessellator.draw();
      GlStateManager.popMatrix();
      GlStateManager.popMatrix();
      GlStateManager.pushMatrix();
      GlStateManager.popMatrix();
      GlStateManager.depthMask(true);
      GL11.glClear(256);
      GlStateManager.depthMask(false);
      renderImage(getStar().getBodyIcon(), 0.0F, 0.0F, getCelestialAngle(getDayLength()) + 180.0F, size);
    }
    if (this.data.getRingOnSkyTexture() != null) {
      GlStateManager.pushMatrix();
      GL11.glScalef(0.8F, 0.6F, 0.8F);
      renderImage(this.data.getRingOnSkyTexture(), getCelestialAngle(getDayLength()) - 180.0F, 80.0F, 0.0F, 500.0F);
      GlStateManager.popMatrix();
    }
    if (!(this.data.getBody() instanceof Satellite)) {
      GL11.glPushMatrix();
      GL11.glRotatef(this.mc.world.getCelestialAngle(ticks) * 360.0F, 0.0F, 0.0F, 1.0F);
      GL11.glDisable(3008);
      GL11.glShadeModel(7425);
      OpenGlHelper.glBlendFunc(770, 771, 1, 0);
    }
    int i = 0;
    for (Planet planet : GalaxyRegistry.getPlanetsForSolarSystem(getSolarSystem())) {
      BodiesData data = BodiesRegistry.getData((CelestialBody) planet);
      if (data != null && data.getType() == IAdvancedSpace.TypeBody.STAR) {
        float distance = (planet.getRelativeDistanceFromCenter()).scaledDistance;
        distance *= 40.0F;
        if (planet.getPhaseShift() < 0.0F && planet.getPhaseShift() > Math.PI)
          distance *= -1.0F;
        GL11.glPushMatrix();
        GL11.glEnable(3042);
        GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef((5 - 8 * i), 1.0F, 0.0F, 0.0F);
        GL11.glRotatef((20 - 12 * i) + distance, 0.0F, 0.0F, 1.0F);
        renderSunAura(tessellator, 3.0F + planet.getRelativeSize(), 0.7F, data.getStarColor());
        GL11.glDisable(3042);
        GL11.glPopMatrix();
        renderImage(planet.getBodyIcon(), -90.0F, 185.0F - (8 * i), (-20 + 12 * i) - distance, 1.0F + planet.getRelativeSize());
        i += 2;
      }

      // More Of Jack's Edits
      for (Moon planetMoon : GalaxyRegistry.getMoonsForPlanet(planet)) {
        BodiesData moonData = BodiesRegistry.getData((CelestialBody) planetMoon);
        if (moonData != null && moonData.getType() == IAdvancedSpace.TypeBody.MOON) {
          if (this.data.getBody() instanceof Moon) {
            if (planetMoon.getParentPlanet().equals(((Moon) this.data.getBody()).getParentPlanet()) && planetMoon != this.data.getBody()) {
              float distance = (planetMoon.getRelativeDistanceFromCenter()).scaledDistance;
              distance *= 40.0F;
              if (planetMoon.getPhaseShift() < 0.0F && planetMoon.getPhaseShift() > Math.PI)
                distance *= -1.0F;
              GL11.glPushMatrix();
              GL11.glEnable(3042);
              GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
              GL11.glRotatef((5 - 8 * i), 1.0F, 0.0F, 0.0F);
              GL11.glRotatef((20 - 12 * i) + distance, 0.0F, 0.0F, 1.0F);
              GL11.glDisable(3042);
              GL11.glPopMatrix();
              renderImage(planetMoon.getBodyIcon(), -90.0F, 185.0F - (8 * i), (-20 + 12 * i) - distance, 1.0F + planetMoon.getRelativeSize() / distance);
              i += 2;
            }
          }
        }
        if (moonData != null && data != null && data.getType() == IAdvancedSpace.TypeBody.PLANET) {
          if (this.data.getBody() instanceof Planet) {
            if (planetMoon.getParentPlanet().equals(this.data.getBody()) && moonData.getType() == IAdvancedSpace.TypeBody.MOON) {
              float distance = (planetMoon.getRelativeDistanceFromCenter()).scaledDistance;
              distance *= 40.0F;
              if (planetMoon.getPhaseShift() < 0.0F && planetMoon.getPhaseShift() > Math.PI)
                distance *= -1.0F;
              GL11.glPushMatrix();
              GL11.glEnable(3042);
              GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
              GL11.glRotatef((5 - 8 * i), 1.0F, 0.0F, 0.0F);
              GL11.glRotatef((20 - 12 * i) + distance, 0.0F, 0.0F, 1.0F);
              GL11.glDisable(3042);
              GL11.glPopMatrix();
              renderImage(planetMoon.getBodyIcon(), -90.0F, 185.0F - (8 * i), (-20 + 12 * i) - distance, 1.0F + planetMoon.getRelativeSize() / distance);
              i += 2;
            }
          }
        }
        for (Satellite planetSatellite : GalaxyRegistry.getSatellites()) {
          BodiesData satelliteData = BodiesRegistry.getData((CelestialBody) planetSatellite);
          if (moonData != null && satelliteData != null && satelliteData.getType() == IAdvancedSpace.TypeBody.SATELLITE) {
            if (this.data.getBody() instanceof Satellite) {
              if (planetMoon.getParentPlanet().equals(((Satellite) this.data.getBody()).getParentPlanet()) && moonData.getType() == IAdvancedSpace.TypeBody.MOON) {
                float distance = (planetMoon.getRelativeDistanceFromCenter()).scaledDistance;
                distance *= 40.0F;
                if (planetMoon.getPhaseShift() < 0.0F && planetMoon.getPhaseShift() > Math.PI)
                  distance *= -1.0F;
                GL11.glPushMatrix();
                GL11.glEnable(3042);
                GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
                GL11.glRotatef((5 - 8 * i), 1.0F, 0.0F, 0.0F);
                GL11.glRotatef((20 - 12 * i) + distance, 0.0F, 0.0F, 1.0F);
                GL11.glDisable(3042);
                GL11.glPopMatrix();
                //renderImage(planetMoon.getBodyIcon(), -90.0F, 185.0F - (8 * i), (-20 + 12 * i) - distance, 1.0F + planetMoon.getRelativeSize() / distance);
                // Added This
                GL11.glPushMatrix();
                // Changed This
                renderImage(planetMoon.getBodyIcon(), -90.0F, (this.mc.world.getCelestialAngle(ticks) * 360F) - (8 * i), (-20 + 12 * i) - distance, 1.0F + planetMoon.getRelativeSize() / distance);
                // Moved This
                renderImage(((Satellite)this.data.getBody()).getParentPlanet().getBodyIcon(), 0.0F, 0.0F, 0.0F, 180.0F, 1.0F, 0.4F);
                //rendererUpdateSky();
                GL11.glDisable(3008);
                GL11.glShadeModel(7425);
                OpenGlHelper.glBlendFunc(770, 771, 1, 0);
                i += 2;
              }
            }
          }
        }
      }
    }
    // End
    GL11.glShadeModel(7424);
    GL11.glPopMatrix();
  }
  
  protected boolean enableBaseImages() {
    return true;
  }
  
  protected ModeLight modeLight() {
    BodiesData bd = null;
    if (this.data.getBody() instanceof Planet) {
      Star star = ((Planet)this.data.getBody()).getParentSolarSystem().getMainStar();
      bd = BodiesRegistry.getData((CelestialBody)star);
    } 
    if (this.data.getBody() instanceof IChildBody)
      bd = BodiesRegistry.getData((CelestialBody)((IChildBody)this.data.getBody()).getParentPlanet().getParentSolarSystem().getMainStar()); 
    if (bd != null && bd.getStarType() == IAdvancedSpace.StarType.BLACKHOLE)
      return ModeLight.DEFAULT;
    return ModeLight.DEFAULT;
  }
  
  protected float sunSize() {
    return (this.data.getSunSize() < -1.0F) ? (10.0F - (this.data.getBody().getRelativeDistanceFromCenter()).unScaledDistance) : this.data.getSunSize();
  }
  
  protected boolean enableStar() {
    return true;
  }
  
  protected ResourceLocation sunImage() {
    if (this.data.getSunTexture() != null)
      return this.data.getSunTexture(); 
    return getSolarSystem().getMainStar().getBodyIcon();
  }
  
  protected IAdvancedSpace.StarColor colorSunAura() {
    Star star = null;
    BodiesData bd = null;
    if (this.data.getBody() instanceof Planet)
      star = ((Planet)this.data.getBody()).getParentSolarSystem().getMainStar(); 
    if (this.data.getBody() instanceof IChildBody)
      star = ((IChildBody)this.data.getBody()).getParentPlanet().getParentSolarSystem().getMainStar(); 
    if (star != null)
      bd = BodiesRegistry.getData((CelestialBody)star); 
    if (star == GalacticraftCore.solarSystemSol.getMainStar())
      bd = null; 
    return (bd != null) ? bd.getStarColor() : IAdvancedSpace.StarColor.WHITE;
  }
  
  private SolarSystem getSolarSystem() {
    if (this.data.getBody() instanceof Planet)
      return ((Planet)this.data.getBody()).getParentSolarSystem(); 
    if (this.data.getBody() instanceof IChildBody)
      return ((IChildBody)this.data.getBody()).getParentPlanet().getParentSolarSystem(); 
    return GalacticraftCore.planetOverworld.getParentSolarSystem();
  }
  
  protected Vector3 getAtmosphereColor() {
    return null;
  }
  
  public int expandSizeAura() {
    return (sunSize() < 2.0F) ? -2 : 0;
  }
  
  public boolean enableLargeSunAura() {
    BodiesData bd = null;
    if (this.data.getBody() instanceof Planet) {
      Star star = ((Planet)this.data.getBody()).getParentSolarSystem().getMainStar();
      bd = BodiesRegistry.getData((CelestialBody)star);
    } 
    if (this.data.getBody() instanceof IChildBody)
      bd = BodiesRegistry.getData((CelestialBody)((IChildBody)this.data.getBody()).getParentPlanet().getParentSolarSystem().getMainStar()); 
    if (bd != null && bd.getStarType() == IAdvancedSpace.StarType.BLACKHOLE)
      return false; 
    return true;
  }
  
  public boolean enableSmoothRender() {
    return (sunSize() < 2.0F) ? false : ((this.data.getSkyColor().lengthSquared() == 0.0D));
  }
  
  private float getMaxDistance() {
    float max = -1.0F;
    if (this.data.getBody() instanceof Moon) {
      Planet parent = ((Moon)this.data.getBody()).getParentPlanet();
      for (Moon moon : GalaxyRegistry.getMoonsForPlanet(parent)) {
        if ((moon.getRelativeDistanceFromCenter()).unScaledDistance > max)
          max = (moon.getRelativeDistanceFromCenter()).unScaledDistance; 
      } 
    } 
    return max;
  }
  
  private Star getStar() {
    Star star = null;
    if (this.data.getBody() instanceof Planet)
      star = ((Planet)this.data.getBody()).getParentSolarSystem().getMainStar(); 
    if (this.data.getBody() instanceof IChildBody)
      star = ((IChildBody)this.data.getBody()).getParentPlanet().getParentSolarSystem().getMainStar(); 
    return star;
  }
  
  private BodiesData getStarData() {
    BodiesData bd = BodiesRegistry.getData((CelestialBody)getStar());
    return bd;
  }
  
  public boolean enableRenderPlanet() {
    return (this.body_data.getType() != IAdvancedSpace.TypeBody.ASTEROID);
  }
}
