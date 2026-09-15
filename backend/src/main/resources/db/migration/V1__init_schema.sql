CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE species (
    id BIGSERIAL PRIMARY KEY,
    scientific_name VARCHAR(255) NOT NULL UNIQUE,
    common_name VARCHAR(255),
    kingdom VARCHAR(255),
    phylum VARCHAR(255),
    class_name VARCHAR(255),
    order_name VARCHAR(255),
    family VARCHAR(255),
    genus VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE observations (
    id BIGSERIAL PRIMARY KEY,
    species_id BIGINT NOT NULL,
    source VARCHAR(50) NOT NULL CHECK (source IN ('GBIF', 'INATURALIST')),
    observed_date DATE,
    location GEOGRAPHY(Point, 4326),
    place_name VARCHAR(255),
    quality_grade VARCHAR(255),
    photo_url VARCHAR(1000),
    external_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_observations_species FOREIGN KEY (species_id) REFERENCES species(id),
    CONSTRAINT uk_observations_source_external_id UNIQUE (source, external_id)
);

CREATE TABLE yearly_trend_aggregates (
    id BIGSERIAL PRIMARY KEY,
    species_id BIGINT NOT NULL,
    region_name VARCHAR(255) NOT NULL,
    year INTEGER NOT NULL,
    observation_count INTEGER NOT NULL,
    source VARCHAR(255),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_yearly_trend_species FOREIGN KEY (species_id) REFERENCES species(id),
    CONSTRAINT uk_yearly_trend_species_region_year_source UNIQUE (species_id, region_name, year, source)
);
