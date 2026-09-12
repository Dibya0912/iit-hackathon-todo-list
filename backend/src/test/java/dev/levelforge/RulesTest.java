package dev.levelforge;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
class RulesTest {
 @Test void leaderboardRejectsInstantCompletionAndCapsTimeBonus(){
  assertEquals(0,Rules.leaderboardXp(10,299));
  assertEquals(13,Rules.leaderboardXp(10,300));
  assertEquals(15,Rules.leaderboardXp(10,600));
  assertEquals(18,Rules.leaderboardXp(10,1800));
  assertEquals(20,Rules.leaderboardXp(10,3600));
  assertEquals(100,Rules.leaderboardXp(50,7200));
 }
 @Test void thresholdsAndOverflow() {
  long[] thresholds={0,100,300,600,1000};
  for(int i=0;i<thresholds.length;i++){var p=Rules.progress(thresholds[i]);assertEquals(i+1,p.level());assertEquals(0,p.current());assertEquals(100*(i+1),p.required());}
  assertEquals(new Rules.Progress(2,15,200,115),Rules.progress(90+25));
  assertEquals(5,Rules.progress(1050).level());assertEquals(50,Rules.progress(1050).current());
  assertThrows(IllegalArgumentException.class,()->Rules.progress(-1));
  assertThrows(IllegalArgumentException.class,()->Rules.progress(Rules.MAX_XP+1));
 }
 @Test void streakDates() {
  LocalDate d=LocalDate.of(2026,3,8);
  assertEquals(1,Rules.nextStreak(null,d,0));assertEquals(4,Rules.nextStreak(d,d,4));
  assertEquals(5,Rules.nextStreak(d,d.plusDays(1),4));assertEquals(1,Rules.nextStreak(d,d.plusDays(2),4));
  assertEquals(4,Rules.visibleStreak(d,d.plusDays(1),4));assertEquals(0,Rules.visibleStreak(d,d.plusDays(2),4));
  assertEquals(4,Rules.nextStreak(d,d.minusDays(1),4));
 }
 @Test void zoneValidationAndDst() {
  assertThrows(ApiError.class,()->Rules.zone("+05:30"));assertThrows(ApiError.class,()->Rules.zone("imaginary"));
  var zone=Rules.zone("America/New_York");var date=LocalDate.of(2026,3,8);
  assertEquals(23,Duration.between(date.atStartOfDay(zone),date.plusDays(1).atStartOfDay(zone)).toHours());
 }
 @Test void attributeMappingAndRewards() {
  assertEquals("INTELLECT",Models.Category.CODING.attribute());assertEquals("INTELLECT",Models.Category.STUDY.attribute());
  assertEquals("STRENGTH",Models.Category.EXERCISE.attribute());assertEquals("WISDOM",Models.Category.READING.attribute());
  assertEquals("FOCUS",Models.Category.MEDITATION.attribute());assertEquals("CHARISMA",Models.Category.COMMUNICATION.attribute());
  assertEquals(10,Models.Difficulty.EASY.xp);assertEquals(10,Models.Difficulty.MEDIUM.gold);assertEquals(50,Models.Difficulty.HARD.xp);
 }
}
