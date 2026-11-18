-- redis添加并设置ttl
local key = KEYS[1]
local timestamp = ARGV[1]
local followUserId = ARGV[2]
local ttl = ARGV[3]

-- 添加记录
redis.call('ZADD', key, timestamp, followUserId);
-- 设置过期时间
redis.call('EXPIRE', key, ttl)
return 0

