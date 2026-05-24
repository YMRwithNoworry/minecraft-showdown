package alku.showdown;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class FeudStatistics {

    private static boolean recording = false;
    private static long startTime = 0;

    private static final Map<String, Long> deaths = new LinkedHashMap<>();
    private static final Map<String, Long> kills = new LinkedHashMap<>();

    public static void start() {
        recording = true;
        startTime = System.currentTimeMillis();
        deaths.clear();
        kills.clear();
    }

    public static Map<String, long[]> stop() {
        recording = false;
        Map<String, long[]> snapshot = new LinkedHashMap<>();
        Set<String> allTeams = new LinkedHashSet<>();
        allTeams.addAll(deaths.keySet());
        allTeams.addAll(kills.keySet());
        for (String team : allTeams) {
            snapshot.put(team, new long[]{
                kills.getOrDefault(team, 0L),
                deaths.getOrDefault(team, 0L)
            });
        }
        return snapshot;
    }

    public static boolean isRecording() {
        return recording;
    }

    public static long getStartTime() {
        return startTime;
    }

    public static void record(String killerTeam, String victimTeam) {
        if (!recording) return;
        kills.merge(killerTeam, 1L, Long::sum);
        deaths.merge(victimTeam, 1L, Long::sum);
    }

    public static long getKills(String team) {
        return kills.getOrDefault(team, 0L);
    }

    public static long getDeaths(String team) {
        return deaths.getOrDefault(team, 0L);
    }
}
