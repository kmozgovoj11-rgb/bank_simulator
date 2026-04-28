package domain.model;

  //Command pattern: Объект банковской операции, который может быть выполнен и отменен.
 
public interface Command {
    void execute();

    void rollback();
}
