package ennuo.craftworld.types;

import ennuo.craftworld.memory.Bytes;
import ennuo.craftworld.memory.Compressor;
import ennuo.craftworld.types.data.ResourcePtr;
import ennuo.craftworld.memory.Data;
import ennuo.craftworld.memory.FileIO;
import ennuo.craftworld.memory.Output;
import ennuo.craftworld.resources.enums.ResourceType;
import ennuo.craftworld.resources.enums.SerializationType;
import ennuo.craftworld.things.InventoryMetadata;
import ennuo.craftworld.things.Serializer;
import ennuo.craftworld.types.data.Revision;
import ennuo.toolkit.utilities.Globals;
import java.util.Arrays;

public class Resource extends Data {
    public ResourceType type = ResourceType.INVALID;
    public SerializationType method = SerializationType.BINARY;
    
    public boolean isStreamingChunk = false;
    public boolean isCompressed = false;
    
    public int dependencyOffset;
    
    public ResourcePtr[] resources = null;
    public FileEntry[] dependencies = null;

    public Resource(byte[] data) {
        super(data);
        if (this.data != null && this.data.length > 0x16) {
            this.type = ResourceType.fromMagic(str(3));
            if (type == ResourceType.INVALID) return;
            this.method = SerializationType.getValue(str(1));
            if (this.method == SerializationType.UNKNOWN) return;
            this.isCompressed = true;
            this.revision = new Revision(int32f());
            this.dependencyOffset = int32f();
            if (revision.head > 0x26e) this.revision.branch = int32f();
            seek(0);
        }
    }

    // TODO: Actually finish this function, I can serialize the metadata, yes, but //
    // I also need to make sure no dependencies are duplicated in the dependency table, //
    // but I also need to make sure none are removed if they do exist elsewhere. //
    public void replaceMetadata(InventoryMetadata data, boolean compressed) {
        if (this.type != ResourceType.PLAN) return;

        if (compressed)
            decompress(true);

        if (revision.head < 0x272) {
            System.out.println("Metadata below r626 is not supported.");
            return;
        }

        InventoryMetadata oldData = new Serializer(this).DeserializeItem().metadata;

        Output output = new Output(InventoryMetadata.MAX_SIZE);
        Serializer serializer = new Serializer(output);

        if (revision.head <= 0x272) serializer.serializeLegacyMetadata(data, true);
        else serializer.serializeMetadata(data, true);

        output.shrinkToFit();
    }

    public void removePlanDescriptors(long GUID, boolean compressed) {
        if (this.type != ResourceType.PLAN) return;
        if (compressed)
            decompress(true);

        if (peek() == 1 || peek() == 0) bool();
        int32();

        int start = offset;
        seek(0);

        byte[] left = bytes(start);

        int size = int32();

        Data thingData = new Data(bytes(size), revision);

        byte[] right = bytes(length - offset);

        Output output = new Output(0x8, revision);
        output.uint32(GUID);
        output.shrinkToFit();

        Bytes.ReplaceAll(thingData, Bytes.createResourceReference(new ResourcePtr(GUID, ResourceType.PLAN), revision.head), new byte[] { 00 });
        Bytes.ReplaceAll(thingData, output.buffer, new byte[] { 00 });

        Output sb = new Output(6, revision);
        sb.int32(thingData.data.length);
        sb.shrinkToFit();

        setData(Bytes.Combine(
            left,
            sb.buffer,
            thingData.data,
            right
        ));

        if (compressed)
            setData(Compressor.compress(this));
    }

