package ca.corbett.movienight;

import ca.corbett.movienight.model.Artist;
import ca.corbett.movienight.model.MusicVideo;
import ca.corbett.movienight.repository.ArtistRepository;
import ca.corbett.movienight.repository.MusicVideoRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class MusicVideoRepositoryTest {

    @Autowired
    private MusicVideoRepository musicVideoRepository;

    @Autowired
    private ArtistRepository artistRepository;

    private Artist beatles;
    private Artist queen;
    private Artist radiohead;

    @BeforeEach
    public void setup() {
        beatles = new Artist("The Beatles", "Legendary British rock band.");
        queen = new Artist("Queen", "British rock band.");
        radiohead = new Artist("Radiohead", "Alternative rock band.");
        artistRepository.save(beatles);
        artistRepository.save(queen);
        artistRepository.save(radiohead);
    }

    @Test
    void saveAndFindMusicVideo() {
        MusicVideo mv = new MusicVideo("Hey Jude", beatles, null, 1968, "A classic Beatles song.");
        mv.setVideoFilePath("/music-videos/beatles/hey_jude.mp4");
        musicVideoRepository.save(mv);

        List<MusicVideo> all = musicVideoRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getTitle()).isEqualTo("Hey Jude");
        assertThat(all.get(0).getArtist().getName()).isEqualTo("The Beatles");
    }

    @Test
    void findByTitleContainingIgnoreCase() {
        MusicVideo mv1 = new MusicVideo("Bohemian Rhapsody", queen, "A Night at the Opera", 1975, "Queen classic.");
        mv1.setVideoFilePath("/music-videos/queen/bohemian_rhapsody.mp4");
        musicVideoRepository.save(mv1);
        MusicVideo mv2 = new MusicVideo("We Will Rock You", queen, "News of the World", 1977, "Anthem.");
        mv2.setVideoFilePath("/music-videos/queen/we_will_rock_you.mp4");
        musicVideoRepository.save(mv2);

        List<MusicVideo> results = musicVideoRepository.findByTitleContainingIgnoreCase("bohemian");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Bohemian Rhapsody");
    }

    @Test
    void saveAndFindMusicVideoWithTags() {
        MusicVideo mv = new MusicVideo("Creep", radiohead, "Pablo Honey", 1993, "Radiohead hit.");
        mv.setVideoFilePath("/music-videos/radiohead/creep.mp4");
        mv.setTags(List.of("Alternative", "Grunge", "Must-See"));
        musicVideoRepository.save(mv);

        List<MusicVideo> all = musicVideoRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getTags()).containsExactlyInAnyOrder("alternative", "grunge", "must-see");
    }

    @Test
    void tagsAreNormalizedToLowercase() {
        MusicVideo mv = new MusicVideo("Karma Police", radiohead, "OK Computer", 1997, "OK Computer track.");
        mv.setVideoFilePath("/music-videos/radiohead/karma_police.mp4");
        mv.setTags(List.of("Rock", "ROCK", "rock", "  Rock  "));
        musicVideoRepository.save(mv);

        List<MusicVideo> all = musicVideoRepository.findAll();
        assertThat(all.get(0).getTags()).containsExactly("rock");
    }

    @Test
    void findByTagsContainingIgnoreCase() {
        MusicVideo mv1 = new MusicVideo("Let It Be", beatles, "Let It Be", 1970, "Beatles classic.");
        mv1.setVideoFilePath("/music-videos/beatles/let_it_be.mp4");
        mv1.setTags(List.of("classic", "pop"));

        MusicVideo mv2 = new MusicVideo("Under Pressure", queen, "Hot Space", 1982, "Queen and Bowie.");
        mv2.setVideoFilePath("/music-videos/queen/under_pressure.mp4");
        mv2.setTags(List.of("rock", "collaboration"));

        musicVideoRepository.saveAll(List.of(mv1, mv2));

        List<MusicVideo> results = musicVideoRepository.findByTagsContainingIgnoreCase("classic");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Let It Be");
    }

    @Test
    void countByArtist() {
        MusicVideo mv1 = new MusicVideo("Come Together", beatles, "Abbey Road", 1969, null);
        mv1.setVideoFilePath("/music-videos/beatles/come_together.mp4");
        MusicVideo mv2 = new MusicVideo("Hey Jude", beatles, null, 1968, null);
        mv2.setVideoFilePath("/music-videos/beatles/hey_jude.mp4");
        MusicVideo mv3 = new MusicVideo("Bohemian Rhapsody", queen, "A Night at the Opera", 1975, null);
        mv3.setVideoFilePath("/music-videos/queen/bohemian_rhapsody.mp4");
        musicVideoRepository.saveAll(List.of(mv1, mv2, mv3));

        long beatlesCount = musicVideoRepository.countByArtist(beatles);
        long queenCount = musicVideoRepository.countByArtist(queen);
        long radioheadCount = musicVideoRepository.countByArtist(radiohead);

        assertThat(beatlesCount).isEqualTo(2);
        assertThat(queenCount).isEqualTo(1);
        assertThat(radioheadCount).isEqualTo(0);
    }

    @Test
    void findByArtist() {
        MusicVideo mv1 = new MusicVideo("Come Together", beatles, "Abbey Road", 1969, null);
        mv1.setVideoFilePath("/music-videos/beatles/come_together.mp4");
        MusicVideo mv2 = new MusicVideo("Hey Jude", beatles, null, 1968, null);
        mv2.setVideoFilePath("/music-videos/beatles/hey_jude.mp4");
        MusicVideo mv3 = new MusicVideo("Bohemian Rhapsody", queen, "A Night at the Opera", 1975, null);
        mv3.setVideoFilePath("/music-videos/queen/bohemian_rhapsody.mp4");
        musicVideoRepository.saveAll(List.of(mv1, mv2, mv3));

        List<MusicVideo> beatlesVideos = musicVideoRepository.findByArtist(beatles);
        List<MusicVideo> queenVideos = musicVideoRepository.findByArtist(queen);
        List<MusicVideo> radioheadVideos = musicVideoRepository.findByArtist(radiohead);

        assertThat(beatlesVideos).hasSize(2)
                .extracting(MusicVideo::getTitle)
                .containsExactlyInAnyOrder("Come Together", "Hey Jude");
        assertThat(queenVideos).hasSize(1)
                .extracting(MusicVideo::getTitle)
                .containsExactly("Bohemian Rhapsody");
        assertThat(radioheadVideos).isEmpty();
    }

    @Test
    void combinedFilterTitleAndTag() {
        MusicVideo mv1 = new MusicVideo("Come Together", beatles, "Abbey Road", 1969, null);
        mv1.setVideoFilePath("/music-videos/beatles/come_together.mp4");
        mv1.setTags(List.of("classic", "rock"));

        MusicVideo mv2 = new MusicVideo("Come As You Are", radiohead, null, 1992, null);
        mv2.setVideoFilePath("/music-videos/radiohead/come_as_you_are.mp4");
        mv2.setTags(List.of("grunge"));

        musicVideoRepository.saveAll(List.of(mv1, mv2));

        // title contains "come", tag contains "rock" → only mv1
        Specification<MusicVideo> spec = Specification
                .<MusicVideo>where((root, query, cb) ->
                        cb.like(cb.lower(root.get("title")), "%come%"))
                .and((root, query, cb) -> {
                    query.distinct(true);
                    Join<MusicVideo, String> tagsJoin = root.join("tags", JoinType.INNER);
                    return cb.like(cb.lower(tagsJoin.as(String.class)), "%rock%");
                });

        List<MusicVideo> results = musicVideoRepository.findAll(spec);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Come Together");
    }
}
