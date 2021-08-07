package ennuo.craftworld.things.parts;

import ennuo.craftworld.resources.enums.ResourceType;
import ennuo.craftworld.types.data.ResourcePtr;
import ennuo.craftworld.things.Part;
import ennuo.craftworld.things.Serializer;

public class PScript implements Part {
    public ResourcePtr script = new ResourcePtr(null, ResourceType.SCRIPT);
    public ScriptInstance instanceLayout;
    
    
    @Override
    public void Serialize(Serializer serializer) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void Deserialize(Serializer serializer) {
        script = serializer.input.resource(ResourceType.SCRIPT);
        if (serializer.input.bool())
            instanceLayout = (ScriptInstance) serializer.deserializePart("SCRIPTINSTANCE");
    }
    
}
