local key = KEYS[1]
local noteId = ARGV[1]
local ttl = ARGV[2]

redis.call("BF.ADD", key, noteId)

redis.call("EXPIRE", key, ttl)

return 0