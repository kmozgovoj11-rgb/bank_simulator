package infrastructure.persistence.sqlite;
import java.sql.Connection;//библиотека для взаимодействия с бд
//хранилище соединения с БД которое привязано к текущему потоку выполнения
final class SqliteConnectionContext {
    private static final ThreadLocal<Connection> CURRENT = new ThreadLocal<>();//создаем потоки для будущего веб приложения

    private SqliteConnectionContext() {}//показать, что класс не для создания объектов

    static Connection get() {
        return CURRENT.get();
    }

    static void set(Connection connection) {
        CURRENT.set(connection);
    }

    static void clear() {
        CURRENT.remove();
    }
}
