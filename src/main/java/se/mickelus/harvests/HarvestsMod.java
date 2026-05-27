package se.mickelus.harvests;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import se.mickelus.harvests.filter.TierFilterStore;
import se.mickelus.harvests.gui.ScrollScreen;

@Mod(value = HarvestsMod.modId, dist = Dist.CLIENT)
@EventBusSubscriber(modid = HarvestsMod.modId, value = Dist.CLIENT)
public class HarvestsMod {

    public static final String modId = "harvests";

    public HarvestsMod(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        new TierFilterStore();
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.register(ScrollScreen.class);
    }
}
