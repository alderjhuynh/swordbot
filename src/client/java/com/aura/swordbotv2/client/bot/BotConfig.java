package com.aura.swordbotv2.client.bot;

public final class BotConfig {
    public enum Skill {
        POTATO(15, 12.0, 6, 6.0, 120, 10),
        AVERAGE(50, 30.0, 3, 2.5, 50, 5),
        PRO(80, 60.0, 2, 0.8, 15, 2),
        ELITE(92, 120.0, 1, 0.3, 5, 1),
        OPTIMAL(100, 180.0, 1, 0.0, 0, 0);

        public final int skill;
        public final double turnDegPerTick;
        public final int thinkTicks;
        public final double aimErrDeg;
        public final int mistakePerMille;
        public final int reactTicks;

        Skill(int skill, double turnDegPerTick, int thinkTicks, double aimErrDeg,
                int mistakePerMille, int reactTicks) {
            this.skill = skill;
            this.turnDegPerTick = turnDegPerTick;
            this.thinkTicks = thinkTicks;
            this.aimErrDeg = aimErrDeg;
            this.mistakePerMille = mistakePerMille;
            this.reactTicks = reactTicks;
        }
    }

    public enum Style {
        AGGRESSIVE(85, 25),
        BALANCED(50, 50),
        DEFENSIVE(30, 80);

        public final int aggression;
        public final int caution;

        Style(int aggression, int caution) {
            this.aggression = aggression;
            this.caution = caution;
        }
    }

    public Skill skill = Skill.OPTIMAL;
    public Style style = Style.BALANCED;

    public double gapHpThreshold = 12.0;
    public double critHpThreshold = 7.0;
    public boolean allowBow = true;
    public boolean allowEat = true;
    public double bowMinDist = 13.0;
    public double bowMaxDist = 30.0;
    public double punishEaterDist = 6.0;
    public boolean autoTarget = false;
    public double autoTargetRange = 100.0;

    private BotConfig() {
    }

    public static BotConfig defaults() {
        return new BotConfig();
    }
}
