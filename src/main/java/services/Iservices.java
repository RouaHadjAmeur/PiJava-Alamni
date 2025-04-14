package services;

import java.util.List;

public interface Iservices <T>{
    public void add(T t);
    public void modify(T t);
    public List<T> afficher();
    void delete(int id);
    public T getOne(int id);
}
