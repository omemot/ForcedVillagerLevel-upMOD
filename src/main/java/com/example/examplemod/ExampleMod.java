package com.example.examplemod;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod("forced_villager_levelup")
public class ExampleMod {

    public static final String MODID = "forced_villager_levelup";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final int[] LEVEL_XP_THRESHOLDS = {0, 10, 70, 150, 250};

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // 村人レベル上げアイテムの登録
    public static final DeferredItem<Item> VILLAGER_UPGRADER = ITEMS.register("villager_upgrader",
            () -> new VillagerUpgraderItem(new Item.Properties()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.forced_villager_levelup"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> VILLAGER_UPGRADER.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(VILLAGER_UPGRADER.get());
            }).build());

    public ExampleMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        // クライアント・サーバー両側でHIGHEST優先度で登録
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::onEntityInteract);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(VILLAGER_UPGRADER);
        }
    }

    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager)) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof VillagerUpgraderItem)) {
            return;
        }

        // SHIFTを押している場合はEasy Villagersに譲る
        if (event.getEntity().isShiftKeyDown()) {
            return;
        }

        // クライアント・サーバー両側でキャンセルして交易画面を防ぐ
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);

        // サーバー側のみレベルアップ処理を実行
        if (villager.level().isClientSide) {
            return;
        }

        var player = event.getEntity();
        VillagerData data = villager.getVillagerData();
        int currentLevel = data.getLevel();

        if (currentLevel >= 5) {
            player.displayClientMessage(Component.literal("この村人はすでに最大レベル（達人）だ。"), true);
            return;
        }

        if (data.getProfession() == VillagerProfession.NONE || data.getProfession() == VillagerProfession.NITWIT) {
            player.displayClientMessage(Component.literal("職業を持つ村人にのみ使用できる。"), true);
            return;
        }

        int nextLevel = currentLevel + 1;

        // レベルを更新（職業・タイプは維持）
        villager.setVillagerData(data.setLevel(nextLevel));

        // XPをそのレベルの閾値に上書き
        int xpThreshold = LEVEL_XP_THRESHOLDS[Math.min(nextLevel - 1, LEVEL_XP_THRESHOLDS.length - 1)];
        villager.setVillagerXp(xpThreshold);

        // 遅延タスクで取引リストを安全に再構築する
        ServerLevel serverLevel = (ServerLevel) villager.level();
        serverLevel.getServer().execute(() -> {
            try {
                var method = Villager.class.getDeclaredMethod("updateTrades");
                method.setAccessible(true);
                method.invoke(villager);
            } catch (Exception e) {
                villager.getOffers().clear();
            }
        });

        player.displayClientMessage(Component.literal("村人をレベル " + nextLevel + " にアップグレードした！"), true);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }
}