    public void replaceDependency(int index, ResourcePtr replacement, boolean compressed) {
        ResourcePtr dependency = resources[index];
        if (dependency == null || (dependency.GUID == -1 && dependency.hash == null) || dependencies.length == 0) return;

        int tRevision = revision.head;
        if (this.type == ResourceType.STATIC_MESH) tRevision = 0x271;

        byte[] oldRes = Bytes.createResourceReference(dependency, tRevision);
        byte[] newRes = Bytes.createResourceReference(replacement, tRevision);

        if (Arrays.equals(oldRes, newRes)) return;


        if (compressed)
            decompress(true);

        Data data = this;

        if (this.type == ResourceType.PLAN) {

            if (data.peek() == 1 || data.peek() == 0 || isStreamingChunk) data.bool();
            data.int32();

            int start = data.offset;

            data.seek(0);

            byte[] left = data.bytes(start);

            int size = data.int32();

            Data thingData = new Data(data.bytes(size), revision);

            byte[] right = data.bytes(data.length - data.offset);


            Bytes.ReplaceAll(thingData, oldRes, newRes);

            Output output = new Output(6, revision);
            output.int32(thingData.data.length);
            output.shrinkToFit();


            setData(Bytes.Combine(
                left,
                output.buffer,
                thingData.data,
                right
            ));

        }

        Bytes.ReplaceAll(data, oldRes, newRes);

        resources[index] = replacement;

        if (compressed)
            setData(Compressor.compress(this));
    }
    
    public Mod recurse(FileEntry entry) {
        Mod mod = new Mod();
        Bytes.recurse(mod, this, entry);
        return mod;
    }

    public Mod hashinate(FileEntry entry) {
        Mod mod = new Mod();
        Bytes.hashinate(mod, this, entry);
        return mod;
    }

    public int getDependencies(FileEntry entry) {
        return getDependencies(entry, true);
    }
    
    public int getDependencies(FileEntry entry, boolean recursive) {
        if (this.method != SerializationType.BINARY)
            return 0;

        ResourcePtr self = new ResourcePtr();
        if (entry.GUID != -1) self.GUID = entry.GUID;
        else self.hash = entry.hash;

        entry.canReplaceDecompressed = true;
        int missingDependencies = 0;
        seek(8);
        int tableOffset = int32f();
        seek(tableOffset);
        int dependencyCount = int32f();
        if (dependencies == null || dependencyCount != dependencies.length)
            dependencies = new FileEntry[dependencyCount];
        resources = new ResourcePtr[dependencyCount];
        for (int i = 0; i < dependencyCount; i++) {
            resources[i] = new ResourcePtr();
            switch (int8()) {
                case 1:
                    byte[] hash = bytes(20);
                    resources[i].hash = hash;
                    dependencies[i] = Globals.findEntry(hash);
                    break;
                case 2:
                    long GUID = uint32f();
                    resources[i].GUID = GUID;
                    dependencies[i] = Globals.findEntry(GUID);
                    break;
            }
            if (dependencies[i] == null) missingDependencies++;
            resources[i].type = ResourceType.fromType(int32f());
            if (dependencies[i] != null && entry != null && recursive && !self.equals(resources[i])) {
                byte[] data = Globals.extractFile(dependencies[i].hash);
                if (data != null) {
                    Resource resource = new Resource(data);
                    if (resource.type == ResourceType.SCRIPT) continue;
                    resource.getDependencies(dependencies[i]);
                    dependencies[i].dependencies = resource.dependencies;
                }
            }
        }
        seek(0);
        return missingDependencies;
    }
    
    public byte[] compress() { return compress(false); }
    public byte[] decompress() {
        return decompress(false);
    }
    
    public byte[] compress(boolean set) {
        if (this.isCompressed || this.method == SerializationType.TEXT) return this.data;
        if (this.type == ResourceType.INVALID) return null;
        byte[] data = Compressor.compress(this);
        if (set) {
            this.isCompressed = true;
            setData(data);
        }
        return data;
    }
    
    public byte[] decompress(boolean set) {
        if (!this.isCompressed) return this.data;
        if (this.type == ResourceType.INVALID) return null;
        if (this.type == ResourceType.STATIC_MESH) {
            seek(0xD);
            byte[] data = bytes(this.dependencyOffset - this.offset);
            if (set)
                setData(data);
            return data;
        }
        
        switch (this.method) {
            case TEXTURE: {
                if (this.type == ResourceType.GTF_TEXTURE) seek(30);
                else seek(4);
                break;
            }
            case GXT_SIMPLE: seek(30); break;
            case GXT_EXTENDED: seek(50); break;
            case BINARY: {
                seek(12);
                if (this.revision.head > 0x26e)
                    forward(5);
                if (this.revision.head > 0x188)
                    forward(1);
                break;
            }
        }
        
        byte[] data = Compressor.decompress(this);
        if (set) {
            this.isCompressed = false;
            setData(data);
        }
        
        return data;
    }
}
