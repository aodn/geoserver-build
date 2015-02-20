set -x
# mvn -s ./settings.xml clean install -U -Dmaven.test.skip=true 
git submodule update --init
mvn clean install -U -Dmaven.test.skip=true 
