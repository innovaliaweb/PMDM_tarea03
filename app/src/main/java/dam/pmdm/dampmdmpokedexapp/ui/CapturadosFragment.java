package dam.pmdm.dampmdmpokedexapp.ui;

import android.content.SharedPreferences;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.util.Log;
import android.app.AlertDialog;
import android.widget.ImageView;
import android.widget.TextView;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import dam.pmdm.dampmdmpokedexapp.R;
import dam.pmdm.dampmdmpokedexapp.adapter.PokemonAdapter;
import dam.pmdm.dampmdmpokedexapp.modelos.Pokemon;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.squareup.picasso.Picasso;

public class CapturadosFragment extends Fragment {
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private PokemonAdapter adapter;
    private List<Pokemon> listaPokemon;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private ItemTouchHelper currentItemTouchHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        
        // Inicializar el listener de preferencias
        preferenceChangeListener = (sharedPreferences, key) -> {
            if ("allow_delete".equals(key)) {
                boolean allowDelete = sharedPreferences.getBoolean(key, false);
                if (allowDelete) {
                    configurarSwipeToDelete();
                } else {
                    if (currentItemTouchHelper != null) {
                        currentItemTouchHelper.attachToRecyclerView(null);
                        currentItemTouchHelper = null;
                    }
                    // Notificar al adaptador para refrescar la vista
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        };
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_capturados, container, false);
        
        recyclerView = view.findViewById(R.id.recycler_view_capturados);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        listaPokemon = new ArrayList<>();
        adapter = new PokemonAdapter(listaPokemon, this::mostrarDetallesPokemon);
        recyclerView.setAdapter(adapter);
        
        db = FirebaseFirestore.getInstance();
        sharedPreferences = requireActivity().getSharedPreferences("PokedexPrefs", Context.MODE_PRIVATE);
        
        //adapter.setOnPokemonClickListener(this::mostrarDetallesPokemon);
        
        // Configurar el deslizamiento lateral si está habilitado
        if (sharedPreferences.getBoolean("allow_delete", false)) {
            configurarSwipeToDelete();
        }
        
        cargarPokemonCapturados();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (adapter == null) {
            listaPokemon = new ArrayList<>();
            adapter = new PokemonAdapter(listaPokemon, this::mostrarDetallesPokemon);
            recyclerView.setAdapter(adapter);
        }
        
        cargarPokemonCapturados();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpiar referencias
        recyclerView.setAdapter(null);
        adapter = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    private void configurarSwipeToDelete() {
        if (currentItemTouchHelper != null) {
            currentItemTouchHelper.attachToRecyclerView(null);
        }
        
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, 
            ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, 
                                @NonNull RecyclerView.ViewHolder viewHolder, 
                                @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Pokemon pokemon = listaPokemon.get(position);
                
                new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.delete_confirmation_title)
                    .setMessage(R.string.delete_confirmation_message)
                    .setPositiveButton(R.string.delete, (dialog, which) -> 
                        eliminarPokemon(pokemon))
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                        // Restaurar el elemento en la lista
                        adapter.notifyItemChanged(position);
                    })
                    .show();
            }
        };

        currentItemTouchHelper = new ItemTouchHelper(swipeCallback);
        currentItemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void cargarPokemonCapturados() {
        try {
            // Limpiar la lista actual
            if (listaPokemon != null) {
                listaPokemon.clear();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null || !isAdded()) return;

            db.collection("pokemon_capturados")
                .document(currentUser.getUid())
                .collection("pokemons")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("CapturadosFragment", "Error al escuchar cambios: ", error);
                        if (isAdded()) {
                            Toast.makeText(getContext(), 
                                getString(R.string.error_loading_captured_pokemon), 
                                Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    if (value != null && isAdded()) {
                        listaPokemon.clear();
                        for (DocumentSnapshot doc : value) {
                            try {
                                Pokemon pokemon = new Pokemon();
                                pokemon.setNombre(doc.getString("nombre"));
                                pokemon.setId(doc.getLong("id") != null ? doc.getLong("id").intValue() : 0);
                                pokemon.setFotoPokemon(doc.getString("fotoPokemon"));
                                
                                List<String> tipos = (List<String>) doc.get("tipos");
                                pokemon.setTypes(tipos != null ? tipos : new ArrayList<>());
                                
                                pokemon.setPeso(doc.getLong("peso") != null ? doc.getLong("peso").intValue() : 0);
                                pokemon.setAltura(doc.getLong("altura") != null ? doc.getLong("altura").intValue() : 0);
                                pokemon.setFechaCaptura(doc.getTimestamp("fecha_captura"));
                                pokemon.setCapturado(true);
                                
                                listaPokemon.add(pokemon);
                            } catch (Exception e) {
                                Log.e("CapturadosFragment", "Error al convertir documento: ", e);
                            }
                        }
                        if (adapter != null) {
                            adapter.setPokemonList(listaPokemon);
                        }
                    }
                });
        } catch (Exception e) {
            Log.e("CapturadosFragment", "Error en cargarPokemonCapturados: ", e);
        }
    }

    private void cargarPokemonCapturados(String userId) {
        if (!isAdded()) return;
        
        db.collection("pokemon_capturados")
            .document(userId)
            .collection("pokemons")
            .addSnapshotListener((value, error) -> {
                if (error != null || !isAdded()) {
                    return;
                }
                
                List<Pokemon> pokemonList = new ArrayList<>();
                if (value != null) {
                    for (DocumentSnapshot doc : value) {
                        Pokemon pokemon = doc.toObject(Pokemon.class);
                        if (pokemon != null) {
                            pokemonList.add(pokemon);
                        }
                    }
                    adapter.setPokemonList(pokemonList);
                }
            });
    }

    private void eliminarPokemon(Pokemon pokemon) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        db.collection("pokemon_capturados")
            .document(currentUser.getUid())
            .collection("pokemons")
            .document(pokemon.getNombre())
            .delete()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), 
                    getString(R.string.pokemon_deleted), 
                    Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), 
                    getString(R.string.error_deleting), 
                    Toast.LENGTH_SHORT).show();
                Log.e("CapturadosFragment", "Error al eliminar: ", e);
            });
    }

    private void mostrarDetallesPokemon(Pokemon pokemon) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_pokemon_details, null);
        
        ImageView imageView = view.findViewById(R.id.pokemon_detail_image);
        TextView nameText = view.findViewById(R.id.pokemon_detail_name);
        TextView numberText = view.findViewById(R.id.pokemon_detail_number);
        TextView typesText = view.findViewById(R.id.pokemon_detail_types);
        TextView heightText = view.findViewById(R.id.pokemon_detail_height);
        TextView weightText = view.findViewById(R.id.pokemon_detail_weight);
        
        // Capitalizar el nombre del Pokémon
        String pokemonName = pokemon.getNombre();
        if (pokemonName != null && !pokemonName.isEmpty()) {
            pokemonName = pokemonName.substring(0, 1).toUpperCase() + pokemonName.substring(1).toLowerCase();
        }
        nameText.setText(pokemonName);
        
        numberText.setText(String.format("#%03d", pokemon.getId()));
        
        // Manejo seguro de los tipos
        String types;
        if (pokemon.getTypes() != null && !pokemon.getTypes().isEmpty()) {
            List<String> capitalizedTypes = new ArrayList<>();
            for (String type : pokemon.getTypes()) {
                if (type != null && !type.isEmpty()) {
                    capitalizedTypes.add(type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase());
                }
            }
            types = TextUtils.join(", ", capitalizedTypes);
        } else {
            types = getString(R.string.unknown);
        }
        typesText.setText(types);
        
        heightText.setText(getString(R.string.pokemon_height, pokemon.getAltura()));
        weightText.setText(getString(R.string.pokemon_weight, pokemon.getPeso()));
        
        // Manejo seguro de la imagen
        if (pokemon.getFotoPokemon() != null && !pokemon.getFotoPokemon().isEmpty()) {
            Picasso.get()
                   .load(pokemon.getFotoPokemon())
                   .placeholder(R.drawable.ic_pokeball)
                   .error(R.drawable.ic_pokeball)
                   .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.ic_pokeball);
        }
        
        // Verificar la configuración actual antes de añadir el botón de eliminar
        boolean allowDelete = sharedPreferences.getBoolean("allow_delete", false);
        if (allowDelete) {
            builder.setNegativeButton(R.string.delete, (dialog, which) -> eliminarPokemon(pokemon));
        }
        
        builder.setPositiveButton(android.R.string.ok, null)
               .setView(view)
               .show();
    }
}