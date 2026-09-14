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
    public record Source(String type,Identifier id,double work) {
        public Source {
            if(!type.equals("builtin")&&!type.equals("event"))throw new IllegalArgumentException("Unknown wear source type "+type);
            if(!Double.isFinite(work)||work<=0)throw new IllegalArgumentException("Wear source work must be finite and positive");
        }
    }
    public record Rule(boolean none,double workPerDamage,List<Source> sources) {
        public Rule { sources=List.copyOf(sources); }
        public static Rule none(){return new Rule(true,0,List.of());}
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
        if(json.has("enabled")&&!json.get("enabled").getAsBoolean())return false;
        if(json.has("requires_mod")&&!FabricLoader.getInstance().isModLoaded(json.get("requires_mod").getAsString()))return false;
        if(json.has("requires_effect")&&!BuiltInRegistries.MOB_EFFECT.containsKey(Identifier.parse(json.get("requires_effect").getAsString())))return false;
        if(json.has("requires_resource")&&manager.getResource(Identifier.parse(json.get("requires_resource").getAsString())).isEmpty())return false;
        return true;
    }

    public static Rule parse(JsonObject json){
        if(json.has("wear")){
            if(!json.get("wear").isJsonPrimitive()||!json.get("wear").getAsString().equals("none"))throw new IllegalArgumentException("Only wear=none is supported as a symbolic wear mode");
            return Rule.none();
        }
        if(!json.has("work_per_damage"))throw new IllegalArgumentException("Missing work_per_damage");
        double threshold=json.get("work_per_damage").getAsDouble();
        if(!Double.isFinite(threshold)||threshold<=0)throw new IllegalArgumentException("work_per_damage must be finite and positive");
        var array=json.getAsJsonArray("sources");
        if(array==null||array.isEmpty())throw new IllegalArgumentException("At least one wear source is required");
        var sources=new ArrayList<Source>();var seen=new HashSet<String>();
        for(var element:array){
            var source=element.getAsJsonObject();String type=source.get("type").getAsString();
            String key=switch(type){case "builtin"->"detector";case "event"->"event";default->throw new IllegalArgumentException("Unknown source type "+type);};
            var id=Identifier.parse(source.get(key).getAsString());double work=source.has("work")?source.get("work").getAsDouble():1.0;
            var parsed=new Source(type,id,work);String unique=type+":"+id;
            if(!seen.add(unique))throw new IllegalArgumentException("Duplicate wear source "+unique);
            sources.add(parsed);
        }
        return new Rule(false,threshold,sources);
    }
}
