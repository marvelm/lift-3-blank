#projectName=`cat build.sbt | grep 'name :='| cut -d' ' -f 3 | cut -d'"' -f 2`
sbt packageWar
warPath=`find target -name "*.war"`

if [! -d "jetty" ]; then
    wget http://download.eclipse.org/jetty/stable-9/dist/jetty-distribution-9.3.6.v20151106.zip
    unzip jetty-distribution-9.3.6.v20151106.zip
    rm jetty-distribution-9.3.6.v20151106.zip
    mv jetty-distribution-9.3.6.v20151106 jetty

    out=$(cat <<EOF
    <?xml version="1.0"?>
    <!DOCTYPE Configure PUBLIC "-//Mort Bay Consulting//DTD Configure//EN" "http://www.eclipse.org/jetty/configure_9_3.dtd">

    <Configure id="Server" class="org.eclipse.jetty.server.Server">
        <Call class="java.lang.System" name="setProperty">
          <Arg>run.mode</Arg>
          <Arg>production</Arg>
        </Call>
    </Configure>
    EOF)
    out | jetty/etc/jetty-web.xml
fi

cp $warPath jetty/webapps/ROOT.war
cd jetty
./bin/jetty.sh restart
