package ennuo.craftworld.memory;

import ennuo.craftworld.resources.enums.ResourceType;
import ennuo.craftworld.resources.enums.SerializationType;
import ennuo.craftworld.types.Resource;
import ennuo.craftworld.types.data.ResourcePtr;
import ennuo.craftworld.types.data.Revision;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class Compressor {
    
    public static byte[] decompressData(byte[] data, int size) {
        try {
            Inflater inflater = new Inflater();
            inflater.setInput(data);
            byte[] chunk = new byte[size];
            inflater.inflate(chunk);
            inflater.end();
            return chunk;
        } catch (DataFormatException ex) {
            Logger.getLogger(Data.class.getName()).log(Level.SEVERE, (String) null, ex);
            return null;
        }
    }

    public static byte[] compressData(byte[] data) {
        try {
            Deflater deflater = new Deflater();
            ByteArrayOutputStream stream = new ByteArrayOutputStream(data.length);
            deflater.setInput(data);
            deflater.finish();
            byte[] chunk = new byte[1024];
            while (!deflater.finished()) {
                int count = deflater.deflate(chunk);
                stream.write(chunk, 0, count);
            }

            stream.close();

            return stream.toByteArray();

        } catch (IOException ex) {
            return null;
        }
    }
    
    public static byte[] decompress(Resource data) {
        if (!data.isCompressed) return data.data;
        
        if (data.method == SerializationType.ENCRYPTED_BINARY) {
            byte[] encrypted = data.bytes(data.int32f());
            data = new Resource(TEA.Decrypt(encrypted));
        }
        
        data.int16(); // Some kind of flag? Irrelevant, moving past! //
        
        short chunks = data.int16();
        
        if (chunks == 0) return data.bytes(data.dependencyOffset - data.offset);
        
        int[] compressed = new int[chunks];
        int[] decompressed = new int[chunks];
        int decompressedSize = 0;
        for (int i = 0; i < chunks; ++i) {
            compressed[i] = data.uint16();
            decompressed[i] = data.uint16();
            decompressedSize += decompressed[i];
        }
        
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream(decompressedSize);
            for (int i = 0; i < chunks; ++i) {
                if (compressed[i] == decompressed[i])
                    stream.write(data.bytes(compressed[i]));
                else
                    stream.write(decompressData(data.bytes(compressed[i]), decompressed[i]));
            }
            return stream.toByteArray();
        } catch (IOException ex) {
            Logger.getLogger(Data.class.getName()).log(Level.SEVERE, (String) null, ex);
            return null;
        }
    }

    public static byte[] compressRaw(byte[] data) {
        if (data == null) return new byte[] {};
        byte[][] chunks = Bytes.Split(data, 0x8000);

        short[] compressedSize = new short[chunks.length];
        short[] uncompressedSize = new short[chunks.length];

        byte[][] zlibStreams = new byte[chunks.length][];
        
        for (int i = 0; i < chunks.length; ++i) {
            byte[] compressed = compressData(chunks[i]);
            zlibStreams[i] = compressed;
            compressedSize[i] = (short) compressed.length;
            uncompressedSize[i] = (short) chunks[i].length;
        }

        Output output = new Output(4 + (chunks.length * 4), 0);
        output.int16((short) 1);
        output.int16((short) zlibStreams.length);

        for (int i = 0; i < zlibStreams.length; ++i) {
            output.int16(compressedSize[i]);
            output.int16(uncompressedSize[i]);
        }

        byte[] compressed = Bytes.Combine(zlibStreams);
        return Bytes.Combine(new byte[][] {
            output.buffer, compressed
        });
    }

    public static byte[] compressStaticMesh(byte[] data, int revision, ResourcePtr[] dependencies) {
        Output output = new Output(0xD);
        output.string("SMHb");
        output.int32f(revision);
        output.int32f(0xD + data.length);
        output.int8(0);

        return Bytes.Combine(output.buffer, data, dependinate(dependencies));

    }

    public static byte[] compress(Resource resource) {
        return Compressor.compress(resource.data, resource.type, resource.method, resource.revision, resource.resources);
    }
    
    public static byte[] compress(byte[] data, ResourceType type, SerializationType method, Revision revision, ResourcePtr[] dependencies) {
        if (type == ResourceType.STATIC_MESH) return compressStaticMesh(data, revision.head, dependencies);
        
        byte[] compressed = compressRaw(data);
        
        byte compressionFlags = 0x7;
        Output output = new Output(18);
        output.string(type.header);
        if (method == SerializationType.ENCRYPTED_BINARY)
            output.string(SerializationType.BINARY.value);
        else output.string(method.value);
        output.int32(revision.head);
        output.int32(output.offset + 4 + compressed.length + ((revision.head > 0x26e) ? 6 : ((revision.head > 0x188) ? 1 : 0)));
        
        if (revision.head > 0x26e) {
            output.int32(revision.branch);
            output.int8(compressionFlags);
        }
        if (revision.head > 0x188)
            output.int8(1);
        
        output.shrinkToFit();
        return Bytes.Combine(new byte[][] {
            output.buffer, compressed, dependinate(dependencies)
        });
    }

    private static byte[] dependinate(ResourcePtr[] resources) {
        Output output = new Output(0x1C * resources.length + 4);

        output.int32(resources.length);
        for (ResourcePtr resource : resources) {
            output.resource(resource, true);
            output.int32(resource.type.value);
        }

        output.shrinkToFit();
        return output.buffer;

    }
}
