package madmike.numismaticgts;

import madmike.config.NumismaticGTSConfig;
import madmike.numismaticgts.command.GTSCommand;
import madmike.numismaticgts.net.ServerReceiver;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NumismaticGTS implements ModInitializer {
	public static final String MOD_ID = "numismatic-gts";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		NumismaticGTSConfig.load();

		GTSCommand.register();

		ServerReceiver.register();

		LOGGER.info("Numismatic GTS initialized");
	}
}