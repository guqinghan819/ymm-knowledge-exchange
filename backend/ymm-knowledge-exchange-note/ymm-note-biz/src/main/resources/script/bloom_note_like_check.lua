local key = KEYS[1]
local noteId = ARGV[1] -- 笔记id


-- 判断布隆过滤器是否存在
local exists = redis.call("EXISTS", key)
if exists == 0 then
	return -1 -- 表示不存在
end

-- 判断该笔记是否已经点赞
local result = redis.call("BF.EXISTS", key, noteId)
if result == 1 then
	return 1 --表示已经点赞
end

-- 进行添加
redis.call("BF.ADD", key, noteId)
return 0

