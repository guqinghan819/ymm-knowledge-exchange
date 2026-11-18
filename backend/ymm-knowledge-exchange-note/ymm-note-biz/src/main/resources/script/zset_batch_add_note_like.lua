local key = KEYS[1]

local zaddArgs = {}

for i = 1, #ARGV - 1, 2 do
   table.insert(zaddArgs, ARGV[i]) -- 点赞时间
   table.insert(zaddArgs, ARGV[i+1]) -- noteId
end

redis.call("ZADD", key, unpack(zaddArgs))

redis.call("EXPIRE", key, ARGV[#ARGV])

return 0