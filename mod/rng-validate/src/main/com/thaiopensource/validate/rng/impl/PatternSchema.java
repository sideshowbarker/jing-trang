package com.thaiopensource.validate.rng.impl;

import com.thaiopensource.relaxng.pattern.Pattern;
import com.thaiopensource.relaxng.pattern.SchemaPatternBuilder;
import com.thaiopensource.relaxng.pattern.ValidatorPatternBuilder;
import com.thaiopensource.util.PropertyMap;
import com.thaiopensource.validate.AbstractSchema;
import com.thaiopensource.validate.ValidateProperty;
import com.thaiopensource.validate.Validator;
import org.xml.sax.ErrorHandler;

public class PatternSchema extends AbstractSchema {
  private final SchemaPatternBuilder spb;
  private final Pattern start;

  public PatternSchema(SchemaPatternBuilder spb, Pattern start, PropertyMap properties) {
    super(properties);
    this.spb = spb;
    this.start = start;
  }
 
  /**
   * Returns the pattern start.
   * 
   * @return the pattern start.
   */
  public Pattern getStart() {
    return start;
  }

  public Validator createValidator(PropertyMap properties) {
    ErrorHandler eh = properties.get(ValidateProperty.ERROR_HANDLER);
    return new RngValidator(start, new ValidatorPatternBuilder(spb), eh);
  }
}
