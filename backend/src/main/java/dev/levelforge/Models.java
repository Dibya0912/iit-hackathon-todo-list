package dev.levelforge;

import jakarta.persistence.*;
import java.time.*;

public final class Models {
    private Models() {}
    public enum Category { CODING, STUDY, EXERCISE, READING, MEDITATION, COMMUNICATION;
        public String attribute() { return switch(this) { case CODING, STUDY -> "INTELLECT"; case EXERCISE -> "STRENGTH"; case READING -> "WISDOM"; case MEDITATION -> "FOCUS"; case COMMUNICATION -> "CHARISMA"; }; }
    }
    public enum Difficulty { EASY(10,5), MEDIUM(25,10), HARD(50,20);
        public final int xp, gold; Difficulty(int xp,int gold){this.xp=xp;this.gold=gold;}
    }
    @Entity @Table(name="users")
    public static class User {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        @Column(nullable=false,unique=true,length=254) public String email;
        @Column(nullable=false) public String passwordHash;
        @Column(nullable=false,length=60) public String displayName;
        @Column(nullable=false) public Instant createdAt=Instant.now();
    }
    @Entity @Table(name="characters")
    public static class Hero {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        @Column(nullable=false,unique=true) public Long userId;
        public long totalXp; public long gold;
        public int streak; public int bestStreak;
        public LocalDate lastActivityDate;
        public Instant lastActivityAt;
        public Instant dayWindowEnd;
        @Column(nullable=false,length=80) public String timezone;
        @Column(length=80) public String pendingTimezone;
        @Column(nullable=false,length=20) public String avatar="RANGER";
    }
    @Entity @Table(name="character_attributes",uniqueConstraints=@UniqueConstraint(columnNames={"characterId","type"}))
    public static class Attribute {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        public Long characterId;
        @Column(length=20) public String type;
        public long xp;
    }
    @Entity @Table(name="tasks")
    public static class Task {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        @Column(nullable=false) public Long userId;
        @Column(nullable=false,length=120) public String title;
        @Column(length=1000) public String description;
        @Enumerated(EnumType.STRING) @Column(length=20) public Category category;
        @Enumerated(EnumType.STRING) @Column(length=10) public Difficulty difficulty;
        public LocalDate dueDate;
        public boolean completed;
        public boolean archived;
        public Instant createdAt=Instant.now();
        public Instant updatedAt=Instant.now();
        public Instant completedAt;
    }
    @Entity @Table(name="task_completions")
    public static class Completion {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        @Column(unique=true,nullable=false) public Long taskId;
        public Long userId;
        @Column(length=120) public String title;
        @Column(length=20) public String category;
        public int xp; public int gold;
        public Instant completedAt;
        public LocalDate localDate;
        @Column(length=80) public String timezone;
    }
    @Entity @Table(name="activity_days",uniqueConstraints=@UniqueConstraint(columnNames={"characterId","windowStart"}))
    public static class ActivityDay {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        public Long characterId;
        public LocalDate localDate;
        @Column(length=80) public String timezone;
        public Instant windowStart;
        public Instant windowEnd;
        public Instant createdAt;
    }
    @Entity @Table(name="shop_items")
    public static class Item {
        @Id public Long id;
        @Column(length=60) public String name;
        @Column(length=10) public String slot;
        @Column(length=30) public String appearance;
        @Column(length=200) public String description;
        public int price;
    }
    @Entity @Table(name="inventory",uniqueConstraints=@UniqueConstraint(columnNames={"characterId","itemId"}))
    public static class Owned {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        public Long characterId;
        public Long itemId;
        public boolean equipped;
        public Instant createdAt=Instant.now();
    }
    @Entity @Table(name="economy_ledger")
    public static class Ledger {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        public Long userId;
        @Column(length=20) public String kind;
        @Column(length=160) public String description;
        public long xp; public long gold;
        public Long taskId; public Long itemId;
        public Instant createdAt=Instant.now();
    }
}

