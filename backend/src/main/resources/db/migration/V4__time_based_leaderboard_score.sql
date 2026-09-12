ALTER TABLE task_completions
 ADD COLUMN duration_seconds BIGINT NOT NULL DEFAULT 0,
 ADD COLUMN leaderboard_xp INT NOT NULL DEFAULT 0;

UPDATE task_completions tc
JOIN tasks t ON t.id=tc.task_id
SET tc.duration_seconds=GREATEST(0,TIMESTAMPDIFF(SECOND,t.created_at,tc.completed_at)),
    tc.leaderboard_xp=CASE
      WHEN TIMESTAMPDIFF(SECOND,t.created_at,tc.completed_at)<300 THEN 0
      ELSE tc.xp+LEAST(tc.xp,FLOOR(TIMESTAMPDIFF(SECOND,t.created_at,tc.completed_at)/300)*5)
    END;
