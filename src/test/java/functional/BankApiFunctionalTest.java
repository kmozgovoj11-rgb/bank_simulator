package functional;

import application.service.AuthService;
import application.service.BankService;
import application.service.CustomerPortalService;
import infrastructure.persistence.sqlite.Database;
import infrastructure.persistence.sqlite.SqlAccountRepository;
import infrastructure.persistence.sqlite.SqlCustomerRepository;
import infrastructure.persistence.sqlite.SqlTransactionRepository;
import infrastructure.persistence.sqlite.SqlUserRepository;
import infrastructure.persistence.sqlite.SqliteTransactionBroker;
import org.junit.jupiter.api.*;
import presentation.http.ApiServer;

import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Функциональные тесты — end-to-end через HTTP API.
 * Поднимаем ApiServer на случайном порту и делаем реальные HTTP-запросы.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BankApiFunctionalTest {

    private static ApiServer apiServer;
    private static HttpClient http;
    private static String base;
    private static Path dbPath;

    private static String customerId;
    private static String debitAccountNumber;
    private static String savingsAccountNumber;
    private static String senderPhone = "+79001234000";
    private static String recipientPhone = "+79009876543";

    @BeforeAll
    static void startServer() throws Exception {
        dbPath = Path.of(System.getProperty("java.io.tmpdir"),
                "functional-test-" + System.nanoTime() + ".db");

        Database database = new Database(dbPath);
        database.initializeSchema();

        var accountRepository = new SqlAccountRepository(database);
        var customerRepository = new SqlCustomerRepository(database);
        var transactionRepository = new SqlTransactionRepository(database);
        var userRepository = new SqlUserRepository(database);
        var transactionBroker = new SqliteTransactionBroker(database);

        var bankService = new BankService(accountRepository, customerRepository,
                transactionRepository, transactionBroker);
        var authService = new AuthService(userRepository, customerRepository);
        var portalService = new CustomerPortalService(bankService, authService,
                accountRepository, customerRepository);

        int port;
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }

        apiServer = new ApiServer(portalService, port);
        apiServer.start();

        base = "http://localhost:" + port;
        http = HttpClient.newHttpClient();
    }

    @AfterAll
    static void cleanup() {
        dbPath.toFile().delete();
    }

    private HttpResponse<String> post(String path, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base + path))
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @Order(1)
    @DisplayName("Health-check: GET /api/health → 200")
    void healthCheck() throws Exception {
        var resp = get("/api/health");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("ok"));
    }

    @Test
    @Order(2)
    @DisplayName("Регистрация: POST /api/auth/register → 201")
    void registerSender() throws Exception {
        var resp = post("/api/auth/register",
                "{\"fullName\":\"Иван Иванов\",\"phone\":\"" + senderPhone + "\",\"password\":\"pass123\"}");

        assertEquals(201, resp.statusCode(), resp.body());
        assertTrue(resp.body().contains("customerId"));
        customerId = extractField(resp.body(), "customerId");
        assertNotNull(customerId);
    }

    @Test
    @Order(3)
    @DisplayName("Вход: POST /api/auth/login → 200")
    void loginSuccess() throws Exception {
        var resp = post("/api/auth/login",
                "{\"phone\":\"" + senderPhone + "\",\"password\":\"pass123\"}");

        assertEquals(200, resp.statusCode(), resp.body());
    }

    @Test
    @Order(4)
    @DisplayName("Вход с неверным паролем → 400")
    void loginWrongPassword() throws Exception {
        var resp = post("/api/auth/login",
                "{\"phone\":\"" + senderPhone + "\",\"password\":\"wrongpass\"}");

        assertEquals(400, resp.statusCode());
    }

    @Test
    @Order(5)
    @DisplayName("Дашборд: GET /api/customers/{id}/dashboard → 200")
    void dashboard() throws Exception {
        var resp = get("/api/customers/" + customerId + "/dashboard");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("accounts"));
    }

    private String extractField(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return null;
        start += pattern.length();
        int end = json.indexOf('"', start);
        return end == -1 ? null : json.substring(start, end);
    }
}