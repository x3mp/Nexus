package dev.nexus.api.messaging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Base class for all packets exchanged between Nexus components.
 *
 * <p>Packets are serialised to JSON with a {@code type} discriminator field.
 * Use {@link #fromJson(String, Class)} to deserialise a known packet subclass,
 * or read the {@code type} field first with {@link #typeOf(String)} to dispatch
 * before deserialising.
 *
 * @since 1.0.0
 */
public abstract class NexusPacket {

    private static final Gson GSON = new GsonBuilder().create();

    private final PacketType type;

    /**
     * @param type the type discriminator for this packet
     */
    protected NexusPacket(PacketType type) {
        this.type = type;
    }

    /** @return the type discriminator for this packet */
    public PacketType getType() { return type; }

    /**
     * Serialises this packet to a JSON string including the {@code type} field.
     *
     * @return JSON representation of this packet
     */
    public String toJson() {
        return GSON.toJson(this);
    }

    /**
     * Deserialises a JSON string into the given packet subclass.
     *
     * @param <T>        the target packet type
     * @param json       JSON string produced by {@link #toJson()}
     * @param packetClass the concrete packet class to deserialise into
     * @return deserialised packet instance
     */
    public static <T extends NexusPacket> T fromJson(String json, Class<T> packetClass) {
        return GSON.fromJson(json, packetClass);
    }

    /**
     * Reads the {@code type} discriminator from a raw JSON string without full deserialisation.
     *
     * @param json JSON string produced by {@link #toJson()}
     * @return the {@link PacketType} encoded in the JSON, or {@code null} if absent
     */
    public static PacketType typeOf(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        if (!obj.has("type")) return null;
        try {
            return PacketType.valueOf(obj.get("type").getAsString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
