package com.wildberries.parser.service;

import com.wildberries.parser.model.Review;
import com.wildberries.parser.service.CsvWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void testWriteReviewsToCsv() throws IOException {
        // Подготовка тестовых данных
        List<Review> reviews = Arrays.asList(
                new Review(
                        LocalDateTime.of(2024, 1, 15, 14, 30),
                        "Иван Петров",
                        "Отличный товар, всем рекомендую!",
                        5,
                        2,
                        true,
                        Arrays.asList("качество", "доставка")
                ),
                new Review(
                        LocalDateTime.of(2024, 1, 14, 10, 15),
                        "Мария Сидорова",
                        "Нормально, но есть недочёты",
                        3,
                        0,
                        false,
                        Arrays.asList("цена")
                )
        );

        // Создание CSV файла
        CsvWriter csvWriter = new CsvWriter();
        Path csvFile = tempDir.resolve("test_reviews.csv");
        csvWriter.writeReviewsToCsv(reviews, csvFile.toString());

        // Проверка существования файла
        assertTrue(Files.exists(csvFile));

        // Проверка содержимого файла
        List<String> lines = Files.readAllLines(csvFile);
        assertFalse(lines.isEmpty());
        assertEquals(3, lines.size()); // Заголовок + 2 строки данных

        // Проверка заголовка
        assertTrue(lines.get(0).contains("Дата публикации"));
        assertTrue(lines.get(0).contains("Автор"));

        // Проверка данных
        assertTrue(lines.get(1).contains("Иван Петров"));
        assertTrue(lines.get(2).contains("Мария Сидорova"));
    }

    @Test
    void testCsvEscaping() {
        CsvWriter csvWriter = new CsvWriter();

        // Тест экранирования кавычек
        String result = csvWriter.escapeCsvField("Текст с \"кавычками\"");
        assertEquals("Текст с \"\"кавычками\"\"", result);

        // Тест null значения
        assertEquals("", csvWriter.escapeCsvField(null));

        // Тест пустой строки
        assertEquals("", csvWriter.escapeCsvField(""));
    }
}