package ca.corbett.movienight.repository;

import ca.corbett.movienight.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {

    List<Movie> findByTitleContainingIgnoreCase(String title);

    List<Movie> findByWatched(Boolean watched);

    List<Movie> findByGenreIgnoreCase(String genre);

    List<Movie> findByTagsContainingIgnoreCase(String tag);
}
