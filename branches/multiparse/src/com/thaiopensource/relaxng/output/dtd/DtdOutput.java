package com.thaiopensource.relaxng.output.dtd;

import com.thaiopensource.relaxng.edit.AbstractVisitor;
import com.thaiopensource.relaxng.edit.AttributeAnnotation;
import com.thaiopensource.relaxng.edit.AttributePattern;
import com.thaiopensource.relaxng.edit.ChoiceNameClass;
import com.thaiopensource.relaxng.edit.ChoicePattern;
import com.thaiopensource.relaxng.edit.Component;
import com.thaiopensource.relaxng.edit.CompositePattern;
import com.thaiopensource.relaxng.edit.Container;
import com.thaiopensource.relaxng.edit.DataPattern;
import com.thaiopensource.relaxng.edit.DefineComponent;
import com.thaiopensource.relaxng.edit.DivComponent;
import com.thaiopensource.relaxng.edit.ElementPattern;
import com.thaiopensource.relaxng.edit.GrammarPattern;
import com.thaiopensource.relaxng.edit.GroupPattern;
import com.thaiopensource.relaxng.edit.InterleavePattern;
import com.thaiopensource.relaxng.edit.MixedPattern;
import com.thaiopensource.relaxng.edit.NameClass;
import com.thaiopensource.relaxng.edit.NameNameClass;
import com.thaiopensource.relaxng.edit.OneOrMorePattern;
import com.thaiopensource.relaxng.edit.OptionalPattern;
import com.thaiopensource.relaxng.edit.Pattern;
import com.thaiopensource.relaxng.edit.PatternVisitor;
import com.thaiopensource.relaxng.edit.RefPattern;
import com.thaiopensource.relaxng.edit.TextPattern;
import com.thaiopensource.relaxng.edit.ValuePattern;
import com.thaiopensource.relaxng.edit.ZeroOrMorePattern;
import com.thaiopensource.relaxng.edit.IncludeComponent;
import com.thaiopensource.relaxng.edit.ListPattern;
import com.thaiopensource.relaxng.edit.Annotated;
import com.thaiopensource.relaxng.edit.Comment;
import com.thaiopensource.relaxng.edit.AnnotationChild;
import com.thaiopensource.relaxng.output.OutputDirectory;
import com.thaiopensource.xml.util.Naming;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

class DtdOutput {
  private String sourceUri;
  private Writer writer;
  private final String lineSep;
  StringBuffer buf = new StringBuffer();
  List elementQueue = new Vector();
  List requiredParamEntities = new Vector();
  List externallyRequiredParamEntities = new Vector();
  Set doneParamEntities = new HashSet();
  Set doneIncludes = new HashSet();
  Set pendingIncludes = new HashSet();
  private Analysis analysis;
  private GrammarPart part;
  private OutputDirectory od;
  private ErrorReporter er;
  private Set reservedEntityNames;

  PatternVisitor topLevelContentModelOutput = new TopLevelContentModelOutput();
  AbstractVisitor nestedContentModelOutput = new ContentModelOutput();
  PatternVisitor expandedContentModelOutput = new ExpandedContentModelOutput();
  PatternVisitor groupContentModelOutput = new GroupContentModelOutput();
  PatternVisitor choiceContentModelOutput = new ChoiceContentModelOutput();
  PatternVisitor occurContentModelOutput = new ParenthesizedContentModelOutput();
  PatternVisitor innerElementClassOutput = new InnerElementClassOutput();
  PatternVisitor expandedInnerElementClassOutput = new ExpandedInnerElementClassOutput();
  AttributeOutput attributeOutput = new AttributeOutput();
  AttributeOutput optionalAttributeOutput = new OptionalAttributeOutput();
  PatternVisitor topLevelSimpleTypeOutput = new TopLevelSimpleTypeOutput();
  PatternVisitor nestedSimpleTypeOutput = new SimpleTypeOutput();
  PatternVisitor valueOutput = new ValueOutput();
  GrammarOutput grammarOutput = new GrammarOutput();

  static private final String COMPATIBILITY_ANNOTATIONS_URI = "http://relaxng.org/ns/compatibility/annotations/1.0";
  static private final String DTD_URI = "http://www.thaiopensource.com/ns/relaxng/dtd";

