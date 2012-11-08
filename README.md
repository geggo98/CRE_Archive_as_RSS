CRE_Archive_as_RSS
==================

Converts the archive of cre.fm into a static RSS file.

RUN / COMPILE / INSTALL

You need SBT (simple build tool) and Java to run this project.

1. Download and install Java (http://www.java.com/de/download/). Use the newest version.
2. Download and install SBT (http://www.scala-sbt.org/release/docs/Getting-Started/Setup.html)
3. Get the code from this project from Github (https://github.com/geggo98/CRE_Archive_as_RSS)
4. Open the command prompt and go into the directory "CRE_Archive_as_RSS".
   There should be a file "build.sbt" in the current directory
5. Run "sbt run"

SBT will then download all necessary libraries from the internet and copy them to "lib_managed" in the current
directory. After that it will compile the source code and run the program.

The program will then load the CRE archive page and extract the podcast episodes. It will transform the episode
information into a RSS file and write this file to "Dropbox/Public/CRE-Archiv.rss" in your home directory. If you
have "Dropbox" installed (get it from http://db.tt/R7sW7gKi) you can then access the RSS file over the Internet with
your favorite Podcast program.

You need not install the program to run it.

DISCLAIMER

I am not affiliated with CRE. I am just a listener who needs a RSS file for the archive, so I can load theses episodes
on my mobile devices and listen to them while I am travelling.

I share this script in the hope, someone will find it useful. Please examine the file carefully before you run it.
I cannot give any guarantee or warranty about the functionality. Run at your own risk.


LICENSE

Please consider this project as part of the public domain.
Bitte Projekt als gemeinfrei betrachten.
