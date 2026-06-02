# MovieNight

## Helper tools

The scripts in this directory can be used to help manage video libraries.

### Thumbnail generation

Use `make_thumbnails` to walk a directory looking for video files.
For each video found, a thumbnail will be extracted from the frame at the 3s mark.
The frame will be scaled as needed so that its width is 640 pixels, then it will
be saved alongside the video file. For example, given `Bladerunner.mkv`, running
the script in that directory will generate a `Bladerunner.jpg` (if it does not
already exist):

```shell
# Scans the given directory and all subdirectories in one shot:
make_thumbnails /path/to/mediaDir
```

This script is great for processing an entire directory of images in one shot.
But what if the 3s mark doesn't result in a good thumbnail? Well, you can use
the `grab_thumbnail` script on a specific video file, and specify the size and the
time index to use. For example:

```shell
# I don't want to do a whole directory! Let's just do one video.
# Grab the frame at the 77s mark and scale it to 800px wide:
grab_thumbnail Bladerunner.mkv 77 800
```

### Track metadata extraction

MovieNight has the ability to start video playback with a specific audio track
and/or subtitle track, other than the defaults. To do this, you must first run
the `extract_track_json.py` script in your media directory. This script walks the
directory looking for video files, and will extract track metadata into a
sidecar json file. For example, given `Bladerunner.mkv`, you will end up
with `Bladerunner.mkv.tracks.json`:

```shell
# Scans the given directory and all subdirectories in one shot:
extract_track_json.py /path/to/mediaDir
```

## What do I have to do after running these scripts?

Nothing! MovieNight will detect the sidecar files and pass the information on
to the UI automatically. The thumbnail you generated will be used in browse
and search modes (hit "refresh" in your browser if you already had it open). 
The track JSON you generated will cause extra choosers
for "audio track" and "subtitles" to appear on the item detail page.

Note that audio/subtitle track selection only work with the "Watch in VLC" action.
The inline HTML 5 video player does not support multiple tracks :(

## What if I add new media after running these scripts?

No worries. The scripts will not overwrite sidecar files if they already exist.
(Actually, the `extract_track_json.py` script will overwrite if you use the `--force` option, but not by default).
So, you can just re-run the same command on your media directory, and any new files will be detected
and processed, while existing sidecar files will be left alone.

## What if I'm not running on Linux?

The python script should work on any platform, as long as you have `ffmpeg` installed.
The bash scripts are Linux-specific. No Windows PowerShell version is available at this time (PRs welcome!).
