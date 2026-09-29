package com.aura.swordbotv2.client.bot;

final class PursueBrain {
    boolean tick(SenseState sense, Targeting targeting, Intents out) {
        out.clear();
        if (sense.target == null) {
            return false;
        }
        if (sense.targetVisible) {
            boolean far = sense.hdist >= 7.0;
            boolean opening = sense.hdist >= 5.0 && sense.radialVel < -0.1;
            if (!far && !opening) {
                return false;
            }
            out.f = 1;
            out.sprint = true;
            double closing = 0.35 + Math.max(0, sense.radialVel);
            boolean foeKbClose = sense.foeKbLevel > 0 && sense.hdist <= 6.0;
            if (!foeKbClose && sense.hdist - 12.0 * closing >= 3.6 && sense.onGround) {
                out.jump = true;
            }
            return true;
        }
        if (!targeting.hasLastSeen()) {
            return false;
        }
        out.f = 1;
        out.sprint = true;
        if (sense.hdist >= 4.0 && sense.onGround
                && !(sense.foeKbLevel > 0 && sense.hdist <= 6.0)) {
            out.jump = true;
        }
        return true;
    }
}
