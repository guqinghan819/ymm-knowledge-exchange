local key = KEYS[1]
local score = ARGV[1]
local noteId = ARGV[2]

-- 判断zset是否存在
local zsetExists = redis.call("EXISTS", key)
if zsetExists == 0 then
	return -1 -- zset不存在
end

redis.call("ZADD", key, score, noteId)
return 0
