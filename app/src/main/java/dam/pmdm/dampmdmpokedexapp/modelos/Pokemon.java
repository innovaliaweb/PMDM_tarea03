package dam.pmdm.dampmdmpokedexapp.modelos;

import java.util.List;
import com.google.firebase.firestore.PropertyName;
import java.text.SimpleDateFormat;
import java.util.Locale;
import com.google.firebase.Timestamp;

public class Pokemon {
    private String name;  // Nombre del Pokémon
    private String url;  // Añadir este campo
    private int id;
    private String fotoPokemon;
    private List<String> types;
    private int peso;
    private int altura;
    private boolean capturado;
    @PropertyName("nombre")
    private String nombre;
    private Timestamp fechaCaptura;

    // Constructor vacío necesario para Firebase
    public Pokemon() {
    }

    // Constructor con parámetros básicos
    public Pokemon(String name, int id) {
        this.name = name;
        this.id = id;
        this.capturado = false;
    }

    // Getters y Setters
    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFotoPokemon() {
        return fotoPokemon;
    }

    public void setFotoPokemon(String fotoPokemon) {
        this.fotoPokemon = fotoPokemon;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    public int getPeso() {
        return peso;
    }

    public void setPeso(int peso) {
        this.peso = peso;
    }

    public int getAltura() {
        return altura;
    }

    public void setAltura(int altura) {
        this.altura = altura;
    }

    public boolean isCapturado() {
        return capturado;
    }

    public void setCapturado(boolean capturado) {
        this.capturado = capturado;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @PropertyName("nombre")
    public String getNombre() {
        return nombre;
    }

    @PropertyName("nombre")
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Timestamp getFechaCaptura() {
        return fechaCaptura;
    }

    public void setFechaCaptura(Timestamp fechaCaptura) {
        this.fechaCaptura = fechaCaptura;
    }

    public String getFechaFormateada() {
        if (fechaCaptura == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(fechaCaptura.toDate());
    }
}
