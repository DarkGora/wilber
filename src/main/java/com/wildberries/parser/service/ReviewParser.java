package com.wildberries.parser.service;

import com.wildberries.parser.model.Review;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReviewParser {
    private final WebDriver driver;
    private final WebDriverWait wait;

    public ReviewParser(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public List<Review> parseReviews(String url) {
        List<Review> reviews = new ArrayList<>();

        try {
            System.out.println("🔄 Открываем страницу: " + url);
            driver.get(url);

            // Ждем загрузки страницы
            Thread.sleep(8000);

            System.out.println("📄 Текущий URL: " + driver.getCurrentUrl());
            System.out.println("🏷️ Заголовок страницы: " + driver.getTitle());

            // Проверяем, не произошел ли редирект или блокировка
            if (driver.getCurrentUrl().contains("blocked") ||
                    driver.getCurrentUrl().contains("captcha") ||
                    !driver.getCurrentUrl().contains("wildberries")) {
                System.out.println("❌ Возможная блокировка или редирект");
                savePageSourceForDebug("blocked_page");
                return reviews;
            }

            // Сохраняем HTML для отладки
            savePageSourceForDebug("initial_page");

            // Прокручиваем страницу для загрузки отзывов
            scrollPage();

            // Ждем еще немного после прокрутки
            Thread.sleep(5000);

            // Сохраняем HTML после прокрутки
            savePageSourceForDebug("after_scroll");

            // Ищем отзывы
            List<WebElement> reviewElements = findReviewElements();

            System.out.println("🔍 Найдено потенциальных элементов отзывов: " + reviewElements.size());

            // Если не нашли стандартными методами, пробуем альтернативные подходы
            if (reviewElements.isEmpty()) {
                reviewElements = findReviewElementsAlternative();
                System.out.println("🔍 Найдено альтернативными методами: " + reviewElements.size());
            }

            for (int i = 0; i < reviewElements.size(); i++) {
                try {
                    System.out.println("📝 Парсим отзыв " + (i + 1) + "...");
                    WebElement reviewElement = reviewElements.get(i);

                    // Сохраняем HTML элемента для отладки
                    saveElementHtml(reviewElement, "review_element_" + (i + 1));

                    Review review = parseSingleReview(reviewElement);
                    if (review != null && isValidReview(review)) {
                        reviews.add(review);
                        System.out.println("✅ Успешно распарсен отзыв от: " + review.getAuthor());
                    } else {
                        System.out.println("❌ Отзыв не прошел валидацию");
                    }
                } catch (Exception e) {
                    System.err.println("❌ Ошибка парсинга отзыва " + (i + 1) + ": " + e.getMessage());
                }
            }

            System.out.println("📊 Итог: успешно распарсено " + reviews.size() + " отзывов");

        } catch (Exception e) {
            System.err.println("💥 Ошибка при загрузке страницы: " + e.getMessage());
            e.printStackTrace();
        }

        return reviews;
    }

    private void scrollPage() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            // Прокручиваем вниз
            js.executeScript("window.scrollTo(0, document.body.scrollHeight/2);");
            Thread.sleep(2000);
            js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
            Thread.sleep(2000);
            // Прокручиваем обратно к отзывам
            js.executeScript("window.scrollTo(0, 500);");
        } catch (Exception e) {
            System.err.println("Ошибка при прокрутке: " + e.getMessage());
        }
    }

    private List<WebElement> findReviewElements() {
        List<WebElement> elements = new ArrayList<>();

        // Актуальные селекторы для Wildberries 2024
        String[] selectors = {
                // Основные селекторы отзывов
                "div.feedback__item",
                "div.feedback__wrapper",
                "div.feedback-item",
                "div[data-tag*='feedback']",
                "div[class*='feedback']",

                // Альтернативные селекторы
                ".feedback",
                ".review",
                ".comment",
                "[id*='feedback']",

                // Более общие селекторы
                "div[class*='item']",
                "div[class*='card']",
                "div[class*='content']"
        };

        for (String selector : selectors) {
            try {
                List<WebElement> found = driver.findElements(By.cssSelector(selector));
                if (!found.isEmpty()) {
                    System.out.println("🎯 Найдено с селектором '" + selector + "': " + found.size());
                    elements.addAll(found);
                }
            } catch (Exception e) {
                System.out.println("⚠️ Ошибка с селектором '" + selector + "': " + e.getMessage());
            }
        }

        return elements;
    }

    private List<WebElement> findReviewElementsAlternative() {
        List<WebElement> elements = new ArrayList<>();

        try {
            // Ищем по тексту "отзыв", "рейтинг" и т.д.
            String[] xpaths = {
                    "//*[contains(text(), 'отзыв') or contains(text(), 'Отзыв')]//ancestor::div[contains(@class, 'item') or contains(@class, 'card')]",
                    "//*[contains(text(), 'оценк') or contains(text(), 'Оценк')]//ancestor::div[1]",
                    "//div[contains(@class, 'rating') or contains(@class, 'star')]//ancestor::div[1]",
                    "//*[contains(text(), 'покупатель') or contains(text(), 'Покупатель')]//ancestor::div[1]"
            };

            for (String xpath : xpaths) {
                try {
                    List<WebElement> found = driver.findElements(By.xpath(xpath));
                    if (!found.isEmpty()) {
                        System.out.println("🎯 Найдено с XPath: " + found.size());
                        elements.addAll(found);
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Ошибка с XPath: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Ошибка в альтернативном поиске: " + e.getMessage());
        }

        return elements;
    }

    private Review parseSingleReview(WebElement reviewElement) {
        try {
            String author = parseAuthor(reviewElement);
            String text = parseText(reviewElement);
            int rating = parseRating(reviewElement);

            System.out.println("👤 Автор: " + author);
            System.out.println("📝 Текст: " + (text != null ? text.substring(0, Math.min(50, text.length())) + "..." : "null"));
            System.out.println("⭐ Рейтинг: " + rating);

            LocalDateTime publishDate = parseDate(reviewElement);
            int photoCount = parsePhotoCount(reviewElement);
            boolean hasVideo = parseHasVideo(reviewElement);
            List<String> tags = parseTags(reviewElement);

            return new Review(publishDate,
                    author != null ? author : "Анонимный покупатель",
                    text != null ? text : "",
                    rating, photoCount, hasVideo, tags);

        } catch (Exception e) {
            System.err.println("❌ Ошибка парсинга элемента отзыва: " + e.getMessage());
            return null;
        }
    }

    private boolean isValidReview(Review review) {
        return review != null &&
                review.getAuthor() != null &&
                !review.getAuthor().isEmpty() &&
                (review.getText() != null && !review.getText().isEmpty()) || review.getRating() > 0;
    }

    private LocalDateTime parseDate(WebElement element) {
        try {
            String[] selectors = {
                    ".feedback__date", ".date", "[class*='date']",
                    ".time", "[class*='time']"
            };

            for (String selector : selectors) {
                try {
                    WebElement dateElement = element.findElement(By.cssSelector(selector));
                    String dateText = dateElement.getText().trim();
                    if (!dateText.isEmpty()) {
                        System.out.println("📅 Найдена дата: " + dateText);
                        // Пробуем разные форматы дат
                        try {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.forLanguageTag("ru"));
                            return LocalDateTime.parse(dateText + " 12:00", DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                        } catch (Exception e) {
                            // Если не получилось, используем текущую дату
                            return LocalDateTime.now();
                        }
                    }
                } catch (Exception e) {
                    // Пробуем следующий селектор
                }
            }
            return LocalDateTime.now();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private String parseAuthor(WebElement element) {
        try {
            String[] selectors = {
                    ".feedback__name", ".author", "[class*='name']",
                    ".user-name", ".username", ".buyer", ".customer"
            };

            for (String selector : selectors) {
                try {
                    WebElement authorElement = element.findElement(By.cssSelector(selector));
                    String author = authorElement.getText().trim();
                    if (!author.isEmpty() && !author.equals("Пользователь")) {
                        return author;
                    }
                } catch (Exception e) {
                    // Пробуем следующий селектор
                }
            }

            // Пробуем найти любой текст, который может быть именем
            String elementText = element.getText();
            String[] lines = elementText.split("\n");
            for (String line : lines) {
                if (line.length() > 2 && line.length() < 50 &&
                        !line.contains("@") && !line.contains("http") &&
                        !line.toLowerCase().contains("отзыв") &&
                        !line.toLowerCase().contains("оценк") &&
                        !line.toLowerCase().contains("фото") &&
                        !line.toLowerCase().contains("видео")) {
                    return line.trim();
                }
            }

            return "Анонимный покупатель";
        } catch (Exception e) {
            return "Анонимный покупатель";
        }
    }

    private String parseText(WebElement element) {
        try {
            String[] selectors = {
                    ".feedback__text", ".text", "[class*='text']",
                    ".review-text", ".comment-text", ".content", ".message"
            };

            for (String selector : selectors) {
                try {
                    WebElement textElement = element.findElement(By.cssSelector(selector));
                    String text = textElement.getText().trim();
                    if (!text.isEmpty()) {
                        return text.replace("\n", " ").replace("\"", "\"\"");
                    }
                } catch (Exception e) {
                    // Пробуем следующий селектор
                }
            }

            // Альтернативный подход: ищем самый длинный текст в элементе
            String fullText = element.getText();
            String[] lines = fullText.split("\n");
            String longestLine = "";
            for (String line : lines) {
                if (line.length() > longestLine.length() &&
                        line.length() > 20 &&
                        !line.toLowerCase().contains("отзыв") &&
                        !line.toLowerCase().contains("оценк") &&
                        !line.toLowerCase().contains("фото") &&
                        !line.toLowerCase().contains("видео") &&
                        !line.contains("@") &&
                        !line.contains("http")) {
                    longestLine = line;
                }
            }

            return longestLine.isEmpty() ? "Текст отзыва недоступен" : longestLine;
        } catch (Exception e) {
            return "Текст отзыва недоступен";
        }
    }

    private int parseRating(WebElement element) {
        try {
            // Ищем звезды рейтинга
            String[] selectors = {
                    ".feedback__rating", ".rating", "[class*='rating']",
                    "[class*='star']", ".stars"
            };

            for (String selector : selectors) {
                try {
                    WebElement ratingContainer = element.findElement(By.cssSelector(selector));
                    // Ищем заполненные звезды
                    List<WebElement> activeStars = ratingContainer.findElements(
                            By.cssSelector(".active, .fill, .filled, [class*='active'], [class*='fill']")
                    );
                    if (!activeStars.isEmpty()) {
                        return activeStars.size();
                    }

                    // Ищем по цвету или другим атрибутам
                    List<WebElement> allStars = ratingContainer.findElements(By.cssSelector("*"));
                    int activeCount = 0;
                    for (WebElement star : allStars) {
                        String style = star.getAttribute("style");
                        String className = star.getAttribute("class");
                        if ((style != null && style.contains("fill")) ||
                                (className != null && (className.contains("active") || className.contains("fill")))) {
                            activeCount++;
                        }
                    }
                    if (activeCount > 0) {
                        return activeCount;
                    }
                } catch (Exception e) {
                    // Пробуем следующий селектор
                }
            }

            return 5; // Дефолтное значение
        } catch (Exception e) {
            return 5;
        }
    }

    private int parsePhotoCount(WebElement element) {
        try {
            List<WebElement> photos = element.findElements(
                    By.cssSelector(".feedback__photos img, .photos img, [class*='photo'] img, img")
            );
            return photos.size();
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean parseHasVideo(WebElement element) {
        try {
            element.findElement(By.cssSelector(".feedback__video, .video, [class*='video'], iframe"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    private List<String> parseTags(WebElement element) {
        List<String> tags = new ArrayList<>();
        try {
            List<WebElement> tagElements = element.findElements(
                    By.cssSelector(".feedback__tags .tag, .tags span, [class*='tag']")
            );
            for (WebElement tagElement : tagElements) {
                String tag = tagElement.getText().trim();
                if (!tag.isEmpty()) {
                    tags.add(tag);
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки парсинга тегов
        }
        return tags;
    }

    private void savePageSourceForDebug(String suffix) {
        try {
            String pageSource = driver.getPageSource();
            Files.write(Paths.get("debug_page_" + suffix + ".html"), pageSource.getBytes());
            System.out.println("💾 Сохранена страница для отладки: debug_page_" + suffix + ".html");
        } catch (IOException e) {
            System.err.println("❌ Ошибка сохранения страницы для отладки: " + e.getMessage());
        }
    }

    private void saveElementHtml(WebElement element, String filename) {
        try {
            String elementHtml = element.getAttribute("outerHTML");
            Files.write(Paths.get(filename + ".html"), elementHtml.getBytes());
        } catch (Exception e) {
            // Игнорируем ошибки сохранения HTML элемента
        }
    }
}