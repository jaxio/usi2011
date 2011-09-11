package usi2011.service;

import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.util.LogUtil.isInfoEnabled;
import static usi2011.util.Specifications.RANKING_STRONGER_SIZE;
import static usi2011.util.Specifications.RANKING_TOP_SIZE;
import static usi2011.util.Specifications.RANKING_WEAKER_SIZE;

import java.util.List;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import usi2011.domain.Ranking;
import usi2011.domain.Ranking.RankingBuilder;
import usi2011.domain.Ranking.UserScore;
import usi2011.repository.ScoreRepository;

@Service
public class RankingService {
    private static final Logger logger = getLogger(RankingService.class);

    @Autowired
    ScoreRepository scoreRepository;

    List<UserScore> cachedTop;

    public Ranking getRanking(UserScore us) {
        RankingBuilder builder = new RankingBuilder();

        builder.setScore(us.getScoreStr());

        // top
        builder.addTop(getTop());

        // stronger
        List<UserScore> stronger = scoreRepository.getStrongerThanUser(us, RANKING_STRONGER_SIZE);
        for (UserScore strongerUs : stronger) {
            builder.addBefore(strongerUs);
        }

        // weaker
        List<UserScore> weaker = scoreRepository.getWeakerThanUser(us, RANKING_WEAKER_SIZE);
        for (UserScore weakerUs : weaker) {
            builder.addAfter(weakerUs);
        }

        return builder.build();
    }

    public List<UserScore> getTop() {
        if (cachedTop == null) {
            synchronized (this) {
                if (cachedTop == null) {
                    cachedTop = scoreRepository.getTop(RANKING_TOP_SIZE);
                }
            }
        }
        return cachedTop;
    }

    public void reset() {
        if (isInfoEnabled) {
            logger.info("Reseting ranking service");
        }
        cachedTop = null;
    }
}