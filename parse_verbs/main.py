# coding=utf-8
import json
import os
import re
import zlib
from time import sleep

import requests
from bs4 import BeautifulSoup


class Verb(object):
    def __init__(self, infinitive, url):
        self.infinitive = infinitive.decode("utf8")
        self.url = url
        self.data = None

    def set_data(self, data):
        self.data = data

    def __unicode__(self):
        return self.infinitive

    def __repr__(self):
        return self.infinitive


class VerbDownloader(object):
    def run(self):
        count = 0
        verbs = []
        for verb in self.get_list():
            if self.parse_page(verb):
                verbs.append(verb.data)
                count += 1
            else:
                print verb.infinitive
        self.write_csv(verbs)
        print count

    def write_json(self, verbs):
        with open("verbs.json", "w") as f:
            f.write(json.dumps(verbs))

    def write_csv(self, verbs, compressed=True):
        FAVORITES = [u'anar',
                     u'llevar',
                     u'viure',
                     u'saber',
                     u'haver',
                     u'tenir',
                     u'voler',
                     u'poder',
                     u'conèixer',
                     u'girar',
                     u'venir',
                     u'tenir',
                     u'saber',
                     u'caure',
                     u'beure',
                     u'dir',
                     u'ser',
                     u'estar',
                    u'fer',
                     u'veure',
                     u'escriure',
                     u'entendre',
                     u'prendre',
                     u'tornar',
                     u'saber',
                     u'obrir',
                     u'cantar',
                     u'perdre',
                     u'dormir',
                     u'practicar',
                     u'jugar',
                     u'plegar',
                     u'llegir',
                     u'passejar',
                     u'preparar',
                     u'llegir',]
        out = u""
        for verb in verbs:
            out += verb["infinitiu"] + u";"
            out += u",".join(verb["participi"]) + u"|"
            out += verb["gerundi"] + u"|"
            for t in ("present", "passat simple", "imperfet", "futur", "condicional"):
                out += u",".join(verb["indicatiu"][t]) + u"|"
            for t in ("present", "imperfet",):
                out += u",".join(verb["subjuntiu"][t]) + u"|"
            out += u",".join(verb["imperatiu"])
            if verb["infinitiu"] in FAVORITES:
                out += u";1"
            else:
                out += u";0"
            out += u"\r\n"
        if compressed:
            out = zlib.compress(out.encode("utf-8"))
        else:
            out = out.encode("utf-8")
        with open("verbs.csv", "w") as f:
            f.write(out)

    def get_list(self):
        RE_VERB_LIST = re.compile(r'<a href="(/wiki[^"]*)"[^>]*>([^>]*)</a>', re.DOTALL)
        with open("verbs_list.txt", "r") as f:
            text = f.read()
            for match in RE_VERB_LIST.finditer(text):
                yield Verb(match.group(2), match.group(1))

    def parse_page(self, verb):
        filename = "source/" + verb.infinitive
        if not os.path.exists(filename):
            self.download_page(verb)
        with open("source/" + verb.infinitive, "r") as f:
            html_doc = f.read()
        soup = BeautifulSoup(html_doc.lower(), 'html.parser')
        found = False
        for div in soup.find_all("div", attrs={"class": "navcontent"}):
            if "formes no personals" in div.text:
                found = True
                break
        if not found:
            return False
        # noinspection PyDictCreation
        data = {
            "infinitiu": verb.infinitive,
            "indicatiu": {
                "present": [],
                "imperfet": [],
                "passat simple": [],
                "futur": [],
                "condicional": []
            },
            "subjuntiu": {
                "present": [],
                "imperfet": [],
            },
            "imperatiu": [],
            "participi": [],
        }
        data["gerundi"] = div.find("th", string="gerundi").find_next_sibling("td").text.split(',')[0].strip()
        tag = div.find("th", string="participi")
        if tag is None:
            tag = div.find("th", string="participis")
        for i in tag.find_next_sibling("td").text.split(','):
            data["participi"].append(i.strip())

        title_tr = div.find("th", string="indicatiu").parent
        for tr in title_tr.find_next_siblings("tr"):
            th = tr.find("th")
            if th is None:
                continue
            if th.text == "subjuntiu":
                break
            if th.text in data["indicatiu"]:
                for i in th.find_next_siblings("td", limit=6):
                    v = i.text.split(',')[0].strip()
                    if v == u"–" or v == u"-":
                        v = u""
                    data["indicatiu"][th.text].append(v)
                if len(data["indicatiu"][th.text]) != 6:
                    raise ValueError

        title_tr = div.find("th", string="subjuntiu").parent
        for tr in title_tr.find_next_siblings("tr"):
            th = tr.find("th")
            if th is None:
                continue
            if th.text == "imperatiu":
                break
            if th.text in data["subjuntiu"]:
                for i in th.find_next_siblings("td", limit=6):
                    v = i.text.split(',')[0].strip()
                    if v == u"–" or v == u"-":
                        v = u""
                    data["subjuntiu"][th.text].append(v)
                if len(data["subjuntiu"][th.text]) != 6:
                    raise ValueError

        title_tr = div.find("th", string="imperatiu").parent
        for tr in title_tr.find_next_siblings("tr"):
            th = tr.find("th")
            if th and th.text == "present":
                for i in th.find_next_siblings("td", limit=6):
                    v = i.text.split(',')[0].strip()
                    if v == u"–" or v == u"-":
                        v = u""
                    data["imperatiu"].append(v)
                if len(data["imperatiu"]) != 6:
                    raise ValueError
        verb.set_data(data)
        return True

    def download_page(self, verb):
        r = requests.get("https://ca.wiktionary.org" + verb.url)
        with open("source/" + verb.infinitive, "w") as f:
            f.write(r.text.encode("utf8"))
        sleep(1)


if __name__ == "__main__":
    d = VerbDownloader()
    d.run()
