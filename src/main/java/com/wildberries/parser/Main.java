package com.wildberries.parser;

import com.wildberries.parser.model.Review;
import com.wildberries.parser.service.DriverManager;
import com.wildberries.parser.service.ReviewParser;
import com.wildberries.parser.service.CsvWriter;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        DriverManager driverManager = null;

        try {
            System.out.println("Запуск парсера отзывов Wildberries...");

            // Инициализация DriverManager
            driverManager = new DriverManager();

            // Создание парсера
            ReviewParser parser = new ReviewParser(
                    driverManager.getDriver(),
                    driverManager.getWait()
            );

            // URL для парсинга
            String url = "https://www.wildberries.ru/catalog/521896959/feedbacks?imtId=234818091&size=720932801";

            // Парсинг отзывов
            System.out.println("Парсинг отзывов...");
            List<Review> reviews = parser.parseReviews(url);

            System.out.println("Найдено отзывов: " + reviews.size());

            // Сохранение в CSV
            CsvWriter csvWriter = new CsvWriter();
            csvWriter.writeReviewsToCsv(reviews, "wildberries_reviews.csv");

        } catch (Exception e) {
            System.err.println("Критическая ошибка: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driverManager != null) {
                driverManager.closeDriver();
            }
        }
    }
}