
name := "RSS_For_CRE_Archive"

version := "1.0"

scalaVersion := "2.9.0"

// Use Selenium (and HttpUnit) to simulate browser
libraryDependencies += "org.seleniumhq.selenium" % "selenium-java" % "2.25.0"

// Include Joda time for handling of date and time
libraryDependencies += "joda-time" % "joda-time" % "2.1"

// Dependency for Joda time must be explicitly listed to make the Scala compiler happy
libraryDependencies += "org.joda" % "joda-convert" % "1.2"

retrieveManaged := true

