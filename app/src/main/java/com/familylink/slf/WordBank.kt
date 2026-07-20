package com.familylink.slf

/**
 * Wortschatz des Computer-Gegners. Für jede Kategorie eine Liste plausibler
 * deutscher Begriffe. Zur Laufzeit werden sie nach Anfangsbuchstabe gefiltert.
 *
 * Der Computer findet nicht zu jedem Buchstaben ein Wort – genau wie ein echter
 * Mitspieler. Das gibt der Spielerin / dem Spieler eine faire Chance.
 */
object WordBank {

    val categories: List<Category> = listOf(
        Category(
            "Stadt",
            listOf(
                "Aachen", "Amsterdam", "Athen", "Augsburg", "Berlin", "Bremen", "Bonn",
                "Bochum", "Bielefeld", "Chemnitz", "Chicago", "Dresden", "Dortmund",
                "Düsseldorf", "Duisburg", "Essen", "Erfurt", "Frankfurt", "Freiburg",
                "Genf", "Graz", "Görlitz", "Hamburg", "Hannover", "Heidelberg",
                "Innsbruck", "Istanbul", "Jena", "Kiel", "Köln", "Kassel", "Karlsruhe",
                "Leipzig", "Lübeck", "London", "Lissabon", "München", "Mainz",
                "Mannheim", "Madrid", "Moskau", "Nürnberg", "Neuss", "Oslo",
                "Osnabrück", "Oberhausen", "Paris", "Passau", "Potsdam", "Prag",
                "Regensburg", "Rom", "Rostock", "Stuttgart", "Salzburg", "Siegen",
                "Trier", "Tokio", "Ulm", "Venedig", "Wien", "Wuppertal", "Würzburg",
                "Warschau", "Zürich", "Zwickau"
            )
        ),
        Category(
            "Land",
            listOf(
                "Ägypten", "Argentinien", "Albanien", "Australien", "Belgien",
                "Brasilien", "Bulgarien", "Bolivien", "Chile", "China", "Dänemark",
                "Deutschland", "Ecuador", "England", "Estland", "Finnland",
                "Frankreich", "Griechenland", "Ghana", "Georgien", "Honduras",
                "Indien", "Indonesien", "Irland", "Island", "Italien", "Japan",
                "Jordanien", "Jamaika", "Kanada", "Kenia", "Kroatien", "Kuba",
                "Litauen", "Luxemburg", "Lettland", "Marokko", "Mexiko", "Mongolei",
                "Namibia", "Niederlande", "Norwegen", "Nepal", "Österreich", "Oman",
                "Polen", "Portugal", "Peru", "Rumänien", "Russland", "Schweden",
                "Schweiz", "Spanien", "Serbien", "Türkei", "Thailand", "Tschechien",
                "Tunesien", "Ungarn", "Uruguay", "Ukraine", "Venezuela", "Vietnam",
                "Zypern"
            )
        ),
        Category(
            "Fluss",
            listOf(
                "Aare", "Amazonas", "Alster", "Donau", "Dnjepr", "Elbe", "Ems",
                "Euphrat", "Fulda", "Ganges", "Garonne", "Havel", "Inn", "Isar",
                "Iller", "Jangtse", "Jordan", "Kongo", "Lech", "Loire", "Leine",
                "Lena", "Main", "Mosel", "Mississippi", "Nil", "Neckar", "Niger",
                "Oder", "Po", "Rhein", "Rhone", "Ruhr", "Saale", "Seine", "Spree",
                "Themse", "Tiber", "Tigris", "Ussuri", "Volta", "Weser", "Werra",
                "Wolga"
            )
        ),
        Category(
            "Name",
            listOf(
                "Anna", "Anton", "Anja", "Bernd", "Bianca", "Bruno", "Carla",
                "Christian", "Claudia", "David", "Dennis", "Diana", "Emma", "Erik",
                "Eva", "Felix", "Frank", "Franziska", "Georg", "Greta", "Gustav",
                "Hanna", "Heinz", "Helga", "Ingo", "Ines", "Irina", "Jan", "Julia",
                "Jonas", "Karl", "Katrin", "Klaus", "Lena", "Lukas", "Laura",
                "Maria", "Max", "Moritz", "Nina", "Nils", "Norbert", "Olaf", "Otto",
                "Paula", "Peter", "Paul", "Quirin", "Rita", "Robert", "Rolf",
                "Sabine", "Stefan", "Sophie", "Thomas", "Tina", "Tobias", "Ulrich",
                "Ute", "Uwe", "Vera", "Viktor", "Werner", "Wolfgang", "Yvonne", "Zoe"
            )
        ),
        Category(
            "Tier",
            listOf(
                "Affe", "Adler", "Ameise", "Antilope", "Bär", "Biber", "Büffel",
                "Dachs", "Delfin", "Elefant", "Ente", "Esel", "Eule", "Fuchs",
                "Fisch", "Frosch", "Gans", "Giraffe", "Gorilla", "Hund", "Hase",
                "Hamster", "Igel", "Iltis", "Jaguar", "Kuh", "Katze", "Kamel",
                "Löwe", "Lama", "Luchs", "Maus", "Marder", "Nashorn", "Natter",
                "Otter", "Ochse", "Pferd", "Panda", "Pinguin", "Rabe", "Reh",
                "Ratte", "Schaf", "Schwein", "Storch", "Tiger", "Taube", "Uhu",
                "Vogel", "Wolf", "Wal", "Wildschwein", "Zebra", "Ziege"
            )
        ),
        Category(
            "Beruf",
            listOf(
                "Arzt", "Anwalt", "Architekt", "Astronaut", "Bäcker", "Bauer",
                "Busfahrer", "Chemiker", "Dachdecker", "Designer", "Elektriker",
                "Erzieher", "Fleischer", "Friseur", "Fotograf", "Gärtner",
                "Goldschmied", "Hebamme", "Ingenieur", "Installateur", "Journalist",
                "Jurist", "Koch", "Kellner", "Krankenpfleger", "Lehrer", "Lokführer",
                "Maler", "Maurer", "Mechaniker", "Metzger", "Notar", "Optiker",
                "Pilot", "Polizist", "Professor", "Richter", "Reporter", "Schlosser",
                "Schreiner", "Sekretär", "Tischler", "Tierarzt", "Übersetzer",
                "Verkäufer", "Winzer", "Zahnarzt", "Zimmermann"
            )
        )
    )

    /** Wählt für den Computer ein zufälliges Wort der Kategorie zum Buchstaben. */
    fun pick(category: Category, letter: Char): String? {
        val matches = category.words.filter { GameLogic.firstLetter(it) == letter }
        return if (matches.isEmpty()) null else matches.random()
    }
}

data class Category(val name: String, val words: List<String>)
