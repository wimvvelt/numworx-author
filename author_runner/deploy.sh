jh="$1"
if [ "$jh" = "" ]
then
	jh=$(/usr/libexec/java_home -v 17)
fi
JAVA_HOME=$jh $jh/bin/jpackage --help 
MAIN=author_runner.jar
VERSION=1.0.2
rm -rf target/jars
mkdir -p target/jars
cp target/*.jar target/jars/
rm -rf target/deploy/*
JAVA_HOME=$jh $jh/bin/jpackage  -t app-image -n NumworxAuthor -d target/deploy -i target/jars \
  --main-jar $MAIN --app-version $VERSION \
  --java-options --add-exports=java.desktop/sun.awt.www.content.image=ALL-UNNAMED \
  --verbose --vendor Numworx --resource-dir .. 