  private DtdOutput(String sourceUri, Analysis analysis, Set reservedEntityNames, OutputDirectory od, ErrorReporter er) {
    this.sourceUri = sourceUri;
    this.analysis = analysis;
    this.reservedEntityNames = reservedEntityNames;
    this.od = od;
    this.er = er;
    this.part = analysis.getGrammarPart(sourceUri);
    try {
      this.writer = od.open(sourceUri);
    }
    catch (IOException e) {
      throw new WrappedIOException(e);
    }
    this.lineSep = od.getLineSeparator();
  }

  class ParenthesizedContentModelOutput extends AbstractVisitor {
    public Object visitPattern(Pattern p) {
      buf.append('(');
      p.accept(nestedContentModelOutput);
      buf.append(')');
      return null;
    }

    public Object visitRef(RefPattern p) {
      Pattern def = getBody(p.getName());
      if (getContentType(def) == ContentType.DIRECT_SINGLE_ELEMENT)
        ((ElementPattern)def).getNameClass().accept(nestedContentModelOutput);
      else
        visitPattern(p);
      return null;
    }

  }

  class ChoiceContentModelOutput extends ParenthesizedContentModelOutput {
    public Object visitOptional(OptionalPattern p) {
      p.accept(nestedContentModelOutput);
      return null;
    }

    public Object visitOneOrMore(OneOrMorePattern p) {
      p.accept(nestedContentModelOutput);
      return null;
    }

    public Object visitZeroOrMore(ZeroOrMorePattern p) {
      p.accept(nestedContentModelOutput);
      return null;
    }

  }

  class GroupContentModelOutput extends ChoiceContentModelOutput {
    public Object visitGroup(Pattern p) {
      p.accept(nestedContentModelOutput);
      return null;
    }
  }

  class ContentModelOutput extends AbstractVisitor {
    public Object visitName(NameNameClass nc) {
      buf.append(nc.getLocalName());
      return null;
    }

    public Object visitChoice(ChoiceNameClass nc) {
      List list = nc.getChildren();
      boolean needSep = false;
      for (int i = 0, len = list.size(); i < len; i++) {
        if (needSep)
          buf.append('|');
        else
          needSep = true;
        ((NameClass)list.get(i)).accept(this);
      }
      return null;
    }

    public Object visitElement(ElementPattern p) {
      p.getNameClass().accept(this);
      elementQueue.add(p);
      return null;
    }

    public Object visitRef(RefPattern p) {
      Pattern def = getBody(p.getName());
      if (getContentType(def) == ContentType.DIRECT_SINGLE_ELEMENT)
        ((ElementPattern)def).getNameClass().accept(this);
      else
        paramEntityRef(p);
      return null;
    }

    public Object visitZeroOrMore(ZeroOrMorePattern p) {
      p.getChild().accept(occurContentModelOutput);
      buf.append('*');
      return null;
    }

    public Object visitOneOrMore(OneOrMorePattern p) {
      p.getChild().accept(occurContentModelOutput);
      buf.append('+');
      return null;
    }

    public Object visitOptional(OptionalPattern p) {
      p.getChild().accept(occurContentModelOutput);
      buf.append('?');
      return null;
    }

    public Object visitText(TextPattern p) {
      buf.append("#PCDATA");
      return null;
    }

    public Object visitMixed(MixedPattern p) {
      buf.append("#PCDATA");
      return null;
    }

    public Object visitGroup(GroupPattern p) {
      List list = p.getChildren();
      boolean needSep = false;
      final int len = list.size();
      for (int i = 0; i < len; i++) {
        Pattern member = (Pattern)list.get(i);
        ContentType t = getContentType(member);
        if (!t.isA(ContentType.EMPTY)) {
          if (needSep)
            buf.append(',');
          else
            needSep = true;
          member.accept(groupContentModelOutput);
        }
      }
      return null;
    }

    public Object visitInterleave(InterleavePattern p) {
      final List list = p.getChildren();
      for (int i = 0, len = list.size(); i < len; i++) {
        Pattern member = (Pattern)list.get(i);
        ContentType t = getContentType(member);
        if (!t.isA(ContentType.EMPTY))
          member.accept(this);
      }
      return null;
    }

