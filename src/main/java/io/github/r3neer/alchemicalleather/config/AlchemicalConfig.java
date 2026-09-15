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
    public static final Settings DEFAULTS=new Settings(true,true);
    private static volatile Settings current=DEFAULTS;

    public record Settings(boolean cauldronTippedArrows,boolean infusionWear){}

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
    public static boolean infusionWear(){return current.infusionWear();}

    /** Public and pure so malformed/partial config behavior can be verified without mutating the live file. */
    public static Settings parse(JsonElement element){
        if(element==null||!element.isJsonObject())return DEFAULTS;
        var root=element.getAsJsonObject();
        boolean arrows=booleanField(root,"cauldronTippedArrows",DEFAULTS.cauldronTippedArrows());
        boolean wear=booleanField(root,"infusionWear",DEFAULTS.infusionWear());
        return new Settings(arrows,wear);
    }

    private static boolean booleanField(JsonObject root,String name,boolean fallback){
        var value=root.get(name);
        return value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isBoolean()?value.getAsBoolean():fallback;
    }

    private static void writeDefaults(){
        try{
            Files.createDirectories(PATH.getParent());
            var root=new JsonObject();
            root.addProperty("cauldronTippedArrows",DEFAULTS.cauldronTippedArrows());
            root.addProperty("infusionWear",DEFAULTS.infusionWear());
            try(var writer=Files.newBufferedWriter(PATH,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){
                GSON.toJson(root,writer);
            }
        }catch(FileAlreadyExistsException ignored){
            // Another startup path won the race; the next launch will read it normally.
        }catch(IOException e){
            LOGGER.warn("Could not create {}; using defaults",PATH,e);
        }
    }
}
