package alku.showdown;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class ModFeudManager {
    private static boolean active = false;

    private static final Map<String, Set<EntityType<?>>> teamEntities = new LinkedHashMap<>();
    private static final Map<EntityType<?>, String> entityToTeam = new HashMap<>();
    /** Pre-computed hostile entity set per team name. */
    private static final Map<String, Set<EntityType<?>>> teamHostileCache = new HashMap<>();
    private static final Map<String, Set<EntityType<?>>> modEntityCache = new HashMap<>();

    public static void start(String a, String b) {
        stop();
        teamEntities.put(a, scanEntitiesForMod(a));
        teamEntities.put(b, scanEntitiesForMod(b));
        rebuildTeamMaps();
        active = true;
    }

    public static void applyTeams() {
        teamEntities.clear();
        entityToTeam.clear();
        teamHostileCache.clear();
        active = false;

        Map<String, Set<String>> allTeams = ModTeamManager.getAllTeams();
        for (Map.Entry<String, Set<String>> entry : allTeams.entrySet()) {
            String teamName = entry.getKey();
            Set<String> mods = entry.getValue();
            Set<EntityType<?>> entities = new HashSet<>();
            for (String modId : mods) {
                entities.addAll(scanEntitiesForMod(modId));
            }
            teamEntities.put(teamName, entities);
            for (EntityType<?> type : entities) {
                entityToTeam.put(type, teamName);
            }
        }

        if (!teamEntities.isEmpty()) {
            active = true;
        }
    }

    public static void stop() {
        active = false;
        teamEntities.clear();
        entityToTeam.clear();
        teamHostileCache.clear();
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean belongsToAny(EntityType<?> type) {
        return entityToTeam.containsKey(type);
    }

    public static boolean belongsToTeam(EntityType<?> type) {
        return entityToTeam.containsKey(type);
    }

    public static String getTeamOfEntity(EntityType<?> type) {
        return entityToTeam.get(type);
    }

    public static boolean areHostile(EntityType<?> attackerType, EntityType<?> targetType) {
        if (!active) return false;
        String attackerTeam = entityToTeam.get(attackerType);
        String targetTeam = entityToTeam.get(targetType);
        return attackerTeam != null && targetTeam != null && !attackerTeam.equals(targetTeam);
    }

    public static boolean areSameTeam(EntityType<?> firstType, EntityType<?> secondType) {
        String firstTeam = entityToTeam.get(firstType);
        String secondTeam = entityToTeam.get(secondType);
        return firstTeam != null && firstTeam.equals(secondTeam);
    }

    public static boolean hasHostileEntities(EntityType<?> type) {
        return active && !getHostileEntities(type).isEmpty();
    }

    /**
     * Returns the pre-computed hostile entity set for a given entity type.
     * Uses per-team caching: all entities in the same team share one hostile set.
     */
    public static Set<EntityType<?>> getHostileEntities(EntityType<?> type) {
        if (!active) return Collections.emptySet();
        String myTeam = entityToTeam.get(type);
        if (myTeam == null) return Collections.emptySet();
        return teamHostileCache.computeIfAbsent(myTeam, team -> {
            Set<EntityType<?>> result = new HashSet<>();
            for (Map.Entry<String, Set<EntityType<?>>> entry : teamEntities.entrySet()) {
                if (!entry.getKey().equals(team)) {
                    result.addAll(entry.getValue());
                }
            }
            return result.isEmpty() ? Collections.emptySet() : result;
        });
    }

    private static void rebuildTeamMaps() {
        entityToTeam.clear();
        teamHostileCache.clear();
        for (Map.Entry<String, Set<EntityType<?>>> entry : teamEntities.entrySet()) {
            for (EntityType<?> type : entry.getValue()) {
                entityToTeam.put(type, entry.getKey());
            }
        }
    }

    private static Set<EntityType<?>> scanEntitiesForMod(String modId) {
        return modEntityCache.computeIfAbsent(modId, id -> {
            Set<EntityType<?>> result = new HashSet<>();
            for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES.getValues()) {
                ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(type);
                if (key != null && key.getNamespace().equals(id)) {
                    result.add(type);
                }
            }
            return result;
        });
    }
}