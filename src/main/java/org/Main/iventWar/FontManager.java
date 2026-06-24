package org.Main.iventWar;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class FontManager {
    private final IventWar plugin;
    private final Map<String, Map<Character, Character>> fonts;

    public FontManager(IventWar plugin) {
        this.plugin = plugin;
        this.fonts = new HashMap<>();
        loadAllFonts();
    }

    // Загружаем все шрифты из папки resources/fonts/
    private void loadAllFonts() {
        String[] fontNames = {"1", "2", "3", "4", "5"};
        for (String name : fontNames) {
            loadFont(name);
        }
        plugin.getLogger().info("Загружено " + fonts.size() + " шрифтов");
    }

    // Загружаем один шрифт из файла
    private void loadFont(String fontName) {
        Map<Character, Character> fontMap = new HashMap<>();
        String resourcePath = "fonts/" + fontName + ".txt";

        try (InputStream input = plugin.getResource(resourcePath);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(input, StandardCharsets.UTF_8))) {

            if (input == null) {
                plugin.getLogger().warning("Шрифт " + fontName + " не найден! Использую стандартный.");
                fonts.put(fontName, new HashMap<>());
                return;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Пропускаем пустые строки и комментарии
                if (line.isEmpty() || line.startsWith("#")) continue;

                // Парсим: A=𝐀
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String from = parts[0].trim();
                    String to = parts[1].trim();
                    if (from.length() == 1 && to.length() == 1) {
                        fontMap.put(from.charAt(0), to.charAt(0));
                    } else {
                        plugin.getLogger().warning("Некорректная строка в шрифте " + fontName + ": " + line);
                    }
                }
            }

            fonts.put(fontName, fontMap);
            plugin.getLogger().info("Загружен шрифт " + fontName + " (" + fontMap.size() + " замен)");

        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось загрузить шрифт " + fontName + ": " + e.getMessage());
            fonts.put(fontName, new HashMap<>());
        }
    }

    // Перезагрузить все шрифты
    public void reloadFonts() {
        fonts.clear();
        loadAllFonts();
    }

    // Применить шрифт к тексту
    public String applyFont(String text, String fontName) {
        if (fontName.equals("1") || !fonts.containsKey(fontName)) {
            return text;
        }

        Map<Character, Character> fontMap = fonts.get(fontName);
        if (fontMap == null || fontMap.isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            result.append(fontMap.getOrDefault(c, c));
        }
        return result.toString();
    }

    // Проверка существования шрифта
    public boolean hasFont(String name) {
        return fonts.containsKey(name);
    }

    // Получить список доступных шрифтов
    public String[] getAvailableFonts() {
        return fonts.keySet().toArray(new String[0]);
    }
}