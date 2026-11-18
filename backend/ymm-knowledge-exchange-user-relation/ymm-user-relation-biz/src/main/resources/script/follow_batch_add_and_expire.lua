-- 将db中的数据同步至redis中

local key = KEYS[1]
local zaddArgs = {}

-- 遍历ARGV参数，将时间戳和关注用户顺序插入到 zaddArgs中
-- 2 步长
-- #ARGV - 1 因为最后一个元素是过期时间 ttl
for i = 1, #ARGV - 1, 2 do
	table.insert(zaddArgs, ARGV[i]) -- 时间戳
	table.insert(zaddArgs, ARGV[i+1]) -- 关注用户
end

-- 插入
redis.call('ZADD', key, unpack(zaddArgs))

-- 设置过期时间
local ttl = ARGV[#ARGV]
redis.call('EXPIRE', key, ttl)

return 0