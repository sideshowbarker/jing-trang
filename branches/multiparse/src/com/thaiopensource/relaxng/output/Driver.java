package com.thaiopensource.relaxng.output;

import com.thaiopensource.relaxng.IncorrectSchemaException;
import com.thaiopensource.relaxng.edit.SchemaBuilderImpl;
import com.thaiopensource.relaxng.edit.SchemaCollection;
import com.thaiopensource.relaxng.output.dtd.DtdOutputFormat;
import com.thaiopensource.relaxng.output.rng.RngOutputFormat;
import com.thaiopensource.relaxng.parse.Parseable;
import com.thaiopensource.relaxng.parse.nonxml.NonXmlParseable;
import com.thaiopensource.relaxng.parse.sax.SAXParseable;
import com.thaiopensource.relaxng.util.ErrorHandlerImpl;
import com.thaiopensource.relaxng.util.Jaxp11XMLReaderCreator;
import com.thaiopensource.relaxng.util.ValidationEngine;
import com.thaiopensource.util.Localizer;
import com.thaiopensource.util.UriOrFile;
import com.thaiopensource.util.OptionParser;
import com.thaiopensource.util.Version;
import org.relaxng.datatype.helpers.DatatypeLibraryLoader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

public class Driver {
  static private Localizer localizer = new Localizer(Driver.class);
  private String encoding;
  private ErrorHandlerImpl eh = new ErrorHandlerImpl();
  private static final String DEFAULT_OUTPUT_ENCODING = "UTF-8";

  static public void main(String[] args) throws IncorrectSchemaException, SAXException, IOException {
    System.exit(new Driver().doMain(args));
  }

  private int doMain(String[] args) throws IncorrectSchemaException, SAXException, IOException {
    try {
      OptionParser op = new OptionParser("e:", args);
      try {
        while (op.moveToNextOption()) {
          switch (op.getOptionChar()) {
          case 'e':
            encoding = op.getOptionArg();
            break;
          }
        }
      }
      catch (OptionParser.InvalidOptionException e) {
        error(localizer.message("invalid_option", op.getOptionCharString()));
        return 2;
      }
      catch (OptionParser.MissingArgumentException e) {
        error(localizer.message("option_missing_argument", op.getOptionCharString()));
        return 2;
      }
      args = op.getRemainingArgs();
      if (args.length != 2) {
        error(localizer.message("wrong_number_of_arguments"));
        eh.print(localizer.message("usage", Version.getVersion(Driver.class)));
        return 2;
      }
      InputSource in = new InputSource(UriOrFile.toUri(args[0]));
      if (encoding != null)
        in.setEncoding(encoding);
      Parseable parseable;
      String ext = extension(args[0]);
      if (ext.equalsIgnoreCase(".rng"))
        parseable = new SAXParseable(new Jaxp11XMLReaderCreator(), in, eh);
      else if (ext.equalsIgnoreCase(".rngnx") || ext.equalsIgnoreCase(".rnx"))
        parseable = new NonXmlParseable(in, eh);
      else {
        error(localizer.message("unrecognized_input_extension", ext));
        return 2;
      }
      OutputFormat of;
      ext = extension(args[1]);
      if (ext.equalsIgnoreCase(".dtd"))
        of = new DtdOutputFormat();
      else if (ext.equalsIgnoreCase(".rng"))
        of = new RngOutputFormat();
      else {
        error(localizer.message("unrecognized_output_extension", ext));
        return 2;
      }
      SchemaCollection sc = SchemaBuilderImpl.parse(parseable,
                                                    new DatatypeLibraryLoader());
      OutputDirectory od = new LocalOutputDirectory(new File(args[1]), ext,
                                                    encoding == null ? DEFAULT_OUTPUT_ENCODING : encoding);
      of.output(sc, od, eh);
      return 0;
    }
    catch (OutputFailedException e) {
    }
    catch (IncorrectSchemaException e) {
    }
    catch (IOException e) {
      eh.printException(e);
    }
    catch (SAXException e) {
      eh.printException(e);
    }
    return 1;
  }

  void error(String message) {
    eh.printException(new SAXException(message));
  }

  static private String extension(String s) {
    int dot = s.lastIndexOf(".");
    if (dot < 0)
      return "";
    return s.substring(dot);
  }
}
