#!/usr/bin/env python3
"""
Publishes the full Google Play store listing (title, descriptions, promo video URL,
icon, feature graphic, and phone/7in/10in screenshots) via the Android Publisher API.

This is separate from the release (AAB) upload, which the r0adkll/upload-google-play
GitHub Action handles as its own edit. The Android Publisher API only allows one open
edit per app at a time, so this script creates and commits its own edit, run as a
step after the release upload has already committed.

Requires GOOGLE_PLAY_SERVICE_ACCOUNT_JSON in the environment (the same service account
secret used for the release upload) and the `google-auth` + `requests` packages.
"""
import glob
import json
import os
import sys

import google.auth.transport.requests
import requests
from google.oauth2 import service_account

PACKAGE_NAME = "com.charles.nutrisnap"
LOCALE = "en-US"
SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]
API_BASE = f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{PACKAGE_NAME}"
UPLOAD_BASE = f"https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/{PACKAGE_NAME}"

ASSETS_DIR = "play-store-assets"
PROMO_VIDEO_URL = "https://youtu.be/txT3lqMDjaM"

TITLE = "NutriSnap: AI Calorie Tracker"
SHORT_DESCRIPTION = "Snap a meal, get instant calories & macros — 100% on-device AI, private."


def get_access_token() -> str:
    sa_json = os.environ["GOOGLE_PLAY_SERVICE_ACCOUNT_JSON"]
    sa_info = json.loads(sa_json)
    creds = service_account.Credentials.from_service_account_info(sa_info, scopes=SCOPES)
    creds.refresh(google.auth.transport.requests.Request())
    return creds.token


def read_full_description() -> str:
    with open(os.path.join(ASSETS_DIR, "listing.md"), encoding="utf-8") as f:
        content = f.read()
    marker = "## Full description (max 4000 chars)"
    after = content.split(marker, 1)[1]
    full_desc = after.split("---", 1)[0].strip()
    if len(full_desc) > 4000:
        raise ValueError(f"Full description is {len(full_desc)} chars, exceeds Play's 4000 limit")
    return full_desc


def check(response: requests.Response, action: str) -> None:
    if not response.ok:
        print(f"FAILED: {action}", file=sys.stderr)
        print(f"  status: {response.status_code}", file=sys.stderr)
        print(f"  body: {response.text}", file=sys.stderr)
        response.raise_for_status()
    print(f"OK: {action}")


def main() -> None:
    token = get_access_token()
    headers = {"Authorization": f"Bearer {token}"}

    r = requests.post(f"{API_BASE}/edits", headers=headers)
    check(r, "create edit")
    edit_id = r.json()["id"]

    full_description = read_full_description()
    listing = {
        "language": LOCALE,
        "title": TITLE,
        "shortDescription": SHORT_DESCRIPTION,
        "fullDescription": full_description,
        "video": PROMO_VIDEO_URL,
    }
    r = requests.put(
        f"{API_BASE}/edits/{edit_id}/listings/{LOCALE}",
        headers=headers,
        json=listing,
    )
    check(r, "update store listing text + promo video URL")

    def clear_images(image_type: str) -> None:
        # Image uploads *append* to whatever this listing already has committed from a
        # previous run — without clearing first, re-running this script repeatedly grows
        # the screenshot count past Play's 8-per-language cap.
        r = requests.delete(f"{API_BASE}/edits/{edit_id}/listings/{LOCALE}/{image_type}", headers=headers)
        check(r, f"clear existing {image_type}")

    def upload_image(image_type: str, path: str) -> None:
        with open(path, "rb") as f:
            data = f.read()
        r = requests.post(
            f"{UPLOAD_BASE}/edits/{edit_id}/listings/{LOCALE}/{image_type}",
            headers={**headers, "Content-Type": "image/png"},
            data=data,
        )
        check(r, f"upload {image_type}: {os.path.basename(path)}")

    for image_type in ["icon", "featureGraphic", "phoneScreenshots", "sevenInchScreenshots", "tenInchScreenshots"]:
        clear_images(image_type)

    upload_image("icon", os.path.join(ASSETS_DIR, "icon", "icon-512.png"))
    upload_image("featureGraphic", os.path.join(ASSETS_DIR, "feature-graphic.png"))

    for image_type, folder in [
        ("phoneScreenshots", "phone"),
        ("sevenInchScreenshots", "sevenInch"),
        ("tenInchScreenshots", "tenInch"),
    ]:
        paths = sorted(glob.glob(os.path.join(ASSETS_DIR, "screenshots", folder, "*.png")))
        for path in paths:
            upload_image(image_type, path)

    # A brand-new app's first-ever store listing submission is always sent for review —
    # Google rejects changesNotSentForReview in that case ("Changes are sent for review
    # automatically"). Once this app has a reviewed listing, later listing-only edits can
    # add that param back to avoid re-triggering review for cosmetic changes. Submitting
    # listing content for review does not, on its own, roll the app out — the production
    # release (uploaded separately with status=draft) still needs a human to review and
    # roll out from Play Console.
    r = requests.post(f"{API_BASE}/edits/{edit_id}:commit", headers=headers)
    check(r, "commit listing edit")


if __name__ == "__main__":
    main()
