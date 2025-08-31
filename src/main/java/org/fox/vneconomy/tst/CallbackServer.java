package org.fox.vneconomy.tst;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.fox.vneconomy.VNEconomy;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CallbackServer {
    private HttpServer server;
    private final VNEconomy plugin;

    public CallbackServer(VNEconomy plugin, int port, String path) throws IOException {
        this.plugin = plugin;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(path, new CallbackHandler());
        server.setExecutor(null);
        server.start();
        plugin.getLogger().info("✅ Callback server started at http://0.0.0.0:" + port + path);
    }

    class CallbackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            // đọc body
            BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            // parse body (application/x-www-form-urlencoded)
            Map<String, String> params = parsePostData(sb.toString());

            String content = params.get("content");
            String status = params.get("status");
            int amount = Integer.parseInt(params.getOrDefault("amount", "0"));

            // gọi xử lý trong TheSieuTocAPI
            TheSieuTocAPI.handleCallback(content, status, amount);

            // response cho TST
            String response = "OK";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }

        private Map<String, String> parsePostData(String body) throws UnsupportedEncodingException {
            Map<String, String> map = new HashMap<>();
            for (String pair : body.split("&")) {
                String[] parts = pair.split("=", 2);
                if (parts.length == 2) {
                    map.put(URLDecoder.decode(parts[0], "UTF-8"), URLDecoder.decode(parts[1], "UTF-8"));
                }
            }
            return map;
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("⚠️ Callback server stopped.");
        }
    }
}
