package usi2011.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static usi2011.Main.context;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.junit.Ignore;
import org.junit.Test;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class TwitterServiceTest {
    @Test
    @Ignore
    public void publish() throws InterruptedException {
        TwitterService twitter = context.getBean(TwitterService.class);
        twitter.tweet("ping ?");
        SECONDS.sleep(10);
    }

    public static void main(String args[]) throws Exception {
        // The factory instance is re-useable and thread safe.
        Twitter twitter = new TwitterFactory().getInstance();
        twitter.setOAuthConsumer("4coD5NN7q8SaOhb5tOxIUQ", "dRyst8pzKAc0XZjJ7ThmoDbsgbhEnSxKcg0wM8ybDA");
        RequestToken requestToken = twitter.getOAuthRequestToken();
        AccessToken accessToken = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (null == accessToken) {
            System.out.println("Open the following URL and grant access to your account:");
            System.out.println(requestToken.getAuthorizationURL());
            System.out.print("Enter the PIN(if available) or just hit enter.[PIN]:");
            String pin = br.readLine();
            try {
                if (pin.length() > 0) {
                    accessToken = twitter.getOAuthAccessToken(requestToken, pin);
                } else {
                    accessToken = twitter.getOAuthAccessToken();
                }
            } catch (TwitterException te) {
                if (401 == te.getStatusCode()) {
                    System.out.println("Unable to get the access token.");
                } else {
                    te.printStackTrace();
                }
            }
        }
        // persist to the accessToken for future reference.
        storeAccessToken(twitter.verifyCredentials().getId(), accessToken);
        Status status = twitter.updateStatus(args[0]);
        System.out.println("Successfully updated the status to [" + status.getText() + "].");
        System.exit(0);
    }

    private static void storeAccessToken(long useId, AccessToken accessToken) {
        System.out.println(" token = " + accessToken.getToken());
        System.out.println(" token secret = " + accessToken.getTokenSecret());
    }
}