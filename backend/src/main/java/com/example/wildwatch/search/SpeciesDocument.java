package com.example.wildwatch.search;

import com.example.wildwatch.entity.Species;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "species")
public class SpeciesDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String scientificName;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String commonName;

    @Field(type = FieldType.Keyword)
    private String region;

    public SpeciesDocument() {
    }

    public SpeciesDocument(Long id, String scientificName, String commonName, String region) {
        this.id = id;
        this.scientificName = scientificName;
        this.commonName = commonName;
        this.region = region;
    }

    public static SpeciesDocument fromEntity(Species species, String region) {
        return new SpeciesDocument(
                species.getId(),
                species.getScientificName(),
                species.getCommonName(),
                region
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
