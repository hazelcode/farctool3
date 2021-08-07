package ennuo.craftworld.resources.structs;

import ennuo.craftworld.memory.Data;
import ennuo.craftworld.memory.Output;
import ennuo.craftworld.resources.enums.ResourceType;
import ennuo.craftworld.types.data.ResourcePtr;

public class Collectable {
    public static int MAX_SIZE = 0x15;
    
    
    public ResourcePtr item = new ResourcePtr();
    public int count = 0;
    
    public Collectable() {}
    public Collectable(Data data) {
        item = data.resource(ResourceType.PLAN, true);
        count = data.int32();
    }
    
    public void serialize(Output output) {
        output.resource(item, true);
        output.int32(count);
    }
    
}
