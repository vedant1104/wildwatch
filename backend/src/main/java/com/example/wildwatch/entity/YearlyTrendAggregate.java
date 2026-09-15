package com.example.wildwatch.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "yearly_trend_aggregates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_yearly_trend_species_region_year_source",
                        columnNames = {"species_id", "region_name", "year", "source"})
        }
)
public class YearlyTrendAggregate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "species_id", nullable = false)
    private Species species;

    @Column(name = "region_name", nullable = false)
    private String regionName;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "observation_count", nullable = false)
    private Integer observationCount;

    @Column(name = "source")
    private String source;

    @CreationTimestamp
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    protected YearlyTrendAggregate() {
    }

    public YearlyTrendAggregate(Species species, String regionName, Integer year,
                               Integer observationCount, String source) {
        this.species = species;
        this.regionName = regionName;
        this.year = year;
        this.observationCount = observationCount;
        this.source = source;
    }

    public Long getId() {
        return id;
    }

    public Species getSpecies() {
        return species;
    }

    public void setSpecies(Species species) {
        this.species = species;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getObservationCount() {
        return observationCount;
    }

    public void setObservationCount(Integer observationCount) {
        this.observationCount = observationCount;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
}
