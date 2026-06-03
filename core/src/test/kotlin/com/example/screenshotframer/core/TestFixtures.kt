package com.example.screenshotframer.core

const val SAMPLE_MANIFEST = """
{
  "version": 1,
  "devices": [
    {
      "id": "pixel-9-pro",
      "brand": "Google",
      "name": "Pixel 9 Pro",
      "screen": { "width": 1344, "height": 2992 },
      "frame": { "width": 1512, "height": 3220 },
      "safeArea": { "left": 92, "top": 116, "width": 1344, "height": 2992 },
      "colors": [
        { "id": "obsidian", "name": "Obsidian", "asset": "pixel_9_pro_obsidian.png" },
        { "id": "porcelain", "name": "Porcelain", "asset": "pixel_9_pro_porcelain.png" }
      ]
    },
    {
      "id": "galaxy-s24-ultra",
      "brand": "Samsung",
      "name": "Galaxy S24 Ultra",
      "screen": { "width": 1440, "height": 3120 },
      "frame": { "width": 1628, "height": 3350 },
      "safeArea": { "left": 94, "top": 112, "width": 1440, "height": 3120 },
      "colors": [
        { "id": "titanium-black", "name": "Titanium Black", "asset": "galaxy_s24_ultra_black.png" }
      ]
    }
  ]
}
"""
