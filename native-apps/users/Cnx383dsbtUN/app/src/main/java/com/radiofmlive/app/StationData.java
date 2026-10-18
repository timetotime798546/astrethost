package com.radiofmlive.app;

import java.util.ArrayList;
import java.util.List;

public class StationData {

    public static List<RadioStation> getPresetStations() {
        List<RadioStation> list = new ArrayList<RadioStation>();

        // Real, widely accessible public streaming URLs (Icecast & Shoutcast reliable servers)
        list.add(new RadioStation(
                "soma_groovesalad",
                "Groove Salad",
                "https://ice1.somafm.com/groovesalad-128-mp3",
                "Chillout / Ambient",
                "English",
                "94.5 FM",
                "USA"
        ));

        list.add(new RadioStation(
                "soma_dronezone",
                "Drone Zone",
                "https://ice1.somafm.com/dronezone-128-mp3",
                "Ambient / Space",
                "English",
                "89.1 FM",
                "USA"
        ));

        list.add(new RadioStation(
                "soma_indiepop",
                "Indie Pop Rocks!",
                "https://ice1.somafm.com/indiepop-128-mp3",
                "Indie Rock",
                "English",
                "98.3 FM",
                "USA"
        ));

        list.add(new RadioStation(
                "soma_secretagent",
                "Secret Agent",
                "https://ice1.somafm.com/secretagent-128-mp3",
                "Lounge / Spy",
                "English",
                "90.7 FM",
                "USA"
        ));

        list.add(new RadioStation(
                "swiss_classic",
                "Radio Swiss Classic",
                "https://stream.srg-ssr.ch/m/rsc_de/mp3_128",
                "Classical",
                "Instrumental",
                "102.1 FM",
                "Switzerland"
        ));

        list.add(new RadioStation(
                "swiss_jazz",
                "Radio Swiss Jazz",
                "https://stream.srg-ssr.ch/m/rsj/mp3_128",
                "Jazz & Blues",
                "English",
                "92.4 FM",
                "Switzerland"
        ));

        list.add(new RadioStation(
                "soma_defcon",
                "DEF CON Radio",
                "https://ice1.somafm.com/defcon-128-mp3",
                "Electronic",
                "English",
                "104.9 FM",
                "USA"
        ));

        list.add(new RadioStation(
                "soma_bootliquor",
                "Boot Liquor",
                "https://ice1.somafm.com/bootliquor-128-mp3",
                "Country / Americana",
                "English",
                "96.7 FM",
                "USA"
        ));

        list.add(new RadioStation(
                "soma_spacestation",
                "Space Station Soma",
                "https://ice1.somafm.com/spacestation-128-mp3",
                "Electronica",
                "English",
                "106.3 FM",
                "USA"
        ));

        list.add(new RadioStation(
                "soma_suburbsofgoa",
                "Suburbs of Goa",
                "https://ice1.somafm.com/suburbsofgoa-128-mp3",
                "World / Desi Chill",
                "Hindi & English",
                "91.9 FM",
                "India"
        ));

        return list;
    }

    public static List<String> getAllGenres(List<RadioStation> stations) {
        List<String> genres = new ArrayList<String>();
        genres.add("All Genres");
        for (int i = 0; i < stations.size(); i++) {
            String g = stations.get(i).getGenre();
            if (!genres.contains(g)) {
                genres.add(g);
            }
        }
        return genres;
    }
}