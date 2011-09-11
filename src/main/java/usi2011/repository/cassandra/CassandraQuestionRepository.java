package usi2011.repository.cassandra;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static me.prettyprint.hector.api.factory.HFactory.createSliceQuery;
import static org.slf4j.LoggerFactory.getLogger;
import static usi2011.repository.QuestionRepository.QuestionsMetadata.CHOICE1;
import static usi2011.repository.QuestionRepository.QuestionsMetadata.CHOICE2;
import static usi2011.repository.QuestionRepository.QuestionsMetadata.CHOICE3;
import static usi2011.repository.QuestionRepository.QuestionsMetadata.CHOICE4;
import static usi2011.repository.QuestionRepository.QuestionsMetadata.GOODCHOICE;
import static usi2011.repository.QuestionRepository.QuestionsMetadata.LABEL;
import static usi2011.repository.cassandra.CassandraRepository.cfName;
import static usi2011.repository.cassandra.CassandraRepository.createStringColumn;
import static usi2011.util.FastIntegerParser.parseInt;
import static usi2011.util.LogUtil.isDebugEnabled;
import static usi2011.util.LogUtil.isInfoEnabled;
import static usi2011.util.Profiles.HECTOR;
import static usi2011.util.Specifications.FIRST_QUESTION;
import static usi2011.util.Specifications.MAX_NUMBER_OF_QUESTIONS;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.mutation.Mutator;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import usi2011.domain.Question;
import usi2011.domain.Session;
import usi2011.repository.ParameterRepository;
import usi2011.repository.QuestionRepository;

@Component
@ManagedResource
@Profile(HECTOR)
public class CassandraQuestionRepository implements QuestionRepository {
    private static final Logger logger = getLogger(CassandraQuestionRepository.class);
    private static final StringSerializer stringSerializer = StringSerializer.get();
    private static final String COLUMN_FAMILY = cfName(QuestionsMetadata.class);
    private static final String QUESTION_KEY_PREFIX = "question-";

    @Autowired
    private CassandraRepository cassandra;
    @Autowired
    private ParameterRepository parameterRepository;

    @Override
    @ManagedOperation
    public void reset() {
        cassandra.truncate(COLUMN_FAMILY);
        parameterRepository.reset();
    }

    @Override
    public void save(final Session session) {
        final Mutator<String> mutator = createMutator(cassandra.keyspace(), stringSerializer);
        for (Question question : session.getQuestions()) {
            batchQuestion(mutator, question);
        }
        mutator.execute();
        parameterRepository.save(session.getParameters());
        if (isDebugEnabled) {
            logger.debug("Saved session");
        }
    }

    private String getQuestionMetadataKey(int questionId) {
        return format("%s%03d", QUESTION_KEY_PREFIX, questionId);
    }

    private void batchQuestion(final Mutator<String> mutator, final Question question) {
        final String key = getQuestionMetadataKey(question.getQuestionId());
        mutator.addInsertion(key, COLUMN_FAMILY, createStringColumn(LABEL, question.getLabel()))
                .addInsertion(key, COLUMN_FAMILY, createStringColumn(GOODCHOICE, question.getGoodChoice()))
                .addInsertion(key, COLUMN_FAMILY, createStringColumn(CHOICE1, question.getChoice1()))
                .addInsertion(key, COLUMN_FAMILY, createStringColumn(CHOICE2, question.getChoice2()))
                .addInsertion(key, COLUMN_FAMILY, createStringColumn(CHOICE3, question.getChoice3()))
                .addInsertion(key, COLUMN_FAMILY, createStringColumn(CHOICE4, question.getChoice4()));
        if (isDebugEnabled) {
            logger.debug("adding question {} in save batch", key);
        }
    }

    @Override
    public Question getQuestion(final int questionId) {
        checkArgument(questionId >= FIRST_QUESTION && questionId <= MAX_NUMBER_OF_QUESTIONS);
        final String key = getQuestionMetadataKey(questionId);
        final ColumnSlice<String, String> result = createSliceQuery(cassandra.keyspace(), stringSerializer, stringSerializer, stringSerializer)
                .setColumnFamily(COLUMN_FAMILY)//
                .setKey(key) //
                .setColumnNames(CHOICE1.name(), CHOICE2.name(), CHOICE3.name(), CHOICE4.name(), GOODCHOICE.name(), LABEL.name()) //
                .execute() //
                .get();
        if (result.getColumns().isEmpty()) {
            if (isInfoEnabled) {
                logger.info("Could not find question {}", questionId);
            }
            return null;
        }
        if (isInfoEnabled) {
            logger.info("Loaded question {}", questionId);
        }
        return new Question(questionId //
                , result.getColumnByName(LABEL.name()).getValue() //
                , parseInt(result.getColumnByName(GOODCHOICE.name()).getValue()) //
                , result.getColumnByName(CHOICE1.name()).getValue() //
                , result.getColumnByName(CHOICE2.name()).getValue() //
                , result.getColumnByName(CHOICE3.name()).getValue() //
                , result.getColumnByName(CHOICE4.name()).getValue());
    }

}