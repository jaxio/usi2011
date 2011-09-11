package usi2011.repository.cassandra;

import static org.fest.assertions.Assertions.assertThat;
import static usi2011.Main.context;

import org.junit.Ignore;
import org.junit.Test;

import usi2011.domain.User;
import usi2011.repository.UserRepository.UsersMetadata;

@Ignore
public class CassandraRepositoryTest {
    @Test
    public void truncate() {
        CassandraUserRepository userRepository = context.getBean(CassandraUserRepository.class);
        CassandraRepository cassandraRepository = context.getBean(CassandraRepository.class);
        User user = new User("email@dummy.com", "firstName", "lastName", "password");

        assertThat(userRepository.save(user)).isTrue();

        User loadedUser = userRepository.get(user.getEmail());
        assertThat(loadedUser).isNotNull();
        assertThat(loadedUser.getPassword()).isEqualTo(user.getPassword());
        assertThat(loadedUser.getFirstName()).isEqualTo(user.getFirstName());
        assertThat(loadedUser.getLastName()).isEqualTo(user.getLastName());

        cassandraRepository.truncate(UsersMetadata.class);

        assertThat(userRepository.get(user.getEmail())).isNull();
    }
}