    public Object visitChoice(ChoicePattern p) {
      List list = p.getChildren();
      boolean needSep = false;
      final int len = list.size();
      if (getContentType(p).isA(ContentType.MIXED_ELEMENT_CLASS)) {
        for (int i = 0; i < len; i++) {
          Pattern member = (Pattern)list.get(i);
          if (getContentType(member).isA(ContentType.MIXED_ELEMENT_CLASS)) {
            member.accept(nestedContentModelOutput);
            needSep = true;
            break;
          }
        }
      }
      for (int i = 0; i < len; i++) {
        Pattern member = (Pattern)list.get(i);
        ContentType t = getContentType(member);
        if (t != ContentType.NOT_ALLOWED && t != ContentType.EMPTY && !t.isA(ContentType.MIXED_ELEMENT_CLASS)) {
          if (needSep)
            buf.append('|');
          else
            needSep = true;
          member.accept(!t.isA(ContentType.ELEMENT_CLASS) ? choiceContentModelOutput : nestedContentModelOutput);
        }
      }
      for (int i = 0; i < len; i++) {
        Pattern member = (Pattern)list.get(i);
        ContentType t = getContentType(member);
        if (t == ContentType.NOT_ALLOWED) {
          if (needSep)
            buf.append(' ');
          else
            needSep = true;
          member.accept(nestedContentModelOutput);
        }
      }
      return null;
    }

    public Object visitGrammar(GrammarPattern p) {
      return getBody(DefineComponent.START).accept(this);
    }
  }

  class TopLevelContentModelOutput extends ContentModelOutput {
    public Object visitZeroOrMore(ZeroOrMorePattern p) {
      buf.append('(');
      p.getChild().accept(nestedContentModelOutput);
      buf.append(')');
      buf.append('*');
      return null;
    }

    public Object visitOneOrMore(OneOrMorePattern p) {
      buf.append('(');
      p.getChild().accept(nestedContentModelOutput);
      buf.append(')');
      buf.append('+');
      return null;
    }

    public Object visitOptional(OptionalPattern p) {
      buf.append('(');
      p.getChild().accept(nestedContentModelOutput);
      buf.append(')');
      buf.append('?');
      return null;
    }

    public Object visitElement(ElementPattern p) {
      buf.append('(');
      super.visitElement(p);
      buf.append(')');
      return null;
    }

    public Object visitRef(RefPattern p) {
      ContentType t = getContentType(p);
      if (t == ContentType.MIXED_MODEL)
        super.visitRef(p);
      else {
        buf.append('(');
        super.visitRef(p);
        buf.append(')');
      }
      return null;
    }

    public Object visitChoice(ChoicePattern p) {
      buf.append('(');
      p.accept(nestedContentModelOutput);
      buf.append(')');
      return null;
    }

    public Object visitText(TextPattern p) {
      buf.append('(');
      p.accept(nestedContentModelOutput);
      buf.append(')');
      return null;
    }

    public Object visitMixed(MixedPattern p) {
      buf.append('(');
      if (getContentType(p.getChild()) == ContentType.EMPTY)
        buf.append("#PCDATA)");
      else {
        buf.append("#PCDATA|");
        p.getChild().accept(innerElementClassOutput);
        buf.append(')');
        buf.append('*');
      }
      return null;
    }

    public Object visitGroup(GroupPattern p) {
      List list = p.getChildren();
      Pattern main = null;
      for (int i = 0, len = list.size(); i < len; i++) {
        Pattern member = (Pattern)list.get(i);
        if (!getContentType(member).isA(ContentType.EMPTY)) {
          if (main == null)
            main = member;
          else {
            buf.append('(');
            nestedContentModelOutput.visitGroup(p);
            buf.append(')');
            return null;
          }
        }
      }
      if (main != null)
        main.accept(this);
      return null;
    }
  }

  class ExpandedContentModelOutput extends ContentModelOutput {
    public Object visitElement(ElementPattern p) {
      p.getNameClass().accept(this);
      return null;
    }
  }

  class InnerElementClassOutput extends AbstractVisitor {
    public Object visitRef(RefPattern p) {
      getBody(p.getName()).accept(expandedInnerElementClassOutput);
      return null;
    }

