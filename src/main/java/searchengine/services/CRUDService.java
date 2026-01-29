package searchengine.services;

import java.util.Collection;

public interface CRUDService <T> {
    void create(T item);
    T getById(Integer id);
    Collection<T> getAll();
    void updateById(T item);
    void delete(Integer id);
}

