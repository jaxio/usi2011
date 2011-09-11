package usi2011.domain;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

public class AuditUserAnswerTest {
    @Test
    public void valid() {
        AuditUserAnswer auditUserAnswer = new AuditUserAnswer(1, 2, "label");
        assertThat(auditUserAnswer.toJsonString()).isEqualTo("{\"user_answer\":1,\"good_answer\":2,\"question\":\"label\"}");
    }

    @Test
    public void validJsonEscaped() {
        AuditUserAnswer auditUserAnswer = new AuditUserAnswer(1, 2, "labe\\tl");
        assertThat(auditUserAnswer.toJsonString()).isEqualTo("{\"user_answer\":1,\"good_answer\":2,\"question\":\"labe\\tl\"}");
    }
}