    public Object visitComposite(CompositePattern p) {
      List list = p.getChildren();
      for (int i = 0, len = list.size(); i < len; i++) {
        Pattern member = (Pattern)list.get(i);
        if (getContentType(member) != ContentType.EMPTY) {
          member.accept(this);
          break;
        }
      }
      return null;
    }

    public Object visitZeroOrMore(ZeroOrMorePattern p) {
      p.getChild().accept(nestedContentModelOutput);
      return null;
    }
  }

  class ExpandedInnerElementClassOutput extends InnerElementClassOutput {
    public Object visitZeroOrMore(ZeroOrMorePattern p) {
      p.getChild().accept(expandedContentModelOutput);
      return null;
    }
  }

  class AttributeOutput extends AbstractVisitor {
    void output(Pattern p) {
      if (getAttributeType(p) != AttributeType.EMPTY)
        p.accept(this);
    }

    void indent() {
      buf.append(lineSep);
      buf.append("  ");
    }

    public Object visitComposite(CompositePattern p) {
      List list = p.getChildren();
      for (int i = 0, len = list.size(); i < len; i++)
        output((Pattern)list.get(i));
      return null;
    }

    public Object visitMixed(MixedPattern p) {
      output(p.getChild());
      return null;
    }

    public Object visitOneOrMore(OneOrMorePattern p) {
      output(p.getChild());
      return null;
    }

    public Object visitZeroOrMore(ZeroOrMorePattern p) {
      if (getAttributeType(p) != AttributeType.SINGLE)
        er.warning("attribute_occur_approx", p.getSourceLocation());
      optionalAttributeOutput.output(p.getChild());
      return null;
    }

    public Object visitRef(RefPattern p) {
      ContentType t = getContentType(p);
      if (t.isA(ContentType.EMPTY) && isRequired()) {
        if (analysis.getParamEntityElementName(p.getName()) == null) {
          indent();
          paramEntityRef(p);
        }
      }
      else
        output(getBody(p.getName()));
      return null;
    }

    public Object visitAttribute(AttributePattern p) {
      ContentType ct = getContentType(p.getChild());
      if (ct == ContentType.NOT_ALLOWED)
        return null;
      List names = NameClassSplitter.split(p.getNameClass());
      int len = names.size();
      if (len > 1)
        er.warning("attribute_occur_approx", p.getSourceLocation());
      for (int i = 0; i < len; i++) {
        indent();
        NameNameClass nnc = (NameNameClass)names.get(i);
        String ns = nnc.getNamespaceUri();
        String prefix = null;
        if (!ns.equals("") && ns != NameClass.INHERIT_NS) {
          prefix = analysis.getPrefixForNamespaceUri(ns);
          buf.append(prefix);
          buf.append(':');
        }
        buf.append(nnc.getLocalName());
        buf.append(" ");
        if (ct == ContentType.VALUE)
          p.getChild().accept(valueOutput);
        else {
          if (ct.isA(ContentType.SIMPLE_TYPE) || ct == ContentType.TEXT)
            p.getChild().accept(topLevelSimpleTypeOutput);
          else if (ct == ContentType.EMPTY) {
            er.warning("empty_attribute_approx", p.getSourceLocation());
            buf.append("CDATA");
          }
          if (isRequired() && len == 1)
            buf.append(" #REQUIRED");
          else {
            String dv = getDefaultValue(p);
            if (dv == null)
              buf.append(" #IMPLIED");
            else {
              buf.append(' ');
              attributeValueLiteral(dv);
            }
          }
        }
      }
      return null;
    }

    boolean isRequired() {
      return true;
    }

    public Object visitChoice(ChoicePattern p) {
      if (getAttributeType(p) != AttributeType.EMPTY)
        er.warning("attribute_occur_approx", p.getSourceLocation());
      optionalAttributeOutput.visitComposite(p);
      return null;
    }

    public Object visitOptional(OptionalPattern p) {
      if (getAttributeType(p) != AttributeType.SINGLE)
        er.warning("attribute_occur_approx", p.getSourceLocation());
      optionalAttributeOutput.output(p.getChild());
      return null;
    }
  }

  class OptionalAttributeOutput extends AttributeOutput {
    boolean isRequired() {
      return false;
    }
  }

  class SimpleTypeOutput extends AbstractVisitor {
    public Object visitText(TextPattern p) {
      buf.append("CDATA");
      return null;
    }

