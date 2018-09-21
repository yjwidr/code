# Prerequisites
## Make sure you have JDK9 installed
## Make sure you have maven 3 installed

# Supported platform
Windows 7
Windows 10
Mac OSX 10.8 or later (sorry we don't support Android)
Major linux distribution

# How to build flow engine
```
cd NG\CrossPlatform\ExecutionFramework\flow-engine-parent 
mvn clean install
```

# How to build flow engine without tests
```
cd NG\CrossPlatform\ExecutionFramework\flow-engine-parent
mvn clean install -DskipTests
```

# How to check unit test coverage
After building Task Engine, go to NG\CrossPlatform\ExecutionFramework\flow-engine-parent\flow-engine-netty\target\coverage-report and open index.html using a web browser.

# How to run flow engine
Create flow-engine-netty\flowengine.properties and add customized settings there.
The values in flow-engine-netty\flowengine.properties override flow-engine-netty\fix_flowengine.properties.
Then run flowengine.bat

```
cd NG\CrossPlatform\ExecutionFramework\flow-engine-parent\flow-engine-netty
flowengine.bat
```