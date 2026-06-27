package alku.showdown;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(Showdown.MODID)
public class Showdown {

    public static final String MODID = "showdown";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block",
        () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block",
        () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item",
        () -> new Item(new Item.Properties().food(
            new FoodProperties.Builder().alwaysEat().nutrition(1).saturationMod(2f).build())));
    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab",
        () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> output.accept(EXAMPLE_ITEM.get()))
            .build());

    public static boolean noItemDrops = true;
    public static boolean noExpDrops = true;
    public static boolean peacefulMode = true;
    public static boolean godMode = true;

    private static boolean clearingTarget = false;
    // WeakHashMap allows GC to reclaim mobs; no synchronization needed (server thread only)
    private static final Set<Mob> mobsWithGoal = Collections.newSetFromMap(new WeakHashMap<>());

    public static void ensureFeudTargetGoal(Mob mob) {
        if (mobsWithGoal.add(mob)) {
            mob.targetSelector.addGoal(0, new ModFeudTargetGoal(mob));
        }
    }

    public Showdown() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
        if (Config.logDirtBlock) LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);
        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(EXAMPLE_BLOCK_ITEM);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommand.register(event.getDispatcher());
        LOGGER.info("Registered /showdown command");
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;

        Entity entity = event.getEntity();

        // Item/experience drop suppression - check flags first for fast skip
        if (entity instanceof ItemEntity) {
            if (noItemDrops) event.setCanceled(true);
            return;
        }
        if (entity instanceof ExperienceOrb) {
            if (noExpDrops) event.setCanceled(true);
            return;
        }

        // Feud goal injection - check isActive() before instanceof to short-circuit early
        if (ModFeudManager.isActive() && entity instanceof Mob mob) {
            if (ModFeudManager.belongsToAny(mob.getType())) {
                ensureFeudTargetGoal(mob);
            }
        }
    }

    @SubscribeEvent
    public void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity newTarget = event.getNewTarget();
        if (event.getEntity() instanceof Mob mob && newTarget != null
            && ModFeudManager.areSameTeam(mob.getType(), newTarget.getType())) {
            event.setNewTarget(null);
            return;
        }

        if (clearingTarget || !peacefulMode) return;

        if (event.getNewTarget() instanceof Player && event.getEntity() instanceof Mob mob) {
            clearingTarget = true;
            event.setNewTarget(null);
            mob.setTarget(null);
            clearingTarget = false;
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Mob victim && event.getSource().getEntity() instanceof Mob attacker
            && ModFeudManager.areSameTeam(attacker.getType(), victim.getType())) {
            event.setCanceled(true);
            return;
        }

        if (godMode && event.getEntity() instanceof Player) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPotionAdded(MobEffectEvent.Added event) {
        if (godMode && event.getEntity() instanceof Player) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        // Check isActive() first - more likely to be false than isRecording()
        if (!ModFeudManager.isActive() || !FeudStatistics.isRecording()) return;
        if (event.getEntity().level().isClientSide()) return;

        if (!(event.getEntity() instanceof Mob victim)) return;
        String victimTeam = ModFeudManager.getTeamOfEntity(victim.getType());
        if (victimTeam == null) return;

        Entity killerEntity = event.getSource().getEntity();
        if (!(killerEntity instanceof Mob killer)) return;
        String killerTeam = ModFeudManager.getTeamOfEntity(killer.getType());
        if (killerTeam == null || killerTeam.equals(victimTeam)) return;

        FeudStatistics.record(killerTeam, victimTeam);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Showdown mod loaded - Features: noItemDrops={}, noExpDrops={}, peacefulMode={}, godMode={}",
            noItemDrops, noExpDrops, peacefulMode, godMode);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}