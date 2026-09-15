package io.github.r3neer.alchemicalleather.data;

import com.google.gson.*;
import java.util.*;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/** Data-driven mapping from potion effects to causal wear sources and work budgets. */
public final class WearRules implements SimpleSynchronousResourceReloadListener {
    /**
     * Builtin detectors are implementation-owned protocol names, not an open extension namespace.
     * Third-party semantics stay extensible through type=event. Keeping this list explicit makes a
     * typo fail resource loading instead of masquerading as a classified effect that can never emit.
     */
    private static final Set<Identifier> BUILTIN_DETECTORS=Set.of(
        id("self_propelled_movement_speed"),
        id("jump_boost_jump"),
        id("damage_prevented"),
        id("effect_health_delta"),
        id("water_breathing_tick"),
        id("slow_falling_tick"),
        id("effect_proc"),
        id("weaving_movement"),
        id("attack_damage_delta"),
        id("extra_reach_use"),
        id("knockback_reduced"),
        id("poison_removed"),
        id("soulsteal_healing"),
        id("scorching_ignition"),
        id("scorching_fire_placement")
    );

    public record Source(String type,Identifier id,double work) {
        public Source {
            if(!type.equals("builtin")&&!type.equals("event"))throw new IllegalArgumentException("Unknown wear source type "+type);
            if(type.equals("builtin")&&!BUILTIN_DETECTORS.contains(id))throw new IllegalArgumentException("Unknown builtin wear detector "+id);
            if(!Double.isFinite(work)||work<=0)throw new IllegalArgumentException("Wear source work must be finite and positive");
        }
    }
    public record Rule(boolean none,double workPerDamage,List<Source> sources) {
        public Rule { sources=List.copyOf(sources); }
        public static Rule noWear(){return new Rule(true,0,List.of());}
        public double work(String type,Identifier source,double amount){
            if(none||!Double.isFinite(amount)||amount<=0)return 0;
            double result=0;
            for(var candidate:sources)if(candidate.type().equals(type)&&candidate.id().equals(source))result+=candidate.work()*amount;
            return result;
        }
    }

    private static volatile Map<Identifier,Rule> rules=Map.of();
    public static Rule rule(Identifier effect){return rules.get(effect);}
    public static boolean classified(Identifier effect){return rules.containsKey(effect);}
    public static Map<Identifier,Rule> snapshot(){return rules;}
    static boolean knownBuiltin(Identifier detector){return BUILTIN_DETECTORS.contains(detector);}

    @Override public Identifier getFabricId(){return Identifier.fromNamespaceAndPath("alchemical_leather","wear_rules");}

    @Override public void onResourceManagerReload(ResourceManager manager){
        var next=new HashMap<Identifier,Rule>();
        String prefix="alchemical_leather/wear_rules/";
        manager.listResources(prefix.substring(0,prefix.length()-1),p->p.getPath().endsWith(".json")).forEach((path,resource)->{
            try(var reader=resource.openAsReader()){
                var json=JsonParser.parseReader(reader).getAsJsonObject();
                if(!enabled(json,manager))return;
                var effect=Identifier.fromNamespaceAndPath(path.getNamespace(),path.getPath().substring(prefix.length(),path.getPath().length()-5));
                if(!BuiltInRegistries.MOB_EFFECT.containsKey(effect))throw new IllegalArgumentException("Unknown effect "+effect);
                var rule=parse(json);
                if(next.put(effect,rule)!=null)throw new IllegalArgumentException("Duplicate active wear rule for "+effect);
            }catch(Exception e){throw new IllegalArgumentException("Invalid wear rule "+path,e);}
        });
        rules=Map.copyOf(next);
    }

    private static boolean enabled(JsonObject json,ResourceManager manager){
        validateMetadataTypes(json);
        if(json.has("enabled")&&!json.get("enabled").getAsBoolean())return false;
        if(json.has("requires_mod")&&!FabricLoader.getInstance().isModLoaded(json.get("requires_mod").getAsString()))return false;
        if(json.has("requires_effect")&&!BuiltInRegistries.MOB_EFFECT.containsKey(Identifier.parse(json.get("requires_effect").getAsString())))return false;
        if(json.has("requires_resource")&&manager.getResource(Identifier.parse(json.get("requires_resource").getAsString())).isEmpty())return false;
        return true;
    }

    public static Rule parse(JsonObject json){
        validateMetadataTypes(json);
        if(json.has("wear")){
            if(!isString(json.get("wear"))||!json.get("wear").getAsString().equals("none"))throw new IllegalArgumentException("Only wear=none is supported as a symbolic wear mode");
            if(json.has("work_per_damage")||json.has("sources"))throw new IllegalArgumentException("wear=none cannot also declare work_per_damage or sources");
            return Rule.noWear();
        }
        if(!json.has("work_per_damage"))throw new IllegalArgumentException("Missing work_per_damage");
        if(!isNumber(json.get("work_per_damage")))throw new IllegalArgumentException("work_per_damage must be a JSON number");
        double threshold=json.get("work_per_damage").getAsDouble();
        if(!Double.isFinite(threshold)||threshold<=0)throw new IllegalArgumentException("work_per_damage must be finite and positive");
        if(!json.has("sources")||!json.get("sources").isJsonArray())throw new IllegalArgumentException("sources must be a JSON array");
        var array=json.getAsJsonArray("sources");
        if(array.isEmpty())throw new IllegalArgumentException("At least one wear source is required");
        var sources=new ArrayList<Source>();var seen=new HashSet<String>();
        for(var element:array){
            if(!element.isJsonObject())throw new IllegalArgumentException("Every wear source must be a JSON object");
            var source=element.getAsJsonObject();
            if(!source.has("type")||!isString(source.get("type")))throw new IllegalArgumentException("Wear source type must be a JSON string");
            String type=source.get("type").getAsString();
            String key=switch(type){case "builtin"->"detector";case "event"->"event";default->throw new IllegalArgumentException("Unknown source type "+type);};
            if(!source.has(key)||!isString(source.get(key)))throw new IllegalArgumentException(key+" must be a JSON string for "+type+" wear source");
            var id=Identifier.parse(source.get(key).getAsString());
            if(source.has("work")&&!isNumber(source.get("work")))throw new IllegalArgumentException("Wear source work must be a JSON number");
            double work=source.has("work")?source.get("work").getAsDouble():1.0;
            var parsed=new Source(type,id,work);String unique=type+":"+id;
            if(!seen.add(unique))throw new IllegalArgumentException("Duplicate wear source "+unique);
            sources.add(parsed);
        }
        return new Rule(false,threshold,sources);
    }

    private static void validateMetadataTypes(JsonObject json){
        if(json.has("enabled")){
            var value=json.get("enabled");
            if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException("enabled must be a JSON boolean");
        }
        for(var key:List.of("requires_mod","requires_effect","requires_resource")){
            if(json.has(key)&&!isString(json.get(key)))throw new IllegalArgumentException(key+" must be a JSON string");
        }
        if(json.has("requires_mod")&&json.get("requires_mod").getAsString().isBlank())throw new IllegalArgumentException("requires_mod must not be blank");
    }

    private static boolean isString(JsonElement value){return value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString();}
    private static boolean isNumber(JsonElement value){return value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isNumber();}
    private static Identifier id(String path){return Identifier.fromNamespaceAndPath("alchemical_leather",path);}
}