    public Object visitValue(ValuePattern p) {
      buf.append(p.getValue());
      return null;
    }

    public Object visitRef(RefPattern p) {
      paramEntityRef(p);
      return null;
    }

    public Object visitData(DataPattern p) {
      Datatypes.Info info = Datatypes.getInfo(p.getDatatypeLibrary(), p.getType());
      if (info == null) {
        er.warning("unrecognized_datatype", p.getSourceLocation());
        buf.append("CDATA");
      }
      else {
        if (!info.isExact())
          er.warning("datatype_approx", p.getType(), info.closestType(), p.getSourceLocation());
        else {
          if (p.getParams().size() > 0)
            er.warning("ignore_params", p.getSourceLocation());
          if (p.getExcept() != null)
            er.warning("ignore_except", p.getSourceLocation());
        }
        buf.append(info.closestType());
      }
      return null;
    }

    public Object visitChoice(ChoicePattern p) {
      List list = p.getChildren();
      boolean needSep = false;
      final int len = list.size();
      for (int i = 0; i < len; i++) {
        Pattern member = (Pattern)list.get(i);
        ContentType t = getContentType(member);
        if (t != ContentType.NOT_ALLOWED) {
          if (needSep)
            buf.append('|');
          else
            needSep = true;
          member.accept(this);
        }
      }
      for (int i = 0; i < len; i++) {
        Pattern member = (Pattern)list.get(i);
        ContentType t = getContentType(member);
        if (t == ContentType.NOT_ALLOWED) {
          if (needSep)
            buf.append(' ');
          else
            needSep = true;
          member.accept(this);
        }
      }
      return null;
    }

  }

  class TopLevelSimpleTypeOutput extends SimpleTypeOutput {
    public Object visitList(ListPattern p) {
      er.warning("list_approx", p.getSourceLocation());
      buf.append("CDATA");
      return null;
    }

    public Object visitValue(ValuePattern p) {
      if (getContentType(p) == ContentType.ENUM) {
        buf.append('(');
        super.visitValue(p);
        buf.append(')');
      }
      else {
        Datatypes.Info info = Datatypes.getInfo(p.getDatatypeLibrary(), p.getType());
        if (info == null) {
          er.warning("unrecognized_datatype", p.getSourceLocation());
          buf.append("CDATA");
        }
        else {
          String type = info.closestType();
          er.warning("value_approx", type, p.getSourceLocation());
          buf.append(type);
        }
      }
      return null;
    }

    public Object visitChoice(ChoicePattern p) {
      ContentType t = getContentType(p);
      if (t == ContentType.ENUM) {
        buf.append('(');
        nestedSimpleTypeOutput.visitChoice(p);
        buf.append(')');
      }
      else if (t == ContentType.SIMPLE_TYPE_CHOICE) {
        er.warning("datatype_choice_approx", p.getSourceLocation());
        buf.append("CDATA");
      }
      else
        super.visitChoice(p);
      return null;
    }

    public Object visitRef(RefPattern p) {
      ContentType t = getContentType(p);
      if (t == ContentType.ENUM) {
        buf.append('(');
        super.visitRef(p);
        buf.append(')');
      }
      else if (t == ContentType.TEXT)
        buf.append("CDATA");
      else
        super.visitRef(p);
      return null;
    }

  }

  class ValueOutput extends AbstractVisitor {
    public Object visitValue(ValuePattern p) {
      buf.append("CDATA #FIXED ");
      attributeValueLiteral(p.getValue());
      return null;
    }

    public Object visitRef(RefPattern p) {
      paramEntityRef(p);
      return null;
    }
  }

  class GrammarOutput extends AbstractVisitor {
    public void visitContainer(Container c) {
      final List list = c.getComponents();
      for (int i = 0, len = list.size(); i < len; i++)
        ((Component)list.get(i)).accept(this);
    }

    public Object visitDiv(DivComponent c) {
      visitContainer(c);
      return null;
    }

