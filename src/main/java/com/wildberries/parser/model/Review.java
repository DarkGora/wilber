package com.wildberries.parser.model;

import java.time.LocalDateTime;
import java.util.List;

public class Review {
    private LocalDateTime publishDate;
    private String author;
    private String text;
    private int rating;
    private int photoCount;
    private boolean hasVideo;
    private List<String> tags;

    public Review(LocalDateTime publishDate, String author, String text,
                  int rating, int photoCount, boolean hasVideo, List<String> tags) {
        this.publishDate = publishDate;
        this.author = author;
        this.text = text;
        this.rating = rating;
        this.photoCount = photoCount;
        this.hasVideo = hasVideo;
        this.tags = tags;
    }

    // Getters and Setters
    public LocalDateTime getPublishDate() { return publishDate; }
    public void setPublishDate(LocalDateTime publishDate) { this.publishDate = publishDate; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public int getPhotoCount() { return photoCount; }
    public void setPhotoCount(int photoCount) { this.photoCount = photoCount; }

    public boolean isHasVideo() { return hasVideo; }
    public void setHasVideo(boolean hasVideo) { this.hasVideo = hasVideo; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    @Override
    public String toString() {
        return "Review{" +
                "publishDate=" + publishDate +
                ", author='" + author + '\'' +
                ", rating=" + rating +
                ", photoCount=" + photoCount +
                ", hasVideo=" + hasVideo +
                ", tags=" + tags +
                '}';
    }
}