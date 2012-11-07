import java.net.{HttpURLConnection, URL}
import org.openqa.selenium.By
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import scala.collection.JavaConversions._
import xml.XML

/**
 * Converts the archive of cre.fm into a static RSS file.
 *
 * The archive contains old files, that are not part of the current RSS feed.
 *
 * User: stefan.schwetschke@googlemail.com
 * Date: 05.11.12
 * Time: 16:03
 *
 */

case class PodcastEpisodeInfo(title : String, imageUrl : String, date : String, duration : String, summary : String)
case class PodcastEntryInArchive(info : PodcastEpisodeInfo, detailPageUrl : String)
case class PodcastMedium(enclosureUrl : String, sizeInBytes : Long)
case class PodcastEpisode(info : PodcastEpisodeInfo, medium : PodcastMedium)

def createWebDriver = new HtmlUnitDriver()

def tryMultipleTimes[B](trials : Int)(function : () => B) : B = {
	var i=1
	var result : Option[B]=None
	while(i<trials && result.isEmpty) {
		try {
			result=Some(function())
			i=i+1
		}catch{
			case _ : Exception =>
		}
	}
	result match {
		case Some(a) => a
		case None => function()
	}
}

val archiveUrl="""http://cre.fm/archiv"""
val webDriver=createWebDriver
webDriver.get(archiveUrl)
println(webDriver.getTitle)
val episodesHtmlFragments=webDriver.findElements(By.className("podcast_archive_element")).toList
val episodesInfoInArchive = episodesHtmlFragments.map{  e =>
	val title= e.findElement(By.xpath("""./td[@class='title']/a/strong""")).getText
	val summary=e.findElement(By.className("title")).getText
	val imageUrl=e.findElement(By.xpath("""./td[@class='thumbnail']/img""")).getAttribute("src")
	val date=e.findElement(By.xpath("""./td[@class='date']/*[@class='release_date']""")).getText
	val detailPageUrl=e.findElement(By.xpath("""./td[@class='title']/a""")).getAttribute("href")
	val duration=e.findElement(By.className("duration")).getText
	PodcastEntryInArchive(PodcastEpisodeInfo(title,imageUrl,date, duration,summary),detailPageUrl)
}
webDriver.quit()

val episodes=episodesInfoInArchive.par.map {  e =>
	tryMultipleTimes(5) { () =>
		println(e.info.title)
		val webDriver=createWebDriver
		webDriver.get(e.detailPageUrl)
		val mediaEnclosureUrl=webDriver.findElement(By.xpath("""//a[@class='powerpress_link_d'][@title='Download']""")).
			getAttribute("href")
		webDriver.close()
		val connection=(new URL(mediaEnclosureUrl)).openConnection()
		connection.connect()
		val length=connection.getContentLengthLong
		connection match {
			case httpConnection : HttpURLConnection => httpConnection.disconnect()
		}
		PodcastEpisode(e.info,PodcastMedium(mediaEnclosureUrl, length))
	}
}

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
					<title>{episode.info.title}</title>
					<itunes:author>Tim Pritlove</itunes:author>
					<itunes:summary>{episode.info.summary}</itunes:summary>
					<itunes:image href={episode.info.imageUrl} />
					<enclosure url={episode.medium.enclosureUrl} length={episode.medium.sizeInBytes.toString}  type="audio/x-mp3" />
					<guid>{episode.medium.enclosureUrl}</guid>
					<pubDate>{episode.info.date}</pubDate>
					<itunes:duration>{episode.info.duration}</itunes:duration>
				</item>
			}
		</channel>
	</rss>
println(rssFeed)
XML.save(System.getProperty("user.home","~")+"/Dropbox/Public/CRE-Archiv.rss",rssFeed,"UTF8")