    public Object visitDefine(DefineComponent c) {
      if (c.getName() == DefineComponent.START) {
        if (analysis.getPattern() == analysis.getGrammarPattern())
          c.getBody().accept(nestedContentModelOutput);
      }
      else {
        if (getContentType(c.getBody()) == ContentType.DIRECT_SINGLE_ELEMENT)
          outputElement((ElementPattern)c.getBody(), c);
        else if (!doneParamEntities.contains(c.getName())) {
          doneParamEntities.add(c.getName());
          outputParamEntity(c);
        }
      }
      outputQueuedElements();
      return null;
    }

    public Object visitInclude(IncludeComponent c) {
      outputInclude(c);
      return null;
    }
  }

  void outputQueuedElements() {
    for (int i = 0; i < elementQueue.size(); i++)
      outputElement((ElementPattern)elementQueue.get(i), null);
    elementQueue.clear();
  }

  static void output(Analysis analysis, OutputDirectory od, ErrorReporter er) throws IOException {
    try {
      new DtdOutput(od.MAIN, analysis, new HashSet(), od, er).topLevelOutput();
    }
    catch (WrappedIOException e) {
      throw e.cause;
    }
  }

  void topLevelOutput() {
    GrammarPattern grammarPattern = analysis.getGrammarPattern();
    xmlDecl();
    Pattern p = analysis.getPattern();
    if (p != grammarPattern) {
      outputLeadingComments(p);
      p.accept(nestedContentModelOutput);
      outputQueuedElements();
    }
    if (grammarPattern != null) {
      outputLeadingComments(grammarPattern);
      grammarOutput.visitContainer(grammarPattern);
      outputFollowingComments(grammarPattern);
    }
    close();
  }

  void subOutput(GrammarPattern grammarPattern) {
    xmlDecl();
    outputLeadingComments(grammarPattern);
    grammarOutput.visitContainer(grammarPattern);
    outputFollowingComments(grammarPattern);
    close();
  }

  void xmlDecl() {
    write("<?xml encoding=\"");
    write(od.getEncoding());
    write("\"?>");
    newline();
  }

  ContentType getContentType(Pattern p) {
    return analysis.getContentType(p);
  }

  AttributeType getAttributeType(Pattern p) {
    return analysis.getAttributeType(p);
  }

  Pattern getBody(String name) {
    return analysis.getBody(name);
  }

  void paramEntityRef(RefPattern p) {
    String name = p.getName();
    buf.append('%');
    buf.append(name);
    buf.append(';');
    requiredParamEntities.add(name);
  }

  void attributeValueLiteral(String value) {
    buf.append('\'');
    for (int i = 0, len = value.length(); i < len; i++) {
      char c = value.charAt(i);
      switch (c) {
      case '<':
        buf.append("&lt;");
        break;
      case '&':
        buf.append("&amp;");
        break;
      case '\'':
        buf.append("&apos;");
        break;
      case '"':
        buf.append("&quot;");
        break;
      case '\r':
        buf.append("&#xD;");
        break;
      case '\n':
        buf.append("&#xA;");
        break;
      case '\t':
        buf.append("&#9;");
        break;
      default:
        buf.append(c);
        break;
      }
    }
    buf.append('\'');
  }

  void outputRequiredComponents() {
    for (int i = 0; i < requiredParamEntities.size(); i++) {
      String name = (String)requiredParamEntities.get(i);
      Component c = part.getWhereProvided(name);
      if (c == null)
        externallyRequiredParamEntities.add(name);
      else if (c instanceof DefineComponent) {
        if (!doneParamEntities.contains(name)) {
          doneParamEntities.add(name);
          outputParamEntity((DefineComponent)c);
        }
      }
      else
        outputInclude((IncludeComponent)c);
    }
    requiredParamEntities.clear();
  }

  void outputInclude(IncludeComponent inc) {
    String href = inc.getHref();
    if (doneIncludes.contains(href))
      return;
    if (pendingIncludes.contains(href)) {
      er.error("sorry_include_depend", inc.getSourceLocation());
      return;
    }
    pendingIncludes.add(href);
    DtdOutput sub = new DtdOutput(href, analysis, reservedEntityNames, od, er);
    GrammarPattern g = (GrammarPattern)analysis.getSchema(href);
    sub.subOutput(g);
    requiredParamEntities.addAll(sub.externallyRequiredParamEntities);
    outputRequiredComponents();
    String entityName = genEntityName(inc);
    newline();
    write("<!ENTITY % ");
    write(entityName);
    write(" SYSTEM ");
    write('"');
    // XXX deal with " in filename (is it allowed by URI syntax?)
    write(od.reference(sourceUri, href));
    write('"');
    write('>');
    newline();
    write('%');
    write(entityName);
    write(';');
    newline();
    doneIncludes.add(href);
    pendingIncludes.remove(href);
  }

