package io.github.r3neer.alchemicalleather.config;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Minimal file-backed configuration. Unknown or malformed fields fall back to defaults. */
public final class AlchemicalConfig {
    private static final Logger LOGGER=LoggerFactory.getLogger("Alchemical Leather");
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH=FabricLoader.getInstance().getConfigDir().resolve("alchemical-leather.json");
    public static final Settings DEFAULTS=new Settings(true);
    private static volatile Settings current=DEFAULTS;

    public record Settings(boolean cauldronTippedArrows){}

    private AlchemicalConfig(){}

    public static void initialize(){
        current=DEFAULTS;
        if(Files.notExists(PATH)){
            writeDefaults();
            return;
        }
        try(var reader=Files.newBufferedReader(PATH,StandardCharsets.UTF_8)){
            current=parse(JsonParser.parseReader(reader));
        }catch(Exception e){
            LOGGER.warn("Could not read {}; using defaults",PATH,e);
        }
    }

    public static boolean cauldronTippedArrows(){return current.cauldronTippedArrows();}

    /** Public and pure so malformed/partial config behavior can be verified without mutating the live file. */
    public static Settings parse(JsonElement element){
        if(element==null||!element.isJsonObject())return DEFAULTS;
        var root=element.getAsJsonObject();
        boolean arrows=DEFAULTS.cauldronTippedArrows();
        var value=root.get("cauldronTippedArrows");
        if(value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isBoolean())arrows=value.getAsBoolean();
        return new Settings(arrows);
    }

    private static void writeDefaults(){
        try{
            Files.createDirectories(PATH.getParent());
            var root=new JsonObject();root.addProperty("cauldronTippedArrows",DEFAULTS.cauldronTippedArrows());
            try(var writer=Files.newBufferedWriter(PATH,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW)){
                GSON.toJson(root,writer);
            }
        }catch(FileAlreadyExistsException ignored){
            // Another startup path won the race; the next launch will read it normally.
        }catch(IOException e){
            LOGGER.warn("Could not create {}; using defaults",PATH,e);
        }
    }
}
