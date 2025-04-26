package com.ratingwidget;

import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.geometry.Insets;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;

public class RatingWidget extends HBox {
    private static final String FILLED_STAR = "★";
    private static final String EMPTY_STAR = "☆";
    private static final int MAX_RATING = 5;
    private static final int NO_REVIEW = -1;
    private static final int NEW_RECLAMATION = -2;
    
    private IntegerProperty rating = new SimpleIntegerProperty(NEW_RECLAMATION);
    private BooleanProperty disabled = new SimpleBooleanProperty(false);
    private StringProperty filledStarStyle = new SimpleStringProperty("-fx-font-size: 24px; -fx-fill: gold;");
    private StringProperty emptyStarStyle = new SimpleStringProperty("-fx-font-size: 24px; -fx-fill: gray;");
    private int hoverRating = 0;

    public RatingWidget() {
        super(5); // 5px spacing
        setPadding(new Insets(5));
        initializeStars();
        // Listen to disabled property changes
        disabled.addListener((obs, oldVal, newVal) -> {
            setDisable(newVal);
            updateStars();
        });
    }

    private void initializeStars() {
        getChildren().clear();
        for (int i = 0; i < MAX_RATING; i++) {
            final int starRating = i + 1;
            Text star = new Text(EMPTY_STAR);
            star.setStyle(emptyStarStyle.get());
            
            star.setOnMouseEntered(e -> {
                if (!disabled.get()) {
                    hoverRating = starRating;
                    updateStars();
                }
            });
            
            star.setOnMouseExited(e -> {
                if (!disabled.get()) {
                    hoverRating = 0;
                    updateStars();
                }
            });
            
            star.setOnMouseClicked(e -> {
                if (!disabled.get()) {
                    rating.set(starRating);
                    updateStars();
                }
            });
            
            getChildren().add(star);
        }
        updateStars();
    }

    private void updateStars() {
        int currentRating = rating.get();
        
        // Gérer les cas spéciaux
        if (currentRating == NEW_RECLAMATION) {
            getChildren().clear();
            Text newText = new Text("_");
            newText.setStyle("-fx-font-size: 24px; -fx-fill: gray;");
            getChildren().add(newText);
            return;
        }
        
        if (currentRating == NO_REVIEW) {
            getChildren().clear();
            Text noReviewText = new Text("No review");
            noReviewText.setStyle("-fx-font-size: 24px; -fx-fill: gray;");
            getChildren().add(noReviewText);
            return;
        }
        
        // Réinitialiser le conteneur si nécessaire
        if (getChildren().size() != MAX_RATING) {
            initializeStars();
        }
        
        // Afficher les étoiles normalement
        int ratingToShow = hoverRating > 0 ? hoverRating : currentRating;
        for (int i = 0; i < MAX_RATING; i++) {
            Text star = (Text) getChildren().get(i);
            if (i < ratingToShow) {
                star.setText(FILLED_STAR);
                star.setStyle(filledStarStyle.get());
            } else {
                star.setText(EMPTY_STAR);
                star.setStyle(emptyStarStyle.get());
            }
        }
    }

    public int getRating() {
        return rating.get();
    }

    public void setRating(int newRating) {
        if (newRating >= NO_REVIEW && newRating <= MAX_RATING) {
            rating.set(newRating);
            updateStars();
        }
    }

    public IntegerProperty ratingProperty() {
        return rating;
    }

    public void setWidgetDisabled(boolean value) {
        this.disabled.set(value);
    }

    public boolean isWidgetDisabled() {
        return this.disabled.get();
    }

    public String getFilledStarStyle() {
        return filledStarStyle.get();
    }

    public void setFilledStarStyle(String style) {
        this.filledStarStyle.set(style);
        updateStars();
    }

    public StringProperty filledStarStyleProperty() {
        return filledStarStyle;
    }

    public String getEmptyStarStyle() {
        return emptyStarStyle.get();
    }

    public void setEmptyStarStyle(String style) {
        this.emptyStarStyle.set(style);
        updateStars();
    }

    public StringProperty emptyStarStyleProperty() {
        return emptyStarStyle;
    }

    public void reset() {
        rating.set(NEW_RECLAMATION);
        hoverRating = 0;
        updateStars();
    }
    
    public void setNoReview() {
        rating.set(NO_REVIEW);
        updateStars();
    }
} 