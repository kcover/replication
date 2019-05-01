package com.connexta.replication.adapters.ddf;

import java.util.Date;
import java.util.List;

public class CqlBuilder {

  private CqlBuilder() {}

  public static String equalTo(String attributeName, String desiredValue) {
    return String.format("[ \"%s\" = '%s' ]", attributeName, desiredValue);
  }

  public static String like(String attributeName, String desiredValue) {
    return String.format("[ \"%s\" like '%s' ]", attributeName, desiredValue);
  }

  public static String negate(String expression) {
    return String.format("[ NOT %s ]", expression);
  }

  public static String after(String attributeName, Date time) {
    // use the instant to get the correct format ("1970-01-01T00:00:00Z" instead of "Wed Dec 31
    // 17:00:00 MST 1969")
    return String.format("[ %s after %s ]", attributeName, time.toInstant().toString());
  }

  public static String anyOf(String... expressions) {
    return concatWithOperator(" OR ", expressions);
  }

  public static String anyOf(List<String> expressions) {
    return anyOf(expressions.toArray(new String[0]));
  }

  public static String allOf(String... expressions) {
    return concatWithOperator(" AND ", expressions);
  }

  public static String allOf(List<String> expressions) {
    return allOf(expressions.toArray(new String[0]));
  }

  private static String concatWithOperator(String operator, String... expressions) {
    StringBuilder filterBuilder = new StringBuilder("[ " + expressions[0]);
    for (int i = 1; i < expressions.length; i++) {
      filterBuilder.append(operator);
      filterBuilder.append(expressions[i]);
    }
    filterBuilder.append(" ]");
    return filterBuilder.toString();
  }
}