  String genEntityName(IncludeComponent inc) {
    String entityName = getAttributeAnnotation(inc, DTD_URI, "entityName");
    if (entityName != null) {
      entityName = entityName.trim();
      if (!Naming.isNcname(entityName)) {
        er.warning("entity_name_not_ncname", entityName, inc.getSourceLocation());
        entityName = null;
      }
    }
    if (entityName == null) {
      String uri = inc.getHref();
      int slash = uri.lastIndexOf('/');
      if (slash >= 0)
        uri = uri.substring(slash + 1);
      int dot = uri.lastIndexOf('.');
      if (dot > 0)
        uri = uri.substring(0, dot);
      if (Naming.isNcname(uri))
        entityName = uri;
    }
    if (entityName == null)
      entityName = "ent";
    if (reserveEntityName(entityName)) {
      for (int i = 1;; i++) {
        String tem = entityName + Integer.toString(i);
        if (reserveEntityName(tem)) {
          entityName = tem;
          break;
        }
      }
    }
    return entityName;
  }

  private boolean reserveEntityName(String name) {
    if (reservedEntityNames.contains(name))
      return false;
    reservedEntityNames.add(name);
    return true;
  }

  void outputParamEntity(DefineComponent def) {
    String name = def.getName();
    Pattern body = def.getBody();
    ContentType t = getContentType(body);
    buf.setLength(0);
    if (t.isA(ContentType.MODEL_GROUP) || t.isA(ContentType.NOT_ALLOWED) || t.isA(ContentType.MIXED_ELEMENT_CLASS))
      body.accept(nestedContentModelOutput);
    else if (t == ContentType.MIXED_MODEL)
      body.accept(topLevelContentModelOutput);
    else if (t.isA(ContentType.EMPTY))
      attributeOutput.output(body);
    else if (t.isA(ContentType.ENUM))
      body.accept(nestedSimpleTypeOutput);
    else if (t.isA(ContentType.VALUE))
      body.accept(valueOutput);
    else if (t.isA(ContentType.SIMPLE_TYPE))
      body.accept(topLevelSimpleTypeOutput);
    String replacement = buf.toString();
    outputRequiredComponents();
    outputLeadingComments(def);
    String elementName = analysis.getParamEntityElementName(name);
    if (elementName != null) {
      if (replacement.length() > 0) {
        newline();
        write("<!ATTLIST ");
        write(elementName);
        outputAttributeNamespaces(body);
        write(replacement);
        write('>');
        newline();
      }
    }
    else {
      doneParamEntities.add(name);
      newline();
      write("<!ENTITY % ");
      write(name);
      write(' ');
      write('"');
      write(replacement);
      write('"');
      write('>');
      newline();
    }
    outputFollowingComments(def);
  }

