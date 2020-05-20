package rtk_contest.templating;

public class StringHelper {

    /**
     * Баловство - ручной сплит, чуток быстрее чем через регулярку String::split
     */
    public static String[] split(String key) {
        int comps = 1;
        for (int i = 0; i < key.length(); i++) {
            if (key.charAt(i) == '.') {
                comps++;
            }
        }
        String[] keyComps = new String[comps];
        int startPos = 0;
        int idx = 0;
        int i = 0;
        do {
            if (i == key.length() || key.charAt(i) == '.') {
                keyComps[idx++] = key.substring(startPos, i);
                startPos = i + 1;
            }
        } while (i++ < key.length());
        return keyComps;
    }
}
