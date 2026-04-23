package ca.corbett.movienight.service;

import ca.corbett.movienight.model.Series;
import ca.corbett.movienight.repository.EpisodeRepository;
import ca.corbett.movienight.repository.SeriesRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class SeriesService {
    private final EpisodeRepository episodeRepository;
    private final SeriesRepository seriesRepository;
    private final ThumbnailService thumbnailService;

    public SeriesService(EpisodeRepository episodeRepository,
                         SeriesRepository seriesRepository,
                         ThumbnailService thumbnailService) {
        this.thumbnailService = thumbnailService;
        this.episodeRepository = episodeRepository;
        this.seriesRepository = seriesRepository;
    }

    public List<Series> getAllSeries() {
        return populateTransientFields(seriesRepository.findAll());
    }

    public Optional<Series> getSeriesById(Long id) {
        return seriesRepository.findById(id).map(this::populateTransientFields);
    }

    public Series requireSeries(Long id) {
        return getSeriesById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                               "Series not found with id: " + id));
    }

    public Series saveSeries(Series series) {
        if (seriesRepository.existsByNameIgnoreCase(series.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                                              "Series already exists with name: " + series.getName());
        }
        return populateTransientFields(seriesRepository.save(series));
    }

    public void deleteSeries(Long id) {
        // Throw a 404 if the Genre doesn't exist:
        if (!seriesRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Series not found with id: " + id);
        }

        // Throw a 409 if any Episode currently references this Series:
        if (episodeRepository.countBySeries(requireSeries(id)) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                                              "Cannot delete series with id: " + id
                                                      + " because it is referenced by existing episodes.");
        }

        thumbnailService.deleteThumbnail("series", id);
        seriesRepository.deleteById(id);
    }

    public Series updateSeries(Long id, Series updatedSeries) {
        Series existingSeries = requireSeries(id);
        existingSeries.setName(updatedSeries.getName());
        existingSeries.setDescription(updatedSeries.getDescription());
        return populateTransientFields(seriesRepository.save(existingSeries));
    }

    private Series populateHasThumbnail(Series series) {
        series.setHasThumbnail(thumbnailService.hasThumbnail("series", series.getId()));
        return series;
    }

    private Series populateEpisodeCount(Series series) {
        series.setEpisodeCount(episodeRepository.countBySeries(series));
        return series;
    }

    private Series populateTransientFields(Series series) {
        populateHasThumbnail(series);
        populateEpisodeCount(series);
        return series;
    }

    private List<Series> populateTransientFields(List<Series> series) {
        series.forEach(this::populateTransientFields);
        return series;
    }

}
