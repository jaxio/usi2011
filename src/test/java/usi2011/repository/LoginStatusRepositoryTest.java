package usi2011.repository;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.fest.assertions.Assertions.assertThat;
import static usi2011.Main.context;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.BeansException;

import usi2011.domain.User;

@Ignore
public class LoginStatusRepositoryTest {

    private static LoginStatusRepository repository = null;

    @BeforeClass
    public static void init() throws BeansException, IOException {
        repository = context.getBean(LoginStatusRepository.class);
    }

    @Test
    public void test() {
        String email = randomAlphabetic(20) + "@domain.com";
        User user = new User(email, "firstname", "lastName", "password");
        assertThat(repository.isLoggedIn(user)).isFalse();
        
        repository.userLoggedIn(user);

        assertThat(repository.isLoggedIn(user)).isTrue();

        repository.reset();

        assertThat(repository.isLoggedIn(user)).isFalse();
    }
}
