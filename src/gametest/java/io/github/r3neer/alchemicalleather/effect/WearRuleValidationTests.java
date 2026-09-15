package io.github.r3neer.alchemicalleather.effect;

import com.google.gson.JsonParser;
import io.github.r3neer.alchemicalleather.data.WearRules;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/** Parser holdouts: builtin detector names are closed; semantic event names remain extensible. */
public final class WearRuleValidationTests {
    @GameTest public void builtinProtocolRejectsUnknownDetectorsButEventsStayOpen(GameTestHelper h){
        var known=WearRules.parse(JsonParser.parseString("""
            {"work_per_damage":16,"sources":[{"type":"builtin","detector":"alchemical_leather:jump_boost_jump"}]}
            """).getAsJsonObject());
        h.assertTrue(known.work("builtin",Identifier.parse("alchemical_leather:jump_boost_jump"),2.0)==2.0,
            "Known Alchemical Leather builtin detector remains valid");

        var external=WearRules.parse(JsonParser.parseString("""
            {"work_per_damage":30,"sources":[{"type":"event","event":"some_future_mod:semantic_success","work":3}]}
            """).getAsJsonObject());
        h.assertTrue(external.work("event",Identifier.parse("some_future_mod:semantic_success"),2.0)==6.0,
            "Third-party semantic events remain an open extension protocol");

        h.assertTrue(throwsIllegal(() -> WearRules.parse(JsonParser.parseString("""
            {"work_per_damage":16,"sources":[{"type":"builtin","detector":"alchemical_leather:jump_bost_jump"}]}
            """).getAsJsonObject())),
            "A misspelled builtin detector must fail instead of becoming inert classified data");
        h.assertTrue(throwsIllegal(() -> WearRules.parse(JsonParser.parseString("""
            {"work_per_damage":16,"sources":[{"type":"builtin"}]}
            """).getAsJsonObject())),
            "Builtin source without a detector id is invalid");
        h.succeed();
    }

    @GameTest public void symbolicNoneCannotAlsoDeclareWork(GameTestHelper h){
        h.assertTrue(WearRules.parse(JsonParser.parseString("{\"wear\":\"none\"}").getAsJsonObject()).none(),
            "Pure wear=none remains the explicit no-wear classification");
        h.assertTrue(throwsIllegal(() -> WearRules.parse(JsonParser.parseString("""
            {"wear":"none","work_per_damage":16,"sources":[{"type":"builtin","detector":"alchemical_leather:jump_boost_jump"}]}
            """).getAsJsonObject())),
            "Contradictory wear=none plus work declaration must fail atomically");
        h.succeed();
    }

    @GameTest public void coercibleJsonTypesAreStillMalformed(GameTestHelper h){
        h.assertTrue(throwsIllegal(() -> WearRules.parse(JsonParser.parseString("""
            {"work_per_damage":"16","sources":[{"type":"builtin","detector":"alchemical_leather:jump_boost_jump"}]}
            """).getAsJsonObject())),
            "Numeric strings do not masquerade as numeric work thresholds");
        h.assertTrue(throwsIllegal(() -> WearRules.parse(JsonParser.parseString("""
            {"work_per_damage":16,"sources":[{"type":"builtin","detector":"alchemical_leather:jump_boost_jump","work":"2"}]}
            """).getAsJsonObject())),
            "Source weights must be JSON numbers rather than coercible strings");
        h.assertTrue(throwsIllegal(() -> WearRules.parse(JsonParser.parseString("""
            {"enabled":"false","wear":"none"}
            """).getAsJsonObject())),
            "enabled metadata must be a real JSON boolean");
        h.assertTrue(throwsIllegal(() -> WearRules.parse(JsonParser.parseString("""
            {"requires_mod":42,"wear":"none"}
            """).getAsJsonObject())),
            "Conditional mod ids must be JSON strings");
        h.succeed();
    }

    private static boolean throwsIllegal(Runnable action){
        try{action.run();return false;}catch(IllegalArgumentException expected){return true;}
    }
}
