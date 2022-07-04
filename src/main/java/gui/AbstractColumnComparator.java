package gui;

import debug.Debug;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractColumnComparator<T extends Comparable<T>> implements Comparator<String> {

  private Map<String, T> convertedStrs = new HashMap<String, T>(100);

  protected AbstractColumnComparator() {
  }

  @Override
  public int compare(String str1, String str2) {
    try {
      return getConvertedStr(str1).compareTo(getConvertedStr(str2));
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
      return str1.compareTo(str2);
    }
  }

  protected abstract T convert(String str) throws Exception;

  public T getConvertedStr(String str) throws Exception {
    T obj = convertedStrs.get(str);
    if (obj == null) {
      convertedStrs.put(str, obj = convert(str));
    }
    return obj;
  }
}
