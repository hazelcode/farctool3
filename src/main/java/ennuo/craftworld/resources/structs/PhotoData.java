package ennuo.craftworld.resources.structs;

import ennuo.craftworld.memory.Data;
import ennuo.craftworld.memory.Output;
import ennuo.craftworld.resources.enums.ResourceType;
import ennuo.craftworld.types.data.ResourcePtr;

public class PhotoData {
    public static int MAX_SIZE = 0x69 + PhotoMetadata.MAX_SIZE + (PhotoUser.MAX_SIZE * 4);
    
    public ResourcePtr icon;
    public ResourcePtr sticker;
    public PhotoMetadata photoMetadata = new PhotoMetadata();
    public ResourcePtr painting;
    
    public PhotoData() {}
    public PhotoData(Data data) {
        icon = data.resource(ResourceType.TEXTURE, true);
        sticker = data.resource(ResourceType.TEXTURE, true);
        photoMetadata = new PhotoMetadata(data);
        if (data.revision.head > 0x395)
            painting = data.resource(ResourceType.PAINTING, true);
    }
    
    public void serialize(Output output) {
        output.resource(icon, true);
        output.resource(sticker, true);
        photoMetadata.serialize(output);
        if (output.revision.head > 0x395)
            output.resource(painting, true);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof PhotoData)) return false;
        PhotoData d = (PhotoData)o;
        return (
                icon.equals(d.icon) &&
                sticker.equals(d.sticker) &&
                photoMetadata.equals(d.photoMetadata) &&
                painting.equals(d.painting)
        );
    }
}
