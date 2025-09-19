import pathlib
import sys

SITE_DIR = pathlib.Path(__file__).resolve().parents[1] / "site"
sys.path.insert(0, str(SITE_DIR))

from projetista import format_status_with_names  # noqa: E402


def test_format_status_with_names_highlights_status_first():
    payload = {"suprimento": ["C", "victorr"]}
    formatted = format_status_with_names(payload["suprimento"])
    assert formatted == "C — victorr"
