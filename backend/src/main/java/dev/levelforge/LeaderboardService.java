package dev.levelforge;

import dev.levelforge.Dtos.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class LeaderboardService {
    private final JdbcTemplate jdbc; private final Clock clock;
    LeaderboardService(JdbcTemplate jdbc,Clock clock){this.jdbc=jdbc;this.clock=clock;}
    private static Instant start(Instant instant){return instant.atZone(ZoneOffset.UTC).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(ZoneOffset.UTC).toInstant();}
    private static int reward(int rank){return rank==1?1000:rank==2?500:rank==3?200:rank<=10?100:0;}
    private static Timestamp ts(Instant i){return Timestamp.from(i);}

    @Transactional
    public void prepareForCompletion(){finalizeExpired();ensure(start(clock.instant()));}

    @Transactional
    public LeaderboardView leaderboard(long currentUser,int page,int size){
        finalizeExpired();Instant week=start(clock.instant()),end=week.plus(7,java.time.temporal.ChronoUnit.DAYS);ensure(week);
        List<LeaderboardEntry> all=rankings(week,end,currentUser);
        int safeSize=Math.clamp(size,1,50),pages=(int)Math.ceil(all.size()/(double)safeSize),safePage=Math.max(0,Math.min(page,Math.max(0,pages-1)));
        int from=Math.min(safePage*safeSize,all.size()),to=Math.min(from+safeSize,all.size());
        LeaderboardEntry mine=all.stream().filter(LeaderboardEntry::currentUser).findFirst().orElseThrow(ApiError::missing);
        return new LeaderboardView(week,end,Math.max(0,Duration.between(clock.instant(),end).toSeconds()),new PageView<>(all.subList(from,to),safePage,pages,all.size()),mine);
    }

    private List<LeaderboardEntry> rankings(Instant week,Instant end,long currentUser){
        String sql="""
          SELECT u.id,u.display_name,c.avatar,c.total_xp,COALESCE(SUM(tc.leaderboard_xp),0) weekly_xp
          FROM users u JOIN characters c ON c.user_id=u.id
          LEFT JOIN task_completions tc ON tc.user_id=u.id AND tc.completed_at>=? AND tc.completed_at<?
          GROUP BY u.id,u.display_name,c.avatar,c.total_xp ORDER BY weekly_xp DESC,u.id ASC
          """;
        var rows=jdbc.query(sql,(r,n)->new Object[]{r.getLong(1),r.getString(2),r.getString(3),r.getLong(4),r.getLong(5)},ts(week),ts(end));
        List<LeaderboardEntry> result=new ArrayList<>();int rank=1;
        for(Object[] row:rows){long id=(long)row[0],xp=(long)row[4];result.add(new LeaderboardEntry(rank,id,(String)row[1],(String)row[2],Rules.progress((long)row[3]),xp,id==currentUser,xp>0?reward(rank):0));rank++;}
        return result;
    }

    private void ensure(Instant week){jdbc.update("INSERT IGNORE INTO leaderboard_weeks(week_start,week_end) VALUES (?,?)",ts(week),ts(week.plus(7,java.time.temporal.ChronoUnit.DAYS)));}

    private void finalizeExpired(){
        Instant now=clock.instant();
        List<Instant> weeks=jdbc.query("SELECT week_start FROM leaderboard_weeks WHERE finalized_at IS NULL AND week_end<=? ORDER BY week_start",(r,n)->r.getTimestamp(1).toInstant(),ts(now));
        for(Instant week:weeks)finalizeWeek(week,now);
    }

    private void finalizeWeek(Instant week,Instant now){
        jdbc.queryForObject("SELECT week_start FROM leaderboard_weeks WHERE week_start=? FOR UPDATE",(r,n)->r.getTimestamp(1),ts(week));
        Integer done=jdbc.queryForObject("SELECT COUNT(*) FROM leaderboard_weeks WHERE week_start=? AND finalized_at IS NOT NULL",Integer.class,ts(week));
        if(done!=null&&done>0)return;
        String sql="""
          SELECT u.id,COALESCE(SUM(tc.leaderboard_xp),0) weekly_xp
          FROM users u JOIN task_completions tc ON tc.user_id=u.id AND tc.completed_at>=? AND tc.completed_at<?
          GROUP BY u.id HAVING weekly_xp>0 ORDER BY weekly_xp DESC,u.id ASC LIMIT 10
          """;
        var winners=jdbc.query(sql,(r,n)->new long[]{r.getLong(1),r.getLong(2)},ts(week),ts(week.plus(7,java.time.temporal.ChronoUnit.DAYS)));
        int rank=1;
        for(long[] winner:winners){
            int bonus=reward(rank);
            jdbc.update("UPDATE characters SET total_xp=LEAST(1000000000,total_xp+?) WHERE user_id=?",bonus,winner[0]);
            jdbc.update("INSERT INTO leaderboard_rewards(week_start,user_id,rank_position,weekly_xp,reward_xp,awarded_at) VALUES (?,?,?,?,?,?)",ts(week),winner[0],rank,winner[1],bonus,ts(now));
            jdbc.update("INSERT INTO economy_ledger(user_id,kind,description,xp,gold,created_at) VALUES (?,?,?,?,0,?)",winner[0],"LEADERBOARD","Weekly leaderboard #"+rank,bonus,ts(now));rank++;
        }
        jdbc.update("UPDATE leaderboard_weeks SET finalized_at=? WHERE week_start=?",ts(now),ts(week));
    }
}
