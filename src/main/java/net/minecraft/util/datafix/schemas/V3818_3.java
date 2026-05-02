package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3818_3 extends NamespacedSchema {
    public V3818_3(int p_333453_, Schema p_330765_) {
        super(p_333453_, p_330765_);
    }

    public static SequencedMap<String, Supplier<TypeTemplate>> components(Schema p_361272_) {
        SequencedMap<String, Supplier<TypeTemplate>> sequencedmap = new LinkedHashMap<String, Supplier<TypeTemplate>>();
        sequencedmap.put("minecraft:bees", (Supplier<TypeTemplate>)() -> DSL.list(DSL.optionalFields("entity_data", References.ENTITY_TREE.in(p_361272_))));
        sequencedmap.put("minecraft:block_entity_data", (Supplier<TypeTemplate>)() -> References.BLOCK_ENTITY.in(p_361272_));
        sequencedmap.put("minecraft:bundle_contents", (Supplier<TypeTemplate>)() -> DSL.list(References.ITEM_STACK.in(p_361272_)));
        sequencedmap.put(
            "minecraft:can_break",
            (Supplier<TypeTemplate>)() -> DSL.optionalFields(
                "predicates", DSL.list(DSL.optionalFields("blocks", DSL.or(References.BLOCK_NAME.in(p_361272_), DSL.list(References.BLOCK_NAME.in(p_361272_)))))
            )
        );
        sequencedmap.put(
            "minecraft:can_place_on",
            (Supplier<TypeTemplate>)() -> DSL.optionalFields(
                "predicates", DSL.list(DSL.optionalFields("blocks", DSL.or(References.BLOCK_NAME.in(p_361272_), DSL.list(References.BLOCK_NAME.in(p_361272_)))))
            )
        );
        sequencedmap.put("minecraft:charged_projectiles", (Supplier<TypeTemplate>)() -> DSL.list(References.ITEM_STACK.in(p_361272_)));
        sequencedmap.put("minecraft:container", (Supplier<TypeTemplate>)() -> DSL.list(DSL.optionalFields("item", References.ITEM_STACK.in(p_361272_))));
        sequencedmap.put("minecraft:entity_data", (Supplier<TypeTemplate>)() -> References.ENTITY_TREE.in(p_361272_));
        sequencedmap.put("minecraft:pot_decorations", (Supplier<TypeTemplate>)() -> DSL.list(References.ITEM_NAME.in(p_361272_)));
        sequencedmap.put("minecraft:food", (Supplier<TypeTemplate>)() -> DSL.optionalFields("using_converts_to", References.ITEM_STACK.in(p_361272_)));
        sequencedmap.put("minecraft:custom_name", (Supplier<TypeTemplate>)() -> References.TEXT_COMPONENT.in(p_361272_));
        sequencedmap.put("minecraft:item_name", (Supplier<TypeTemplate>)() -> References.TEXT_COMPONENT.in(p_361272_));
        sequencedmap.put("minecraft:lore", (Supplier<TypeTemplate>)() -> DSL.list(References.TEXT_COMPONENT.in(p_361272_)));
        sequencedmap.put(
            "minecraft:written_book_content",
            (Supplier<TypeTemplate>)() -> DSL.optionalFields(
                "pages",
                DSL.list(
                    DSL.or(
                        DSL.optionalFields("raw", References.TEXT_COMPONENT.in(p_361272_), "filtered", References.TEXT_COMPONENT.in(p_361272_)),
                        References.TEXT_COMPONENT.in(p_361272_)
                    )
                )
            )
        );
        return sequencedmap;
    }

    @Override
    public void registerTypes(Schema p_332951_, Map<String, Supplier<TypeTemplate>> p_332616_, Map<String, Supplier<TypeTemplate>> p_331121_) {
        super.registerTypes(p_332951_, p_332616_, p_331121_);
        p_332951_.registerType(true, References.DATA_COMPONENTS, () -> DSL.optionalFieldsLazy(components(p_332951_)));
    }
}
