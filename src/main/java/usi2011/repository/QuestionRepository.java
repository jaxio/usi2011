package usi2011.repository;

import usi2011.domain.Question;
import usi2011.domain.Session;

public interface QuestionRepository {
    public static enum QuestionsMetadata {
        LABEL, GOODCHOICE, CHOICE1, CHOICE2, CHOICE3, CHOICE4
    }
    
    void save(Session session);

    void reset();

    Question getQuestion(int questionId);

}
