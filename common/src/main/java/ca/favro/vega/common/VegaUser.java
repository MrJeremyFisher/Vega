package ca.favro.vega.common;

import ca.favro.vega.common.waypoint.VegaPlayer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.UUID;

public record VegaUser(String name, UUID uuid, int status, int permission) {
    public static class VegaUserSerializer implements JsonSerializer<VegaUser> {
        @Override
        public JsonElement serialize(VegaUser src, Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", src.name);
            obj.addProperty("uuid", String.valueOf(src.uuid));
            obj.addProperty("status", src.status);
            obj.addProperty("permission", src.permission);

            return obj;
        }
    }

    public VegaUser withStatus(int status) {
        return new VegaUser(name, uuid, status, permission);
    }

    public VegaUser withPermission(int permission) {
        return new VegaUser(name, uuid, status, permission);
    }

    public enum Status {
        NEUTRAL,
        ALLY,
        WATCH,
        ENEMY
    }
}
