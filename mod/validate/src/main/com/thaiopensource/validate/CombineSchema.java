package com.thaiopensource.validate;

import com.thaiopensource.util.PropertyMap;

public class CombineSchema extends AbstractSchema {
  private final Schema schema1;
  private final Schema schema2;

  public CombineSchema(Schema schema1, Schema schema2, PropertyMap properties) {
    super(properties);
    this.schema1 = schema1;
    this.schema2 = schema2;
  }

  public Schema getSchema1() {
    return schema1;
  }
  
  public Schema getSchema2() {
    return schema2;
  }
  
  public Validator createValidator(PropertyMap properties) {
    return new CombineValidator(schema1.createValidator(properties),
                                schema2.createValidator(properties));
  }
}
