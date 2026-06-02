#!/usr/bin/env python3
"""
extract_track_json.py

Walks a directory tree, finds all video files, extracts per-track
language metadata using ffprobe, and writes a sidecar .tracks.json file
next to each media file. This sidecar file describes the available
audio and subtitle tracks that were found in the video file.

Requirements: 
    ffprobe (comes with ffmpeg)

Usage:
    python3 extract_track_json.py /path/to/your/media [--dry-run] [--force]

Sidecar file format example (e.g. "Movie.mkv.tracks.json"):
{
  "file": "Movie.mkv",
  "audio": [
    {"index": 1, "language": "eng", "language_name": "English", "codec": "aac", "title": ""},
    {"index": 2, "language": "deu", "language_name": "German",  "codec": "aac", "title": ""},
    {"index": 3, "language": "fra", "language_name": "French",  "codec": "aac", "title": ""}
  ],
  "subtitle": [
    {"index": 4, "language": "eng", "language_name": "English", "codec": "dvd_subtitle", "title": ""},
    {"index": 5, "language": "deu", "language_name": "German",  "codec": "dvd_subtitle", "title": ""},
    {"index": 6, "language": "fra", "language_name": "French",  "codec": "dvd_subtitle", "title": ""}
  ]
}

Note that the "index" mentioned above is the unique numeric 0-based track id. This can be used
with "audio-track-id" and "subtitle-track-id" when generating VLC playlist files. Be careful
not to confuse this with "audio-track" and "subtitle-track"! They are different concepts in VLC.
In the example above, track id 2 points to the German audio track, and track id 5 points to the 
German subtitle track. But "audio-track" would be "1" for German audio, and "subtitle-track"
would also be "1" for German subtitles. (0=English, 1=German, 2=French, for both audio tracks
and subtitle tracks in this example). 

Think of it like absolute paths (the track-id) versus relative paths (the track number).
In this script, we only care about the absolute path.

"""

import argparse
import json
import subprocess
import sys
from pathlib import Path

# ISO 639-2/B and 639-1 codes → human-readable names.
# This covers the most common cases. VLC uses these same codes internally.
LANGUAGE_NAMES = {
    # 3-letter ISO 639-2 codes
    "eng": "English",
    "fra": "French",
    "fre": "French",
    "deu": "German",
    "ger": "German",
    "spa": "Spanish",
    "ita": "Italian",
    "jpn": "Japanese",
    "zho": "Chinese",
    "chi": "Chinese",
    "kor": "Korean",
    "por": "Portuguese",
    "rus": "Russian",
    "ara": "Arabic",
    "hin": "Hindi",
    "pol": "Polish",
    "nld": "Dutch",
    "dut": "Dutch",
    "swe": "Swedish",
    "nor": "Norwegian",
    "dan": "Danish",
    "fin": "Finnish",
    "ces": "Czech",
    "cze": "Czech",
    "hun": "Hungarian",
    "rum": "Romanian",
    "ron": "Romanian",
    "tur": "Turkish",
    "heb": "Hebrew",
    "tha": "Thai",
    "vie": "Vietnamese",
    "ind": "Indonesian",
    "msa": "Malay",
    "may": "Malay",
    "ukr": "Ukrainian",
    "cat": "Catalan",
    "hrv": "Croatian",
    "slk": "Slovak",
    "slo": "Slovak",
    "slv": "Slovenian",
    "bul": "Bulgarian",
    "ell": "Greek",
    "gre": "Greek",
    "lit": "Lithuanian",
    "lav": "Latvian",
    "est": "Estonian",
    "srp": "Serbian",
    "bos": "Bosnian",
    "glg": "Galician",
    "eus": "Basque",
    "lat": "Latin",
    "und": "Undetermined",
    # 2-letter ISO 639-1 codes (some video files use these instead)
    "en": "English",
    "fr": "French",
    "de": "German",
    "es": "Spanish",
    "it": "Italian",
    "ja": "Japanese",
    "zh": "Chinese",
    "ko": "Korean",
    "pt": "Portuguese",
    "ru": "Russian",
    "ar": "Arabic",
    "hi": "Hindi",
    "pl": "Polish",
    "nl": "Dutch",
    "sv": "Swedish",
    "no": "Norwegian",
    "da": "Danish",
    "fi": "Finnish",
    "cs": "Czech",
    "hu": "Hungarian",
    "ro": "Romanian",
    "tr": "Turkish",
    "he": "Hebrew",
    "th": "Thai",
    "vi": "Vietnamese",
    "id": "Indonesian",
    "ms": "Malay",
    "uk": "Ukrainian",
}

# ffprobe abstracts away the details of pulling out track info for us.
# It should work with any of these extensions, but it really depends
# on how the video file was created. No guarantees that any given file
# will have good metadata.
MEDIA_EXTENSIONS = {".mkv", ".mp4", ".avi", ".mov", ".m4v", ".ts", ".m2ts"}


def language_display_name(code: str) -> str:
    """Return a human-readable name for an ISO language code, falling back to the raw code."""
    if not code:
        return "Unknown"
    normalized = code.lower().strip()
    return LANGUAGE_NAMES.get(normalized, code.upper())


