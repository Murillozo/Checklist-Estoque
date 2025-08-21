# Projetista API

This module exposes endpoints for working with project photos.

## GET /api/fotos/raw/<path>
Returns the original image file located under `FOTOS_DIR`.

## GET /api/fotos/thumb/<path>
Generates (on first request) and returns a cached thumbnail of the image with
maximum dimension 300px. Thumbnails are stored under `_thumb_cache` inside
`FOTOS_DIR` and reused on subsequent requests. Use this endpoint when
requesting images for galleries or previews.
