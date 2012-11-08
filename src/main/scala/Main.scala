import java.net.{HttpURLConnection, URL}
import java.util.Locale
import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.{DateTimeZone, DateTime}
import org.openqa.selenium.By
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import scala.collection.JavaConversions._
import xml.XML

/**
 * Converts the archive of cre.fm into a static RSS file.
 *
 * CRE is a podcast about technology. It has a RSS feed describing this podcast. One can use this RSS file to download
 * episodes of the podcast. Unfortunately the RSS feed is incomplete. There are several episodes in an archive not
 * included in the RSS feed any more. This script creates a RSS file for the episodes in the archive, that are not
 * part of the RSS feed.
 *
 * The archive contains old files, that are not part of the current RSS feed. All new podcasts will be listed in the
 * current RSS feed. The RSS file for the archive can be generated once, because it must contain only the old files.
 *
 * This script opens the archive web page in a simulated browser and extracts the content of the DOM of the archive
 * page. For each entry in the archive page there is a page containing mor details. The script then opens each detail
 * pages in the simulated browser and also extracts the content. It then uses this extracted content to construct a
 * static RSS file as a static XML file.
 *
 * DISCLAIMER:
 *
 * I am not affiliated with CRE. I am just a listener who needs a RSS file for the archive, so I can
 * load theses episodes on my mobile devices and listen to them while I am travelling.
 *
 * I share this script in the hope, someone will find it useful. Please examine the file carefully before you run it.
 * I cannot give any guarantee or warranty about the functionality. Run at your own risk.
 *
 * LICENSE
 *
 * Please consider this project as part of the public domain.
 * Bitte Projekt als gemeinfrei betrachten.
 *
 * User: stefan.schwetschke@googlemail.com
 * Date: 05.11.12
 * Time: 16:03
 *
 */

// All the content of this "object" will be executed af is it was a script. Putting the commands into a OO construct
// makes it easier for IDEs to deal with them.
object Main extends App {
  // These "case classes" are containers for the content extracted from the website
  /** Contains data about each entry from the main archive page */
  case class PodcastEntryInArchivePage(title : String, date : DateTime, duration : String, thumbnailImageUrl : String,
                                       detailPageUrl : String)
  /**Contains the content extracted from a detail page*/
  case class PodcastEntryInDetailPage(summary : String, imageUrl : String)
  /**Contains data about the mp3 file with the podcast audio, referenced from the details page */
  case class PodcastMedium(enclosureUrl : String, sizeInBytes : Long)
  /** Holds the different information snips together for each podcast episode */
  case class PodcastEpisode(archive : PodcastEntryInArchivePage, detail : PodcastEntryInDetailPage, medium : PodcastMedium)

  /** Opens a new instance of the simulated browser */
  def createWebDriver = new HtmlUnitDriver()

  /** Tries to execute a block multiple times until it either finishes without errors or the maximum number of trials
    * is reached
    */
  def tryMultipleTimes[B](maxNumberOfTrials : Int)(function : () => B) : B = {
    var i=1
    var result : Option[B]=None
    while(i<maxNumberOfTrials && result.isEmpty) {
      try {
        i=i+1
        result=Some(function())
      }catch{
        case _ : Exception =>
      }
    }
    result match {
      case Some(a) => a
      case None => function()
    }
  }

  // Some information about the archive page
  val archiveUrl="""http://cre.fm/archiv"""
  val archiveDateFormat="""\s*(\d{1,2})\.(\d{1,2})\.(\d{2,4})\s*""".r
  val archiveTimeZone=DateTimeZone.forID("Europe/Berlin")

  // Fetch the archive page
  val webDriver=createWebDriver
  webDriver.get(archiveUrl)
  println(webDriver.getTitle)

  // Extract a list of podcast episodes from the archive page
  val episodesHtmlFragments=webDriver.findElements(By.className("podcast_archive_element")).toList
  val episodesInfoInArchive = episodesHtmlFragments.map{  e =>
    val title= e.findElement(By.xpath("""./td[@class='title']/a/strong""")).getText
    val thumbnailImageUrl=e.findElement(By.xpath("""./td[@class='thumbnail']/img""")).getAttribute("src")
    val dateString=e.findElement(By.xpath("""./td[@class='date']/*[@class='release_date']""")).getText
    val date=dateString match {
      case f@archiveDateFormat(day, month, year) => new DateTime(year.toInt, month.toInt, day.toInt,0,0,0,archiveTimeZone)
      case _ => new DateTime()
    }
    val detailPageUrl=e.findElement(By.xpath("""./td[@class='title']/a""")).getAttribute("href")
    val duration=e.findElement(By.className("duration")).getText
    PodcastEntryInArchivePage(title,date,duration,thumbnailImageUrl,detailPageUrl)
  }
  webDriver.quit()

