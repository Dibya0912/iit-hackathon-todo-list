package dev.levelforge;
import java.time.*;
public final class Rules {
    private Rules() {}
    public static final long MAX_XP=1_000_000_000L;
    public record Progress(int level,long current,long required,long total) {}
    public static Progress progress(long xp) {
        if(xp<0 || xp>MAX_XP) throw new IllegalArgumentException("XP out of bounds");
        int level=1;
        while(50L*level*(level+1)<=xp) level++;
        return new Progress(level,xp-50L*level*(level-1),100L*level,xp);
    }
    public static int nextStreak(LocalDate last,LocalDate today,int streak) {
        if(last==null) return 1;
        if(!today.isAfter(last)) return streak;
        return last.plusDays(1).equals(today)?streak+1:1;
    }
    public static int visibleStreak(LocalDate last,LocalDate today,int streak) {
        return last==null || last.plusDays(1).isBefore(today)?0:streak;
    }
    public static int leaderboardXp(int difficultyXp,long elapsedSeconds) {
        if(elapsedSeconds<300)return 0;
        if(elapsedSeconds<600)return (int)Math.ceil(difficultyXp*1.25);
        if(elapsedSeconds<1800)return (int)Math.ceil(difficultyXp*1.50);
        if(elapsedSeconds<3600)return (int)Math.ceil(difficultyXp*1.75);
        return difficultyXp*2;
    }
    public static ZoneId zone(String value) {
        if(value==null || !ZoneId.getAvailableZoneIds().contains(value)) throw new ApiError(400,"INVALID_TIMEZONE","Choose a valid IANA timezone.");
        return ZoneId.of(value);
    }
}

