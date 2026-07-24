:start
@start /B 
gradlew clean build smJar deobfSmJar
@pause >nul
@goto start