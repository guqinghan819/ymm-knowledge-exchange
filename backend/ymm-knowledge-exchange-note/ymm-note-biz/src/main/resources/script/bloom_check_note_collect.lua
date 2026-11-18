local key = KEYS[1]
local noteId = ARGV[1]

-- 查看bloom是否存在
local exists = redis.call("EXISTS", key)
if exists == 0 then
	return -1 --不存在
end

-- 查看bloom中是否存在该元素
local result = redis.call("BF.EXISTS", key, noteId)
if result == 0 then
	return 2 -- 笔记Id不存在
end

return result




