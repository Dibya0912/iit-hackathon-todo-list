package dev.levelforge;
import dev.levelforge.Models.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
public final class Dtos {
    public record Signup(@NotBlank @Size(max=60) String displayName,@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(min=10,max=72) String password,@NotBlank String timezone) {}
    public record Login(@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(max=72) String password) {}
    public record Profile(@NotBlank @Size(max=60) String displayName,@NotBlank String timezone,@Pattern(regexp="RANGER|MAGE|KNIGHT") String avatar) {}
    public record QuestInput(@NotBlank @Size(max=120) String title,@Size(max=1000) String description,@NotNull Category category,@NotNull Difficulty difficulty,LocalDate dueDate) {}
    public record UserView(long id,String displayName,String email) {}
    public record Quest(long id,String title,String description,Category category,Difficulty difficulty,LocalDate dueDate,boolean completed,boolean archived,Instant createdAt,Instant updatedAt,Instant completedAt,int rewardXp,int rewardGold) {
        static Quest of(Task t){return new Quest(t.id,t.title,t.description,t.category,t.difficulty,t.dueDate,t.completed,t.archived,t.createdAt,t.updatedAt,t.completedAt,t.difficulty.xp,t.difficulty.gold);}
    }
    public record AttributeView(String type,Rules.Progress progress) {}
    public record ItemView(long id,String name,String slot,String appearance,String description,int price,Long inventoryId,boolean owned,boolean equipped) {}
    public record HeroView(String displayName,String avatar,Rules.Progress progress,long gold,int streak,int bestStreak,String timezone,String pendingTimezone,List<AttributeView> attributes,List<ItemView> cosmetics) {}
    public record CompletionResult(boolean applied,int xp,int gold,boolean levelUp,HeroView character) {}
    public record PurchaseResult(boolean applied,HeroView character) {}
    public record Activity(long id,String kind,String description,long xp,long gold,Instant createdAt) {}
    public record LeaderboardEntry(int rank,long userId,String displayName,String avatar,Rules.Progress progress,long weeklyXp,boolean currentUser,int projectedRewardXp) {}
    public record LeaderboardView(Instant weekStart,Instant weekEnd,long secondsRemaining,PageView<LeaderboardEntry> rankings,LeaderboardEntry currentUser) {}
    public record PageView<T>(List<T> content,int page,int totalPages,long totalElements) {}
}

