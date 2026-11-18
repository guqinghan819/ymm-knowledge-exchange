local key = KEYS[1]
local noteId = ARGV[1]
local ttl = ARGV[2]

-- BF.ADD 也会创建一个布隆过滤器，使用 BF.RESERVE 可以指定初始化的配置
redis.call("BF.ADD", key, noteId)

-- 设置过期时间
redis.call("EXPIRE", key, ttl)
return 0