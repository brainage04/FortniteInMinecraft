import java.io.File;
import se.llbit.chunky.block.Block;
import se.llbit.chunky.block.minecraft.Air;
import se.llbit.chunky.resources.Texture;
import se.llbit.resources.ImageLoader;

public final class MaterialProbe {
  public static void main(String[] args) throws Exception {
    for (String material : new String[]{"wood", "stone", "metal"}) {
      Texture texture = new Texture(ImageLoader.read(new File(args[0], "build_hologram_" + material + ".png")));
      Block block = new Block("fortniteinminecraft:build_hologram_" + material, texture) {};
      block.opaque = false;
      block.solid = false;
      block.ior = 1.0f;
      block.refractive = false;
      block.specular = 0;
      float n1 = Air.INSTANCE.ior;
      float n2 = block.ior;
      float ratio = n1 / n2;
      float a = ratio - 1;
      float b = ratio + 1;
      double r0 = a * a / (b * b);
      double cosine = 1 / Math.sqrt(3);
      double fresnel = r0 + (1 - r0) * Math.pow(1 - cosine, 5);
      double alpha = texture.getColor(.5, .5)[3];
      System.out.printf("material=%s airIOR=%.9f hologramIOR=%.9f differentIOR=%s specular=%.1f metalness=%.1f refractive=%s alpha=%.9f isometricFresnel=%.9f reflectedProbability=%.9f%n", block.name, n1, n2, n1 != n2, block.specular, block.metalness, block.refractive, alpha, fresnel, (1-alpha)*fresnel);
    }
    if (Air.INSTANCE.ior == 1.0f) throw new AssertionError("Expected the observed air-IOR mismatch");
  }
}
