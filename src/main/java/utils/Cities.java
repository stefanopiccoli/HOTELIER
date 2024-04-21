package utils;

import java.util.ArrayList;

public class Cities {
    private static final ArrayList<String> cities = new ArrayList<>();

    static {
        cities.add("Aosta");
        cities.add("Torino");
        cities.add("Genova");
        cities.add("Milano");
        cities.add("Trento");
        cities.add("Venezia");
        cities.add("Trieste");
        cities.add("Bologna");
        cities.add("Firenze");
        cities.add("Ancona");
        cities.add("Perugia");
        cities.add("Roma");
        cities.add("L'Aquila");
        cities.add("Campobasso");
        cities.add("Napoli");
        cities.add("Bari");
        cities.add("Potenza");
        cities.add("Catanzaro");
        cities.add("Palermo");
        cities.add("Cagliari");
    }

    // Metodo per ottenere il capoluogo di regione dato il nome della regione
    public static ArrayList<String> getCities() {
        return cities;
    }
}
