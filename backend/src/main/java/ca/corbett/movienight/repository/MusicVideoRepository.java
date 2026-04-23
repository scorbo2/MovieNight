package ca.corbett.movienight.repository;

import ca.corbett.movienight.model.Artist;
import ca.corbett.movienight.model.MusicVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MusicVideoRepository extends JpaRepository<MusicVideo, Long>, JpaSpecificationExecutor<MusicVideo> {

    List<MusicVideo> findByTitleContainingIgnoreCase(String title);

    List<MusicVideo> findByArtist(Artist artist);

    long countByArtist(Artist artist);

    List<MusicVideo> findByTagsContainingIgnoreCase(String tag);
}
