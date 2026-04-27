@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script
@REM ----------------------------------------------------------------------------
@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET __ MVNW_CMD__=%MAVEN_WRAPPER_JAR%
@SET MAVEN_WRAPPER_JAR=%~dp0.mvn\wrapper\maven-wrapper.jar
@SET MAVEN_WRAPPER_PROPERTIES=%~dp0.mvn\wrapper\maven-wrapper.properties
@SET DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar

@FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_WRAPPER_PROPERTIES%") DO (
    @IF "%%A"=="distributionUrl" SET DISTRIBUTION_URL=%%B
)

@IF NOT EXIST "%MAVEN_WRAPPER_JAR%" (
    @IF NOT "%MVNW_REPOURL%"=="" (
        SET DOWNLOAD_URL=%MVNW_REPOURL%/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
    )
    @ECHO Downloading %DOWNLOAD_URL% to %MAVEN_WRAPPER_JAR%
    @powershell -Command "Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%MAVEN_WRAPPER_JAR%'"
)

@SET JAVA_HOME_CANDIDATE=%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-17.0.8.101-hotspot
@IF EXIST "%JAVA_HOME_CANDIDATE%\bin\java.exe" SET JAVA_HOME=%JAVA_HOME_CANDIDATE%

@"%JAVA_HOME%\bin\java.exe" -jar "%MAVEN_WRAPPER_JAR%" %MAVEN_WRAPPER_PROPERTIES% %*
