package services;

import model.Conversation;

import java.util.List;

public interface Iservices <T>{
    void ajouter(Conversation conversation);

    void modifier(Conversation conversation);

    public int add(T t);
    public void modify(T t);
    public List<T> afficher();
    void delete(int id);
    public T getOne(int id);

    // For CRUD operations
    List<Conversation> readAll();
}
