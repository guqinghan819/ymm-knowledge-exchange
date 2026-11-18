-- 用于校验或添加
local key = KEYS[1]
local noteId = ARGV[1]

local bloomExists = redis.call("EXISTS", key)
if bloomExists == 0 then
	return -1 -- 说明bloom不存在
end

local exists = redis.call("BF.EXISTS", key, noteId)
if exists == 1 then
	return 1 -- 说明笔记已经收藏(可能出现误判)
end

--加入
redis.call("BF.ADD", key, noteId)
return 0