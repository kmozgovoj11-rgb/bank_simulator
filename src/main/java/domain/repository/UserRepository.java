package domain.repository;

import java.util.Optional;

import domain.model.User;

public interface UserRepository {
    Optional<User> findByLogin(String login);

    void save(User user);

    void delete(String login);
}