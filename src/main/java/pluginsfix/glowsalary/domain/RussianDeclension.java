package pluginsfix.glowsalary.domain;

public final class RussianDeclension {

    private RussianDeclension() {}

    public static String plural(long n, String one, String two, String five) {
        long abs = Math.abs(n);
        long mod100 = abs % 100;
        long mod10 = abs % 10;

        if (mod100 >= 11 && mod100 <= 19) {
            return five;
        }

        if (mod10 == 1) {
            return one;
        }

        if (mod10 >= 2 && mod10 <= 4) {
            return two;
        }

        return five;
    }

    public static String formatWithWord(long n, String one, String two, String five) {
        return n + " " + plural(n, one, two, five);
    }
}
