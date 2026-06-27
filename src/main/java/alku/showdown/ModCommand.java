package alku.showdown;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.fml.ModList;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ModCommand {

    // Cached mod ID list - rebuilt only when suggestions are requested
    private static List<String> cachedModIds = null;

    private static List<String> getModIds() {
        if (cachedModIds == null) {
            cachedModIds = ModList.get().getMods().stream()
                .map(info -> info.getModId())
                .filter(id -> !id.equals("minecraft") && !id.equals("forge"))
                .collect(Collectors.toList());
        }
        return cachedModIds;
    }

    private static final SuggestionProvider<CommandSourceStack> MOD_ID_SUGGESTIONS =
        (ctx, builder) -> SharedSuggestionProvider.suggest(getModIds().stream(), builder);

    private static final SuggestionProvider<CommandSourceStack> TEAM_NAME_SUGGESTIONS =
        (ctx, builder) -> SharedSuggestionProvider.suggest(
            ModTeamManager.getTeamNames(),
            builder
        );

    private static final SuggestionProvider<CommandSourceStack> MOD_IN_TEAM_SUGGESTIONS =
        (ctx, builder) -> SharedSuggestionProvider.suggest(
            ModTeamManager.getAllTeams().values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet()),
            builder
        );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("showdown")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("hostile")
                    .then(Commands.argument("mod1", StringArgumentType.word())
                        .suggests(MOD_ID_SUGGESTIONS)
                        .then(Commands.argument("mod2", StringArgumentType.word())
                            .suggests(MOD_ID_SUGGESTIONS)
                            .executes(ctx -> startFeud(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "mod1"),
                                StringArgumentType.getString(ctx, "mod2")
                            ))
                        )
                    )
                )
                .then(Commands.literal("cancel")
                    .executes(ctx -> cancelFeud(ctx.getSource()))
                )
                .then(Commands.literal("nodrops")
                    .executes(ctx -> toggleNoDrops(ctx.getSource()))
                )
                .then(Commands.literal("noexps")
                    .executes(ctx -> toggleNoExpDrops(ctx.getSource()))
                )
                .then(Commands.literal("peaceful")
                    .executes(ctx -> togglePeaceful(ctx.getSource()))
                )
                .then(Commands.literal("godmode")
                    .executes(ctx -> toggleGodMode(ctx.getSource()))
                )
                .then(Commands.literal("record")
                    .executes(ctx -> toggleRecord(ctx.getSource()))
                )
                .then(Commands.literal("team")
                    .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .executes(ctx -> createTeam(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "name")
                            ))
                        )
                    )
                    .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(TEAM_NAME_SUGGESTIONS)
                            .executes(ctx -> deleteTeam(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "name")
                            ))
                        )
                    )
                    .then(Commands.literal("add")
                        .then(Commands.argument("team", StringArgumentType.word())
                            .suggests(TEAM_NAME_SUGGESTIONS)
                            .then(Commands.argument("mod", StringArgumentType.word())
                                .suggests(MOD_ID_SUGGESTIONS)
                                .executes(ctx -> addModToTeam(
                                    ctx.getSource(),
                                    StringArgumentType.getString(ctx, "team"),
                                    StringArgumentType.getString(ctx, "mod")
                                ))
                            )
                        )
                    )
                    .then(Commands.literal("remove")
                        .then(Commands.argument("mod", StringArgumentType.word())
                            .suggests(MOD_IN_TEAM_SUGGESTIONS)
                            .executes(ctx -> removeModFromTeam(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "mod")
                            ))
                        )
                    )
                    .then(Commands.literal("list")
                        .executes(ctx -> listTeams(ctx.getSource()))
                    )
                    .then(Commands.literal("apply")
                        .executes(ctx -> applyTeams(ctx.getSource()))
                    )
                    .then(Commands.literal("clear")
                        .executes(ctx -> clearTeams(ctx.getSource()))
                    )
                )
        );
    }

    private static int startFeud(CommandSourceStack source, String mod1, String mod2) {
        if (mod1.equals(mod2)) {
            source.sendFailure(Component.literal("两个模组ID不能相同"));
            return 0;
        }

        ModFeudManager.start(mod1, mod2);

        ServerLevel serverLevel = (ServerLevel) source.getLevel();
        for (Entity entity : serverLevel.getAllEntities()) {
            if (entity instanceof Mob mob && ModFeudManager.belongsToAny(mob.getType())) {
                Showdown.ensureFeudTargetGoal(mob);
                mob.setTarget(null);
            }
        }

        source.sendSuccess(() -> Component.literal("已设置敌对: mod1=§a" + mod1 + "§r, mod2=§c" + mod2 + "§r — 两模组生物将互相攻击"), true);
        return 1;
    }

    private static int cancelFeud(CommandSourceStack source) {
        if (!ModFeudManager.isActive()) {
            source.sendFailure(Component.literal("当前没有活跃的敌对设置"));
            return 0;
        }

        ServerLevel serverLevel = (ServerLevel) source.getLevel();
        for (Entity entity : serverLevel.getAllEntities()) {
            if (entity instanceof Mob mob && ModFeudManager.belongsToAny(mob.getType())) {
                mob.setTarget(null);
            }
        }

        ModFeudManager.stop();
        source.sendSuccess(() -> Component.literal("已取消所有模组敌对"), true);
        return 1;
    }

    private static int toggleNoDrops(CommandSourceStack source) {
        Showdown.noItemDrops = !Showdown.noItemDrops;
        source.sendSuccess(() -> Component.literal(
            "禁止掉落物: " + (Showdown.noItemDrops ? "§a开启" : "§c关闭")
        ), true);
        return 1;
    }

    private static int toggleNoExpDrops(CommandSourceStack source) {
        Showdown.noExpDrops = !Showdown.noExpDrops;
        source.sendSuccess(() -> Component.literal(
            "禁止经验球: " + (Showdown.noExpDrops ? "§a开启" : "§c关闭")
        ), true);
        return 1;
    }

    private static int togglePeaceful(CommandSourceStack source) {
        Showdown.peacefulMode = !Showdown.peacefulMode;
        source.sendSuccess(() -> Component.literal(
            "生物和平模式: " + (Showdown.peacefulMode ? "§a开启" : "§c关闭")
        ), true);
        return 1;
    }

    private static int toggleGodMode(CommandSourceStack source) {
        Showdown.godMode = !Showdown.godMode;
        source.sendSuccess(() -> Component.literal(
            "玩家免疫模式: " + (Showdown.godMode ? "§a开启" : "§c关闭")
        ), true);
        return 1;
    }

    private static int createTeam(CommandSourceStack source, String teamName) {
        if (ModTeamManager.createTeam(teamName)) {
            source.sendSuccess(() -> Component.literal("已创建队伍: §a" + teamName), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("队伍 §c" + teamName + "§r 已存在"));
            return 0;
        }
    }

    private static int deleteTeam(CommandSourceStack source, String teamName) {
        if (!ModTeamManager.teamExists(teamName)) {
            source.sendFailure(Component.literal("队伍 §c" + teamName + "§r 不存在"));
            return 0;
        }
        ModTeamManager.deleteTeam(teamName);
        source.sendSuccess(() -> Component.literal("已删除队伍: §c" + teamName), true);
        return 1;
    }

    private static int addModToTeam(CommandSourceStack source, String teamName, String modId) {
        if (!ModTeamManager.teamExists(teamName)) {
            source.sendFailure(Component.literal("队伍 §c" + teamName + "§r 不存在"));
            return 0;
        }
        if (ModTeamManager.isModInAnyTeam(modId)) {
            String currentTeam = ModTeamManager.getTeamOfMod(modId);
            source.sendFailure(Component.literal("模组 §c" + modId + "§r 已在队伍 §e" + currentTeam + "§r 中"));
            return 0;
        }
        ModTeamManager.addModToTeam(teamName, modId);
        source.sendSuccess(() -> Component.literal("已将 §a" + modId + "§r 添加到队伍 §e" + teamName), true);
        return 1;
    }

    private static int removeModFromTeam(CommandSourceStack source, String modId) {
        if (!ModTeamManager.isModInAnyTeam(modId)) {
            source.sendFailure(Component.literal("模组 §c" + modId + "§r 不在任何队伍中"));
            return 0;
        }
        String teamName = ModTeamManager.getTeamOfMod(modId);
        ModTeamManager.removeModFromTeam(modId);
        source.sendSuccess(() -> Component.literal("已将 §c" + modId + "§r 从队伍 §e" + teamName + "§r 移除"), true);
        return 1;
    }

    private static int listTeams(CommandSourceStack source) {
        Map<String, Set<String>> allTeams = ModTeamManager.getAllTeams();
        if (allTeams.isEmpty()) {
            source.sendSuccess(() -> Component.literal("当前没有队伍"), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal("§6=== 队伍列表 ==="), false);
        for (Map.Entry<String, Set<String>> entry : allTeams.entrySet()) {
            String teamName = entry.getKey();
            Set<String> mods = entry.getValue();
            if (mods.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§e" + teamName + "§r: (空)"), false);
            } else {
                String modList = String.join(", ", mods);
                source.sendSuccess(() -> Component.literal("§e" + teamName + "§r: " + modList), false);
            }
        }
        return 1;
    }

    private static int applyTeams(CommandSourceStack source) {
        ModFeudManager.applyTeams();

        ServerLevel serverLevel = (ServerLevel) source.getLevel();
        for (Entity entity : serverLevel.getAllEntities()) {
            if (entity instanceof Mob mob && ModFeudManager.belongsToTeam(mob.getType())) {
                Showdown.ensureFeudTargetGoal(mob);
                mob.setTarget(null);
            }
        }

        source.sendSuccess(() -> Component.literal("§a已应用队伍敌对设置，不同队伍的生物将互相攻击"), true);
        return 1;
    }

    private static int clearTeams(CommandSourceStack source) {
        if (!ModFeudManager.isActive()) {
            source.sendFailure(Component.literal("当前没有活跃的敌对设置"));
            return 0;
        }

        ServerLevel serverLevel = (ServerLevel) source.getLevel();
        for (Entity entity : serverLevel.getAllEntities()) {
            if (entity instanceof Mob mob && ModFeudManager.belongsToTeam(mob.getType())) {
                mob.setTarget(null);
            }
        }

        ModFeudManager.stop();
        source.sendSuccess(() -> Component.literal("§a已清除所有敌对状态"), true);
        return 1;
    }

    private static int toggleRecord(CommandSourceStack source) {
        if (!ModFeudManager.isActive()) {
            source.sendFailure(Component.literal("当前没有活跃的敌对设置，请先使用 /showdown hostile 或 /showdown team apply"));
            return 0;
        }

        if (FeudStatistics.isRecording()) {
            Map<String, long[]> stats = FeudStatistics.stop();
            long elapsed = (System.currentTimeMillis() - FeudStatistics.getStartTime()) / 1000;
            source.sendSuccess(() -> Component.literal("§6=== 战斗统计 (记录时长: " + elapsed + "秒) ==="), false);
            for (Map.Entry<String, long[]> entry : stats.entrySet()) {
                String team = entry.getKey();
                long kills = entry.getValue()[0];
                long deaths = entry.getValue()[1];
                source.sendSuccess(() -> Component.literal(
                    "§e" + team + "§r: 击杀 §a" + kills + "§r / 死亡 §c" + deaths
                ), false);
            }
            source.sendSuccess(() -> Component.literal("§a已停止数据记录"), true);
        } else {
            FeudStatistics.start();
            source.sendSuccess(() -> Component.literal("§a已开始数据记录，使用 /showdown record 停止并查看统计"), true);
        }
        return 1;
    }
}
