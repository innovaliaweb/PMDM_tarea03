package dam.pmdm.dampmdmpokedexapp.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import dam.pmdm.dampmdmpokedexapp.R;
import dam.pmdm.dampmdmpokedexapp.modelos.Pokemon;

public class PokemonAdapter extends RecyclerView.Adapter<PokemonAdapter.PokemonViewHolder> {

    // Interface para el click en un pokémon
    public interface OnPokemonClickListener {
        void onPokemonClick(Pokemon pokemon);
    }

    private List<Pokemon> pokemonList;
    private final OnPokemonClickListener listener;

    // Constructor
    public PokemonAdapter(List<Pokemon> pokemonList, OnPokemonClickListener listener) {
        this.pokemonList = pokemonList != null ? pokemonList : new ArrayList<>();
        this.listener = listener;
    }

    // Método para actualizar la lista
    public void setPokemonList(List<Pokemon> newList) {
        this.pokemonList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PokemonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pokemon, parent, false);
        return new PokemonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PokemonViewHolder holder, int position) {
        Pokemon pokemon = pokemonList.get(position);
        
        // Capitalizar el nombre del Pokémon
        String pokemonName = pokemon.getName();
        if (pokemonName != null && !pokemonName.isEmpty()) {
            pokemonName = pokemonName.substring(0, 1).toUpperCase() + pokemonName.substring(1).toLowerCase();
            holder.pokemonName.setText(pokemonName);
        } else {
            pokemonName = pokemon.getNombre();
            if (pokemonName != null && !pokemonName.isEmpty()) {
                pokemonName = pokemonName.substring(0, 1).toUpperCase() + pokemonName.substring(1).toLowerCase();
                holder.pokemonName.setText(pokemonName);
            } else {
                holder.pokemonName.setText(holder.itemView.getContext().getString(R.string.unknown));
            }
        }

        // Solo cambiar el color si el Pokémon está en la lista de la Pokédex (tiene name) y está capturado
        if (pokemon.getName() != null && pokemon.isCapturado()) {
            holder.pokemonName.setTextColor(Color.RED);
            holder.captureStatus.setVisibility(View.VISIBLE);
        } else {
            holder.pokemonName.setTextColor(Color.BLACK);
            holder.captureStatus.setVisibility(View.GONE);
        }

        // Cargar imagen del Pokémon
        String imageUrl = pokemon.getFotoPokemon();
        if (imageUrl == null || imageUrl.isEmpty()) {
            // Si no hay foto, usar la URL basada en el ID
            imageUrl = String.format("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/%d.png", 
                                   pokemon.getId());
        }
        
        Picasso.get()
               .load(imageUrl)
               .placeholder(R.drawable.ic_pokeball)
               .error(R.drawable.ic_pokeball)
               .into(holder.pokemonImage);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPokemonClick(pokemon);
            }
        });
    }

    @Override
    public int getItemCount() {
        return pokemonList.size();
    }

    public void actualizarLista(List<Pokemon> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return pokemonList.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return pokemonList.get(oldItemPosition).getNombre().equals(
                    newList.get(newItemPosition).getNombre());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Pokemon oldPokemon = pokemonList.get(oldItemPosition);
                Pokemon newPokemon = newList.get(newItemPosition);
                return oldPokemon.getNombre().equals(newPokemon.getNombre()) &&
                       oldPokemon.isCapturado() == newPokemon.isCapturado();
            }
        });
        pokemonList = new ArrayList<>(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    public void marcarComoCapturado(String nombrePokemon) {
        if (nombrePokemon == null) return;
        
        for (int i = 0; i < pokemonList.size(); i++) {
            Pokemon pokemon = pokemonList.get(i);
            String nombre = pokemon.getName();
            if (nombre == null) {
                nombre = pokemon.getNombre();
            }
            
            if (nombrePokemon.equals(nombre)) {
                pokemon.setCapturado(true);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public List<Pokemon> getPokemonList() {
        return new ArrayList<>(pokemonList);
    }

    static class PokemonViewHolder extends RecyclerView.ViewHolder {
        ImageView pokemonImage;
        TextView pokemonName;
        ImageView captureStatus;

        PokemonViewHolder(@NonNull View itemView) {
            super(itemView);
            pokemonImage = itemView.findViewById(R.id.pokemon_image);
            pokemonName = itemView.findViewById(R.id.pokemon_name);
            captureStatus = itemView.findViewById(R.id.capture_status);
        }
    }
}
