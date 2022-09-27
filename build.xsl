<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output indent="yes"
	    encoding="utf-8"/>

<xsl:variable name="build" select="'${build.dir}'"/>
<xsl:variable name="doc" select="'${doc.dir}'"/>
<xsl:template match="/">
  <project>
    <xmlproperty file="version.xml"/>
    <property name="build.dir" value="${{basedir}}/build"/>
    <property name="doc.dir" value="${{basedir}}/doc"/>
    <property name="javacc.dir" value="${{lib.dir}}"/>
    <property name="ant.build.javac.source" value="8"/>
    <property name="ant.build.javac.target" value="1.8"/>
    <taskdef name="testng" classname="org.testng.TestNGAntTask">
      <classpath>
	<pathelement location="${{lib.dir}}/testng.jar"/>
      </classpath>
    </taskdef>
    <target name="dummy"/>
    <target name="init">
      <mkdir dir="{$build}"/>
      <exec executable="git">
        <arg value="submodule"/>
        <arg value="update"/>
        <arg value="--init"/>
        <arg value="--recursive"/>
      </exec>
      <copy todir="{$doc}">
        <fileset dir="relaxng.org/jclark" includes="**"/>
      </copy>
    </target>
    <xsl:for-each select="modules/module">
      <xsl:apply-templates select="document(concat('mod/', .,'/mod.xml'), .)/module">
	<xsl:with-param name="name" select="string(.)"/>
	<xsl:with-param name="root" select="/"/>
      </xsl:apply-templates>
    </xsl:for-each>
    <target name="gen">
      <xsl:attribute name="depends">
	<xsl:text>init</xsl:text>
	<xsl:for-each select="modules/module">
	  <xsl:for-each select="document(concat('mod/', .,'/mod.xml'), .)/module/ant/@precompile">
	    <xsl:text>,</xsl:text>
	    <xsl:value-of select="."/>
	  </xsl:for-each>
	</xsl:for-each>
      </xsl:attribute>
    </target>
    <target name="compile" depends="mod.jing.compile-main,mod.trang.compile-main"/>
    <target name="ant-jar" depends="mod.dtdinst.jar,mod.jing.jar,mod.trang.jar"/>
    <target name="srczip" depends="mod.dtdinst.srczip,mod.jing.srczip,mod.trang.srczip"/>
    <target name="jing-jar" depends="mod.jing.jar">
      <taskdef name="jing" classname="com.thaiopensource.relaxng.util.JingTask">
	<classpath>
	  <pathelement location="{$build}/jing.jar"/>
	</classpath>
      </taskdef>
    </target>

    <target name="test">
      <xsl:attribute name="depends">
	<xsl:text>init</xsl:text>
	<xsl:for-each select="modules/module">
	  <xsl:text>,mod.</xsl:text>
	  <xsl:value-of select="."/>
	  <xsl:text>.test</xsl:text>
	</xsl:for-each>
      </xsl:attribute>
    </target>
    <target name="services">
      <xsl:attribute name="depends">
	<xsl:text>init</xsl:text>
	<xsl:for-each select="modules/module">
	  <xsl:text>,mod.</xsl:text>
	  <xsl:value-of select="."/>
	  <xsl:text>.services</xsl:text>
	</xsl:for-each>
      </xsl:attribute>
    </target>
  </project>
</xsl:template>


<xsl:template match="*"/>

