package ru.shortcut.app;

import ru.shortcut.model.Data;
import ru.shortcut.repository.DataRepository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class DataService {

    private AccessService access;
    private DataRepository repository;
    private MessageDigest digest = get();

    private DataService(AccessService access, DataRepository repository) {
        this.access = access;
        this.repository = repository;
    }

    public Data get(String uid) {
        access.checkRead();
        return repository.get(uid);
    }

    public void create(String name) {
        access.checkWrite();
        digest = get();
        repository.save(new Data(
                HexFormat.of().formatHex(digest.digest(name.getBytes())),
                name
        ));
    }

    public static MessageDigest get() {
        try {
            return MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