def probe_file(path: Path) -> list[dict]:
    """
    Run ffprobe on a file and return a list of stream dicts, each containing:
      index, codec_type, codec_name, language (raw tag), language_name, title
    """
    cmd = [
        "ffprobe",
        "-v", "quiet",
        "-print_format", "json",
        "-show_streams",
        str(path),
    ]
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
    except FileNotFoundError:
        print("ERROR: ffprobe not found. Please install ffmpeg.", file=sys.stderr)
        sys.exit(1)
    except subprocess.TimeoutExpired:
        print(f"  WARNING: ffprobe timed out on {path.name}", file=sys.stderr)
        return []

    if result.returncode != 0:
        print(f"  WARNING: ffprobe failed on {path.name}: {result.stderr.strip()}", file=sys.stderr)
        return []

    try:
        data = json.loads(result.stdout)
    except json.JSONDecodeError:
        print(f"  WARNING: couldn't parse ffprobe JSON for {path.name}", file=sys.stderr)
        return []

    if not isinstance(data, dict) or "streams" not in data:
        print(f"  WARNING: ffprobe output missing 'streams' for {path.name}", file=sys.stderr)
        return []

    streams = []
    for s in data.get("streams", []):
        tags = s.get("tags", {})
        # Language tag can appear as "language", "LANGUAGE", or occasionally "lang"
        raw_lang = (
            tags.get("language")
            or tags.get("LANGUAGE")
            or tags.get("lang")
            or ""
        )
        streams.append({
            "index": s.get("index"),
            "codec_type": s.get("codec_type", "unknown"),
            "codec": s.get("codec_name", "unknown"),
            "language": raw_lang,
            "language_name": language_display_name(raw_lang) if raw_lang else "Unknown",
            "title": tags.get("title") or tags.get("TITLE") or "",
        })
    return streams


def build_sidecar(file_path: Path, streams: list[dict]) -> dict:
    """Organise stream data into the sidecar structure grouped by type."""
    result: dict[str, list] = {"file": file_path.name, "audio": [], "subtitle": []}
    for s in streams:
        if s["codec_type"] not in ("audio", "subtitle"):
            continue
        track = {
            "index": s["index"],
            "language": s["language"],
            "language_name": s["language_name"],
            "codec": s["codec"],
            "title": s["title"],
        }
        bucket = s["codec_type"]
        if bucket not in result:
            result[bucket] = []
        result[bucket].append(track)
    return result


def sidecar_path(media_path: Path) -> Path:
    return media_path.parent / (media_path.name + ".tracks.json")


def process_file(media_path: Path, dry_run: bool, force: bool) -> bool:
    """Process one media file. Returns True if a sidecar was written (or would be)."""
    out = sidecar_path(media_path)
    if out.exists() and not force:
        print(f"  SKIP (sidecar exists, use --force to overwrite): {media_path.name}")
        return False

    streams = probe_file(media_path)
    if not streams:
        print(f"  SKIP (no streams found): {media_path.name}")
        return False

    sidecar = build_sidecar(media_path, streams)

    audio_summary = ", ".join(
        t["language_name"] + (f" [{t['title']}]" if t["title"] else "")
        for t in sidecar.get("audio", [])
    ) or "(none)"
    sub_summary = ", ".join(
        t["language_name"] + (f" [{t['title']}]" if t["title"] else "")
        for t in sidecar.get("subtitle", [])
    ) or "(none)"
    print(f"  {'[DRY RUN] ' if dry_run else ''}{'writing' if not dry_run else 'would write'}: {out.name}")
    print(f"    audio:    {audio_summary}")
    print(f"    subtitle: {sub_summary}")

    if not dry_run:
        out.write_text(json.dumps(sidecar, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    return True


def walk_directory(root: Path, dry_run: bool, force: bool):
    media_files = sorted(
        p for p in root.rglob("*") if p.is_file() and p.suffix.lower() in MEDIA_EXTENSIONS
    )
    if not media_files:
        print(f"No media files found under {root}")
        return

    print(f"Found {len(media_files)} media file(s) under {root}\n")
    written = 0
    for mf in media_files:
        print(f"{mf.relative_to(root)}")
        if process_file(mf, dry_run=dry_run, force=force):
            written += 1

    noun = "sidecar" if written == 1 else "sidecars"
    action = "would write" if dry_run else "wrote"
    print(f"\nDone. {action} {written} {noun}.")


def main():
    parser = argparse.ArgumentParser(
        description="Extract per-track language metadata from media files and write .tracks.json sidecars."
    )
    parser.add_argument("directory", help="Root directory to walk")
    parser.add_argument("--dry-run", action="store_true", help="Print what would be done without writing anything")
    parser.add_argument("--force", action="store_true", help="Overwrite existing sidecar files")
    args = parser.parse_args()

    root = Path(args.directory).expanduser().resolve()
    if not root.is_dir():
        print(f"ERROR: not a directory: {root}", file=sys.stderr)
        sys.exit(1)

    walk_directory(root, dry_run=args.dry_run, force=args.force)


if __name__ == "__main__":
    main()

