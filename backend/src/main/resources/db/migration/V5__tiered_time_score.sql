UPDATE task_completions
SET leaderboard_xp=CASE
 WHEN duration_seconds<300 THEN 0
 WHEN duration_seconds<600 THEN CEIL(xp*1.25)
 WHEN duration_seconds<1800 THEN CEIL(xp*1.50)
 WHEN duration_seconds<3600 THEN CEIL(xp*1.75)
 ELSE xp*2
END;
