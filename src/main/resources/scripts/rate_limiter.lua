local key = KEYS[1]

local capacity = tonumber(ARGV[1])
local refill_interval = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local ttl = tonumber(ARGV[4])

local tokens = redis.call("HGET", key, "tokens")
local last_refill = redis.call("HGET", key, "last_refill")

if not tokens or not last_refill then
    tokens = capacity
    last_refill = now
else
    tokens = tonumber(tokens)
    last_refill = tonumber(last_refill)

    local elapsed = now - last_refill
    local refills = math.floor(elapsed / refill_interval)

    if refills > 0 then
        tokens = math.min(capacity, tokens + refills)
        last_refill = last_refill + (refills * refill_interval)
    end
end

local allowed = 0
local retry_after = 0

if tokens >= 1 then
    tokens = tokens - 1
    allowed = 1
else
    retry_after = math.ceil((refill_interval - (now - last_refill)) / 1000)
end

redis.call("HSET", key,
    "tokens", tokens,
    "last_refill", last_refill
)

redis.call("EXPIRE", key, ttl)

return {
    allowed,
    tokens,
    retry_after
}