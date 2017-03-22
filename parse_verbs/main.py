import json
import os
import re
import zlib
from collections import defaultdict
from time import sleep

import requests
from bs4 import BeautifulSoup


class Verb:
    def __init__(self, infinitive, url):
        self.infinitive = infinitive
        self.url = url
        self.data = None
        self.translations = defaultdict(set)

    def set_data(self, data):
        self.data = data

    def add_translation(self, lang, value):
        if value:
            self.translations[lang].add(value)

    def get_translation(self, lang, default=None):
        return self.translations.get(lang, default)

    def __str__(self):
        return self.infinitive


class VerbDownloader:
    RE_VERB_LIST = re.compile(r'<a href="(/wiki[^"]*)"[^>]*>([^>]*)</a>', re.DOTALL)
    LANGUAGES = ('en', 'es')
    BASE_URL = 'https://ca.wiktionary.org'
    TRANSLATE_URL = 'https://translation.googleapis.com/language/translate/v2'
    FAVORITES = (
        'anar', 'llevar', 'viure', 'saber', 'haver', 'tenir', 'voler', 'poder', 'conèixer', 'girar', 'venir', 'tenir',
        'saber', 'caure', 'beure', 'dir', 'ser', 'estar', 'fer', 'veure', 'escriure', 'entendre',
        'prendre''tornar', 'saber', 'obrir', 'cantar', 'perdre', 'dormir', 'practicar', 'jugar', 'plegar', 'llegir',
        'passejar', 'preparar', 'llegir',)

    def __init__(self):
        self.translate_key = os.getenv('TRANSLATE_KEY', None)

    def run(self):
        verbs = []
        for verb in self.get_list():
            if self.parse_forms(verb):
                self.parse_translations(verb)
                verbs.append(verb)
        self.write_forms_csv(verbs, True)
        self.write_translations_csv(verbs, True)

    def write_json(self, verbs):
        with open('verbs.json', 'w') as f:
            f.write(json.dumps(verbs))

    def _write_csv(self, filename, data, compressed):
        if compressed:
            out = zlib.compress(data.encode('utf-8'))
        else:
            out = data.encode('utf-8')
        with open(filename, 'wb') as f:
            f.write(out)

    def write_translations_csv(self, verbs, compressed=True):
        out = ''
        for verb in verbs:
            out += verb.infinitive
            for lang in self.LANGUAGES:
                out += ';' + ','.join(list(verb.get_translation(lang, ()))[:5])
            out += '\r\n'
        self._write_csv('trans.csv', out, compressed)

    def write_forms_csv(self, verbs, compressed=True):
        out = ''
        for verb in verbs:
            out += verb.data['infinitiu'] + ';'
            out += ','.join(verb.data['participi']) + '|'
            out += verb.data['gerundi'] + '|'
            for t in ('present', 'passat simple', 'imperfet', 'futur', 'condicional'):
                out += ','.join(verb.data['indicatiu'][t]) + '|'
            for t in ('present', 'imperfet',):
                out += ','.join(verb.data['subjuntiu'][t]) + '|'
            out += ','.join(verb.data['imperatiu'])
            if verb.data['infinitiu'] in self.FAVORITES:
                out += ';1'
            else:
                out += ';0'
            for lang in self.LANGUAGES:
                out += ';' + ','.join(list(verb.get_translation(lang, ()))[:5])
            out += '\r\n'
        self._write_csv('verbs.csv', out, compressed)

    def get_list(self):
        with open('verbs_list.txt', 'r') as f:
            text = f.read()
            for match in self.RE_VERB_LIST.finditer(text):
                yield Verb(match.group(2), match.group(1))

    def get_soup_page(self, verb):
        filename = 'source/' + verb.infinitive
        if not os.path.exists(filename):
            self.download_page(verb)
        with open('source/' + verb.infinitive, 'r') as f:
            html_doc = f.read()
        return BeautifulSoup(html_doc.lower(), 'html.parser')

    def parse_translations(self, verb):
        soup = self.get_soup_page(verb)
        for lang in self.LANGUAGES:
            found = False
            #for span in soup.find_all('span', attrs={'class': f'lang-{lang}'}):
            #   link = span.find('a')
            #  if link:
            #     translation = link.text
            #    if translation:
            #       verb.add_translation(lang, translation)
            #      found = True
            if not found:
                translation = self.translate_word(lang, verb.infinitive)
                if translation:
                    verb.add_translation(lang, translation)
                    found = True
        return found

    def parse_forms(self, verb):
        soup = self.get_soup_page(verb)
        found = False
        for div in soup.find_all('div', attrs={'class': 'navcontent'}):
            if 'formes no personals' in div.text:
                found = True
                break
        if not found:
            return False
        # noinspection PyDictCreation
        data = {
            'infinitiu': verb.infinitive,
            'indicatiu': {
                'present': [],
                'imperfet': [],
                'passat simple': [],
                'futur': [],
                'condicional': []
            },
            'subjuntiu': {
                'present': [],
                'imperfet': [],
            },
            'imperatiu': [],
            'participi': [],
        }
        data['gerundi'] = div.find('th', string='gerundi').find_next_sibling('td').text.split(',')[0].strip()
        tag = div.find('th', string='participi')
        if tag is None:
            tag = div.find('th', string='participis')
        for i in tag.find_next_sibling('td').text.split(','):
            data['participi'].append(i.strip())

        title_tr = div.find('th', string='indicatiu').parent
        for tr in title_tr.find_next_siblings('tr'):
            th = tr.find('th')
            if th is None:
                continue
            if th.text == 'subjuntiu':
                break
            if th.text in data['indicatiu']:
                for i in th.find_next_siblings('td', limit=6):
                    v = i.text.split(',')[0].strip()
                    if v == u'–' or v == u'-':
                        v = u''
                    data['indicatiu'][th.text].append(v)
                if len(data['indicatiu'][th.text]) != 6:
                    raise ValueError

        title_tr = div.find('th', string='subjuntiu').parent
        for tr in title_tr.find_next_siblings('tr'):
            th = tr.find('th')
            if th is None:
                continue
            if th.text == 'imperatiu':
                break
            if th.text in data['subjuntiu']:
                for i in th.find_next_siblings('td', limit=6):
                    v = i.text.split(',')[0].strip()
                    if v == u'–' or v == u'-':
                        v = u''
                    data['subjuntiu'][th.text].append(v)
                if len(data['subjuntiu'][th.text]) != 6:
                    raise ValueError

        title_tr = div.find('th', string='imperatiu').parent
        for tr in title_tr.find_next_siblings('tr'):
            th = tr.find('th')
            if th and th.text == 'present':
                for i in th.find_next_siblings('td', limit=6):
                    v = i.text.split(',')[0].strip()
                    if v == u'–' or v == u'-':
                        v = u''
                    data['imperatiu'].append(v)
                if len(data['imperatiu']) != 6:
                    raise ValueError
        verb.set_data(data)
        return True

    def translate_word(self, target, word):
        if not self.translate_key:
            return None
        filename = f'trans/{word}_{target}'
        if not os.path.exists(filename):
            r = requests.get(self.TRANSLATE_URL, {
                'key': self.translate_key,
                'source': 'ca',
                'target': target,
                'q': word
            })
            if r.ok:
                text = r.text
                with open(filename, 'w') as f:
                    f.write(text)
            else:
                return None
        else:
            with open(filename, 'r') as f:
                text = f.read()
        return json.loads(text)['data']['translations'][0]['translatedText'].lower()

    def download_page(self, verb):
        r = requests.get(self.BASE_URL + verb.url)
        with open('source/' + verb.infinitive, 'w') as f:
            f.write(r.text)
        sleep(1)


if __name__ == '__main__':
    d = VerbDownloader()
    d.run()
