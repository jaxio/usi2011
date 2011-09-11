package usi2011.service;

import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.util.LogUtil.isWarnEnabled;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

@Service
public class TwitterService {
    private static final Logger logger = getLogger(TwitterService.class);
    @Value("${twitter.api.enabled:true}")
    private boolean enabled;
    @Value("${twitter.api.key}")
    private String key;
    @Value("${twitter.api.secret}")
    private String secret;
    @Value("${twitter.api.access.token}")
    private String apiToken;
    @Value("${twitter.api.access.secret}")
    private String apiSecret;
    @Value("${proxy.enabled:false}")
    private boolean proxyEnabled;
    @Value("${proxy.host}")
    private String proxyHost;
    @Value("${proxy.port:80}")
    private int proxyPort;
    @Value("${proxy.user}")
    private String proxyUser;
    @Value("${proxy.password}")
    private String proxyPassword;

    private TwitterFactory twitterFactory;

    @PostConstruct
    public void postConstruct() {
        ConfigurationBuilder builder = new ConfigurationBuilder() //
                .setOAuthAccessToken(apiToken) //
                .setOAuthAccessTokenSecret(apiSecret) //
                .setOAuthConsumerKey(key) //
                .setOAuthConsumerSecret(secret);

        if (proxyEnabled) {
            builder.setHttpProxyHost(proxyHost) //
                    .setHttpProxyPort(proxyPort) //
                    .setHttpProxyUser(proxyUser) //
                    .setHttpProxyPassword(proxyPassword);
        }
        twitterFactory = new TwitterFactory(builder.build());
    }

    @Async
    public void tweet(String message) {
        if (!enabled) {
            if (isWarnEnabled) {
                logger.warn("Skipping sending tweet {}", message);
            }
            return;
        }
        try {
            twitterFactory.getInstance().updateStatus(message);
            if (isWarnEnabled) {
                logger.warn("{} published on twitter", message);
            }
        } catch (TwitterException e) {
            if (isWarnEnabled) {
                logger.warn("Failed to publish on twitter", e);
            }
        }
    }
}
