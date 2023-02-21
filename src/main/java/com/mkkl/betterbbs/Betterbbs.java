package com.mkkl.betterbbs;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Betterbbs implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("betterbbs");

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> ScoreboardUpdateCommand.register(dispatcher)));
        LOGGER.info("Loaded!");
    }
}