<xsl:template match="module">
  <xsl:param name="name"/>
  <xsl:param name="root"/>
  <xsl:copy-of select="ant/*"/>
  <target name="mod.{$name}.compile-main">
    <xsl:attribute name="depends">
      <xsl:text>init</xsl:text>
      <xsl:if test="ant/@precompile">
	<xsl:text>,</xsl:text>
	<xsl:value-of select="ant/@precompile"/>
      </xsl:if>
      <xsl:if test="ant/@rescompile">
	<xsl:text>,</xsl:text>
	<xsl:value-of select="ant/@rescompile"/>
      </xsl:if>
      <xsl:for-each select="depends[@module]">
	<xsl:text>,mod.</xsl:text>
	<xsl:value-of select="@module"/>
	<xsl:text>.compile-main</xsl:text>
      </xsl:for-each>
    </xsl:attribute>
    <mkdir dir="{$build}/mod/{$name}/classes/main"/>
    <xsl:if test="compile">
      <javac destdir="{$build}/mod/{$name}/classes/main" debug="true" debuglevel="lines,source">
	<src>
	  <pathelement location="mod/{$name}/src/main"/>
	  <xsl:if test="ant/@precompile">
	    <pathelement location="{$build}/mod/{$name}/gensrc/main"/>
	  </xsl:if>
	</src>
	<classpath>
	  <xsl:for-each select="depends[@module]">
	    <pathelement location="{$build}/mod/{@module}/classes/main"/>
	  </xsl:for-each>
	  <xsl:for-each select="depends[@lib]">
	    <pathelement location="${{lib.dir}}/{@lib}.jar"/>
	  </xsl:for-each>
	</classpath>
      </javac>
      <copy todir="{$build}/mod/{$name}/classes/main">
        <fileset dir="mod/{$name}/src/main" includes="**/resources/*"/>
      </copy>
    </xsl:if>
    <xsl:if test="version">
      <xsl:variable name="resdir"
		    select="concat($build, '/mod/', $name, '/classes/main/',
			           version/@package, '/resources')"/>
      <mkdir dir="{$resdir}"/>
      <echo file="{$resdir}/Version.properties" message="version=${{version}}&#xA;"/>
    </xsl:if>
  </target>
  <target name="mod.{$name}.compile-test">
    <xsl:attribute name="depends">
      <xsl:text>mod.</xsl:text>
      <xsl:value-of select="$name"/>
      <xsl:text>.compile-main</xsl:text>
      <xsl:for-each select="depends[@module]">
	<xsl:text>,mod.</xsl:text>
	<xsl:value-of select="@module"/>
	<xsl:text>.compile-test</xsl:text>
      </xsl:for-each>
    </xsl:attribute>
    <mkdir dir="{$build}/mod/{$name}/classes/test"/>
    <xsl:if test="compile[@test]">
      <javac destdir="{$build}/mod/{$name}/classes/test" debug="true">
	<src>
	  <pathelement location="mod/{$name}/src/test"/>
	</src>
	<classpath>
	  <pathelement location="{$build}/mod/{$name}/classes/main"/>
	  <xsl:for-each select="depends[@module]">
	    <pathelement location="{$build}/mod/{@module}/classes/test"/>
	    <pathelement location="{$build}/mod/{@module}/classes/main"/>
	  </xsl:for-each>
	  <xsl:for-each select="depends[@lib]">
	    <pathelement location="${{lib.dir}}/{@lib}.jar"/>
	  </xsl:for-each>
	  <xsl:if test="test[@type='testng']">
	    <pathelement location="${{lib.dir}}/testng.jar"/>
	  </xsl:if>
	</classpath>
      </javac>
    </xsl:if>
  </target>
  <target name="mod.{$name}.jar" depends="mod.{$name}.compile-main">
    <jar jarfile="{$build}/{$name}.jar" duplicate="fail">
      <xsl:apply-templates select="jar/*" mode="jar">
	<xsl:with-param name="root" select="$root"/>
      </xsl:apply-templates>
      <xsl:if test="compile">
	<fileset dir="{$build}/mod/{$name}/classes/main" includes="**/*.class,**/resources/*"/>
      </xsl:if>
      <xsl:for-each select="depends[@module]">
	<fileset dir="{$build}/mod/{@module}/classes/main" includes="**/*.class,**/resources/*"/>
      </xsl:for-each>
    </jar>
  </target>
  <target name="mod.{$name}.srczip">
    <xsl:attribute name="depends">
      <xsl:text>init</xsl:text>
      <xsl:if test="ant/@precompile">
	<xsl:text>,</xsl:text>
	<xsl:value-of select="ant/@precompile"/>
      </xsl:if>
      <xsl:for-each select="depends[@module]">
	<xsl:for-each select="document(concat('mod/', @module,'/mod.xml'), $root)/module/ant/@precompile">
	  <xsl:text>,</xsl:text>
	  <xsl:value-of select="."/>
	</xsl:for-each>
      </xsl:for-each>
    </xsl:attribute>
    <zip zipfile="{$build}/mod/{$name}/src.zip">
      <xsl:apply-templates select="." mode="src-fileset">
	<xsl:with-param name="name" select="$name"/>
      </xsl:apply-templates>
      <xsl:for-each select="depends[@module]">
	<xsl:apply-templates select="document(concat('mod/', @module,'/mod.xml'), $root)/module"
			     mode="src-fileset">
	  <xsl:with-param name="name" select="@module"/>
	</xsl:apply-templates>
      </xsl:for-each>
    </zip>
  </target>
  <target name="mod.{$name}.test">
    <xsl:attribute name="depends">
      <xsl:text>dummy</xsl:text>
      <xsl:for-each select="test">
	<xsl:text>,mod.</xsl:text>
	<xsl:value-of select="$name"/>
	<xsl:text>.test-</xsl:text>
	<xsl:value-of select="@name"/>
      </xsl:for-each>
    </xsl:attribute>
  </target>
  <xsl:apply-templates select="test">
    <xsl:with-param name="name" select="$name"/>
  </xsl:apply-templates>
  <target name="mod.{$name}.services">
    <xsl:for-each select="service">
      <mkdir dir="{$build}/mod/{$name}/classes/main/META-INF/services"/>
      <delete file="{$build}/mod/{$name}/classes/main/META-INF/services/{@type}"
	      quiet="true"
	      failonerror="false"/>
      <xsl:for-each select="provider">
	<echo file="{$build}/mod/{$name}/classes/main/META-INF/services/{../@type}"
	      append="true"
	      message="{@classname}${{line.separator}}"/>
      </xsl:for-each>
    </xsl:for-each>
  </target>
