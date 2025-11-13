package com.wildberries.parser.service;

import com.wildberries.parser.model.Review;

import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CsvWriter {

    public void writeReviewsToCsv(List<Review> reviews, String filename) {
        try (FileWriter writer = new FileWriter(filename, java.nio.charset.StandardCharsets.UTF_8)) {
            // Записываем заголовок
            writer.write("Дата публикации,Автор,Текст отзыва,Оценка,Количество фотографий,Наличие видео,Теги\n");

            // Записываем данные
            for (Review review : reviews) {
                String line = buildCsvLine(review);
                writer.write(line + "\n");
            }

            System.out.println("Успешно сохранено " + reviews.size() + " отзывов в файл: " + filename);

        } catch (IOException e) {
            System.err.println("Ошибка записи в CSV файл: " + e.getMessage());
        }
    }

    private String buildCsvLine(Review review) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return String.format("\"%s\",\"%s\",\"%s\",%d,%d,%s,\"%s\"",
                review.getPublishDate().format(formatter),
                escapeCsvField(review.getAuthor()),
                escapeCsvField(review.getText()),
                review.getRating(),
                review.getPhotoCount(),
                review.isHasVideo() ? "Да" : "Нет",
                String.join("; ", review.getTags())
        );
    }

    private String escapeCsvField(String field) {
        if (field == null) return "";
        return field.replace("\"", "\"\"");
    }
}