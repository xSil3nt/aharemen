package app.shazin.aharemen;

/** Mirrors Mamen 1.2.8 x.p.g: raw KB -> whole MB; raw seconds -> whole minutes. */
final class Allowance {
    static double preciseData(double raw) { return raw / 1024.0; }
    static double preciseVoice(double raw) { return raw / 60.0; }
    static long data(long raw) { return Math.round(raw / 1024.0f); }
    static long voice(long raw) { return (long) Math.floor(raw / 60.0f); }
    static long totalData(long... rawPlans) {
        long result = 0;
        for (long plan : rawPlans) result += data(plan);
        return result;
    }
}
