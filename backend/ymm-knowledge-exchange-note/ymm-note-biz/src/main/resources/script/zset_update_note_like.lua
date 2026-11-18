local key = KEYS[1]
local noteId = ARGV[1]
local score = ARGV[2]
-- 判断zSet是否存在
local result = redis.call("EXISTS", key)
if result == 0 then
	return 2 --不存在
end

--添加记录
redis.call("ZADD", key, score, noteId)
return 0
