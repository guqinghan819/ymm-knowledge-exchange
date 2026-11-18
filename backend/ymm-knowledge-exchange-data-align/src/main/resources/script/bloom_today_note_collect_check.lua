-- 检查笔记收藏日增量
local key = KEYS[1]
local noteIdOrNoteCreatorId = ARGV[1]

--判断bloom是否存在
local bloomIsExists = redis.call("EXISTS", key)
if bloomIsExists == 0 then
	-- 不存在就创建
	redis.call("BF.ADD", key, '')
	redis.call("EXPIRE", key, 20 * 60 * 60)
end

return redis.call("BF.EXISTS", key, noteIdOrNoteCreatorId)