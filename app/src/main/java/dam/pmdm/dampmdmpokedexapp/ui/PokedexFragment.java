package dam.pmdm.dampmdmpokedexapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.view.Gravity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dam.pmdm.dampmdmpokedexapp.Login;
import dam.pmdm.dampmdmpokedexapp.R;
import dam.pmdm.dampmdmpokedexapp.adapter.PokemonAdapter;
import dam.pmdm.dampmdmpokedexapp.api.PokeApiService;
import dam.pmdm.dampmdmpokedexapp.modelos.Pokemon;
import dam.pmdm.dampmdmpokedexapp.modelos.PokemonResponse;
import dam.pmdm.dampmdmpokedexapp.modelos.PokemonDetails;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;

public class PokedexFragment extends Fragment {

    private RecyclerView recyclerView;
    private PokemonAdapter adapter;
    private PokeApiService api;
    private FirebaseFirestore db;
    private List<String> pokemonCapturados;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pokedex, container, false);

        try {
            recyclerView = view.findViewById(R.id.recycler_view_pokedex);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

            pokemonCapturados = new ArrayList<>();
            adapter = new PokemonAdapter(new ArrayList<>(), this::mostrarDetallesPokemon);
            recyclerView.setAdapter(adapter);

            db = FirebaseFirestore.getInstance();

            // Crear instancia de Retrofit directamente
            Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://pokeapi.co/api/v2/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

            api = retrofit.create(PokeApiService.class);

            cargarPokemonCapturados();

        } catch (Exception e) {
            Log.e("PokedexFragment", "Error en onCreateView: ", e);
        }

        return view;
    }

    private void cargarPokemonCapturados() {
        if (!isAdded() || getActivity() == null) {
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String userId = currentUser.getUid();
        db.collection("pokemon_capturados")
            .document(userId)
            .collection("pokemons")
            .get()
            .addOnSuccessListener(documents -> {
                if (!isAdded()) return;

                pokemonCapturados.clear();
                for (DocumentSnapshot doc : documents) {
                    String pokemonName = doc.getId();
                    if (pokemonName != null) {
                        pokemonCapturados.add(pokemonName);
                    }
                }
                cargarListaPokemon();
            })
            .addOnFailureListener(e -> {
                if (isAdded()) {
                    cargarListaPokemon();
                }
            });
    }

    private void cargarListaPokemon() {
        if (!isAdded()) return;

        api.getPokemonList(0, 150).enqueue(new Callback<PokemonResponse>() {
            @Override
            public void onResponse(Call<PokemonResponse> call, Response<PokemonResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<Pokemon> pokemonList = response.body().getResults();
                    for (Pokemon pokemon : pokemonList) {
                        // Extraer el ID del Pokémon de la URL
                        String url = pokemon.getUrl();
                        if (url != null && !url.isEmpty()) {
                            String[] urlParts = url.split("/");
                            try {
                                int id = Integer.parseInt(urlParts[urlParts.length - 1]);
                                pokemon.setId(id);
                            } catch (NumberFormatException e) {
                                Log.e("PokedexFragment", "Error parsing pokemon id", e);
                            }
                        }
                        pokemon.setCapturado(pokemonCapturados.contains(pokemon.getName()));
                    }
                    if (adapter != null) {
                        adapter.setPokemonList(pokemonList);
                    }
                }
            }

            @Override
            public void onFailure(Call<PokemonResponse> call, Throwable t) {
                if (isAdded()) {
                    mostrarToast(getString(R.string.error_loading_pokedex));
                }
            }
        });
    }

    private void mostrarToast(String mensaje) {
        if (getContext() != null) {
            Toast toast = Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    private void mostrarDetallesPokemon(Pokemon pokemon) {
        if (!pokemon.isCapturado()) {
            api.getPokemonDetails(pokemon.getName()).enqueue(new Callback<PokemonDetails>() {
                @Override
                public void onResponse(Call<PokemonDetails> call, Response<PokemonDetails> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        guardarPokemonCapturado(response.body());
                    }
                }

                @Override
                public void onFailure(Call<PokemonDetails> call, Throwable t) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(),
                            getString(R.string.error_capturing_pokemon),
                            Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            if (getContext() != null) {
                Toast.makeText(getContext(),
                    getString(R.string.pokemon_already_captured),
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void capturarPokemon(Pokemon pokemon) {
        if (pokemon.isCapturado()) {
            mostrarToast(getString(R.string.pokemon_already_captured));
            return;
        }

        api.getPokemonDetails(pokemon.getName()).enqueue(new Callback<PokemonDetails>() {
            @Override
            public void onResponse(Call<PokemonDetails> call, Response<PokemonDetails> response) {
                if (response.isSuccessful() && response.body() != null) {
                    guardarPokemonCapturado(response.body());
                }
            }

            @Override
            public void onFailure(Call<PokemonDetails> call, Throwable t) {
                mostrarToast(getString(R.string.error_capturing_pokemon));
            }
        });
    }

    private void guardarPokemonCapturado(PokemonDetails pokemonDetails) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        Map<String, Object> pokemonData = new HashMap<>();
        pokemonData.put("nombre", pokemonDetails.getName());
        pokemonData.put("fecha_captura", FieldValue.serverTimestamp());
        pokemonData.put("id", pokemonDetails.getId());
        pokemonData.put("fotoPokemon", pokemonDetails.getSprites().getFrontDefault());
        pokemonData.put("tipos", pokemonDetails.getTypes());
        pokemonData.put("peso", pokemonDetails.getWeight());
        pokemonData.put("altura", pokemonDetails.getHeight());

        db.collection("pokemon_capturados")
            .document(userId)
            .collection("pokemons")
            .document(pokemonDetails.getName())
            .set(pokemonData)
            .addOnSuccessListener(aVoid -> {
                mostrarToast(getString(R.string.pokemon_captured));
                pokemonCapturados.add(pokemonDetails.getName());
                adapter.marcarComoCapturado(pokemonDetails.getName());
            })
            .addOnFailureListener(e -> {
                mostrarToast(getString(R.string.error_saving_pokemon));
            });
    }
}
