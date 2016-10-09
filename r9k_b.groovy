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

while(true) {
	final String BOARD_NAME = "/r9k/"
	final String THREAD_CATALOG_REQUEST_URL = "https://a.4cdn.org" + BOARD_NAME + "threads.json"

	println "Retrieving " + BOARD_NAME + " data..."

	threadCatalogData = new URL(THREAD_CATALOG_REQUEST_URL).getText() // Makes the HTTP request.
	List threadCatalog = new JsonSlurper().parseText(threadCatalogData) // Parses JSONObjects into Maps and JSONArrays into Lists.

	// Store all active threads from the board.
	listOfThreads = []
	numOfPages = threadCatalog.size()
	numOfPages.times { i ->
		Map catalogPage = threadCatalog.get(i)
		List threadsInPage = catalogPage.threads
		numOfThreadsInPage = threadsInPage.size()
		numOfThreadsInPage.times { j -> listOfThreads << threadsInPage.get(j).no }
	}

	listOfComments = []

	// Retrieve all posts from 20% of the most recent threads in the board catalog.
	1.upto(listOfThreads.size() / 5) { i ->
		chosenThreadNo = listOfThreads.get(i)
		threadPageRequestUrl = "https://a.4cdn.org" + BOARD_NAME + "thread/" + 
			chosenThreadNo + ".json"

		threadPageData = new URL(threadPageRequestUrl).getText()
		Map threadPage = new JsonSlurper().parseText(threadPageData)

		List posts = threadPage.posts

		// Grab all the thread comments of Twitter-able length (with no URLs or web links).
		final int CHARACTER_LENGTH = 140
		numOfPosts = posts.size()
		numOfPosts.times { j ->
			Map post = posts.get(j)
			if (post.com != null) {
				comment = Jsoup.parse(post.com).text() // Removes HTML from comment.
				if (comment.length() <= CHARACTER_LENGTH && !comment.contains("www") 
					&& !comment.contains("http"))
					listOfComments << comment
			}
		}

		Thread.sleep(2000) // Respect the 4chan API rules.
	}

	// Choose a comment for Twitter posting.
	chosenCommentIndex = new Random().nextInt(listOfComments.size())
	chosenComment = listOfComments.get(chosenCommentIndex)

	twitter = TwitterFactory.getSingleton()
	twitter.updateStatus(chosenComment)

	Date date = new Date()
	SimpleDateFormat today = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z")
	println today.format(date) + " - Updated Twitter status with: " + chosenComment

	// Wait for a while (15 mins) until the next tweet.
	Thread.sleep(900000)
}
