import http from 'k6/http';

const BASE_URL = 'http://localhost:8080';

const TOKEN = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhcnlhbnBhdGVsMjU5M0BnbWFpbC5jb20iLCJpYXQiOjE3ODk0NTY1MjIsImV4cCI6MTc4OTQ2MDEyMn0.ciczHBSeQrYs0bklCokMN-lfGTShevooZ4UaL9aHZog';
export const options = {
    vus: 10,
    iterations: 1000,
};

export default function () {
    const response = http.get(`${BASE_URL}/api/v1/urls`, {
        headers: {
            Authorization: `Bearer ${TOKEN}`,
        },
    });

    if (response.status !== 200) {
        console.log(`Request failed: ${response.status} ${response.body}`);
    }
}