  void outputElement(ElementPattern p, Annotated def) {
    buf.setLength(0);
    Pattern content = p.getChild();
    ContentType ct = getContentType(content);
    if (ct == ContentType.EMPTY)
      ;
    else if (ct == ContentType.MIXED_ELEMENT_CLASS) {
      er.warning("mixed_choice_approx", p.getSourceLocation());
      buf.append("(");
      content.accept(nestedContentModelOutput);
      buf.append(")*");
    }
    else if (ct.isA(ContentType.SIMPLE_TYPE)) {
      er.warning("data_content_approx", p.getSourceLocation());
      buf.append("(#PCDATA)");
    }
    else if (ct == ContentType.NOT_ALLOWED)
      return; // leave it undefined
    else
      content.accept(topLevelContentModelOutput);
    String contentModel = buf.length() == 0 ? "EMPTY" : buf.toString();
    buf.setLength(0);
    attributeOutput.output(content);
    String atts = buf.toString();
    outputRequiredComponents();
    if (def != null)
      outputLeadingComments(def);
    outputLeadingComments(p);
    List names = NameClassSplitter.split(p.getNameClass());
    for (Iterator iter = names.iterator(); iter.hasNext();) {
      NameNameClass name = (NameNameClass)iter.next();
      String ns = name.getNamespaceUri();
      String qName;
      String prefix;
      if (ns.equals("") || ns.equals(analysis.getDefaultNamespaceUri()) || ns == NameClass.INHERIT_NS) {
        qName = name.getLocalName();
        prefix = null;
      }
      else {
        prefix = analysis.getPrefixForNamespaceUri(ns);
        qName = prefix + ":" + name.getLocalName();
      }
      newline();
      write("<!ELEMENT ");
      write(qName);
      write(' ');
      write(contentModel);
      write('>');
      newline();
      boolean needXmlns;
      if (ns == NameClass.INHERIT_NS)
        needXmlns = false;
      else if (prefix == null)
        needXmlns = true;
      else
        needXmlns = !analysis.getAttributeNamespaces(content).contains(ns);
      if (atts.length() != 0 || needXmlns) {
        write("<!ATTLIST ");
        write(qName);
        if (needXmlns) {
          newline();
          write("  ");
          if (prefix != null) {
            write("xmlns:");
            write(prefix);
          }
          else
            write("xmlns");
          write(" CDATA #FIXED ");
          buf.setLength(0);
          attributeValueLiteral(ns);
          write(buf.toString());
        }
        if (atts.length() != 0)
          outputAttributeNamespaces(content);
        write(atts);
        write('>');
        newline();
      }
    }
    if (def != null)
      outputFollowingComments(def);
  }

  void outputAttributeNamespaces(Pattern p) {
    Set namespaces = analysis.getAttributeNamespaces(p);
    for (Iterator iter = namespaces.iterator(); iter.hasNext();) {
      String ns = (String)iter.next();
      String prefix = analysis.getPrefixForNamespaceUri(ns);
      newline();
      write("  ");
      write("xmlns:");
      write(prefix);
      write(" CDATA #FIXED ");
      buf.setLength(0);
      attributeValueLiteral(ns);
      write(buf.toString());
    }
  }

  void outputLeadingComments(Annotated a) {
    for (Iterator iter = a.getLeadingComments().iterator(); iter.hasNext();)
      outputComment(((Comment)iter.next()).getValue());
  }

  void outputFollowingComments(Annotated a) {
    for (Iterator iter = a.getFollowingElementAnnotations().iterator(); iter.hasNext();) {
      AnnotationChild child = (AnnotationChild)iter.next();
      if (child instanceof Comment)
        outputComment(((Comment)child).getValue());
    }
  }

  void outputComment(String value) {
    newline();
    write("<!--");
    int start = 0;
    for (;;) {
      int i = value.indexOf('\n', start);
      if (i < 0) {
        if (start == 0) {
          write(' ');
          write(value);
          write(' ');
        }
        else {
          newline();
          write(value.substring(start));
          newline();
        }
        break;
      }
      newline();
      write(value.substring(start, i));
      start = i + 1;
    }
    write("-->");
    newline();
  }

  void newline() {
    write(lineSep);
  }

  static class WrappedIOException extends RuntimeException {
    IOException cause;

    WrappedIOException(IOException cause) {
      this.cause = cause;
    }

    Throwable getCause() {
      return cause;
    }
  }

  void write(String s) {
    try {
      writer.write(s);
    }
    catch (IOException e) {
      throw new WrappedIOException(e);
    }
  }

  void write(char c) {
    try {
      writer.write(c);
    }
    catch (IOException e) {
      throw new WrappedIOException(e);
    }
  }

  void close() {
    try {
      writer.close();
    }
    catch (IOException e) {
      throw new WrappedIOException(e);
    }
  }

  private static String getDefaultValue(AttributePattern p) {
    return getAttributeAnnotation(p, COMPATIBILITY_ANNOTATIONS_URI, "defaultValue");
  }

  private static String getAttributeAnnotation(Annotated p, String ns, String localName) {
    List list = p.getAttributeAnnotations();
    for (int i = 0, len = list.size(); i < len; i++) {
      AttributeAnnotation att = (AttributeAnnotation)list.get(i);
      if (att.getLocalName().equals(localName)
          && att.getNamespaceUri().equals(ns))
        return att.getValue();
    }
    return null;
  }
}
