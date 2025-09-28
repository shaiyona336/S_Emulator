CLASSPATH="mod/jaxb-xjc.jar:mod/jaxb-core.jar:mod/jakarta.xml.bind-api.jar:mod/jaxb-impl.jar"
java -cp "$CLASSPATH" com.sun.tools.xjc.Driver "$@"