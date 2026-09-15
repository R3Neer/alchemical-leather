package io.github.r3neer.alchemicalleather.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.resources.Identifier;

/** Persistent fractional work carried by one infused armor stack. */
public record WearProgress(List<Entry> entries) {
    public static final WearProgress EMPTY=new WearProgress(List.of());

    public record Entry(Identifier effect,double work) {
        public static final Codec<Entry> CODEC=RecordCodecBuilder.<Entry>create(i->i.group(
            Identifier.CODEC.fieldOf("effect").forGetter(Entry::effect),
            Codec.DOUBLE.fieldOf("work").forGetter(Entry::work)
        ).apply(i,Entry::new)).validate(e->Double.isFinite(e.work())&&e.work()>=0
            ?DataResult.success(e):DataResult.error(()->"Wear work must be finite and non-negative"));
    }

    public WearProgress { entries=List.copyOf(entries); }

    public static final Codec<WearProgress> CODEC=Entry.CODEC.listOf().xmap(WearProgress::new,WearProgress::entries)
        .validate(value->value.valid()?DataResult.success(value):DataResult.error(()->"Wear progress contains duplicate effects"));

    private boolean valid(){
        var seen=new HashSet<Identifier>();
        for(var entry:entries)if(!seen.add(entry.effect()))return false;
        return true;
    }

    public double work(Identifier effect){
        for(var entry:entries)if(entry.effect().equals(effect))return entry.work();
        return 0.0;
    }

    public WearProgress with(Identifier effect,double work){
        if(!Double.isFinite(work)||work<0)throw new IllegalArgumentException("work must be finite and non-negative");
        var next=new ArrayList<Entry>(entries.size()+1);
        for(var entry:entries)if(!entry.effect().equals(effect))next.add(entry);
        if(work>1.0E-9)next.add(new Entry(effect,work));
        return next.isEmpty()?EMPTY:new WearProgress(next);
    }

    public WearProgress retain(Set<Identifier> effects){
        var next=entries.stream().filter(e->effects.contains(e.effect())).toList();
        return next.isEmpty()?EMPTY:new WearProgress(next);
    }

    public boolean isEmpty(){return entries.isEmpty();}
}
