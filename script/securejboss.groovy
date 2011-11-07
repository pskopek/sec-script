#! /usr/bin/groovy

 /*
  * JBoss, Home of Professional Open Source
  * Copyright 2005, JBoss Inc., and individual contributors as indicated
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */

/**
 * Script: securejboss.groovy
 * Purpose: Secure JBoss AS 6.1.0.Final
 *
 * @author Peter Skopek <pskopek at redhat dot com>
 *
 */

public class Const {

    public static final boolean MODIFY_FILES = true;

    public static final String COMMENT_MARKER_BEGIN = " BEGIN: SECURED BY JBOSS ";
    public static final String COMMENT_MARKER_END   = " END: SECURED BY JBOSS ";

    public static final String PROPS_COMMENT_BEGIN = "# ";
    public static final String PROPS_COMMENT_END = "# ";
    public static final String PROPS_COMMENT_INTERIM = "# ";

    public static final String XML_COMMENT_BEGIN = "<!--";
    public static final String XML_COMMENT_END = "-->";
    public static final String XML_COMMENT_INTERIM = "";

    public static final String FILE_SUFFIX = ".tmp";
}

def checkArguments(String[] args) {

    if (args.length == 0) {
        println "Missing argument 'server home'"
        usage()
        System.exit(1)
    }

}

def usage() {
    println ""
    println "usage: groovy securejboss <server home> | [ <server home> ]"
    println "- at least one <sever home> has to be specified"
}



def secureServer(String serverHome) {
    println "Working on ServerHome=$serverHome"

    secureDomainJMXConsole(serverHome)
    secureJMXConsole(serverHome)
    secureHttpInvoker(serverHome)
    secureDomainJBossWS(serverHome)
    secureJBossWSConsole(serverHome)
    secureJMXConnector(serverHome)
    secureDomainHornetQ(serverHome)

    println ""
}

def secureDomainJMXConsole(String serverHome) {
    println "securing JBoss Security Domain: jmx-console"
    String f = "$serverHome/conf/props/jmx-console-users.properties"
    if (new File(f).exists()) {
        commentProps(f, "admin=admin")
        println "Done"
    }
    else {
        println "$f doesn't exist,  nothing to secure."
    }
}

def secureDomainHornetQ(String serverHome) {
    println "securing JBoss Security Domain: hornetq"
    String f = "$serverHome/conf/props/hornetq-users.properties"
    if (new File(f).exists()) {
        commentProps(f, "guest=guest", "dynsub=dynsub")
        println "Done"
    }
    else {
        println "$f doesn't exist,  nothing to secure."
    }
}

def secureDomainJBossWS(String serverHome) {
    println "securing JBoss Security Domain: JBossWS"
    String f = "$serverHome/conf/props/jbossws-users.properties"
    if (new File(f).exists()) {
        commentProps(f, "kermit=thefrog")
        println "Done"
    }
    else {
        println "$f doesn't exist,  nothing to secure."
    }
}

def secureJMXConsole(String serverHome) {
    println "securing jmx-console.war"
    uncommentXML("$serverHome/../../common/deploy/jmx-console.war/WEB-INF/jboss-web.xml", 5, 7)
    uncommentXML("$serverHome/../../common/deploy/jmx-console.war/WEB-INF/web.xml", 95, 98)
    println "Done"
}

def secureJBossWSConsole(String serverHome) {
    println "securing jbossws-console.war"
    uncommentXML("$serverHome/../../common/deploy/jbossws-console.war/WEB-INF/jboss-web.xml", 8, 8)
    uncommentXML("$serverHome/../../common/deploy/jbossws-console.war/WEB-INF/web.xml", 30, 30)
    println "Done"
}

def secureJMXConnector(String serverHome) {
    println "securing JMXConnector"
    String f = "$serverHome/deploy/jmx-jboss-beans.xml"
    if (new File(f).exists()) {
        uncommentXML(f, 20, 20)
        println "Done"
    }
    else {
        println "$f doesn't exist,  nothing to secure."
    }
}


