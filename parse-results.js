const fs = require('fs');
const readline = require('readline');

async function run() {
    const files = fs.readdirSync('results').filter(f => f.endsWith('_k6.json'));
    for (let f of files) {
        let total = 0;
        let failed = 0;
        let rateLimited = 0;
        const stream = fs.createReadStream('results/' + f);
        const rl = readline.createInterface({ input: stream });
        for await (const line of rl) {
            if (!line.trim()) continue;
            const obj = JSON.parse(line);
            if (obj.type === 'Point' && obj.metric === 'http_reqs') {
                total++;
                if (obj.data && obj.data.tags && obj.data.tags.status) {
                    const status = parseInt(obj.data.tags.status, 10);
                    if (status >= 500) failed++;
                    if (status === 429) rateLimited++;
                }
            }
        }
        console.log(`${f}: Total Requests=${total}, 5xx Errors=${failed}, 429 Rate Limited=${rateLimited}`);
    }
}
run();