  // For each episode, extract data from the corresponding detail page.
  // Handle multiple pages at the same time for better performance.
  val episodes=episodesInfoInArchive.par.map {  e =>
    tryMultipleTimes(5) { () =>
      println(e.title)
      val webDriver=createWebDriver
      webDriver.get(e.detailPageUrl)
      val summary=webDriver.findElement(By.className("entry-content")).getText
      val mediaEnclosureUrl=webDriver.findElement(By.xpath("""//a[@class='powerpress_link_d'][@title='Download']""")).
        getAttribute("href")
      webDriver.close()
      val connection=(new URL(mediaEnclosureUrl)).openConnection()
      connection.connect()
      val sizeInBytes=connection.getContentLengthLong
      connection match {
        case httpConnection : HttpURLConnection => httpConnection.disconnect()
      }
      PodcastEpisode(e,PodcastEntryInDetailPage(summary,"imageUrl"),PodcastMedium(mediaEnclosureUrl,sizeInBytes))
    }
  }

  // Some information about the RSS format
  // Example date: Sat, 24 Apr 2010 14:01:00 GMT
  val podcastDateFormat=new DateTimeFormatterBuilder().appendPattern("""EEE, dd MMM yyyy HH:mm:ss zzz""").toFormatter
  val podcastDateFormatInEnglish = podcastDateFormat.withLocale(Locale.ENGLISH)
  // Create a XML object containing the RSS feed
  val rssFeed=
    <rss xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" version="2.0">
      <channel>
        <title>CRE: Technik, Kultur, Gesellschaft - ARCHIV</title>
        <description>Der Interview-Podcast mit Tim Pritlove</description>
        <link>http://cre.fm/archiv</link>
        <language>de</language>
        <itunes:summary>Intensive und ausfuehrliche Gespraeche ueber Themen aus Technik, Kultur und Gesellschaft, das ist CRE. Interessante Gespraechspartner stehen Rede und Antwort zu Fragen, die man normalerweise selten gestellt bekommt. CRE moechte  aufklaeren, weiterbilden und unterhalten.</itunes:summary>
        <itunes:author>Tim Pritlove</itunes:author>
        <itunes:explicit>no</itunes:explicit>
        <itunes:image href="http://meta.metaebene.me/media/cre/cre-logo-600x600.jpg" />
        <managingEditor>cre@metaebene.me (Tim Pritlove)</managingEditor>
        <copyright>Metaebene Personal Media</copyright>
        <itunes:subtitle>Der Interview-Podcast mit Tim Pritlove</itunes:subtitle>
        <image>
          <title>CRE: Technik, Kultur, Gesellschaft - ARCHIV</title>
          <url>http://meta.metaebene.me/media/cre/cre-logo-600x600.jpg</url>
          <link>http://cre.fm/archiv</link>
        </image>
        <itunes:owner>
          <itunes:email>cre@metaebene.me</itunes:email>
        </itunes:owner>
        <itunes:category text="Technology" />
        { for (episode <- episodes.toList) yield
          <item>
            <title>{episode.archive.title}</title>
            <itunes:author>Tim Pritlove</itunes:author>
            <itunes:summary>{episode.detail.summary}</itunes:summary>
            <itunes:image href={episode.archive.thumbnailImageUrl} />
            <enclosure url={episode.medium.enclosureUrl} length={episode.medium.sizeInBytes.toString}  type="audio/x-mp3" />
            <guid>{episode.medium.enclosureUrl}</guid>
            <pubDate>{episode.archive.date.toString(podcastDateFormatInEnglish)}</pubDate>
            <itunes:duration>{episode.archive.duration}</itunes:duration>
          </item>
        }
      </channel>
    </rss>
  // Write the XML file containing the RSS feed
  println(rssFeed)
  XML.save(System.getProperty("user.home","~")+"/Dropbox/Public/CRE-Archiv.rss",rssFeed,"UTF8")
}



