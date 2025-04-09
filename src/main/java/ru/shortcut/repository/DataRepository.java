package ru.shortcut.repository;

import ru.shortcut.model.Data;

public interface DataRepository {

    void save(Data data);
    Data get(String uid);
}