</xsl:template>

<xsl:template match="module" mode="src-fileset">
  <xsl:param name="name"/>
  <xsl:if test="compile">
    <fileset dir="mod/{$name}/src/main" includes="**/*.java"/>
    <xsl:if test="ant/@precompile">
      <fileset dir="{$build}/mod/{$name}/gensrc/main" includes="**/*.java"/>
    </xsl:if>
  </xsl:if>
</xsl:template>

<xsl:template match="test[@type='validate']">
  <xsl:param name="name"/>
  <xsl:call-template name="split-test">
    <xsl:with-param name="name" select="$name"/>
    <xsl:with-param name="class">com.thaiopensource.relaxng.util.TestDriver</xsl:with-param>
    <xsl:with-param name="split">test/split.xsl</xsl:with-param>
    <xsl:with-param name="app">jing</xsl:with-param>
  </xsl:call-template>
</xsl:template>

<xsl:template match="test[@type='convert']">
  <xsl:param name="name"/>
  <xsl:call-template name="split-test">
    <xsl:with-param name="name" select="$name"/>
    <xsl:with-param name="class">com.thaiopensource.relaxng.translate.test.CompactTestDriver</xsl:with-param>
    <xsl:with-param name="split">trang/test/compactsplit.xsl</xsl:with-param>
    <xsl:with-param name="app">trang</xsl:with-param>
  </xsl:call-template>
</xsl:template>

<xsl:template name="split-test">
  <xsl:param name="name"/>
  <xsl:param name="class"/>
  <xsl:param name="split"/>
  <xsl:param name="app"/>
  <xsl:variable name="runtestdir">
    <xsl:value-of select="$build"/>
    <xsl:text>/mod/</xsl:text>
    <xsl:value-of select="$name"/>
    <xsl:text>/test-</xsl:text>
    <xsl:value-of select="@name"/>
  </xsl:variable>
  <xsl:variable name="srctestdir">
    <xsl:text>mod/</xsl:text>
    <xsl:value-of select="$name"/>
    <xsl:text>/test</xsl:text>
  </xsl:variable>
  <xsl:variable name="srctest">
    <xsl:choose>
      <xsl:when test="@in">
	<xsl:value-of select="@in"/>
      </xsl:when>
      <xsl:otherwise>
	<xsl:value-of select="concat($srctestdir,'/',@name,'test.xml')"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <target name="mod.{$name}.test-{@name}"
	  depends="mod.{$name}.compile-test,mod.{$app}.jar,mod.{$name}.split-{@name}">
    <java classname="{$class}"
	  fork="yes"
	  failonerror="yes">
      <arg value="{$runtestdir}/out.log"/>
      <arg value="{$runtestdir}"/>
      <xsl:if test="@output">
	<arg value="{@output}"/>
      </xsl:if>
      <classpath>
	<pathelement location="{$build}/{$app}.jar"/>
	<xsl:if test="@lib">
	  <pathelement location="${{lib.dir}}/{@lib}.jar"/>
	  <xsl:if test="@lib='xalan'">
	    <pathelement location="${{lib.dir}}/serializer.jar"/>
	  </xsl:if>
	</xsl:if>
	<xsl:if test="$app = 'jing'">
	  <pathelement location="${{lib.dir}}/xercesImpl.jar"/>
	</xsl:if>
      </classpath>
    </java>
  </target>
  <target name="mod.{$name}.split-{@name}"
	  depends="mod.{$name}.uptodate-split-{@name},jing-jar"
	  unless="mod.{$name}.uptodate-split-{@name}">
    <xsl:if test="@schema">
      <jing rngfile="{@schema}" file="{$srctest}">
	<xsl:if test="substring(@schema, string-length(@schema) - 3, 4) = '.rnc'">
	  <xsl:attribute name="compactsyntax">true</xsl:attribute>
	</xsl:if>
      </jing>
    </xsl:if>
    <delete dir="{$runtestdir}"/>
    <mkdir dir="{$runtestdir}"/>
    <xsl:if test="@transform">
      <xslt style="{$srctestdir}/{@transform}"
	    in="{$srctest}"
	    out="{$runtestdir}/{@name}test.xml">
	<factory name="net.sf.saxon.TransformerFactoryImpl" />
  <classpath>
    <fileset dir="lib">
      <filename name="saxon9.jar"/>
    </fileset>
  </classpath>
      </xslt>
      <!-- XXX Could validate intermediate result against a schema -->
    </xsl:if>
    <xslt style="{$split}"
	  out="{$runtestdir}/stamp">
      <xsl:attribute name="in">
	<xsl:choose>
	  <xsl:when test="@transform">
	    <xsl:value-of select="concat($runtestdir, '/',@name,'test.xml')"/>
	  </xsl:when>
	  <xsl:otherwise>
	    <xsl:value-of select="$srctest"/>
	  </xsl:otherwise>
	</xsl:choose>
      </xsl:attribute>
      <factory name="net.sf.saxon.TransformerFactoryImpl"/>
      <classpath>
        <fileset dir="lib">
          <filename name="saxon9.jar"/>
        </fileset>
      </classpath>
      <param name="dir" expression="{$runtestdir}"/>
    </xslt>
  </target>
  <target name="mod.{$name}.uptodate-split-{@name}">
    <!-- XXX include split.xsl in source files -->
    <uptodate property="mod.{$name}.uptodate-split-{@name}"
	      targetfile="{$runtestdir}/stamp"
	      srcfile="{$srctest}"/>
  </target>
