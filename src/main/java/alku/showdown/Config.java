package alku.showdown;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = Showdown.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK =
        BUILDER.comment("Whether to log the dirt block on common setup")
               .define("logDirtBlock", true);

    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER =
        BUILDER.comment("A magic number")
               .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION =
        BUILDER.comment("What you want the introduction message to be for the magic number")
               .define("magicNumberIntroduction", "The magic number is... ");

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS =
        BUILDER.comment("A list of items to log on common setup.")
               .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    private static final ForgeConfigSpec.BooleanValue NO_ITEM_DROPS =
        BUILDER.comment("Prevent all item entities from spawning in the world")
               .define("noItemDrops", true);

    private static final ForgeConfigSpec.BooleanValue NO_EXP_DROPS =
        BUILDER.comment("Prevent all experience orbs from spawning in the world")
               .define("noExpDrops", true);

    private static final ForgeConfigSpec.BooleanValue PEACEFUL_MODE =
        BUILDER.comment("Prevent all mobs from targeting players")
               .define("peacefulMode", true);

    private static final ForgeConfigSpec.BooleanValue GOD_MODE =
        BUILDER.comment("Prevent players from taking damage and receiving potion effects")
               .define("godMode", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;

    private static boolean validateItemName(final Object obj) {
        return obj instanceof final String itemName
            && ResourceLocation.tryParse(itemName) != null
            && ForgeRegistries.ITEMS.containsKey(ResourceLocation.tryParse(itemName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();
        items = ITEM_STRINGS.get().stream()
              .map(ResourceLocation::tryParse)
              .filter(rl -> rl != null && ForgeRegistries.ITEMS.containsKey(rl))
              .map(ForgeRegistries.ITEMS::getValue)
              .collect(Collectors.toSet());

        Showdown.noItemDrops = NO_ITEM_DROPS.get();
        Showdown.noExpDrops = NO_EXP_DROPS.get();
        Showdown.peacefulMode = PEACEFUL_MODE.get();
        Showdown.godMode = GOD_MODE.get();
    }
}
