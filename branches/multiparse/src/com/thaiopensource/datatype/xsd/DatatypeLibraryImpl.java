package com.thaiopensource.datatype.xsd;

import java.util.Hashtable;
import java.util.Enumeration;

import com.thaiopensource.util.Service;
import com.thaiopensource.datatype.xsd.regex.RegexEngine;
import com.thaiopensource.datatype.xsd.regex.RegexSyntaxException;
import org.xml.sax.XMLReader;

import org.relaxng.datatype.DatatypeLibrary;
import org.relaxng.datatype.Datatype;
import org.relaxng.datatype.DatatypeException;
import org.relaxng.datatype.ValidationContext;
import org.relaxng.datatype.DatatypeBuilder;

public class DatatypeLibraryImpl implements DatatypeLibrary {
  private final Hashtable typeTable = new Hashtable();
  private RegexEngine regexEngine;

  static private final String LONG_MAX = "9223372036854775807";
  static private final String LONG_MIN = "-9223372036854775808";
  static private final String INT_MAX = "2147483647";
  static private final String INT_MIN = "-2147483648";
  static private final String SHORT_MAX = "32767";
  static private final String SHORT_MIN = "-32768";
  static private final String BYTE_MAX = "127";
  static private final String BYTE_MIN = "-128";

  static private final String UNSIGNED_LONG_MAX = "18446744073709551615";
  static private final String UNSIGNED_INT_MAX = "4294967295";
  static private final String UNSIGNED_SHORT_MAX = "65535";
  static private final String UNSIGNED_BYTE_MAX = "255";
  static private final String LANGUAGE_PATTERN = "([a-zA-Z]{8}|[iI]-[a-zA-Z]+|[xX]-[a-zA-Z]{1,8})(-[a-zA-Z]{1,8})*";

  public DatatypeLibraryImpl() {
    this.regexEngine = findRegexEngine();
    typeTable.put("string", new StringDatatype());
    typeTable.put("normalizedString", new CdataDatatype());
    typeTable.put("token", new TokenDatatype());
    typeTable.put("boolean", new BooleanDatatype());

    DatatypeBase decimalType = new DecimalDatatype();
    typeTable.put("decimal", decimalType);
    DatatypeBase integerType = new ScaleRestrictDatatype(decimalType, 0);
    typeTable.put("integer", integerType);
    typeTable.put("nonPositiveInteger", restrictMax(integerType, "0"));
    typeTable.put("negativeInteger", restrictMax(integerType, "-1"));
    typeTable.put("long", restrictMax(restrictMin(integerType, LONG_MIN), LONG_MAX));
    typeTable.put("int", restrictMax(restrictMin(integerType, INT_MIN), INT_MAX));
    typeTable.put("short", restrictMax(restrictMin(integerType, SHORT_MIN), SHORT_MAX));
    typeTable.put("byte", restrictMax(restrictMin(integerType, BYTE_MIN), BYTE_MAX));
    DatatypeBase nonNegativeIntegerType = restrictMin(integerType, "0");
    typeTable.put("nonNegativeInteger", nonNegativeIntegerType);
    typeTable.put("unsignedLong", restrictMax(nonNegativeIntegerType, UNSIGNED_LONG_MAX));
    typeTable.put("unsignedInt", restrictMax(nonNegativeIntegerType, UNSIGNED_INT_MAX));
    typeTable.put("unsignedShort", restrictMax(nonNegativeIntegerType, UNSIGNED_SHORT_MAX));
    typeTable.put("unsignedByte", restrictMax(nonNegativeIntegerType, UNSIGNED_BYTE_MAX));
    typeTable.put("positiveInteger", restrictMin(integerType, "1"));
    typeTable.put("double", new DoubleDatatype());
    typeTable.put("float", new FloatDatatype());

    typeTable.put("Name", new NameDatatype());
    typeTable.put("QName", new QNameDatatype());

    DatatypeBase ncNameType = new NCNameDatatype();
    typeTable.put("NCName", ncNameType);

    DatatypeBase nmtokenDatatype = new NmtokenDatatype();
    typeTable.put("NMTOKEN", nmtokenDatatype);
    typeTable.put("NMTOKENS", list(nmtokenDatatype));

    typeTable.put("ID", new IdDatatype());
    DatatypeBase idrefType = new IdrefDatatype();
    typeTable.put("IDREF", idrefType);
    typeTable.put("IDREFS", list(idrefType));

    typeTable.put("NOTATION", new QNameDatatype());

    typeTable.put("base64Binary", new Base64BinaryDatatype());
    typeTable.put("hexBinary", new HexBinaryDatatype());
    typeTable.put("anyURI", new AnyUriDatatype());
    typeTable.put("language", new RegexDatatype(LANGUAGE_PATTERN));

    // Partially implemented
    DatatypeBase entityType = ncNameType;
    typeTable.put("ENTITY", entityType);
    typeTable.put("ENTITIES", list(entityType));

    // Not implemented yet
    typeTable.put("duration", new StringDatatype());
    typeTable.put("dateTime", new StringDatatype());
    typeTable.put("time", new StringDatatype());
    typeTable.put("date", new StringDatatype());
    typeTable.put("gYearMonth", new StringDatatype());
    typeTable.put("gYear", new StringDatatype());
    typeTable.put("gMonthDay", new StringDatatype());
    typeTable.put("gDay", new StringDatatype());
    typeTable.put("gMonth", new StringDatatype());
  }

  public DatatypeBuilder createDatatypeBuilder(String localName) throws DatatypeException {
    DatatypeBase base = (DatatypeBase)typeTable.get(localName);
    if (base == null)
      throw new DatatypeException();
    if (base instanceof RegexDatatype) {
      try {
        ((RegexDatatype)base).compile(getRegexEngine());
      }
      catch (RegexSyntaxException e) {
        throw new DatatypeException(DatatypeBuilderImpl.localizer.message("regex_internal_error", localName));
      }
    }
    return new DatatypeBuilderImpl(this, base);
  }

  RegexEngine getRegexEngine() throws DatatypeException {
    if (regexEngine == null)
      throw new DatatypeException(DatatypeBuilderImpl.localizer.message("regex_impl_not_found"));
    return regexEngine;
  }

  private DatatypeBase restrictMax(DatatypeBase base, String limit) {
    return new MaxInclusiveRestrictDatatype(base, base.getValue(limit, null));
  }

  private DatatypeBase restrictMin(DatatypeBase base, String limit) {
    return new MinInclusiveRestrictDatatype(base, base.getValue(limit, null));
  }

  private DatatypeBase list(DatatypeBase base) {
    return new MinLengthRestrictDatatype(new ListDatatype(base), 1);
  }

  private RegexEngine findRegexEngine() {
    Enumeration e = new Service(RegexEngine.class).getProviders();
    if (!e.hasMoreElements())
      return null;
    return (RegexEngine)e.nextElement();
  }

  public Datatype createDatatype(String type) throws DatatypeException {
    return createDatatypeBuilder(type).createDatatype();
  }
}
