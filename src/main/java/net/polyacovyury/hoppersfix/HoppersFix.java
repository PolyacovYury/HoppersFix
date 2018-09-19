package net.polyacovyury.hoppersfix;

import net.minecraftforge.fml.common.Mod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = HoppersFix.MODID, name = HoppersFix.NAME, version = HoppersFix.VERSION, useMetadata = true)
public class HoppersFix {
    public static final String MODID = "hoppersfix";
    public static final String NAME = "HoppersFix";
    public static final String VERSION = "1.2.0.2";
    public static final Logger logger = LogManager.getLogger("HoppersFix");
    public static boolean IGNORE_TILE_UPDATES = false;
}
