package com.banksim.domain.repository;

import com.banksim.domain.model.User;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByLogin(String login);

    void save(User user);

    void delete(String login);
}
