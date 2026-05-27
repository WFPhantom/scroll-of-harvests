package se.mickelus.harvests;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue BUTTON_X = BUILDER
            .comment("Horizontal positioning for the scroll button in the player inventory")
            .defineInRange("button_x", 125, -1000, 1000);

    public static final ModConfigSpec.IntValue BUTTON_Y = BUILDER
            .comment("Vertical positioning for the scroll button in the player inventory")
            .defineInRange("button_y", 61, -1000, 1000);

    static final ModConfigSpec SPEC = BUILDER.build();
}
