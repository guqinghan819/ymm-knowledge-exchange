local key = KEYS[1]

for i = 1, #ARGV - 1 do
 redis.call("BF.ADD", key, ARGV[i])
end

redis.call("EXPIRE", key, ARGV[#ARGV]) --设置过期时间
return 0