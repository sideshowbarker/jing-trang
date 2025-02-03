package com.thaiopensource.xml.util;

public class Naming {

  private Naming() { }

  private static final int CT_NAME = 1;
  private static final int CT_NMSTRT = 2;

  private static final String nameStartSingles =
  "\u003a\u005f";
  // TODO: Support the unicode 0x10000-0xEFFFF range
  private static final String nameStartRanges =
  "\u0041\u005a\u0061\u007a\u00c0\u00d6\u00d8\u00f6\u00f8\u02ff\u0370\u037d" +
  "\u037f\u1fff\u200c\u200d\u2070\u218f\u2c00\u2fef\u3001\ud7ff\uf900\ufdcf" +
  "\ufdf0\ufffd";
  private static final String nameSingles =
  "\u002d\u002e";
  private static final String nameRanges =
  "\u0030\u0039\u0300\u036f\u203f\u2040";

  private final static byte[][] charTypeTable;

  static {
    charTypeTable = new byte[256][];
    for (int i = 0; i < nameSingles.length(); i++)
      setCharType(nameSingles.charAt(i), CT_NAME);
    for (int i = 0; i < nameRanges.length(); i += 2)
      setCharType(nameRanges.charAt(i), nameRanges.charAt(i + 1), CT_NAME);
    for (int i = 0; i < nameStartSingles.length(); i++)
      setCharType(nameStartSingles.charAt(i), CT_NMSTRT);
    for (int i = 0; i < nameStartRanges.length(); i += 2)
      setCharType(nameStartRanges.charAt(i), nameStartRanges.charAt(i + 1),
		  CT_NMSTRT);
    byte[] other = new byte[256];
    for (int i = 0; i < 256; i++)
      if (charTypeTable[i] == null)
	charTypeTable[i] = other;
  }

  private static void setCharType(char c, int type) {
    int hi = c >> 8;
    if (charTypeTable[hi] == null)
      charTypeTable[hi] = new byte[256];
    charTypeTable[hi][c & 0xFF] = (byte)type;
  }

  private static void setCharType(char min, char max, int type) {
    byte[] shared = null;
    do {
      if ((min & 0xFF) == 0) {
	for (; min + 0xFF <= max; min += 0x100) {
	  if (shared == null) {
	    shared = new byte[256];
	    for (int i = 0; i < 256; i++)
	      shared[i] = (byte)type;
	  }
	  charTypeTable[min >> 8] = shared;
	  if (min + 0xFF == max)
	    return;
	}
      }
      setCharType(min, type);
    } while (min++ != max);
  }

  private static boolean isNameStartChar(char c) {
    return charTypeTable[c >> 8][c & 0xff] == CT_NMSTRT;
  }

  private static boolean isNameStartCharNs(char c) {
    return isNameStartChar(c) && c != ':';
  }

  private static boolean isNameChar(char c) {
    return charTypeTable[c >> 8][c & 0xff] != 0;
  }

  private static boolean isNameCharNs(char c) {
    return isNameChar(c) && c != ':';
  }

  public static boolean isName(String s) {
    int len = s.length();
    if (len == 0)
      return false;
    if (!isNameStartChar(s.charAt(0)))
      return false;
    for (int i = 1; i < len; i++)
      if (!isNameChar(s.charAt(i)))
        return false;
    return true;
  }

  public static boolean isNmtoken(String s) {
    int len = s.length();
    if (len == 0)
      return false;
    for (int i = 0; i < len; i++)
      if (!isNameChar(s.charAt(i)))
        return false;
    return true;
  }

  public static boolean isNcname(String s) {
    int len = s.length();
    if (len == 0)
      return false;
    if (!isNameStartCharNs(s.charAt(0)))
      return false;
    for (int i = 1; i < len; i++)
      if (!isNameCharNs(s.charAt(i)))
        return false;
    return true;
  }

  public static boolean isQname(String s) {
    int len = s.length();
    if (len == 0)
      return false;
    if (!isNameStartCharNs(s.charAt(0)))
      return false;
    for (int i = 1; i < len; i++) {
      char c = s.charAt(i);
      if (!isNameCharNs(c)) {
        if (c == ':' && ++i < len && isNameStartCharNs(s.charAt(i))) {
          for (++i; i < len; i++)
            if (!isNameCharNs(s.charAt(i)))
              return false;
          return true;
        }
        return false;
      }
    }
    return true;
  }


}
