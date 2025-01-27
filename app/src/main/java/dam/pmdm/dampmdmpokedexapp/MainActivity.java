package dam.pmdm.dampmdmpokedexapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import dam.pmdm.dampmdmpokedexapp.ui.CapturadosFragment;
import dam.pmdm.dampmdmpokedexapp.ui.PokedexFragment;
import dam.pmdm.dampmdmpokedexapp.ui.SettingsFragment;

import java.util.Locale;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ViewPagerAdapter adapter;
    private static boolean isFirstLaunch = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Cargar el idioma guardado
        SharedPreferences sharedPreferences = getSharedPreferences("PokedexPrefs", Context.MODE_PRIVATE);
        String savedLanguage = sharedPreferences.getString("selected_language", "es");
        
        // Aplicar el idioma guardado
        Locale locale = new Locale(savedLanguage);
        Locale.setDefault(locale);
        
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        
        createConfigurationContext(config);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);
        //viewPager.setOffscreenPageLimit(3);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.tab_pokedex);
                    break;
                case 1:
                    tab.setText(R.string.tab_capturados);
                    break;
                case 2:
                    tab.setText(R.string.tab_settings);
                    break;
            }
        }).attach();
        // Restaurar la pestaña activa si existe
        int currentTab = getIntent().getIntExtra("current_tab", 0);
        viewPager.setCurrentItem(currentTab, false);
        //        // Restaurar el estado del ViewPager
        //        if (savedInstanceState != null) {
        //            int currentTab = savedInstanceState.getInt("current_tab", 0);
        //            viewPager.setCurrentItem(currentTab, false);
        //        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (viewPager != null) {
            outState.putInt("current_tab", viewPager.getCurrentItem());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy llamado");
    }
    public int getCurrentTabIndex() {
        return viewPager != null ? viewPager.getCurrentItem() : 0;
    }
    private class ViewPagerAdapter extends FragmentStateAdapter {
        public ViewPagerAdapter(@NonNull FragmentActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new PokedexFragment();
                case 1:
                    return new CapturadosFragment();
                case 2:
                    return new SettingsFragment();
                default:
                    return new PokedexFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            viewPager.setCurrentItem(2);
            return true;
        } else if (item.getItemId() == R.id.action_logout) {
            // Limpiar SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("PokedexPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("allow_delete");
            editor.apply();

            // Cerrar sesión de Firebase
            FirebaseAuth.getInstance().signOut();

            // Limpiar la caché de Firestore
            FirebaseFirestore.getInstance().clearPersistence()
                .addOnCompleteListener(task -> {
                    Intent intent = new Intent(this, Login.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}