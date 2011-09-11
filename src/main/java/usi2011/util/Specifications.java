package usi2011.util;

/**
 * see https://sites.google.com/a/octo.com/challengeusi2011/l-application-de-quiz
 */
public interface Specifications {
    public static final String USER_EMAIL = "mail";
    public static final String USER_PASSWORD = "password";
    public static final String USER_FIRSTNAME = "firstname";
    public static final String USER_LASTNAME = "lastname";

    public static final int USER_INFO_MIN_LENGTH = 2;
    public static final int USER_INFO_MAX_LENGTH = 50;

    public static final int QUESTION_MIN_LENGTH = 2;
    public static final int QUESTION_MAX_LENGTH = 150;

    public static final String ANSWER_ANSWER = "answer";
    public static final String ANSWER_GOOD_ANSWER = "good_answer";
    public static final String ANSWER_ARE_YOU_RIGHT = "are_u_right";
    public static final String ANSWER_SCORE = "score";

    public static final String QUESTION_QUESTION = "question";
    public static final String QUESTION_ANSWER_1 = "answer_1";
    public static final String QUESTION_ANSWER_2 = "answer_2";
    public static final String QUESTION_ANSWER_3 = "answer_3";
    public static final String QUESTION_ANSWER_4 = "answer_4";
    public static final String QUESTION_SCORE = "score";
    public static final int MAX_NUMBER_OF_ANSWERS = 4;

    public static final String AUDIT_USER_EMAIL = "user_mail";
    public static final String AUDIT_USER_ANSWER = "user_answer";
    public static final String AUDIT_GOOD_ANSWER = "good_answer";
    public static final String AUDIT_QUESTION = "question";

    public static final String AUDIT_USER_ANSWERS = "user_answers";
    public static final String AUDIT_GOOD_ANSWERS = "good_answers";

    public static final String SESSION_KEY = "session_key";
    public static final String AUTHENTICATION_KEY = "authentication_key";
    public static final String AUTHENTICATION_SECRET_KEY = "jaxio";
    public static final String PARAMETERS = "parameters";

    public static final String SCORE_USER_EMAIL = "user_mail";
    public static final String SCORE_SCORE = "score";
    public static final String SCORE_EMAIL = "mail";
    public static final String SCORE_SCORES = "scores";
    public static final String SCORE_FIRST_NAME = "firstname";
    public static final String SCORE_LAST_NAME = "lastname";
    public static final String SCORE_TOP_SCORE = "top_scores";
    public static final String SCORE_BEFORE_SCORE = "before";
    public static final String SCORE_AFTER_SCORE = "after";

    public static final String URI_USER = "/api/user";
    public static final String URI_GAME = "/api/game";
    public static final String URI_LOGIN = "/api/login";
    public static final String URI_QUESTION = "/api/question";
    public static final String URI_ANSWER = "/api/answer";
    public static final String URI_RANKING = "/api/ranking";
    public static final String URI_SCORE = "/api/score";
    public static final String URI_AUDIT = "/api/audit";
    public static final String URI_ALIVE = "/alive";
    public static final String URI_HOSTNAME = "/hostname";

    public static final int RANKING_TOP_SIZE = 100;
    public static final int RANKING_WEAKER_SIZE = 5;
    public static final int RANKING_STRONGER_SIZE = 5;
    
    public static final int FIRST_QUESTION = 1;
    public static final int MAX_NUMBER_OF_QUESTIONS = 20;
    public static final int MAX_POSSIBLE_SCORE = 350;// max is  345, but I do not want a mistake
    

}