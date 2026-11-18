-- 判断当前笔记点赞增量记录

local key = KEYS[1]
local noteIdOrNoteCreatorId = ARGV[1]

-- 检查bloom是否存在
local bloomIsExists = redis.call("EXISTS", key)
if bloomIsExists == 0 then
	-- 创建bloom
	redis.call("BF.ADD", key, '')
	--设置过期时间
	redis.call("EXPIRE", key, 20*60*60)
end

-- 返回笔记id是否已经存在bloom中的结果
return redis.call("BF.EXISTS", key, noteIdOrNoteCreatorId)