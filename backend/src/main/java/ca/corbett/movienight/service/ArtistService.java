package ca.corbett.movienight.service;

import ca.corbett.movienight.model.Artist;
import ca.corbett.movienight.repository.ArtistRepository;
import ca.corbett.movienight.repository.MusicVideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class ArtistService {

    private final MusicVideoRepository musicVideoRepository;
    private final ArtistRepository artistRepository;
    private final ThumbnailService thumbnailService;

    public ArtistService(ArtistRepository artistRepository,
                         MusicVideoRepository musicVideoRepository,
                         ThumbnailService thumbnailService) {
        this.thumbnailService = thumbnailService;
        this.musicVideoRepository = musicVideoRepository;
        this.artistRepository = artistRepository;
    }

    public List<Artist> getAllArtists() {
        return populateTransientFields(artistRepository.findAll());
    }

    public Optional<Artist> getArtistById(Long id) {
        return artistRepository.findById(id).map(this::populateTransientFields);
    }

    public Artist requireArtist(Long id) {
        return getArtistById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                               "Artist not found with id: " + id));
    }

    public Artist saveArtist(Artist artist) {
        if (artistRepository.existsByNameIgnoreCase(artist.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                                              "Artist with name '" + artist.getName() + "' already exists.");
        }
        return populateTransientFields(artistRepository.save(artist));
    }

    public void deleteArtist(Long id) {
        if (!artistRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Artist not found with id: " + id);
        }

        if (musicVideoRepository.countByArtist(requireArtist(id)) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                                              "Cannot delete artist with id: " + id
                                                      + " because it is referenced by existing music videos.");
        }

        thumbnailService.deleteThumbnail("artists", id);
        artistRepository.deleteById(id);
    }

    public Artist updateArtist(Long id, Artist updatedArtist) {
        Artist existingName = artistRepository.findByNameIgnoreCase(updatedArtist.getName()).orElse(null);
        if (existingName != null && !existingName.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                                              "Artist with name '" + updatedArtist.getName() + "' already exists.");
        }
        Artist existingArtist = requireArtist(id);
        existingArtist.setName(updatedArtist.getName());
        existingArtist.setDescription(updatedArtist.getDescription());
        return populateTransientFields(artistRepository.save(existingArtist));
    }

    private Artist populateHasThumbnail(Artist artist) {
        artist.setHasThumbnail(thumbnailService.hasThumbnail("artists", artist.getId()));
        return artist;
    }

    private Artist populateVideoCount(Artist artist) {
        artist.setVideoCount(musicVideoRepository.countByArtist(artist));
        return artist;
    }

    private Artist populateTransientFields(Artist artist) {
        populateHasThumbnail(artist);
        populateVideoCount(artist);
        return artist;
    }

    private List<Artist> populateTransientFields(List<Artist> artists) {
        artists.forEach(this::populateTransientFields);
        return artists;
    }
}
