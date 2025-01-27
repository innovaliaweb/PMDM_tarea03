package dam.pmdm.dampmdmpokedexapp.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.Button;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.app.AlertDialog;
import android.util.Log;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import dam.pmdm.dampmdmpokedexapp.MainActivity;
import dam.pmdm.dampmdmpokedexapp.R;

import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import dam.pmdm.dampmdmpokedexapp.Login;
import android.widget.ArrayAdapter;

import java.util.Locale;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsFragment extends Fragment {
    private SharedPreferences sharedPreferences;
    private Switch switchDelete;
    private Spinner spinnerIdioma;
    private Button btnLogout;
    private Button btnAbout;
    private FirebaseAuth mAuth;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        try {
            sharedPreferences = requireActivity().getSharedPreferences("PokedexPrefs", Context.MODE_PRIVATE);

            // Inicializar vistas
            switchDelete = view.findViewById(R.id.switch_delete);
            spinnerIdioma = view.findViewById(R.id.spinner_idioma);
            btnLogout = view.findViewById(R.id.button_logout);
            btnAbout = view.findViewById(R.id.button_about);

            // Configurar switch
            switchDelete.setChecked(sharedPreferences.getBoolean("allow_delete", false));
            switchDelete.setOnCheckedChangeListener((buttonView, isChecked) -> {
                sharedPreferences.edit().putBoolean("allow_delete", isChecked).apply();
                
                // Recrear la actividad para aplicar los cambios
                Intent intent = requireActivity().getIntent();
                requireActivity().finish();
                startActivity(intent);
            });

            // Configurar spinner
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    requireContext(),
                    R.array.idiomas,
                    android.R.layout.simple_spinner_item
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerIdioma.setAdapter(adapter);

            // Cargar idioma guardado
            int savedLanguage = sharedPreferences.getInt("language", 0);
            spinnerIdioma.setSelection(savedLanguage);

            // Cambiar idioma cuando se selecciona una opción
            spinnerIdioma.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position != savedLanguage) {
                        sharedPreferences.edit().putInt("language", position).apply();
                        cambiarIdioma(position);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            // Configurar botones
            btnAbout.setOnClickListener(v -> showAboutDialog());
            btnLogout.setOnClickListener(v -> cerrarSesion());

        } catch (Exception e) {
            Log.e("SettingsFragment", "Error al inflar la vista: ", e);
            Toast.makeText(requireContext(), "Error al cargar configuración.", Toast.LENGTH_SHORT).show();
        }
        return view;
    }

    private void showAboutDialog() {
        if (getActivity() != null && !getActivity().isFinishing()) {
            PackageManager pm = requireContext().getPackageManager();
            String version = "1.0.0";
            try {
                PackageInfo pInfo = pm.getPackageInfo(requireContext().getPackageName(), 0);
                version = pInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("SettingsFragment", "Error al obtener versión", e);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(R.string.about_title)
                   .setMessage(getString(R.string.about_message) + "\nVersión " + version)
                   .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                   .create()
                   .show();
        }
    }
    private void cambiarIdioma(int position) {
        String languageCode = (position == 0) ? "es" : "en";
        Locale newLocale = new Locale(languageCode);
        Locale.setDefault(newLocale);

        Resources resources = requireActivity().getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        config.setLocale(newLocale);
        
        requireActivity().createConfigurationContext(config);
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        // Guardar el idioma seleccionado
        sharedPreferences.edit()
            .putString("selected_language", languageCode)
            .putInt("language", position)
            .apply();

        // Recrear la actividad correctamente
        Intent intent = requireActivity().getIntent();
        requireActivity().finish();
        startActivity(intent);
    }

    private void cerrarSesion() {
        try {
            if (!isAdded() || getContext() == null) return;

            // Limpiar SharedPreferences relacionadas con Pokémon
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("allow_delete");
            editor.apply();

            // Primero cerrar sesión de Firebase
            FirebaseAuth.getInstance().signOut();

            // Luego cerrar sesión de Google
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();

            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(requireContext(), gso);
            googleSignInClient.signOut()
                    .addOnCompleteListener(task -> {
                        // Limpiar la caché de Firestore
                        FirebaseFirestore.getInstance().clearPersistence()
                            .addOnCompleteListener(clearTask -> {
                                if (isAdded() && getActivity() != null) {
                                    Intent intent = new Intent(getActivity(), Login.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    getActivity().finish();
                                }
                            });
                    });
        } catch (Exception e) {
            Log.e("SettingsFragment", "Error al cerrar sesión: ", e);
        }
    }

}
