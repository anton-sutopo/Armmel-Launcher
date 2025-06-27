package armmel.contacts.utils;

public class Utils {
  public static boolean isEmpty(String s) {
    return s == null || s.isEmpty();
  }

  public static String removeForPhone(String input) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (i == 0 && c == '+') sb.append(c);
      else if (Character.isDigit(c)) sb.append(c);
    }
    return sb.toString();
  }

  public static boolean isPartialPhoneNumber(String str) {
    if (str == null || str.isEmpty()) return false;

    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (i == 0 && c == '+') continue;
      if (!Character.isDigit(c) && c != '-' && c != ' ') {
        return false;
      }
    }
    return true;
  }
}