</xsl:template>

<xsl:template match="test[@type='java']">
  <xsl:param name="name"/>
  <target name="mod.{$name}.test-{@name}" depends="mod.{$name}.compile-test">
    <mkdir dir="{$build}/mod/{$name}/test-{@name}"/>
    <java classname="{@class}"
	  fork="yes"
	  failonerror="yes">
      <xsl:copy-of select="*"/>
      <classpath>
	<pathelement location="{$build}/mod/{$name}/classes/test"/>
	<pathelement location="{$build}/mod/{$name}/classes/main"/>
	<pathelement location="mod/{$name}/src/test"/>
	<pathelement location="mod/{$name}/src/main"/>
	<xsl:for-each select="../depends[@module]">
	  <pathelement location="{$build}/mod/{@module}/classes/test"/>
	  <pathelement location="{$build}/mod/{@module}/classes/main"/>
	  <pathelement location="mod/{@module}/src/test"/>
	  <pathelement location="mod/{@module}/src/main"/>
	</xsl:for-each>
	<xsl:for-each select="../depends[@lib]">
	  <pathelement location="${{lib.dir}}/{@lib}.jar"/>
	</xsl:for-each>
      </classpath>
    </java>
  </target>
</xsl:template>

<xsl:template match="test[@type='testng']">
  <xsl:param name="name"/>
  <target name="mod.{$name}.test-{@name}" depends="mod.{$name}.compile-test">
    <mkdir dir="{$build}/mod/{$name}/test-{@name}"/>
    <testng workingDir="{$build}/mod/{$name}/test-{@name}"
	    outputdir="{$build}/mod/{$name}/test-{@name}/report"
	    haltonfailure="true"
	    suiteName="mod.{$name}.test-{@name}"
	    listeners="org.testng.reporters.DotTestListener">
      <classfileset dir="{$build}/mod/{$name}/classes/test" includes="**/*.class"/>
      <classpath>
	<pathelement location="{$build}/mod/{$name}/classes/test"/>
	<pathelement location="{$build}/mod/{$name}/classes/main"/>
	<pathelement location="mod/{$name}/src/test"/>
	<pathelement location="mod/{$name}/src/main"/>
	<xsl:for-each select="../depends[@module]">
	  <pathelement location="{$build}/mod/{@module}/classes/test"/>
	  <pathelement location="{$build}/mod/{@module}/classes/main"/>
	  <pathelement location="mod/{@module}/src/test"/>
	  <pathelement location="mod/{@module}/src/main"/>
	</xsl:for-each>
	<xsl:for-each select="../depends[@lib]">
	  <pathelement location="${{lib.dir}}/{@lib}.jar"/>
	</xsl:for-each>
      </classpath>
    </testng>
  </target>
</xsl:template>

<xsl:template match="service" mode="jar">
  <xsl:param name="root"/>
  <xsl:copy>
    <xsl:variable name="type" select="@type"/>
    <xsl:copy-of select="@type"/>
    <xsl:copy-of select="/module/service[@type=$type]/provider"/>
    <xsl:for-each select="/module/depends[@module]">
      <xsl:copy-of select="document(concat('mod/', @module, '/mod.xml'), $root)
			   /module/service[@type=$type]/provider"/>
    </xsl:for-each>
  </xsl:copy>
</xsl:template>

<xsl:template match="*" mode="jar">
  <xsl:param name="root"/>
  <xsl:copy>
    <xsl:copy-of select="@*"/>
    <xsl:apply-templates select="node()" mode="jar">
      <xsl:with-param name="root" select="$root"/>
    </xsl:apply-templates>
  </xsl:copy>
</xsl:template>

</xsl:stylesheet>
