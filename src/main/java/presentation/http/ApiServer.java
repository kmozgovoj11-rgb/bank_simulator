package presentation.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import application.service.CustomerPortalService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public final class ApiServer {
    private final CustomerPortalService portalService;
    private final HttpServer server;

    public ApiServer(CustomerPortalService portalService, int port) throws IOException {
        this.portalService = portalService;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.setExecutor(Executors.newCachedThreadPool());
        registerRoutes();
    }

    public void start() {
        server.start();
    }

    private void registerRoutes() {
        server.createContext("/api/health", this::handleHealth);
        server.createContext("/api/auth/register", this::handleRegister);
        server.createContext("/api/auth/login", this::handleLogin);
        server.createContext("/api/customers", this::handleCustomers);
        server.createContext("/api/accounts", this::handleAccounts);
        server.createContext("/api/transfers", this::handleTransfers);
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!allowMethod(exchange, "GET")) {
            return;
        }
        writeJson(exchange, 200, Map.of("status", "ok"));
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        if (!allowMethod(exchange, "POST")) {
            return;
        }
        try {
            Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
            String fullName = requireString(body, "fullName");
            String phone = requireString(body, "phone");
            String password = requireString(body, "password");
            writeJson(exchange, 201, portalService.registerCustomer(fullName, phone, password));
        } catch (IllegalArgumentException | IllegalStateException e) {
            writeError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            writeError(exchange, 500, "Internal server error");
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        if (!allowMethod(exchange, "POST")) {
            return;
        }
        try {
            Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
            String phone = requireString(body, "phone");
            String password = requireString(body, "password");
            writeJson(exchange, 200, portalService.loginCustomer(phone, password));
        } catch (IllegalArgumentException | IllegalStateException e) {
            writeError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            writeError(exchange, 500, "Internal server error");
        }
    }

    private void handleCustomers(HttpExchange exchange) throws IOException {
        String[] parts = exchange.getRequestURI().getPath().split("/");
        if (parts.length == 5 && "dashboard".equals(parts[4])) {
            if (!allowMethod(exchange, "GET")) {
                return;
            }
            try {
                writeJson(exchange, 200, portalService.getDashboard(parts[3]));
            } catch (IllegalArgumentException e) {
                writeError(exchange, 404, e.getMessage());
            } catch (IllegalStateException e) {
                writeError(exchange, 400, e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                writeError(exchange, 500, "Internal server error");
            }
            return;
        }
        if (parts.length == 5 && "accounts".equals(parts[4])) {
            if (!allowMethod(exchange, "POST")) {
                return;
            }
            try {
                Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
                String accountKind = requireString(body, "accountKind");
                writeJson(exchange, 201, portalService.createAccount(parts[3], accountKind));
            } catch (IllegalArgumentException | IllegalStateException e) {
                writeError(exchange, 400, e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                writeError(exchange, 500, "Internal server error");
            }
            return;
        }
        if (parts.length == 6 && "accounts".equals(parts[4]) && "delete".equals(parts[5])) {
            if (!allowMethod(exchange, "POST")) {
                return;
            }
            try {
                Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
                String accountNumber = requireString(body, "accountNumber");
                writeJson(exchange, 200, portalService.deleteAdditionalAccount(parts[3], accountNumber));
            } catch (IllegalArgumentException | IllegalStateException e) {
                writeError(exchange, 400, e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                writeError(exchange, 500, "Internal server error");
            }
            return;
        }
        if (parts.length == 5 && "deposits".equals(parts[4])) {
            if (!allowMethod(exchange, "POST")) {
                return;
            }
            try {
                Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
                BigDecimal amount = new BigDecimal(requireString(body, "amount"));
                String description = optionalString(body, "description");
                writeJson(exchange, 201, portalService.topUpStandardDebit(parts[3], amount, description));
            } catch (IllegalArgumentException | IllegalStateException e) {
                writeError(exchange, 400, e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                writeError(exchange, 500, "Internal server error");
            }
            return;
        }
        if (parts.length == 5 && "savings-deposits".equals(parts[4])) {
            if (!allowMethod(exchange, "POST")) {
                return;
            }
            try {
                Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
                String accountNumber = requireString(body, "accountNumber");
                BigDecimal amount = new BigDecimal(requireString(body, "amount"));
                String description = optionalString(body, "description");
                writeJson(
                        exchange,
                        201,
                        portalService.fundSavingsFromStandardDebit(
                                parts[3], accountNumber, amount, description));
            } catch (IllegalArgumentException | IllegalStateException e) {
                writeError(exchange, 400, e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                writeError(exchange, 500, "Internal server error");
            }
            return;
        }
        writeError(exchange, 404, "Endpoint not found");
    }

    private void handleAccounts(HttpExchange exchange) throws IOException {
        String[] parts = exchange.getRequestURI().getPath().split("/");
        if (parts.length == 5 && "history".equals(parts[4])) {
            if (!allowMethod(exchange, "GET")) {
                return;
            }
            try {
                writeJson(exchange, 200, portalService.getAccountHistory(parts[3]));
            } catch (IllegalArgumentException e) {
                writeError(exchange, 404, e.getMessage());
            } catch (IllegalStateException e) {
                writeError(exchange, 400, e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                writeError(exchange, 500, "Internal server error");
            }
            return;
        }
        if (parts.length == 5 && "accrue-interest".equals(parts[4])) {
            if (!allowMethod(exchange, "POST")) {
                return;
            }
            try {
                Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
                String customerId = requireString(body, "customerId");
                writeJson(exchange, 200, portalService.accrueInterest(customerId, parts[3]));
            } catch (IllegalArgumentException | IllegalStateException e) {
                writeError(exchange, 400, e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                writeError(exchange, 500, "Internal server error");
            }
            return;
        }
        addCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }
        writeError(exchange, 404, "Endpoint not found");
    }

    private void handleTransfers(HttpExchange exchange) throws IOException {
        if (!allowMethod(exchange, "POST")) {
            return;
        }
        try {
            Map<String, Object> body = JsonUtil.parseObject(readBody(exchange));
            String customerId = requireString(body, "customerId");
            String fromAccountNumber = requireString(body, "fromAccountNumber");
            String recipient = requireString(body, "recipient");
            BigDecimal amount = new BigDecimal(requireString(body, "amount"));
            String description = optionalString(body, "description");
            writeJson(
                    exchange,
                    201,
                    portalService.createTransfer(customerId, fromAccountNumber, recipient, amount, description));
        } catch (IllegalArgumentException | IllegalStateException e) {
            writeError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            writeError(exchange, 500, "Internal server error");
        }
    }

    private boolean allowMethod(HttpExchange exchange, String expectedMethod) throws IOException {
        addCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return false;
        }
        if (!expectedMethod.equalsIgnoreCase(exchange.getRequestMethod())) {
            writeError(exchange, 405, "Method not allowed");
            return false;
        }
        return true;
    }

    private void addCors(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void writeJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        addCors(exchange);
        byte[] bytes = JsonUtil.stringify(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private void writeError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("error", message);
        writeJson(exchange, statusCode, payload);
    }

    private static String requireString(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Field '" + key + "' is required");
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Field '" + key + "' cannot be empty");
        }
        return text;
    }

    private static String optionalString(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }
}
