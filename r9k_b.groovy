/* Written by Damián Adams
 * October 2016
 * https://mianlabs.com/2016/10/09/making-a-4chan-twitter-bot-with-groovy-in-8-easy-steps/
 */
import groovy.json.JsonSlurper
import java.net.URL
import java.text.SimpleDateFormat

@Grab(group='org.twitter4j', module='twitter4j-core', version='4.0.5')
import twitter4j.Status
import twitter4j.Twitter
import twitter4j.TwitterException
import twitter4j.TwitterFactory

@Grab(group='org.jsoup', module='jsoup', version='1.9.2')
import org.jsoup.Jsoup

final int CHARACTER_LENGTH = 140
// NO need to create on each iteration
twitter = TwitterFactory.getSingleton()

while(true) {
    final String BOARD_NAME = "/r9k/"
    final URL THREAD_CATALOG_REQUEST_URL = "https://a.4cdn.org${BOARD_NAME}threads.json".toURL()

    println "Retrieving $BOARD_NAME data..."

    List threadCatalog = new JsonSlurper().parse(THREAD_CATALOG_REQUEST_URL) // Parses JSONObjects into Maps and JSONArrays into Lists.

    // Store all active threads from the board.
    List<String> listOfThreads = threadCatalog.collect {
         // Get the threads no as a list
         it.threads.no
    }.flatten()

    // Retrieve all posts from 20% of the most recent threads in the board catalog.
    List<String> listOfComments = listOfThreads.take(listOfThreads.size().intdiv(5)).collect { choosenThreadNo ->
        threadPageUrl = "https://a.4cdn.org${BOARD_NAME}thread/${choosenThreadNo}.json".toURL()

        Map threadPage = new JsonSlurper().parse(threadPageUrl)

        Thread.sleep(2000) // Respect the 4chan API rules.
        // Get comments from the posts as a list
        threadPage.posts.com
    }.flatten().collect {
        // Get the text comment
        Jsoup.parse(it).text()
    }.findAll {
        // Limit to tweetable comments
        it.size() <= CHARACTER_LENGTH && !it.contains("www") && !it.contains("http")
    } 

    // Choose a random comment for Twitter posting.
    String chosenComment = listOfComment[new Random().nextInt(listOfComments.size())]
	  
    twitter.updateStatus(chosenComment)

    println "${new Date().format('EEE, d MMM yyyy HH:mm:ss Z')} - Updated Twitter status with: $chosenComment"
    // Wait for a while (15 mins) until the next tweet.
    Thread.sleep(900000)
}
