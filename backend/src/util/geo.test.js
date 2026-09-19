const test = require('node:test');
const assert = require('node:assert');
const { haversineKm } = require('./geo');

test('distance between identical points is 0', () => {
    assert.strictEqual(haversineKm(-26.2041, 28.0473, -26.2041, 28.0473), 0);
});

test('Johannesburg to Cape Town is roughly 1270km (within 5%)', () => {
    // JHB: -26.2041, 28.0473 | CPT: -33.9249, 18.4241
    const distance = haversineKm(-26.2041, 28.0473, -33.9249, 18.4241);
    assert.ok(Math.abs(distance - 1270) / 1270 < 0.05, `expected ~1270km, got ${distance}`);
});

test('distance is symmetric', () => {
    const a = haversineKm(-26.2041, 28.0473, -29.8587, 31.0218);
    const b = haversineKm(-29.8587, 31.0218, -26.2041, 28.0473);
    assert.strictEqual(a, b);
});
