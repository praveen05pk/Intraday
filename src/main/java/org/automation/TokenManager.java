package org.automation;

import java.io.*;
import java.util.*;

public class TokenManager {

        private static final String TOKEN_FILE = "token.csv";

        public static void saveTokens(String accessToken, String refreshToken) throws IOException {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(TOKEN_FILE))) {
                writer.write("access_token,refresh_token\n");
                writer.write(accessToken + "," + refreshToken + "\n");
            }
        }

        public static Map<String, String> loadTokens() throws IOException {
            Map<String, String> tokens = new HashMap<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(TOKEN_FILE))) {
                reader.readLine(); // skip header
                String line = reader.readLine();
                if (line != null) {
                    String[] parts = line.split(",");
                    tokens.put("access_token", parts[0]);
                    tokens.put("refresh_token", parts[1]);
                }
            }
            return tokens;
        }
    }


