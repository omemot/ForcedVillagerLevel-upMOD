package com.example.examplemod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class VillagerUpgraderItem extends Item {

    // 各レベルに到達するための累計必要XP（バニラ準拠）
    private static final int[] LEVEL_XP_THRESHOLDS = {0, 10, 70, 150, 250};

    public VillagerUpgraderItem(Properties properties) {
        super(properties);
    }

    @Override
    @NotNull
    public InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull LivingEntity target, @NotNull InteractionHand hand) {
        if (!(target instanceof Villager villager)) {
            return InteractionResult.PASS;
        }

        if (villager.level().isClientSide) {
            // クライアント側はSUCCESSを返してEasy Villagersの処理をキャンセルする
            return InteractionResult.SUCCESS;
        }

        VillagerData data = villager.getVillagerData();
        int currentLevel = data.getLevel();

        if (currentLevel >= 5) {
            // すでに最大レベルの場合
            player.displayClientMessage(Component.literal("この村人はすでに最大レベル（達人）だ。"), true);
            return InteractionResult.SUCCESS;
        }

        if (data.getProfession() == VillagerProfession.NONE || data.getProfession() == VillagerProfession.NITWIT) {
            // 職業を持たない村人には使えない
            player.displayClientMessage(Component.literal("職業を持つ村人にのみ使用できる。"), true);
            return InteractionResult.SUCCESS;
        }

        int nextLevel = currentLevel + 1;

        // レベルを更新（職業・タイプは維持）
        villager.setVillagerData(data.setLevel(nextLevel));

        // XPをそのレベルの閾値に上書き（加算ではなく上書きで安定させる）
        int xpThreshold = LEVEL_XP_THRESHOLDS[Math.min(nextLevel - 1, LEVEL_XP_THRESHOLDS.length - 1)];
        villager.setVillagerXp(xpThreshold);

        // 遅延タスクで取引リストを安全に再構築する
        ServerLevel serverLevel = (ServerLevel) villager.level();
        serverLevel.getServer().execute(() -> {
            try {
                // リフレクションでprotectedのupdateTrades()を呼ぶ
                var method = Villager.class.getDeclaredMethod("updateTrades");
                method.setAccessible(true);
                method.invoke(villager);
            } catch (Exception e) {
                // フォールバック: offersをクリアして次回話しかけ時に再生成させる
                villager.getOffers().clear();
            }
        });

        player.displayClientMessage(Component.literal("村人をレベル " + nextLevel + " にアップグレードした！"), true);

        if (!player.getAbilities().instabuild) {
            // サバイバルモードならアイテムを1つ消費する
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }
}