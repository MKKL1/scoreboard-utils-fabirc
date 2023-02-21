package com.mkkl.betterbbs;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.command.argument.ScoreboardObjectiveArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.ServerStatHandler;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.UserCache;
import net.minecraft.util.WorldSavePath;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.UUID;

public class ScoreboardUpdateCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register((CommandManager.literal("scoreupdate")
                .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(2)))
                .then(CommandManager.argument("objective", ScoreboardObjectiveArgumentType.scoreboardObjective())
                        .then(CommandManager.argument("ignore-zeros", BoolArgumentType.bool())
                            .executes(context ->
                                    updateObjective(
                                            context.getSource(),
                                            ScoreboardObjectiveArgumentType.getObjective(context, "objective"),
                                            BoolArgumentType.getBool(context, "ignore-zeros"))))
                        .executes(context ->
                                updateObjective(
                                        context.getSource(),
                                        ScoreboardObjectiveArgumentType.getObjective(context, "objective"),
                                        true)))
        );
    }

    private static int updateObjective(ServerCommandSource source, ScoreboardObjective objective, boolean ingoreZeros) {
        Stat<?> stat;
        try {
            stat = getStatFromCriterium(objective.getCriterion());
        } catch (Exception e) {
            Betterbbs.LOGGER.error("Failed to get stat of criterion " + objective.getCriterion().getName() + ". Reason: " + e.getMessage());
            source.sendError(Text.literal("Couldn't parse " + objective.getCriterion().getName()));
            return 0;
        }
        MinecraftServer server = source.getServer();
        UserCache userCache = server.getUserCache();
        Scoreboard scoreboard = server.getScoreboard();
        File statFile = server.getSavePath(WorldSavePath.STATS).toFile();
        File[] jsonFileList = statFile.listFiles(pathname -> !pathname.isDirectory() && FilenameUtils.getExtension(pathname.getName()).equals("json"));
//        int successCount = 0;
//        int failCount = 0;
        if (jsonFileList == null) {
            Betterbbs.LOGGER.error("Failed to get files from stats folder");
            source.sendError(Text.literal("Failed to get files from stats folder"));
            return 0;
        }
        for (File jsonFile : jsonFileList) {
            UUID playeruuid = UUID.fromString(FilenameUtils.getBaseName(jsonFile.getName()));
//            source.sendMessage(Text.literal("uuid " + playeruuid));

            ServerPlayerEntity serverPlayerEntity = server.getPlayerManager().getPlayer(playeruuid);
            ServerStatHandler serverStatHandler;
            String playerName;
            if (serverPlayerEntity != null) {
                //Online
                serverStatHandler = serverPlayerEntity.getStatHandler();
                playerName = serverPlayerEntity.getEntityName();
            } else {
                //Offline
                playerName = userCache.getByUuid(playeruuid)
                        .orElse(new GameProfile(null, "null"))
                        .getName();
                //TODO check if reading was successful
                serverStatHandler = new ServerStatHandler(server, jsonFile);
            }

            int scoreValue = serverStatHandler.getStat(stat);
            if (!ingoreZeros || scoreValue != 0)
                scoreboard.forEachScore(objective.getCriterion(), playerName, score -> score.setScore(scoreValue));
        }
        source.sendFeedback(Text.literal("Successfully updated ").append(objective.getDisplayName()), true);
        //Betterbbs.LOGGER.info("Successfully copied data of " + successCount);
        return 1;
    }

    private static <T> Stat<T> getStatFromCriterium(ScoreboardCriterion scoreboardCriterion) {
        String[] s = scoreboardCriterion.getName().split(":");
        String statTypeName = s[0].substring(s[0].lastIndexOf(".") + 1);
        String statValue = s[1].substring(s[1].lastIndexOf(".") + 1);
        //TODO dangerous operation
        StatType<T> statType = (StatType<T>) Registries.STAT_TYPE.get(RegistryKey.of(Registries.STAT_TYPE.getKey(), new Identifier(statTypeName)));
        Registry<T> registry = statType.getRegistry();
        T regVal = registry.get(RegistryKey.of(registry.getKey(), new Identifier(statValue)));

        for (Stat<T> t : statType) {
            if (t.getValue() == regVal) return t;
        }
        return null;
    }
}
