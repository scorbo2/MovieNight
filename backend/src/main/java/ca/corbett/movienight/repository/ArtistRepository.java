package ca.corbett.movienight.repository;

import ca.corbett.movienight.model.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    Optional<Artist> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
