#!/usr/bin/env python3
"""Verify the bundled reference art and its explicit Kotlin resource mapping (stdlib only)."""
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
manifest = json.loads((root / 'docs/CARD_ART_SOURCES.json').read_text())
cards = manifest['cards']
ids = [card['id'] for card in cards]
assert len(ids) == len(set(ids)), 'Repeated card design id'
resources = (root / 'app/src/main/java/com/orel/wallet/ui/components/CardDesignResources.kt').read_text()
metadata = (root / 'app/src/main/java/com/orel/wallet/domain/CardDesignCatalog.kt').read_text()
for card in cards:
    path = root / card['asset']
    assert hashlib.sha256(path.read_bytes()).hexdigest() == card['sha256'], path
    assert f'R.drawable.{card["id"]}' in resources, card['id']
    assert f'CardDesign("{card["id"]}"' in metadata, card['id']
    assert card['sourceAsset'].startswith('https://'), card['id']
    assert card['width'] >= 240 and card['height'] >= 150, card['id']
files = {path.stem for path in (root / 'app/src/main/res/drawable-nodpi').glob('amex_*.webp')}
assert files == set(ids), 'Unlisted or missing artwork'
print(f'OK: {len(cards)} designs; hashes, provenance and resource mappings verified.')
