package net.optifine.entity.model.anim;

import java.util.HashMap;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.optifine.render.RenderState;

public class EntityVariableBool implements IModelVariableBool {
    private String name;

    public EntityVariableBool(String name) {
        this.name = name;
    }

    @Override
    public boolean eval() {
        return this.getValue();
    }

    @Override
    public boolean getValue() {
        SynchedEntityData synchedentitydata = this.getEntityData();
        if (synchedentitydata == null) {
            return false;
        }

        if (synchedentitydata.modelVariables == null) {
            return false;
        }

        Boolean obool = (Boolean)synchedentitydata.modelVariables.get(this.name);
        return obool == null ? false : obool;
    }

    @Override
    public void setValue(boolean value) {
        SynchedEntityData synchedentitydata = this.getEntityData();
        if (synchedentitydata != null) {
            if (synchedentitydata.modelVariables == null) {
                synchedentitydata.modelVariables = new HashMap<>();
            }

            synchedentitydata.modelVariables.put(this.name, value);
        }
    }

    private SynchedEntityData getEntityData() {
        Entity entity = RenderState.getEntity();
        return entity == null ? null : entity.getEntityData();
    }
}
