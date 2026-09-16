package io.github.r3neer.alchemicalleather.effect;

/** Counterfactual combat attribution shared by Strength and Weakness. */
public final class CombatWear {
    private CombatWear(){}

    /**
     * effectDamageDelta is signed: positive means the effect added raw attack damage (Strength),
     * negative means it suppressed raw damage (Weakness). Work is the absolute difference that
     * actually crosses vanilla's hurt cooldown with and without that effect.
     */
    public static double attackDeltaWork(double totalDamage,double effectDamageDelta,int invulnerableTime,
                                         float lastHurt,boolean bypassesCooldown){
        if(!Double.isFinite(totalDamage)||!Double.isFinite(effectDamageDelta)||!Float.isFinite(lastHurt)
            ||totalDamage<0.0||Math.abs(effectDamageDelta)<=1.0E-9)return 0.0;
        double counterfactual=Math.max(0.0,totalDamage-effectDamageDelta);
        double with=WearPredicates.damagePassingCooldown(totalDamage,invulnerableTime,lastHurt,bypassesCooldown);
        double without=WearPredicates.damagePassingCooldown(counterfactual,invulnerableTime,lastHurt,bypassesCooldown);
        double work=Math.abs(with-without);
        return Double.isFinite(work)&&work>0.0?work:0.0;
    }
}
