local key = KEYS[1]
local args = {}

for i = 1, #ARGV -1, 2 do
	table.insert(args, ARGV[i]) -- 收藏时间
    table.insert(args, ARGV[i+1]) -- noteId
end

redis.call("ZADD", key, unpack(args))

redis.call("EXPIRE", key, ARGV[#ARGV])

return 0
