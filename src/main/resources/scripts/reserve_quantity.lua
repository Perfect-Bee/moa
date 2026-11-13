-- 참여 수량 선점 (원자적 실행: 끝날 때까지 다른 명령이 끼어들 수 없음)
-- KEYS[1] = groupbuy:{id}:count, KEYS[2] = groupbuy:{id}:max, ARGV[1] = 참여 수량
local max = tonumber(redis.call('GET', KEYS[2]))
if max == nil then
    return -2            -- 초기화 안 됨
end
local current = tonumber(redis.call('GET', KEYS[1]) or '0')
local qty = tonumber(ARGV[1])
if current + qty > max then
    return -1            -- 최대 수량 초과
end
return redis.call('INCRBY', KEYS[1], qty)  -- 선점 후 수량