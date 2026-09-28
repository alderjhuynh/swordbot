package com.aura.swordbotv2.client.bot;

public final class Intents {
    public int f;
    public int s;
    public boolean jump;
    public boolean sprint;
    public boolean attack;
    public boolean use;

    public void clear() {
        f = 0;
        s = 0;
        jump = false;
        sprint = false;
        attack = false;
        use = false;
    }

    public void set(int f, int s, boolean jump, boolean sprint) {
        this.f = f;
        this.s = s;
        this.jump = jump;
        this.sprint = sprint;
    }
}
