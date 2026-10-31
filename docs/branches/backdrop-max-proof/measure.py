"""Read-only TMDB proof; reads the existing profile key without printing/storing it."""
import io
import json
from pathlib import Path
from urllib.parse import urlencode
from urllib.request import urlopen
from PIL import Image

key = next(line.partition('=')[2].strip() for line in
           (Path.home() / '.config/nuvio/nuvio_tmdb_settings.properties').read_text().splitlines()
           if line.startswith('tmdb_api_key_1='))

def api(endpoint, **query):
    with urlopen('https://api.themoviedb.org/3/' + endpoint + '?' + urlencode(dict(api_key=key, **query)), timeout=20) as response:
        return json.load(response)

def dimensions(url):
    with urlopen(url, timeout=30) as response:
        image = Image.open(io.BytesIO(response.read()))
        return list(image.size)

cases = [
    ('Samurai Champloo', 'tt0423731', 'tv', 'https://image.tmdb.org/t/p/original/aZRGh7ccNAdqQWhHypvlT9UuXjZ.jpg'),
    ('Severance', 'tt11280740', 'tv', 'https://image.tmdb.org/t/p/original/ixgFmf1X59PUZam2qbAfskx2gQr.jpg'),
    ('Parasite', 'tt6751668', 'movie', 'https://image.tmdb.org/t/p/original/TU9NIjwzjoKPwQHoHshkFcQUCG.jpg'),
]
proof = []
for title, imdb, media, before in cases:
    tmdb = api('find/' + imdb, external_source='imdb_id')[media + '_results'][0]['id']
    images = api(f'{media}/{tmdb}/images', include_image_language='null,xx')
    eligible = [i for i in images['backdrops'] if i.get('iso_639_1') in (None, 'xx') and i.get('width', 0) > 0 and i.get('height', 0) > 0]
    selected = max(eligible, key=lambda i: (i['width'] * i['height'], i.get('vote_count', 0)))
    after = 'https://image.tmdb.org/t/p/original' + selected['file_path']
    proof.append(dict(title=title, imdb_id=imdb, tmdb_id=tmdb, media_type=media,
                      before_url=before, before_dimensions=dimensions(before),
                      after_url=after, after_dimensions=dimensions(after),
                      selected_votes=selected['vote_count'], selected_language=selected['iso_639_1']))
    (Path(__file__).parent / f'{media}-{tmdb}-images.json').write_text(json.dumps(images, indent=2) + '\n')
    print(title, proof[-1]['before_dimensions'], '->', proof[-1]['after_dimensions'], after, flush=True)
(Path(__file__).parent / 'dimensions.json').write_text(json.dumps(proof, indent=2) + '\n')