def secureHttpInvoker(String serverHome) {
    println "securing HttpInvoker"

    String httpInvokerDir = "$serverHome/deploy/httpha-invoker.sar"
    if (! new File(httpInvokerDir).exists()) {
        httpInvokerDir = "$serverHome/deploy/http-invoker.sar"
        if (! new File(httpInvokerDir).exists()) {
            httpInvokerDir = null
        }
    }

    if (httpInvokerDir != null) {
        commentXML("$httpInvokerDir/invoker.war/WEB-INF/web.xml", "<http-method>GET</http-method>", "<http-method>POST</http-method>")
        println "Done"
    }
    else {
        println "$serverHome/deploy/httpha-invoker.sar or $serverHome/deploy/http-invoker.sar doesn't exist,  nothing to secure."
    }

}

def commentProps(String file, String line) {
    commentProps(file, line, line)
}

def commentProps(String file, String line1, String line2) {
    comment(file, line1, line2, "#", "#", "# ", false)
}

def commentXML(String file, String line) {
    commentXML(file, line, line)
}

def commentXML(String file, String line1, String line2) {
    comment(file, line1, line2, "<!-- ", " -->", "", true)
}

def uncommentXML(String file, int lineNumberBegin, int lineNumberEnd) {
    File f = new File(file)

    File r = new File(file+Const.FILE_SUFFIX)
    r.withWriter {out ->
        int i = 0;
        boolean inComment = false;
        boolean changed = false;
        f.eachLine {line ->
            if (i == lineNumberBegin
                    && line.contains(Const.XML_COMMENT_BEGIN)
                    && !line.contains(Const.COMMENT_MARKER_BEGIN)) {
                inComment = true
            }

            if (inComment) {
                String l = line;
                if (l.contains(Const.XML_COMMENT_END)) {
                    l = line.replaceFirst(Const.XML_COMMENT_END, "")
                    inComment = false
                    changed = true
                }

                if (i == lineNumberBegin) {
                    l = l.replaceFirst(Const.XML_COMMENT_BEGIN, "$Const.XML_COMMENT_BEGIN $Const.COMMENT_MARKER_BEGIN")
                    changed = true
                }


                if (i == lineNumberEnd) {
                    out.println "$l $Const.COMMENT_MARKER_END $Const.XML_COMMENT_END"
                    changed = true
                }
                else {
                    out.println l
                }

            }
            else {
                out.println line
            }

            if (line.contains(Const.XML_COMMENT_END)) {
                inComment = false
            }

            i++
        }

        if (! changed) {
            println "$file was has not been changed."
        }
    }

    if (Const.MODIFY_FILES)
        r.renameTo(f)

}


def comment(String file, String line1, String line2, String commentBegin, String commentEnd, String commentInterim, boolean appendCommentEnd) {
    File f = new File(file)
    int commark1 = -100;
    int commark2 = -100;
    int i = 0;
    boolean inComment = false;
    boolean everInComment = false;
    f.eachLine {line ->
        if (line.contains(Const.COMMENT_MARKER_BEGIN)) {
            inComment = true
            everInComment = true
        }
        if (line.contains(Const.COMMENT_MARKER_END))
            inComment = false

        if (!inComment && commark1 == -100 && line.contains(line1))
            commark1 = i - 1;

        if (!inComment && commark1 > -100 && commark2 == -100 && line.contains(line2))
            commark2 = i + 1;

        i++
    }

    if (commark1 == -100) {
        if (everInComment) {
            println "WARNING: cannot find '$line1' in non-commneted part of file $file."
            return
        }
        else {
            println "ERROR: cannot find '$line1' in non-commneted part of file $file."
            System.exit(1);
        }
    }
    if (commark2 == -100) {
        if (everInComment) {
            println "WARNING: cannot find '$line2' in non-commneted part of file $file."
            return
        }
        else {
            println "ERROR: cannot find '$line2' in non-commneted part of file $file."
            System.exit(1);
        }
    }


    File r = new File(file+Const.FILE_SUFFIX)
    r.withWriter {out ->

        i = 0;
        inComment = false;
        f.eachLine {line ->
            if (i - 1 == commark1) {
                inComment = true
                out.println "$commentBegin$Const.COMMENT_MARKER_BEGIN"
            }

            if (inComment) {
                out.println "$commentInterim$line"
            }
            else {
                out.println line
            }

            if (i + 1 == commark2) {
                if (appendCommentEnd)
                    out.println "$Const.COMMENT_MARKER_END$commentEnd"
                else
                    out.println "$commentEnd$Const.COMMENT_MARKER_END"
            }

            i++
        }

    }

    if (Const.MODIFY_FILES)
        r.renameTo(f)

}


checkArguments(this.args)
this.args.each {p -> secureServer(p)}
