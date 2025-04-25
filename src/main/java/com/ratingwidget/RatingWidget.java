package com.ratingwidget;

import javafx.scene.control.Control;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.geometry.Insets;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class RatingWidget extends Control {
    private static final String FILLED_STAR = "★";
    private static final String EMPTY_STAR = "☆";
    private static final int MAX_RATING = 5;
    
    private HBox starsContainer;
    private IntegerProperty rating = new SimpleIntegerProperty(0);
    private int hoverRating = 0;

    public RatingWidget() {
        starsContainer = new HBox(5); // 5px spacing between stars
        starsContainer.setPadding(new Insets(5));
        initializeStars();
        getChildren().add(starsContainer);
    }

    private void initializeStars() {
        starsContainer.getChildren().clear();
        for (int i = 0; i < MAX_RATING; i++) {
            final int starRating = i + 1;
            Text star = new Text(EMPTY_STAR);
            star.setStyle("-fx-font-size: 24px;");
            
            star.setOnMouseEntered(e -> {
                hoverRating = starRating;
                updateStars();
            });
            
            star.setOnMouseExited(e -> {
                hoverRating = 0;
                updateStars();
            });
            
            star.setOnMouseClicked(e -> {
                rating.set(starRating);
                updateStars();
            });
            
            starsContainer.getChildren().add(star);
        }
        updateStars();
    }

    private void updateStars() {
        int ratingToShow = hoverRating > 0 ? hoverRating : rating.get();
        for (int i = 0; i < MAX_RATING; i++) {
            Text star = (Text) starsContainer.getChildren().get(i);
            if (i < ratingToShow) {
                star.setText(FILLED_STAR);
                star.setStyle("-fx-font-size: 24px; -fx-fill: gold;");
            } else {
                star.setText(EMPTY_STAR);
                star.setStyle("-fx-font-size: 24px; -fx-fill: gray;");
            }
        }
    }

    public int getRating() {
        return rating.get();
    }

    public void setRating(int newRating) {
        rating.set(newRating);
        updateStars();
    }

    public IntegerProperty ratingProperty() {
        return rating;
    }
} 