package dam.pmdm.dampmdmpokedexapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class Login extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> signInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        try {
            // Inicializar Firebase antes de cualquier otra operación
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
            }
            
            mAuth = FirebaseAuth.getInstance();
            
            // Configuración de Google Sign In
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
                
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
            
            SignInButton signInButton = findViewById(R.id.google_sign_in_button);
            signInButton.setOnClickListener(v -> signIn());
            
            signInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleSignInResult);
            
        } catch (Exception e) {
            Log.e(TAG, "Error en onCreate: ", e);
            Toast.makeText(this, "Error al inicializar la aplicación", Toast.LENGTH_SHORT).show();
            finish(); // Cerrar la actividad si hay un error crítico
        }

        EditText emailField = findViewById(R.id.email);
        EditText passwordField = findViewById(R.id.password);
        Button loginButton = findViewById(R.id.login_button);
        Button registerButton = findViewById(R.id.register_button);

        loginButton.setOnClickListener(v -> {
            String email = emailField.getText().toString();
            String password = passwordField.getText().toString();
            if (validateInputs(email, password)) {
                login(email, password);
            }
        });

        registerButton.setOnClickListener(v -> {
            String email = emailField.getText().toString();
            String password = passwordField.getText().toString();
            if (validateInputs(email, password)) {
                register(email, password);
            }
        });
    }

    private boolean validateInputs(String email, String password) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Los campos no pueden estar vacíos", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void login(String email, String password) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Los campos no pueden estar vacíos", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        navigateToMain();
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void register(String email, String password) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Los campos no pueden estar vacíos", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signIn() {
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            signInLauncher.launch(signInIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error en signIn: ", e);
            Toast.makeText(this, "Error al iniciar sesión con Google", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSignInResult(ActivityResult result) {
        try {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            }
        } catch (ApiException e) {
            Log.e(TAG, "Error en handleSignInResult: ", e);
            Toast.makeText(this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        Log.d(TAG, "Iniciando autenticación con Firebase");
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(this, authResult -> {
                    Log.d(TAG, "Autenticación con Firebase exitosa");
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        Log.d(TAG, "Usuario autenticado: " + user.getEmail());
                        createUserDocument(user);
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Error en la autenticación con Firebase", e);
                    Toast.makeText(this, "Error en la autenticación: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
    }

    private void createUserDocument(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("displayName", user.getDisplayName());
        userData.put("lastLogin", FieldValue.serverTimestamp());

        db.collection("users")
            .document(user.getUid())
            .set(userData, SetOptions.merge())
            .addOnSuccessListener(aVoid -> navigateToMain())
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error al crear documento de usuario", e);
                navigateToMain();
            });
    }

    private void navigateToMain() {
        Log.d(TAG, "Navegando a MainActivity");
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
        Log.d(TAG, "Navegación completada");
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            // Verificar si hay un usuario ya autenticado
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                Log.d(TAG, "Usuario detectado en onStart: " + currentUser.getEmail());
                // Navegar a MainActivity solo si no estamos ya en proceso de cierre de sesión
                if (!isFinishing()) {
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en onStart: ", e);
        }
    }

    private void signOut() {
        // Cerrar sesión de Firebase
        FirebaseAuth.getInstance().signOut();
        
        // Cerrar sesión de Google
        GoogleSignIn.getClient(this, new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .setAccountName(null)
                .build())
                .signOut()
                .addOnCompleteListener(task -> {
                    // Forzar que muestre el selector de cuentas la próxima vez
                    mGoogleSignInClient = GoogleSignIn.getClient(this, 
                        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.default_web_client_id))
                            .requestEmail()
                            .setAccountName(null)
                            .build());
                    
                    Log.d(TAG, "Usuario desconectado completamente");
                });
    }
}
