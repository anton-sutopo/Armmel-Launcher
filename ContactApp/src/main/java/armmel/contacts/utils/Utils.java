package armmel.contacts.utils;

public class Utils {
    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }
    public static boolean isNumeric(String str) {
        if (isEmpty(str)) return false;
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    } 
}

