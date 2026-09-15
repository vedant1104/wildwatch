package com.example.wildwatch.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.wildwatch.entity.Species;

@Repository
public interface SpeciesRepository extends JpaRepository<Species, Long> {
    Optional<Species> findByScientificName(String scientificName);

        @Query("select distinct s from Species s join Observation o on o.species = s " +
            "where o.region = :region")
        List<Species> findDistinctByRegionContaining(@Param("region") String region);

        @Query("SELECT new com.example.wildwatch.dto.SpeciesSummaryDto(s.scientificName, s.commonName, COUNT(o.id)) " +
            "FROM Observation o JOIN o.species s " +
            "WHERE o.region = :region " +
            "GROUP BY s.id, s.scientificName, s.commonName " +
            "ORDER BY COUNT(o.id) DESC")
        List<com.example.wildwatch.dto.SpeciesSummaryDto> findSpeciesSummaryByRegion(@Param("region") String region);
}
