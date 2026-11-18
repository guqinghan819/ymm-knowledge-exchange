local key = KEYS[1]
local unfollowUserId = ARGV[1]

-- 检查对应的zset是否存在
local exists = redis.call('EXISTS', key)

if exists == 0 then
    return -1
end

-- 检查目标用户是否被关注
local score = redis.call('ZSCORE', key, unfollowUserId)
if score == false or score == nil then
    return -4
end

-- 删除
redis.call('ZREM', key, unfollowUserId)
return 0


