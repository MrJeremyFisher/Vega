package ca.favro.vega.common.waypoint;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Type;
import java.util.UUID;

public record VegaPlayer(String name, UUID uuid, Vec3 position, String world, long time, Source source) {

    public static class VegaPlayerSerializer implements JsonSerializer<VegaPlayer> {
        @Override
        public JsonElement serialize(VegaPlayer src, Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("time", src.time);
            obj.addProperty("name", src.name);
            obj.addProperty("uuid", String.valueOf(src.uuid));
            JsonObject position = new JsonObject();
            position.addProperty("x", src.position.x);
            position.addProperty("y", src.position.y);
            position.addProperty("z", src.position.z);
            position.addProperty("world", src.world);
            obj.add("position", position);

            return obj;
        }
    }

    public static class VegaPlayerDeserializer implements JsonDeserializer<VegaPlayer> {
        @Override
        public VegaPlayer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String name = json.getAsJsonObject().get("name").getAsString();
            UUID uuid = UUID.fromString(json.getAsJsonObject().get("uuid").getAsString());

            JsonObject positionObject = json.getAsJsonObject().get("position").getAsJsonObject();
            Vec3 position = new Vec3(
                    positionObject.get("x").getAsDouble(),
                    positionObject.get("y").getAsDouble(),
                    positionObject.get("z").getAsDouble()
            );
            String world = positionObject.get("world").getAsString();
            long time = json.getAsJsonObject().get("time").getAsLong();

            // Only time deserialized is when coming from remote
            return new VegaPlayer(name, uuid, position, world, time, Source.REMOTE);
        }
    }


    public enum Source {
        LOCAL, // Waypoint comes from an entity in view distance. Should not be shared unless the waypoint is for the local player
        SNITCH, // Waypoint comes from a snitch hit
        REMOTE, // Waypoint comes from remote server
    }
}

