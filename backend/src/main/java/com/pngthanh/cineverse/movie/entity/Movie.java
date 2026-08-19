package com.pngthanh.cineverse.movie.entity;

import com.pngthanh.cineverse.common.enums.MovieStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "movies")
public class Movie {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 160)
    private String title;
    @Column(nullable = false, length = 2000)
    private String description;
    @Column(nullable = false, length = 200)
    private String genres;
    @Column(nullable = false)
    private Integer durationMinutes;
    private LocalDate releaseDate;
    private LocalDate endDate;
    private String director;
    @Column(length = 1000)
    private String castNames;
    private String ageRating;
    @Column(length = 1000)
    private String posterUrl;
    @Column(length = 1000)
    private String backdropUrl;
    @Column(length = 1000)
    private String trailerUrl;
    private Long ticketsSold = 0L;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private MovieStatus status = MovieStatus.NOW_SHOWING;

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getGenres() { return genres; }
    public void setGenres(String genres) { this.genres = genres; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }
    public String getCastNames() { return castNames; }
    public void setCastNames(String castNames) { this.castNames = castNames; }
    public String getAgeRating() { return ageRating; }
    public void setAgeRating(String ageRating) { this.ageRating = ageRating; }
    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    public String getBackdropUrl() { return backdropUrl; }
    public void setBackdropUrl(String backdropUrl) { this.backdropUrl = backdropUrl; }
    public String getTrailerUrl() { return trailerUrl; }
    public void setTrailerUrl(String trailerUrl) { this.trailerUrl = trailerUrl; }
    public Long getTicketsSold() { return ticketsSold; }
    public void setTicketsSold(Long ticketsSold) { this.ticketsSold = ticketsSold; }
    public MovieStatus getStatus() { return status; }
    public void setStatus(MovieStatus status) { this.status = status; }
}
