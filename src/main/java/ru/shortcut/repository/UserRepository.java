package ru.shortcut.repository;

import org.apache.catalina.User;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository {

    User findById(String userId);
}
