package ru.shortcut.repository;

import org.springframework.stereotype.Repository;

@Repository
public interface FeeRepository {

    void save(Fee fee);
}
