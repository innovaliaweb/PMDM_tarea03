package dam.pmdm.dampmdmpokedexapp.modelos;

import java.util.ArrayList;
import java.util.List;

public class PokemonDetails {
    private String name;
    private int id;
    private List<Type> types;
    private int weight;
    private int height;
    private Sprites sprites;

    // Clase interna para los tipos
    public static class Type {
        private TypeInfo type;

        public TypeInfo getType() {
            return type;
        }
    }

    public static class TypeInfo {
        private String name;

        public String getName() {
            return name;
        }
    }

    // Clase interna para los sprites
    public static class Sprites {
        private String front_default;

        public String getFrontDefault() {
            return front_default;
        }
    }

    // Getters
    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public List<String> getTypes() {
        List<String> typeNames = new ArrayList<>();
        for (Type type : types) {
            typeNames.add(type.getType().getName());
        }
        return typeNames;
    }

    public int getWeight() {
        return weight;
    }

    public int getHeight() {
        return height;
    }

    public Sprites getSprites() {
        return sprites;
    }
} 