CREATE TABLE leaderboard_weeks (
 week_start DATETIME(6) NOT NULL PRIMARY KEY,
 week_end DATETIME(6) NOT NULL,
 finalized_at DATETIME(6)
) ENGINE=InnoDB;

CREATE TABLE leaderboard_rewards (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 week_start DATETIME(6) NOT NULL,
 user_id BIGINT NOT NULL,
 rank_position INT NOT NULL CHECK(rank_position BETWEEN 1 AND 10),
 weekly_xp BIGINT NOT NULL CHECK(weekly_xp > 0),
 reward_xp INT NOT NULL CHECK(reward_xp > 0),
 awarded_at DATETIME(6) NOT NULL,
 UNIQUE(week_start,user_id), UNIQUE(week_start,rank_position),
 FOREIGN KEY(week_start) REFERENCES leaderboard_weeks(week_start),
 FOREIGN KEY(user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE INDEX completions_week ON task_completions(completed_at,user_id);
