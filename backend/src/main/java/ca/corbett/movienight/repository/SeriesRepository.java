package ca.corbett.movienight.repository;

import ca.corbett.movienight.model.Series;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SeriesRepository extends JpaRepository<Series, Long> {

    Optional<Series> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
