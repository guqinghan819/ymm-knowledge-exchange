-- 校验并添加关注关系

local key = KEYS[1] -- 操作的 redis key
local timestamp = ARGV[1] -- 用于排序的时间戳
local followUserId = ARGV[2] -- 关注用户id


-- 使用EXISTS 命令判断 ZSET 是否存在
local exists = redis.call('EXISTS', key)
if exists == 0 then
	return -1
end

-- 使用 ZCARD 校验是否达到关注上限(1000)
local size = redis.call('ZCARD', key)
if size >= 1000 then
	return -2
end

-- ZSCORE获取成员分数 判断是否已经关注该用户
if redis.call('ZSCORE', key, followUserId) then
	return -3
end

-- ZADD 校验并添加关注关系
redis.call('ZADD', key, timestamp, followUserId)
return 0