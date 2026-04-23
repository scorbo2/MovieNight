import { useEffect, useState } from 'react'
import MovieList from '../components/MovieList'
import MovieForm from '../components/MovieForm'
import EpisodeList from '../components/EpisodeList'
import EpisodeForm from '../components/EpisodeForm'
import GenreList from '../components/GenreList'
import GenreForm from '../components/GenreForm'
import SeriesList from '../components/SeriesList'
import SeriesForm from '../components/SeriesForm'
import ArtistList from '../components/ArtistList'
import ArtistForm from '../components/ArtistForm'
import MusicVideoList from '../components/MusicVideoList'
import MusicVideoForm from '../components/MusicVideoForm'

const MOVIES_API = '/api/movies'
const EPISODES_API = '/api/episodes'
const GENRES_API = '/api/genres'
const SERIES_API = '/api/series'
const ARTISTS_API = '/api/artists'
const MUSIC_VIDEOS_API = '/api/music-videos'

export default function MediaLibraryPage({ mode }) {
  const isAdmin = mode === 'admin'
  const [activeTab, setActiveTab] = useState('movies')

  const [movies, setMovies] = useState([])
  const [moviesLoading, setMoviesLoading] = useState(true)
  const [moviesError, setMoviesError] = useState(null)
  const [showMovieForm, setShowMovieForm] = useState(false)
  const [editingMovie, setEditingMovie] = useState(null)
  const [movieSearchQuery, setMovieSearchQuery] = useState('')
  const [filterWatched, setFilterWatched] = useState('')
  const [movieTagQuery, setMovieTagQuery] = useState('')

  const [episodes, setEpisodes] = useState([])
  const [episodesLoading, setEpisodesLoading] = useState(true)
  const [episodesError, setEpisodesError] = useState(null)
  const [showEpisodeForm, setShowEpisodeForm] = useState(false)
  const [editingEpisode, setEditingEpisode] = useState(null)
  const [episodeSeriesQuery, setEpisodeSeriesQuery] = useState('')
  const [episodeSeasonQuery, setEpisodeSeasonQuery] = useState('')
  const [filterEpisodeWatched, setFilterEpisodeWatched] = useState('')
  const [episodeTagQuery, setEpisodeTagQuery] = useState('')

  const [genres, setGenres] = useState([])
  const [genresLoading, setGenresLoading] = useState(true)
  const [genresError, setGenresError] = useState(null)
  const [showGenreForm, setShowGenreForm] = useState(false)
  const [editingGenre, setEditingGenre] = useState(null)
  const [genreSearchQuery, setGenreSearchQuery] = useState('')
  const [selectedGenre, setSelectedGenre] = useState(null)

  const [series, setSeries] = useState([])
  const [seriesLoading, setSeriesLoading] = useState(true)
  const [seriesError, setSeriesError] = useState(null)
  const [showSeriesForm, setShowSeriesForm] = useState(false)
  const [editingSeries, setEditingSeries] = useState(null)
  const [seriesSearchQuery, setSeriesSearchQuery] = useState('')
  const [selectedSeries, setSelectedSeries] = useState(null)

  const [artists, setArtists] = useState([])
  const [artistsLoading, setArtistsLoading] = useState(true)
  const [artistsError, setArtistsError] = useState(null)
  const [showArtistForm, setShowArtistForm] = useState(false)
  const [editingArtist, setEditingArtist] = useState(null)
  const [artistSearchQuery, setArtistSearchQuery] = useState('')
  const [selectedArtist, setSelectedArtist] = useState(null)

  const [musicVideos, setMusicVideos] = useState([])
  const [musicVideosLoading, setMusicVideosLoading] = useState(true)
  const [musicVideosError, setMusicVideosError] = useState(null)
  const [showMusicVideoForm, setShowMusicVideoForm] = useState(false)
  const [editingMusicVideo, setEditingMusicVideo] = useState(null)
  const [musicVideoTitleQuery, setMusicVideoTitleQuery] = useState('')
  const [musicVideoTagQuery, setMusicVideoTagQuery] = useState('')

  const getErrorMessage = async (response, fallbackMessage) => {
    const body = await response.text()
    if (!body) return fallbackMessage

    try {
      const parsed = JSON.parse(body)
      return parsed.message || parsed.error || fallbackMessage
    } catch {
      return body
    }
  }

  const fetchMovies = async () => {
    try {
      setMoviesLoading(true)
      const params = new URLSearchParams()
      if (movieSearchQuery) params.append('title', movieSearchQuery)
      if (filterWatched !== '') params.append('watched', filterWatched)
      if (movieTagQuery) params.append('tag', movieTagQuery)
      if (selectedGenre) params.append('genreId', selectedGenre.id)
      const res = await fetch(`${MOVIES_API}?${params}`)
      if (!res.ok) throw new Error('Failed to fetch movies')
      setMovies(await res.json())
      setMoviesError(null)
    } catch (err) {
      setMoviesError(err.message)
    } finally {
      setMoviesLoading(false)
    }
  }

  useEffect(() => {
    fetchMovies()
  }, [movieSearchQuery, filterWatched, movieTagQuery, selectedGenre])

  const isEpisodeSearchActive = !!(episodeSeriesQuery || episodeSeasonQuery || filterEpisodeWatched || episodeTagQuery)

  const fetchEpisodes = async () => {
    try {
      setEpisodesLoading(true)
      const params = new URLSearchParams()
      if (selectedSeries) {
        params.append('seriesId', selectedSeries.id)
      } else if (episodeSeriesQuery) {
        params.append('seriesName', episodeSeriesQuery)
      }
      if (episodeSeasonQuery !== '') params.append('season', episodeSeasonQuery)
      if (filterEpisodeWatched !== '') params.append('watched', filterEpisodeWatched)
      if (episodeTagQuery) params.append('tag', episodeTagQuery)
      const res = await fetch(`${EPISODES_API}?${params}`)
      if (!res.ok) throw new Error('Failed to fetch episodes')
      setEpisodes(await res.json())
      setEpisodesError(null)
    } catch (err) {
      setEpisodesError(err.message)
    } finally {
      setEpisodesLoading(false)
    }
  }

  useEffect(() => {
    if (activeTab !== 'episodes') return
    if (!selectedSeries && !isEpisodeSearchActive) return
    fetchEpisodes()
  }, [activeTab, selectedSeries, episodeSeriesQuery, episodeSeasonQuery, filterEpisodeWatched, episodeTagQuery])

  const fetchSeries = async () => {
    try {
      setSeriesLoading(true)
      const res = await fetch(SERIES_API)
      if (!res.ok) throw new Error('Failed to fetch series')
      const data = await res.json()
      const sortedSeries = Array.isArray(data)
        ? [...data].sort((a, b) => a.name.localeCompare(b.name))
        : []
      setSeries(sortedSeries)
      setSeriesError(null)
    } catch (err) {
      setSeriesError(err.message)
    } finally {
      setSeriesLoading(false)
    }
  }

  useEffect(() => {
    if (activeTab !== 'series' && activeTab !== 'episodes') return
    fetchSeries()
  }, [activeTab])

  const fetchGenres = async () => {
    try {
      setGenresLoading(true)
      const res = await fetch(GENRES_API)
      if (!res.ok) throw new Error('Failed to fetch genres')
      const data = await res.json()
      const sortedGenres = Array.isArray(data)
        ? [...data].sort((a, b) => a.name.localeCompare(b.name))
        : []
      setGenres(sortedGenres)
      setGenresError(null)
    } catch (err) {
      setGenresError(err.message)
    } finally {
      setGenresLoading(false)
    }
  }

  useEffect(() => {
    if (activeTab !== 'genres' && activeTab !== 'movies') return
    fetchGenres()
  }, [activeTab])

  const fetchArtists = async () => {
    try {
      setArtistsLoading(true)
      const res = await fetch(ARTISTS_API)
      if (!res.ok) throw new Error('Failed to fetch artists')
      const data = await res.json()
      const sortedArtists = Array.isArray(data)
        ? [...data].sort((a, b) => a.name.localeCompare(b.name))
        : []
      setArtists(sortedArtists)
      setArtistsError(null)
    } catch (err) {
      setArtistsError(err.message)
    } finally {
      setArtistsLoading(false)
    }
  }

  useEffect(() => {
    if (activeTab !== 'artists' && activeTab !== 'musicvideos') return
    fetchArtists()
  }, [activeTab])

  const isMusicVideoSearchActive = !!(musicVideoTitleQuery || musicVideoTagQuery)

  const fetchMusicVideos = async () => {
    try {
      setMusicVideosLoading(true)
      const params = new URLSearchParams()
      if (selectedArtist) {
        params.append('artistId', selectedArtist.id)
      } else if (musicVideoTitleQuery) {
        params.append('title', musicVideoTitleQuery)
      }
      if (musicVideoTagQuery) params.append('tag', musicVideoTagQuery)
      const res = await fetch(`${MUSIC_VIDEOS_API}?${params}`)
      if (!res.ok) throw new Error('Failed to fetch music videos')
      setMusicVideos(await res.json())
      setMusicVideosError(null)
    } catch (err) {
      setMusicVideosError(err.message)
    } finally {
      setMusicVideosLoading(false)
    }
  }

  useEffect(() => {
    if (activeTab !== 'musicvideos') return
    if (!selectedArtist && !isMusicVideoSearchActive) return
    fetchMusicVideos()
  }, [activeTab, selectedArtist, musicVideoTitleQuery, musicVideoTagQuery])

  const handleSaveMovie = async (movieData) => {
    try {
      const { _thumbnail, _clearThumbnail, ...data } = movieData
      const isEdit = data.id != null
      const url = isEdit ? `${MOVIES_API}/${data.id}` : MOVIES_API
      const res = await fetch(url, {
        method: isEdit ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      })
      if (!res.ok) throw new Error('Failed to save movie')
      const saved = await res.json()
      if (_clearThumbnail) {
        const thumbnailRes = await fetch(`${MOVIES_API}/${saved.id}/thumbnail`, { method: 'DELETE' })
        if (!thumbnailRes.ok) throw new Error('Failed to delete movie thumbnail')
      } else if (_thumbnail) {
        const formData = new FormData()
        formData.append('file', _thumbnail)
        const thumbnailRes = await fetch(`${MOVIES_API}/${saved.id}/thumbnail`, { method: 'POST', body: formData })
        if (!thumbnailRes.ok) throw new Error('Failed to upload movie thumbnail')
      }
      setShowMovieForm(false)
      setEditingMovie(null)
      fetchMovies()
    } catch (err) {
      setMoviesError(err.message)
    }
  }

  const handleDeleteMovie = async (id) => {
    if (!confirm('Delete this movie?')) return
    try {
      const res = await fetch(`${MOVIES_API}/${id}`, { method: 'DELETE' })
      if (!res.ok) throw new Error('Failed to delete movie')
      fetchMovies()
    } catch (err) {
      setMoviesError(err.message)
    }
  }

  const handleSaveEpisode = async (episodeData) => {
    try {
      const { _thumbnail, _clearThumbnail, ...data } = episodeData
      const isEdit = data.id != null
      const url = isEdit ? `${EPISODES_API}/${data.id}` : EPISODES_API
      const res = await fetch(url, {
        method: isEdit ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      })
      if (!res.ok) throw new Error('Failed to save episode')
      const saved = await res.json()
      if (_clearThumbnail) {
        const thumbnailRes = await fetch(`${EPISODES_API}/${saved.id}/thumbnail`, { method: 'DELETE' })
        if (!thumbnailRes.ok) throw new Error('Failed to clear episode thumbnail')
      } else if (_thumbnail) {
        const formData = new FormData()
        formData.append('file', _thumbnail)
        const thumbnailRes = await fetch(`${EPISODES_API}/${saved.id}/thumbnail`, { method: 'POST', body: formData })
        if (!thumbnailRes.ok) throw new Error('Failed to upload episode thumbnail')
      }
      setShowEpisodeForm(false)
      setEditingEpisode(null)
      fetchEpisodes()
    } catch (err) {
      setEpisodesError(err.message)
    }
  }

  const handleDeleteEpisode = async (id) => {
    if (!confirm('Delete this episode?')) return
    try {
      const res = await fetch(`${EPISODES_API}/${id}`, { method: 'DELETE' })
      if (!res.ok) throw new Error('Failed to delete episode')
      fetchEpisodes()
    } catch (err) {
      setEpisodesError(err.message)
    }
  }

  const handleSaveGenre = async (genreData) => {
    try {
      const { _thumbnail, _clearThumbnail, ...data } = genreData
      const isEdit = data.id != null
      const url = isEdit ? `${GENRES_API}/${data.id}` : GENRES_API
      const res = await fetch(url, {
        method: isEdit ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      })
      if (!res.ok) throw new Error(await getErrorMessage(res, 'Failed to save genre'))

      const saved = await res.json()
      if (_clearThumbnail) {
        const thumbnailRes = await fetch(`${GENRES_API}/${saved.id}/thumbnail`, { method: 'DELETE' })
        if (!thumbnailRes.ok) throw new Error('Failed to delete genre thumbnail')
      } else if (_thumbnail) {
        const formData = new FormData()
        formData.append('file', _thumbnail)
        const thumbnailRes = await fetch(`${GENRES_API}/${saved.id}/thumbnail`, { method: 'POST', body: formData })
        if (!thumbnailRes.ok) throw new Error('Failed to upload genre thumbnail')
      }

      setShowGenreForm(false)
      setEditingGenre(null)
      fetchGenres()
    } catch (err) {
      setGenresError(err.message)
    }
  }

  const handleDeleteGenre = async (id) => {
    if (!confirm('Delete this genre?')) return
    try {
      const res = await fetch(`${GENRES_API}/${id}`, { method: 'DELETE' })
      if (!res.ok) throw new Error(await getErrorMessage(res, 'Failed to delete genre'))
      fetchGenres()
    } catch (err) {
      setGenresError(err.message)
    }
  }

  const adminActionLabel = activeTab === 'movies'
    ? '+ Add Movie'
    : activeTab === 'episodes'
      ? '+ Add Episode'
      : activeTab === 'genres'
      ? '+ Add Genre'
      : activeTab === 'series'
      ? '+ Add Series'
      : activeTab === 'musicvideos'
      ? '+ Add Music Video'
      : '+ Add Artist'

  const filteredGenres = genres.filter((genre) => {
    const q = genreSearchQuery.trim().toLowerCase()
    if (!q) return true
    return (genre.name || '').toLowerCase().includes(q)
      || (genre.description || '').toLowerCase().includes(q)
  })

  const handleSaveSeries = async (seriesData) => {
    try {
      const { _thumbnail, _clearThumbnail, ...data } = seriesData
      const isEdit = data.id != null
      const url = isEdit ? `${SERIES_API}/${data.id}` : SERIES_API
      const res = await fetch(url, {
        method: isEdit ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      })
      if (!res.ok) throw new Error(await getErrorMessage(res, 'Failed to save series'))

      const saved = await res.json()
      if (_clearThumbnail) {
        const thumbnailRes = await fetch(`${SERIES_API}/${saved.id}/thumbnail`, { method: 'DELETE' })
        if (!thumbnailRes.ok) throw new Error('Failed to delete series thumbnail')
      } else if (_thumbnail) {
        const formData = new FormData()
        formData.append('file', _thumbnail)
        const thumbnailRes = await fetch(`${SERIES_API}/${saved.id}/thumbnail`, { method: 'POST', body: formData })
        if (!thumbnailRes.ok) throw new Error('Failed to upload series thumbnail')
      }

      setShowSeriesForm(false)
      setEditingSeries(null)
      fetchSeries()
    } catch (err) {
      setSeriesError(err.message)
    }
  }

  const handleDeleteSeries = async (id) => {
    if (!confirm('Delete this series?')) return
    try {
      const res = await fetch(`${SERIES_API}/${id}`, { method: 'DELETE' })
      if (!res.ok) throw new Error(await getErrorMessage(res, 'Failed to delete series'))
      fetchSeries()
    } catch (err) {
      setSeriesError(err.message)
    }
  }

  const filteredSeries = series.filter((series) => {
    const q = seriesSearchQuery.trim().toLowerCase()
    if (!q) return true
    return (series.name || '').toLowerCase().includes(q)
      || (series.description || '').toLowerCase().includes(q)
  })

  const handleSaveArtist = async (artistData) => {
    try {
      const { _thumbnail, _clearThumbnail, ...data } = artistData
      const isEdit = data.id != null
      const url = isEdit ? `${ARTISTS_API}/${data.id}` : ARTISTS_API
      const res = await fetch(url, {
        method: isEdit ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      })
      if (!res.ok) throw new Error(await getErrorMessage(res, 'Failed to save artist'))

      const saved = await res.json()
      if (_clearThumbnail) {
        const thumbnailRes = await fetch(`${ARTISTS_API}/${saved.id}/thumbnail`, { method: 'DELETE' })
        if (!thumbnailRes.ok) throw new Error('Failed to delete artist thumbnail')
      } else if (_thumbnail) {
        const formData = new FormData()
        formData.append('file', _thumbnail)
        const thumbnailRes = await fetch(`${ARTISTS_API}/${saved.id}/thumbnail`, { method: 'POST', body: formData })
        if (!thumbnailRes.ok) throw new Error('Failed to upload artist thumbnail')
      }

      setShowArtistForm(false)
      setEditingArtist(null)
      fetchArtists()
    } catch (err) {
      setArtistsError(err.message)
    }
  }

  const handleDeleteArtist = async (id) => {
    if (!confirm('Delete this artist?')) return
    try {
      const res = await fetch(`${ARTISTS_API}/${id}`, { method: 'DELETE' })
      if (!res.ok) throw new Error(await getErrorMessage(res, 'Failed to delete artist'))
      fetchArtists()
    } catch (err) {
      setArtistsError(err.message)
    }
  }

  const handleSaveMusicVideo = async (musicVideoData) => {
    try {
      const { _thumbnail, _clearThumbnail, ...data } = musicVideoData
      const isEdit = data.id != null
      const url = isEdit ? `${MUSIC_VIDEOS_API}/${data.id}` : MUSIC_VIDEOS_API
      const res = await fetch(url, {
        method: isEdit ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      })
      if (!res.ok) throw new Error(await getErrorMessage(res, 'Failed to save music video'))
      const saved = await res.json()
      if (_clearThumbnail) {
        const thumbnailRes = await fetch(`${MUSIC_VIDEOS_API}/${saved.id}/thumbnail`, { method: 'DELETE' })
        if (!thumbnailRes.ok) throw new Error('Failed to delete music video thumbnail')
      } else if (_thumbnail) {
        const formData = new FormData()
        formData.append('file', _thumbnail)
        const thumbnailRes = await fetch(`${MUSIC_VIDEOS_API}/${saved.id}/thumbnail`, { method: 'POST', body: formData })
        if (!thumbnailRes.ok) throw new Error('Failed to upload music video thumbnail')
      }
      setShowMusicVideoForm(false)
      setEditingMusicVideo(null)
      fetchMusicVideos()
    } catch (err) {
      setMusicVideosError(err.message)
    }
  }

  const handleDeleteMusicVideo = async (id) => {
    if (!confirm('Delete this music video?')) return
    try {
      const res = await fetch(`${MUSIC_VIDEOS_API}/${id}`, { method: 'DELETE' })
      if (!res.ok) throw new Error(await getErrorMessage(res, 'Failed to delete music video'))
      fetchMusicVideos()
    } catch (err) {
      setMusicVideosError(err.message)
    }
  }

  const filteredArtists = artists.filter((artist) => {
    const q = artistSearchQuery.trim().toLowerCase()
    if (!q) return true
    return (artist.name || '').toLowerCase().includes(q)
      || (artist.description || '').toLowerCase().includes(q)
  })

  return (
    <>
      <section className="mb-6 rounded-xl border border-gray-800 bg-gray-900 px-5 py-4">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-indigo-300">
              {isAdmin ? 'Admin interface' : 'Read-only interface'}
            </p>
            <h2 className="mt-1 text-xl font-semibold text-white">
              {isAdmin ? 'Manage your media library' : 'Browse your media library'}
            </h2>
            <p className="mt-1 text-sm text-gray-400">
              {isAdmin
                ? 'Create, edit, and delete entries from this machine only. Basic auth is required for admin access.'
                : 'Browse movies, episodes, and genres without any write controls.'}
            </p>
          </div>
          {isAdmin && (
            <button
              onClick={() => {
                if (activeTab === 'movies') {
                  setEditingMovie(null)
                  setShowMovieForm(true)
                } else if (activeTab === 'episodes') {
                  setEditingEpisode(null)
                  setShowEpisodeForm(true)
                } else if (activeTab === 'genres') {
                  setEditingGenre(null)
                  setShowGenreForm(true)
                } else if (activeTab === 'series') {
                    setEditingSeries(null)
                    setShowSeriesForm(true)
                } else if (activeTab === 'musicvideos') {
                  setEditingMusicVideo(null)
                  setShowMusicVideoForm(true)
                } else if (activeTab === 'artists') {
                  setEditingArtist(null)
                  setShowArtistForm(true)
                }
              }}
              className="bg-indigo-600 hover:bg-indigo-500 text-white px-4 py-2 rounded-lg font-medium transition-colors"
            >
              {adminActionLabel}
            </button>
          )}
        </div>
      </section>

      <div className="flex flex-wrap gap-1 mb-6 bg-gray-900 border border-gray-800 rounded-lg p-1 w-fit">
        <button
          onClick={() => {
            setActiveTab('movies')
            setShowEpisodeForm(false)
            setEditingEpisode(null)
            setShowGenreForm(false)
            setEditingGenre(null)
            setShowSeriesForm(false)
            setEditingSeries(null)
            setShowArtistForm(false)
            setEditingArtist(null)
            setShowMusicVideoForm(false)
            setEditingMusicVideo(null)
          }}
          className={`px-5 py-2 rounded-md text-sm font-medium transition-colors ${
            activeTab === 'movies'
              ? 'bg-indigo-600 text-white'
              : 'text-gray-400 hover:text-gray-200'
          }`}
        >
          🎬 Movies
        </button>
        <button
          onClick={() => {
            setActiveTab('episodes')
            setShowMovieForm(false)
            setEditingMovie(null)
            setShowGenreForm(false)
            setEditingGenre(null)
            setShowSeriesForm(false)
            setEditingSeries(null)
            setShowArtistForm(false)
            setEditingArtist(null)
            setShowMusicVideoForm(false)
            setEditingMusicVideo(null)
          }}
          className={`px-5 py-2 rounded-md text-sm font-medium transition-colors ${
            activeTab === 'episodes'
              ? 'bg-indigo-600 text-white'
              : 'text-gray-400 hover:text-gray-200'
          }`}
        >
          📺 Episodes
        </button>
        <button
          onClick={() => {
            setActiveTab('musicvideos')
            setShowMovieForm(false)
            setEditingMovie(null)
            setShowEpisodeForm(false)
            setEditingEpisode(null)
            setShowGenreForm(false)
            setEditingGenre(null)
            setShowSeriesForm(false)
            setEditingSeries(null)
            setShowArtistForm(false)
            setEditingArtist(null)
          }}
          className={`px-5 py-2 rounded-md text-sm font-medium transition-colors ${
            activeTab === 'musicvideos'
              ? 'bg-indigo-600 text-white'
              : 'text-gray-400 hover:text-gray-200'
          }`}
        >
          🎵 Music Videos
        </button>
        {isAdmin && (
          <>
            <button
              onClick={() => {
                setActiveTab('genres')
                setShowMovieForm(false)
                setEditingMovie(null)
                setShowEpisodeForm(false)
                setEditingEpisode(null)
                setShowSeriesForm(false)
                setEditingSeries(null)
                setShowArtistForm(false)
                setEditingArtist(null)
                setShowMusicVideoForm(false)
                setEditingMusicVideo(null)
              }}
              className={`px-5 py-2 rounded-md text-sm font-medium transition-colors ${
                activeTab === 'genres'
                  ? 'bg-indigo-600 text-white'
                  : 'text-gray-400 hover:text-gray-200'
              }`}
            >
              🏷️ Genres
            </button>
            <button
              onClick={() => {
                setActiveTab('series')
                setShowMovieForm(false)
                setEditingMovie(null)
                setShowEpisodeForm(false)
                setEditingEpisode(null)
                setShowGenreForm(false)
                setEditingGenre(null)
                setShowArtistForm(false)
                setEditingArtist(null)
                setShowMusicVideoForm(false)
                setEditingMusicVideo(null)
              }}
              className={`px-5 py-2 rounded-md text-sm font-medium transition-colors ${
                activeTab === 'series'
                  ? 'bg-indigo-600 text-white'
                  : 'text-gray-400 hover:text-gray-200'
              }`}
            >
              Series
            </button>
            <button
              onClick={() => {
                setActiveTab('artists')
                setShowMovieForm(false)
                setEditingMovie(null)
                setShowEpisodeForm(false)
                setEditingEpisode(null)
                setShowGenreForm(false)
                setEditingGenre(null)
                setShowSeriesForm(false)
                setEditingSeries(null)
                setShowMusicVideoForm(false)
                setEditingMusicVideo(null)
              }}
              className={`px-5 py-2 rounded-md text-sm font-medium transition-colors ${
                activeTab === 'artists'
                  ? 'bg-indigo-600 text-white'
                  : 'text-gray-400 hover:text-gray-200'
              }`}
            >
              🎤 Artists
            </button>
          </>
        )}
      </div>

      {activeTab === 'movies' && (
        <>
          <div className="flex flex-col sm:flex-row gap-3 mb-6">
            <input
              type="text"
              placeholder="Search by title…"
              value={movieSearchQuery}
              onChange={(e) => {
                setMovieSearchQuery(e.target.value)
                if (e.target.value) setSelectedGenre(null)
              }}
              className="flex-1 bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-gray-100 placeholder-gray-500 focus:outline-none focus:border-indigo-500"
            />
            {(selectedGenre || movieSearchQuery || movieTagQuery) && (
              <select
                value={filterWatched}
                onChange={(e) => setFilterWatched(e.target.value)}
                className="bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-gray-100 focus:outline-none focus:border-indigo-500"
              >
                <option value="">All movies</option>
                <option value="true">Watched</option>
                <option value="false">Unwatched</option>
              </select>
            )}
          </div>

          {selectedGenre && !movieSearchQuery && (
            <div className="flex items-center gap-3 mb-4">
              <button
                type="button"
                onClick={() => setSelectedGenre(null)}
                className="flex items-center gap-1 text-sm text-indigo-400 hover:text-indigo-200 transition-colors"
              >
                ← All genres
              </button>
              <span className="text-gray-500">|</span>
              <span className="text-white font-medium">🎭 {selectedGenre.name}</span>
            </div>
          )}

          {movieSearchQuery && (
            <div className="flex items-center gap-2 mb-4">
              <span className="text-sm text-gray-400">Title search:</span>
              <span className="flex items-center gap-1 text-xs bg-indigo-800/40 text-indigo-300 px-2 py-0.5 rounded-full">
                {movieSearchQuery}
                <button
                  type="button"
                  onClick={() => setMovieSearchQuery('')}
                  className="hover:text-white ml-0.5"
                  aria-label="Clear title search"
                >
                  ×
                </button>
              </span>
            </div>
          )}

          {movieTagQuery && (
            <div className="flex items-center gap-2 mb-4">
              <span className="text-sm text-gray-400">Tag filter:</span>
              <span className="flex items-center gap-1 text-xs bg-indigo-800/40 text-indigo-300 px-2 py-0.5 rounded-full">
                {movieTagQuery}
                <button
                  type="button"
                  onClick={() => setMovieTagQuery('')}
                  className="hover:text-white ml-0.5"
                  aria-label={`Remove tag filter ${movieTagQuery}`}
                >
                  ×
                </button>
              </span>
            </div>
          )}

          {moviesError && (
            <div className="bg-red-900/50 border border-red-700 text-red-300 px-4 py-3 rounded-lg mb-6">
              {moviesError}
            </div>
          )}

          {isAdmin && showMovieForm && (
            <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
              <div className="bg-gray-900 border border-gray-700 rounded-xl shadow-2xl w-full max-w-lg">
                <MovieForm
                  movie={editingMovie}
                  onSave={handleSaveMovie}
                  onCancel={() => {
                    setShowMovieForm(false)
                    setEditingMovie(null)
                  }}
                />
              </div>
            </div>
          )}

          {!selectedGenre && !movieSearchQuery && !movieTagQuery ? (
            genresLoading ? (
              <div className="text-center text-gray-400 py-16">Loading…</div>
            ) : (
              <>
                {genresError && (
                  <div className="bg-red-900/50 border border-red-700 text-red-300 px-4 py-3 rounded-lg mb-6">
                    {genresError}
                  </div>
                )}
                <GenreList
                  genres={genres}
                  onGenreClick={(genre) => {
                    setSelectedGenre(genre)
                    setMovieSearchQuery('')
                  }}
                  readOnly={true}
                />
              </>
            )
          ) : (
            moviesLoading ? (
              <div className="text-center text-gray-400 py-16">Loading…</div>
            ) : (
              <MovieList
                movies={movies}
                onEdit={(movie) => {
                  setEditingMovie(movie)
                  setShowMovieForm(true)
                }}
                onDelete={handleDeleteMovie}
                onTagClick={(tag) => setMovieTagQuery(tag)}
                readOnly={!isAdmin}
              />
            )
          )}
        </>
      )}

      {activeTab === 'episodes' && (
        <>
          <div className="flex flex-col sm:flex-row gap-3 mb-6">
            <input
              type="text"
              placeholder="Search by series…"
              value={episodeSeriesQuery}
              onChange={(e) => {
                setEpisodeSeriesQuery(e.target.value)
                if (e.target.value) setSelectedSeries(null)
              }}
              className="flex-1 bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-gray-100 placeholder-gray-500 focus:outline-none focus:border-indigo-500"
            />
            <input
              type="number"
              placeholder="Season"
              value={episodeSeasonQuery}
              onChange={(e) => setEpisodeSeasonQuery(e.target.value)}
              min="1"
              className="w-28 bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-gray-100 placeholder-gray-500 focus:outline-none focus:border-indigo-500"
            />
            <select
              value={filterEpisodeWatched}
              onChange={(e) => setFilterEpisodeWatched(e.target.value)}
              className="bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-gray-100 focus:outline-none focus:border-indigo-500"
            >
              <option value="">All episodes</option>
              <option value="true">Watched</option>
              <option value="false">Unwatched</option>
            </select>
          </div>

          {selectedSeries && !episodeSeriesQuery && (
            <div className="flex items-center gap-3 mb-4">
              <button
                type="button"
                onClick={() => setSelectedSeries(null)}
                className="flex items-center gap-1 text-sm text-indigo-400 hover:text-indigo-200 transition-colors"
              >
                ← All series
              </button>
              <span className="text-gray-500">|</span>
              <span className="text-white font-medium">📺 {selectedSeries.name}</span>
            </div>
          )}

          {episodeSeriesQuery && (
            <div className="flex items-center gap-2 mb-4">
              <span className="text-sm text-gray-400">Series search:</span>
              <span className="flex items-center gap-1 text-xs bg-indigo-800/40 text-indigo-300 px-2 py-0.5 rounded-full">
                {episodeSeriesQuery}
                <button
                  type="button"
                  onClick={() => setEpisodeSeriesQuery('')}
                  className="hover:text-white ml-0.5"
                  aria-label="Clear series search"
                >
                  ×
                </button>
              </span>
            </div>
          )}

          {episodeTagQuery && (
            <div className="flex items-center gap-2 mb-4">
              <span className="text-sm text-gray-400">Tag filter:</span>
              <span className="flex items-center gap-1 text-xs bg-indigo-800/40 text-indigo-300 px-2 py-0.5 rounded-full">
                {episodeTagQuery}
                <button
                  type="button"
                  onClick={() => setEpisodeTagQuery('')}
                  className="hover:text-white ml-0.5"
                  aria-label={`Remove tag filter ${episodeTagQuery}`}
                >
                  ×
                </button>
              </span>
            </div>
          )}

          {episodesError && (
            <div className="bg-red-900/50 border border-red-700 text-red-300 px-4 py-3 rounded-lg mb-6">
              {episodesError}
            </div>
          )}

          {isAdmin && showEpisodeForm && (
            <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
              <div className="bg-gray-900 border border-gray-700 rounded-xl shadow-2xl w-full max-w-lg overflow-y-auto max-h-[90vh]">
                <EpisodeForm
                  episode={editingEpisode}
                  onSave={handleSaveEpisode}
                  onCancel={() => {
                    setShowEpisodeForm(false)
                    setEditingEpisode(null)
                  }}
                />
              </div>
            </div>
          )}

          {!selectedSeries && !isEpisodeSearchActive ? (
            seriesLoading ? (
              <div className="text-center text-gray-400 py-16">Loading…</div>
            ) : (
              <>
                {seriesError && (
                  <div className="bg-red-900/50 border border-red-700 text-red-300 px-4 py-3 rounded-lg mb-6">
                    {seriesError}
                  </div>
                )}
                <SeriesList
                  series={series}
                  onSeriesClick={(s) => {
                    setSelectedSeries(s)
                    setEpisodeSeriesQuery('')
                    setEpisodeSeasonQuery('')
                    setFilterEpisodeWatched('')
                    setEpisodeTagQuery('')
                  }}
                  readOnly={true}
                />
              </>
            )
          ) : (
            episodesLoading ? (
              <div className="text-center text-gray-400 py-16">Loading…</div>
            ) : (
              <EpisodeList
                episodes={episodes}
                onEdit={(episode) => {
                  setEditingEpisode(episode)
                  setShowEpisodeForm(true)
                }}
                onDelete={handleDeleteEpisode}
                onTagClick={(tag) => setEpisodeTagQuery(tag)}
                readOnly={!isAdmin}
              />
            )
          )}
        </>
      )}

      {activeTab === 'genres' && (
        <>
          <div className="flex flex-col sm:flex-row gap-3 mb-6">
            <input
              type="text"
              placeholder="Search genres…"
              value={genreSearchQuery}
              onChange={(e) => setGenreSearchQuery(e.target.value)}
              className="flex-1 bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-gray-100 placeholder-gray-500 focus:outline-none focus:border-indigo-500"
            />
          </div>

          {genresError && (
            <div className="bg-red-900/50 border border-red-700 text-red-300 px-4 py-3 rounded-lg mb-6">
              {genresError}
            </div>
          )}

          {isAdmin && showGenreForm && (
            <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
              <div className="bg-gray-900 border border-gray-700 rounded-xl shadow-2xl w-full max-w-lg overflow-y-auto max-h-[90vh]">
                <GenreForm
                  genre={editingGenre}
                  onSave={handleSaveGenre}
                  onCancel={() => {
                    setShowGenreForm(false)
                    setEditingGenre(null)
                  }}
                />
              </div>
            </div>
          )}

          {genresLoading ? (
            <div className="text-center text-gray-400 py-16">Loading…</div>
          ) : (
            <GenreList
              genres={filteredGenres}
              onEdit={(genre) => {
                setEditingGenre(genre)
                setShowGenreForm(true)
              }}
              onDelete={handleDeleteGenre}
              readOnly={!isAdmin}
            />
          )}
        </>
      )}
      {activeTab === 'series' && (
        <>
          <div className="flex flex-col sm:flex-row gap-3 mb-6">
            <input
              type="text"
              placeholder="Search series…"
              value={seriesSearchQuery}
              onChange={(e) => setSeriesSearchQuery(e.target.value)}
              className="flex-1 bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-gray-100 placeholder-gray-500 focus:outline-none focus:border-indigo-500"
            />
          </div>

          {seriesError && (
            <div className="bg-red-900/50 border border-red-700 text-red-300 px-4 py-3 rounded-lg mb-6">
              {seriesError}
            </div>
          )}

          {isAdmin && showSeriesForm && (
            <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
              <div className="bg-gray-900 border border-gray-700 rounded-xl shadow-2xl w-full max-w-lg overflow-y-auto max-h-[90vh]">
                <SeriesForm
                  series={editingSeries}
                  onSave={handleSaveSeries}
                  onCancel={() => {
                    setShowSeriesForm(false)
                    setEditingSeries(null)
                  }}
                />
              </div>
            </div>
          )}

          {seriesLoading ? (
            <div className="text-center text-gray-400 py-16">Loading…</div>
          ) : (
            <SeriesList
              series={filteredSeries}
              onEdit={(series) => {
                setEditingSeries(series)
                setShowSeriesForm(true)
              }}
              onDelete={handleDeleteSeries}
              readOnly={!isAdmin}
            />
          )}
        </>
      )}

      {activeTab === 'musicvideos' && (
        <>
          <div className="flex flex-col sm:flex-row gap-3 mb-6">
            <input
              type="text"
              placeholder="Search by title…"
              value={musicVideoTitleQuery}
              onChange={(e) => {
                setMusicVideoTitleQuery(e.target.value)
                if (e.target.value) setSelectedArtist(null)
              }}
              className="flex-1 bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-gray-100 placeholder-gray-500 focus:outline-none focus:border-indigo-500"
            />
          </div>

          {selectedArtist && !musicVideoTitleQuery && (
            <div className="flex items-center gap-3 mb-4">
              <button
                type="button"
                onClick={() => setSelectedArtist(null)}
                className="flex items-center gap-1 text-sm text-indigo-400 hover:text-indigo-200 transition-colors"
              >
                ← All artists
              </button>
              <span className="text-gray-500">|</span>
              <span className="text-white font-medium">🎤 {selectedArtist.name}</span>
            </div>
          )}

          {musicVideoTitleQuery && (
            <div className="flex items-center gap-2 mb-4">
              <span className="text-sm text-gray-400">Title search:</span>
              <span className="flex items-center gap-1 text-xs bg-indigo-800/40 text-indigo-300 px-2 py-0.5 rounded-full">
                {musicVideoTitleQuery}
                <button
                  type="button"
                  onClick={() => setMusicVideoTitleQuery('')}
                  className="hover:text-white ml-0.5"
                  aria-label="Clear title search"
                >
                  ×
                </button>
              </span>
            </div>
          )}

          {musicVideoTagQuery && (
            <div className="flex items-center gap-2 mb-4">
              <span className="text-sm text-gray-400">Tag filter:</span>
              <span className="flex items-center gap-1 text-xs bg-indigo-800/40 text-indigo-300 px-2 py-0.5 rounded-full">
                {musicVideoTagQuery}
                <button
                  type="button"
                  onClick={() => setMusicVideoTagQuery('')}
                  className="hover:text-white ml-0.5"
                  aria-label={`Remove tag filter ${musicVideoTagQuery}`}
                >
                  ×
                </button>
              </span>
            </div>
          )}

          {musicVideosError && (
            <div className="bg-red-900/50 border border-red-700 text-red-300 px-4 py-3 rounded-lg mb-6">
              {musicVideosError}
            </div>
          )}

          {isAdmin && showMusicVideoForm && (
            <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
              <div className="bg-gray-900 border border-gray-700 rounded-xl shadow-2xl w-full max-w-lg overflow-y-auto max-h-[90vh]">
                <MusicVideoForm
                  musicVideo={editingMusicVideo}
                  onSave={handleSaveMusicVideo}
                  onCancel={() => {
                    setShowMusicVideoForm(false)
                    setEditingMusicVideo(null)
                  }}
                />
              </div>
            </div>
          )}

          {!selectedArtist && !musicVideoTitleQuery && !musicVideoTagQuery ? (
            artistsLoading ? (
              <div className="text-center text-gray-400 py-16">Loading…</div>
            ) : (
              <>
                {artistsError && (
                  <div className="bg-red-900/50 border border-red-700 text-red-300 px-4 py-3 rounded-lg mb-6">
                    {artistsError}
                  </div>
                )}
                <ArtistList
                  artists={artists}
                  onArtistClick={(artist) => {
                    setSelectedArtist(artist)
                    setMusicVideoTitleQuery('')
                  }}
                  readOnly={true}
                />
              </>
            )
          ) : (
            musicVideosLoading ? (
              <div className="text-center text-gray-400 py-16">Loading…</div>
            ) : (
              <MusicVideoList
                musicVideos={musicVideos}
                onEdit={(mv) => {
                  setEditingMusicVideo(mv)
                  setShowMusicVideoForm(true)
                }}
                onDelete={handleDeleteMusicVideo}
                onTagClick={(tag) => setMusicVideoTagQuery(tag)}
                readOnly={!isAdmin}
              />
            )
          )}
        </>
      )}

      {activeTab === 'artists' && (
        <>
          <div className="flex flex-col sm:flex-row gap-3 mb-6">
            <input
              type="text"
              placeholder="Search artists…"
              value={artistSearchQuery}
              onChange={(e) => setArtistSearchQuery(e.target.value)}
              className="flex-1 bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-gray-100 placeholder-gray-500 focus:outline-none focus:border-indigo-500"
            />
          </div>

          {artistsError && (
            <div className="bg-red-900/50 border border-red-700 text-red-300 px-4 py-3 rounded-lg mb-6">
              {artistsError}
            </div>
          )}

          {isAdmin && showArtistForm && (
            <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
              <div className="bg-gray-900 border border-gray-700 rounded-xl shadow-2xl w-full max-w-lg overflow-y-auto max-h-[90vh]">
                <ArtistForm
                  artist={editingArtist}
                  onSave={handleSaveArtist}
                  onCancel={() => {
                    setShowArtistForm(false)
                    setEditingArtist(null)
                  }}
                />
              </div>
            </div>
          )}

          {artistsLoading ? (
            <div className="text-center text-gray-400 py-16">Loading…</div>
          ) : (
            <ArtistList
              artists={filteredArtists}
              onEdit={(artist) => {
                setEditingArtist(artist)
                setShowArtistForm(true)
              }}
              onDelete={handleDeleteArtist}
              readOnly={!isAdmin}
            />
          )}
        </>
      )}
    </>
  )
}
