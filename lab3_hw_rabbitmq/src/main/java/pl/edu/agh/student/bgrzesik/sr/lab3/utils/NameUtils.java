package pl.edu.agh.student.bgrzesik.sr.lab3.utils;

import java.util.Locale;

public class NameUtils {
    public static String nameToId(String name) {
        String id = name;
        id = id.toUpperCase(Locale.ROOT);
        id = id.replaceAll("\\s", "-");
        return id;
    }
}
