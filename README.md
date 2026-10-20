# MusicFlow v1 · Android projekat

Prva probna verzija Android aplikacije koja otvara **YouTube Music** ili mobilni **YouTube** u sopstvenom WebView pregledaču.

## Funkcije
- Pretraga pesama u izabranom režimu (YouTube Music / YouTube).
- Štit: osnovna blokada mrežnih zahteva ka poznatim reklamnim domenima. Uključen je podrazumevano, uz mogućnost isključivanja.
- Omiljene: čuvanje do 50 URL-ova pesama/stranica, otvaranje i brisanje.
- Tamni interfejs, navigacija, istorija učitanih stranica, pamćenje poslednje stranice.
- Podešavanja: obaveštenje o ograničenjima, brisanje kolačića i keša.
- Radi kao zasebno instalirana aplikacija (paket `rs.musicflow.app`).

## Važna ograničenja
- **Ovo nije izvorni YouTube Music plejer ni kopija Brave mehanizma za blokiranje reklama.** YouTube može prikazivati reklame i posle filtracije, jer koristi i sopstvene serverske metode. Ne postoji garancija blokiranja reklama.
- **Muzika sa zaključanim ekranom i rad u pozadini nisu garantovani.** Android WebView i sam YouTube mogu da pauziraju reprodukciju kada aplikacija ode u pozadinu. Nije dodat foreground audio servis.
- **Prijavljivanje na Google nalog može biti odbijeno u ugrađenom WebView-u**; ovo ograničenje je van kontrole aplikacije.
- Reprodukcija YouTube sadržaja podleže uslovima njihovog servisa. Nije omogućen download YouTube audio/video sadržaja.
- Ovo je **neisprobana probna verzija**: izvorni kod je pripremljen, ali nije izgrađen i testiran kao APK u ovoj sesiji.

## Kako napraviti APK preko GitHub-a
1. Raspakuj `MusicFlow-v1.zip` na uređaju ili računaru.
2. Napravi **nov GitHub repozitorijum** (npr. `MusicFlow-v1`).
3. U njegov koren prebaci **sadržaj raspakovanog direktorijuma** (mora se uključiti i skriveni `.github/workflows/android.yml`). **Nemoj ubacivati samo ZIP** – GitHub ga ne raspakuje automatski. Struktura mora imati `app/`, `.github/`, `build.gradle`, `settings.gradle` i `gradle.properties` odmah u korenu repozitorijuma.
4. Izaberi karticu **Actions** i pokreni `Napravi MusicFlow APK` > **Run workflow**. Prvo slanje fajlova će takođe automatski pokrenuti postupak.
5. Kada se pojavi zelena kvačica, otvori taj run, odeljak **Artifacts**, pa preuzmi `MusicFlow-v1-APK`.
6. Raspakuj preuzeti artifact ZIP: u njemu je **`app-debug.apk`**. Instaliraj ga na Android uređaj i odobri instalaciju iz tog izvora ako te telefon pita.

Android build: Java 17, Android Gradle Plugin 8.13.0, Gradle 8.13, API 35.

## Problemi koji se mogu javiti
- `YouTube Music` ne može da se učita → probaj dugme `YouTube`.
- Nema pesme ili je prijava blokirana → to su ograničenja WebView/YouTube okruženja.
- I dalje vidiš YouTube reklame → trenutna osnovna filtracija ne uklanja oglase koje YouTube isporučuje iz svog domena.
- Audio se prekida pri zaključavanju → ova verzija ne garantuje rad u pozadini.

## Sadržaj arhive
- `app/` Android aplikacija (Java, bez biblioteka trećih strana)
- `.github/workflows/android.yml` GitHub Actions postupak za pravljenje APK-a
- `build.gradle`, `settings.gradle`, `gradle.properties` podešavanja projekta
- `README.md` ovo uputstvo
