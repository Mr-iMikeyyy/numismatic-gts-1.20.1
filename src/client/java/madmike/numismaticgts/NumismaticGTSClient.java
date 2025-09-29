package madmike.numismaticgts;

import madmike.numismaticgts.keybind.TradingScreenKeyBind;
import madmike.numismaticgts.net.ClientReceiver;
import net.fabricmc.api.ClientModInitializer;

public class NumismaticGTSClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		TradingScreenKeyBind.register();

		ClientReceiver.register();
	}
}