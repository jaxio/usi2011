package usi2011.domain;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

public class AnswerTest {
    
    @Test
    public void orderIsOk() {
        Answer answer = new Answer("the good answer", true, 11);        
        String a = answer.toJsonString();
        assertThat(a).isEqualTo("{\"are_u_right\":\"true\",\"good_answer\":\"the good answer\",\"score\":\"11\"}");
    }
}