package com.thaiopensource.xml.dtd;

import java.io.IOException;

public class SchemaWriter implements TopLevelVisitor,
				     ModelGroupVisitor,
				     AttributeGroupVisitor,
				     DatatypeVisitor,
				     EnumGroupVisitor {
  private XmlWriter w;
  
  public SchemaWriter(XmlWriter writer) {
    this.w = writer;
  }

  public void writeDtd(Dtd dtd) throws IOException {
    w.startElement("doctype");
    try {
      dtd.accept(this);
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw (IOException)e;
    }
    w.endElement();
  }

  public void elementDecl(String name, ModelGroup modelGroup)
    throws Exception {
    w.startElement("element");
    w.attribute("name", name);
    modelGroup.accept(this);
    w.endElement();
  }

  public void attlistDecl(String name, AttributeGroup attributeGroup)
    throws Exception {
    w.startElement("attlist");
    w.attribute("name", name);
    attributeGroup.accept(this);
    w.endElement();
  }

  public void processingInstruction(String target, String value) throws Exception {
    w.startElement("processingInstruction");
    w.attribute("target", target);
    w.characters(value);
    w.endElement();
  }

  public void comment(String value) throws Exception {
    w.startElement("comment");
    w.characters(value);
    w.endElement();
  }

  public void ignoredSection(String value) throws Exception {
  }

  public void modelGroupDef(String name, ModelGroup modelGroup) throws Exception {
    w.startElement("modelGroup");
    w.attribute("name", name);
    modelGroup.accept(this);
    w.endElement();
  }

  public void attributeGroupDef(String name, AttributeGroup attributeGroup)
    throws Exception {
    w.startElement("attributeGroup");
    w.attribute("name", name);
    attributeGroup.accept(this);
    w.endElement();
  }

  public void enumGroupDef(String name, EnumGroup enumGroup) throws Exception {
    w.startElement("enumGroup");
    w.attribute("name", name);
    enumGroup.accept(this);
    w.endElement();
  }
  
  public void datatypeDef(String name, Datatype datatype) throws Exception {
    w.startElement("datatype");
    w.attribute("name", name);
    datatype.accept(this);
    w.endElement();
  }

  public void choice(ModelGroup[] members) throws Exception {
    w.startElement("choice");
    for (int i = 0; i < members.length; i++)
      members[i].accept(this);
    w.endElement();
  }

  public void sequence(ModelGroup[] members) throws Exception {
    w.startElement("sequence");
    for (int i = 0; i < members.length; i++)
      members[i].accept(this);
    w.endElement();
  }

  public void oneOrMore(ModelGroup member) throws Exception {
    w.startElement("oneOrMore");
    member.accept(this);
    w.endElement();
  }

  public void zeroOrMore(ModelGroup member) throws Exception {
    w.startElement("zeroOrMore");
    member.accept(this);
    w.endElement();
  }

  public void optional(ModelGroup member) throws Exception {
    w.startElement("optional");
    member.accept(this);
    w.endElement();
  }

  public void modelGroupRef(String name, ModelGroup modelGroup) throws Exception {
    w.startElement("modelGroupRef");
    w.attribute("name", name);
    w.endElement();
  }

  public void elementRef(String name) throws Exception {
    w.startElement("elementRef");
    w.attribute("name", name);
    w.endElement();
  }

  public void pcdata() throws Exception {
    w.startElement("pcdata");
    w.endElement();
  }

  public void any() throws Exception {
    w.startElement("any");
    w.endElement();
  }

  public void attribute(String name,
			boolean optional,
			Datatype datatype,
			String defaultValue)
    throws Exception {
    w.startElement("attribute");
    w.attribute("name", name);
    w.attribute("use", optional ? "optional" : "required");
    if (defaultValue != null)
      w.attribute("default", defaultValue);
    datatype.accept(this);
    w.endElement();
  }

  public void attributeGroupRef(String name, AttributeGroup attributeGroup)
    throws Exception {
    w.startElement("attributeGroupRef");
    w.attribute("name", name);
    w.endElement();
  }

  public void enumValue(String value) throws Exception {
    w.startElement("enum");
    w.characters(value);
    w.endElement();
  }

  public void enumGroupRef(String name, EnumGroup enumGroup) throws Exception {
    w.startElement("enumGroupRef");
    w.attribute("name", name);
    w.endElement();
  }

  public void basicDatatype(String typeName) throws IOException {
    w.startElement("basic");
    w.attribute("name", typeName);
    w.endElement();
  }

  public void enumDatatype(EnumGroup enumGroup) throws Exception {
    w.startElement("enumChoice");
    enumGroup.accept(this);
    w.endElement();
  }

  public void notationDatatype(EnumGroup enumGroup) throws Exception {
    w.startElement("notation");
    enumGroup.accept(this);
    w.endElement();
  }

  public void datatypeRef(String name, Datatype datatype) throws IOException {
    w.startElement("datatypeRef");
    w.attribute("name", name);
    w.endElement();
  }

}
