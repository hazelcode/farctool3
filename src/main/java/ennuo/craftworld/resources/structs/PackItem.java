package ennuo.craftworld.resources.structs;

import ennuo.craftworld.memory.Data;
import ennuo.craftworld.memory.Output;
import ennuo.craftworld.types.data.ResourcePtr;
import ennuo.craftworld.resources.enums.ContentsType;
import ennuo.craftworld.resources.enums.ResourceType;
import java.util.Date;

public class PackItem {
    
    public static int MAX_SIZE = 0x125 + Slot.MAX_SIZE;
    
    public ContentsType contentsType = ContentsType.LEVEL;
    public ResourcePtr mesh = new ResourcePtr(16006, ResourceType.MESH);
    public Slot slot = new Slot();
    public String contentID = "";
    public long timestamp = new Date().getTime() * 2 / 1000;
    public boolean crossBuyCompatible = false;
    
    public PackItem() {}
    public PackItem(Data data) {
        contentsType = ContentsType.getValue(data.int32());
        mesh = data.resource(ResourceType.MESH, true);
        slot = new Slot(data, true, false);
        contentID = data.str8();
        timestamp = data.uint32();
        if (data.revision.head == 0x3e2)
            crossBuyCompatible = data.bool();
    }
    
    public void serialize(Output output) {
        output.int8(contentsType.value);
        output.resource(mesh, true);
        slot.serialize(output, true, false);
        output.str8(contentID);
        output.uint32(timestamp);
        if (output.revision.head == 0x3e2)
            output.bool(crossBuyCompatible);
    }
}
