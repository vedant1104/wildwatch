package com.example.wildwatch.search;

import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpeciesSearchRepository extends ElasticsearchRepository<SpeciesDocument, Long> {

    @Query("""
        {
          "multi_match": {
            "query": "?0",
            "fields": ["scientificName^3", "commonName^2"],
            "type": "best_fields",
            "fuzziness": "AUTO"
          }
        }
        """)
    List<SpeciesDocument> searchByQuery(String query);
}
