package com.example.wildwatch.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.wildwatch.entity.Observation;

@Repository
public interface ObservationRepository extends JpaRepository<Observation, Long> {
        Optional<Observation> findBySourceAndExternalId(String source, String externalId);

        @Query(value = """
                        select EXTRACT(YEAR FROM o.observed_date) as year,
                               count(o.id) as observation_count
                        from observations o
                        join species s on s.id = o.species_id
                                                            where lower(s.scientific_name) = lower(:scientificName)
                                                                    and o.region = :region
                        group by EXTRACT(YEAR FROM o.observed_date)
                        order by year
                        """, nativeQuery = true)
        List<Object[]> findYearlyTrendByScientificNameAndRegion(@Param("scientificName") String scientificName,
                        @Param("region") String region);

        @Query(value = """
                        select s.scientific_name as species_name,
                               o.observed_date as observed_date,
                               ST_Y(o.location::geometry) as lat,
                               ST_X(o.location::geometry) as lng,
                               o.photo_url as photo_url
                        from observations o
                        join species s on s.id = o.species_id
                                                            where ST_DWithin(o.location, ST_MakePoint(:lng, :lat)::geography, :radiusMeters)
                                                                    and (:region is null OR o.region = :region)
                                                            order by o.observed_date desc
                        """, nativeQuery = true)
        List<Object[]> findNearbyObservationsByRegion(@Param("lat") double lat,
                        @Param("lng") double lng,
                        @Param("radiusMeters") double radiusMeters,
                        @Param("region") String region);

        @Modifying
        @Query("UPDATE Observation o SET o.region = :region WHERE o.source = :source AND o.region IS NULL")
        int backfillRegion(@Param("source") String source, @Param("region") String region);

        @Modifying
        @Query(value = """
                        UPDATE observations
                                 SET region = CASE
                                         WHEN place_name ILIKE '%Kerala%' THEN 'Kerala'
                                         WHEN place_name ILIKE '%Tamil Nadu%' THEN 'Tamil Nadu'
                                         WHEN place_name ILIKE '%Maharashtra%' THEN 'Maharashtra'
                                         WHEN place_name ILIKE '%Karnataka%' THEN 'Karnataka'
                                         ELSE region
                                 END
                        """, nativeQuery = true)
        int backfillRegionSmart();
}
