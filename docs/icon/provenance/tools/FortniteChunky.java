import java.io.File;
import java.util.*;
import se.llbit.chunky.block.*;
import se.llbit.chunky.block.minecraft.Air;
import se.llbit.chunky.resources.Texture;
import se.llbit.nbt.Tag;
import se.llbit.resources.ImageLoader;

/** Unchanged full-cube geometry and packaged textures; optically flat holograms. */
public final class FortniteChunky {
  public static void main(String[] args) throws Exception {
    boolean baseline = Boolean.getBoolean("fim.baseline");
    Map<String, Block> blocks = new HashMap<>();
    for (String material : List.of("wood", "stone", "metal")) {
      String name = "fortniteinminecraft:build_hologram_" + material;
      Texture texture = new Texture(ImageLoader.read(new File(System.getProperty("fim.textures"), "build_hologram_" + material + ".png")));
      Block block = new Block(name, texture) {};
      block.opaque = false;
      block.solid = false;
      block.ior = baseline ? 1.0f : Air.INSTANCE.ior;
      block.refractive = false;
      block.specular = 0;
      block.metalness = 0;
      block.emittance = baseline ? 0 : 1;
      blocks.put(name, block);
      System.out.println("FIM_PROVIDER " + name + " cube_all [0,0,0]-[1,1,1] ior=" + block.ior + " airIOR=" + Air.INSTANCE.ior + " refractive=" + block.refractive + " specular=" + block.specular + " texture=" + texture.getWidth() + "x" + texture.getHeight());
    }
    BlockSpec.blockProviders.add(0, new BlockProvider() {
      public Block getBlockByTag(String name, Tag tag) { return blocks.get(name); }
      public Collection<String> getSupportedBlocks() { return blocks.keySet(); }
    });
    se.llbit.chunky.main.Chunky.main(args);
  }
}
