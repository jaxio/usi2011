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
public class UserRepositoryTest {
    private static UserRepository repository = null;

    @BeforeClass
    public static void init() throws BeansException, IOException {
        repository = context.getBean(UserRepository.class);
    }

    @Test
    public void reset() {
        User user = user();
        assertThat(repository.save(user)).isTrue();
        repository.reset();
        assertThat(repository.get(user.getEmail())).isNull();
    }

    @Test
    public void saveAndGet() {
        repository.reset();
        User user = user("email@domain.com");
        assertThat(repository.save(user)).isTrue();

        User loaded = repository.get(user.getEmail());
        assertThat(loaded).isNotNull();
        assertThat(loaded.getEmail()).isEqualTo(user.getEmail());
        assertThat(loaded.getEmailJsonEscaped()).isEqualTo(user.getEmailJsonEscaped());
        assertThat(loaded.getPassword()).isEqualTo(user.getPassword());
        assertThat(loaded.getPasswordJsonEscaped()).isEqualTo(user.getPasswordJsonEscaped());
        assertThat(loaded.getFirstName()).isEqualTo(user.getFirstName());
        assertThat(loaded.getFirstNameJsonEscaped()).isEqualTo(user.getFirstNameJsonEscaped());
        assertThat(loaded.getLastName()).isEqualTo(user.getLastName());
        assertThat(loaded.getLastNameJsonEscaped()).isEqualTo(user.getLastNameJsonEscaped());

        assertThat(repository.save(user)).isFalse();

        repository.reset();

        assertThat(repository.get(user.getEmail())).isNull();
    }

    @Test
    public void saveAndGetWithJson() {
        repository.reset();
        User user = new User("email@domain.com", "firstname with Euro sign {\u20AC}", "lastName with tab [\t]", "password");
        assertThat(repository.save(user)).isTrue();

        User loaded = repository.get(user.getEmail());
        assertThat(loaded).isNotNull();
        assertThat(loaded.getEmail()).isEqualTo(user.getEmail());
        assertThat(loaded.getEmailJsonEscaped()).isEqualTo(user.getEmailJsonEscaped());
        assertThat(loaded.getPassword()).isEqualTo(user.getPassword());
        assertThat(loaded.getPasswordJsonEscaped()).isEqualTo(user.getPasswordJsonEscaped());
        assertThat(loaded.getFirstName()).isEqualTo(user.getFirstName());
        assertThat(loaded.getFirstNameJsonEscaped()).isEqualTo(user.getFirstNameJsonEscaped());
        assertThat(loaded.getLastName()).isEqualTo(user.getLastName());
        assertThat(loaded.getLastNameJsonEscaped()).isEqualTo(user.getLastNameJsonEscaped());

        assertThat(repository.save(user)).isFalse();

        repository.reset();

        assertThat(repository.get(user.getEmail())).isNull();
    }

    User user() {
        return user(randomAlphabetic(20) + "-email@domain.com");
    }

    User user(String email) {
        return new User(email, "firstName", "lastName", "password");
    }
}