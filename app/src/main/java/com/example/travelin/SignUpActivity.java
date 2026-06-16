package com.example.travelin;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Arrays;

public class SignUpActivity extends Activity {
    private static final String PREFS_NAME = "travelin_prefs";
    private static final String KEY_LANGUAGE = "language";
    private static final String LANG_EN = "en";
    private static final String LANG_FR = "fr";
    private static final String LANG_AR = "ar";
    private static final int RC_GOOGLE_SIGN_IN = 1001;

    private TextView languageText;
    private TextView titleText;
    private TextView subtitleText;
    private EditText fullNameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button signUpButton;
    private TextView socialLabelText;
    private TextView haveAccountText;
    private Button signInButton;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        bindViews();
        auth = getFirebaseAuthOrNull();
        setupGoogleSignIn();
        setupFacebookSignIn();
        applyLanguage(getSavedLanguage());

        languageText.setOnClickListener(v -> showLanguageMenu());
        signUpButton.setOnClickListener(v -> createAccountWithEmail());
        signInButton.setOnClickListener(v ->
                startActivity(new Intent(SignUpActivity.this, SignInActivity.class)));
        findViewById(R.id.btn_google).setOnClickListener(v -> signInWithGoogle());
        findViewById(R.id.btn_facebook).setOnClickListener(v -> signInWithFacebook());
    }

    private void bindViews() {
        languageText = findViewById(R.id.txt_language);
        titleText = findViewById(R.id.txt_sign_up_title);
        subtitleText = findViewById(R.id.txt_sign_up_subtitle);
        fullNameEditText = findViewById(R.id.edt_full_name);
        emailEditText = findViewById(R.id.edt_email);
        passwordEditText = findViewById(R.id.edt_password);
        confirmPasswordEditText = findViewById(R.id.edt_confirm_password);
        signUpButton = findViewById(R.id.btn_sign_up);
        socialLabelText = findViewById(R.id.txt_social_label);
        haveAccountText = findViewById(R.id.txt_have_account);
        signInButton = findViewById(R.id.btn_go_sign_in);
    }

    private FirebaseAuth getFirebaseAuthOrNull() {
        try {
            FirebaseApp.initializeApp(this);
            return FirebaseAuth.getInstance();
        } catch (IllegalStateException exception) {
            return null;
        }
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getWebClientId())
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, options);
    }

    private void setupFacebookSignIn() {
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                firebaseAuthWithFacebook(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Toast.makeText(SignUpActivity.this, "Connexion Facebook annulee", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(SignUpActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void createAccountWithEmail() {
        if (!isFirebaseReady()) {
            return;
        }

        String fullName = fullNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        if (!validateSignUp(fullName, email, password, confirmPassword)) {
            return;
        }

        setLoading(true);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        if (auth.getCurrentUser() != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName)
                                    .build();
                            auth.getCurrentUser().updateProfile(profileUpdates);
                        }
                        Toast.makeText(this, "Compte cree avec succes", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
                        finish();
                    } else {
                        showAuthError(task.getException());
                    }
                });
    }

    private void signInWithGoogle() {
        if (!isFirebaseReady() || !isGoogleConfigured()) {
            return;
        }
        startActivityForResult(googleSignInClient.getSignInIntent(), RC_GOOGLE_SIGN_IN);
    }

    private void signInWithFacebook() {
        if (!isFirebaseReady() || !isFacebookConfigured()) {
            return;
        }
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException exception) {
                Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Connexion Google reussie", Toast.LENGTH_SHORT).show();
                    } else {
                        showAuthError(task.getException());
                    }
                });
    }

    private void firebaseAuthWithFacebook(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Connexion Facebook reussie", Toast.LENGTH_SHORT).show();
                    } else {
                        showAuthError(task.getException());
                    }
                });
    }

    private boolean validateSignUp(String fullName, String email, String password, String confirmPassword) {
        if (fullName.length() < 2) {
            fullNameEditText.setError("Entrez votre nom complet");
            fullNameEditText.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Entrez un e-mail valide");
            emailEditText.requestFocus();
            return false;
        }
        if (password.length() < 6) {
            passwordEditText.setError("Le mot de passe doit contenir au moins 6 caracteres");
            passwordEditText.requestFocus();
            return false;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Les mots de passe ne correspondent pas");
            confirmPasswordEditText.requestFocus();
            return false;
        }
        return true;
    }

    private boolean isFirebaseReady() {
        if (auth != null) {
            return true;
        }
        Toast.makeText(this, "Ajoutez d'abord app/google-services.json depuis Firebase Console", Toast.LENGTH_LONG).show();
        return false;
    }

    private boolean isGoogleConfigured() {
        if (!getWebClientId().startsWith("YOUR_")) {
            return true;
        }
        Toast.makeText(this, "Configurez default_web_client_id depuis Firebase", Toast.LENGTH_LONG).show();
        return false;
    }

    private String getWebClientId() {
        String configuredId = getString(R.string.travelin_web_client_id);
        if (!configuredId.startsWith("YOUR_")) {
            return configuredId;
        }

        int generatedId = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
        if (generatedId != 0) {
            return getString(generatedId);
        }
        return configuredId;
    }

    private boolean isFacebookConfigured() {
        if (!getString(R.string.facebook_app_id).startsWith("YOUR_")
                && !getString(R.string.facebook_client_token).startsWith("YOUR_")) {
            return true;
        }
        Toast.makeText(this, "Configurez l'App ID Facebook et le Client Token", Toast.LENGTH_LONG).show();
        return false;
    }

    private void showAuthError(Exception exception) {
        String message = exception == null ? "Authentification echouee" : exception.getMessage();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void setLoading(boolean isLoading) {
        signUpButton.setEnabled(!isLoading);
        signUpButton.setText(isLoading ? "Veuillez patienter..." : "Creer un compte");
    }

    private void showLanguageMenu() {
        PopupMenu menu = new PopupMenu(this, languageText);
        menu.getMenu().add("English");
        menu.getMenu().add("Français");
        menu.getMenu().add("العربية");
        menu.setOnMenuItemClickListener(item -> {
            String selected = item.getTitle().toString();
            if ("Français".equals(selected)) {
                saveLanguage(LANG_FR);
                applyLanguage(LANG_FR);
            } else if ("العربية".equals(selected)) {
                saveLanguage(LANG_AR);
                applyLanguage(LANG_AR);
            } else {
                saveLanguage(LANG_EN);
                applyLanguage(LANG_EN);
            }
            return true;
        });
        menu.show();
    }

    private void applyLanguage(String language) {
        boolean isArabic = LANG_AR.equals(language);
        getWindow().getDecorView().setLayoutDirection(isArabic ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);

        if (LANG_FR.equals(language)) {
            languageText.setText("Français v");
            titleText.setText("Créer votre compte Travelin");
            subtitleText.setText("Commencez votre voyage avec nous");
            fullNameEditText.setHint("Nom complet");
            emailEditText.setHint("E-mail ou numéro de téléphone");
            passwordEditText.setHint("Mot de passe");
            confirmPasswordEditText.setHint("Confirmer le mot de passe");
            signUpButton.setText("Créer un compte");
            socialLabelText.setText("ou s'inscrire avec");
            haveAccountText.setText("Vous avez déjà un compte ?");
            signInButton.setText("Se connecter");
        } else if (isArabic) {
            languageText.setText("العربية v");
            titleText.setText("أنشئ حسابك في Travelin");
            subtitleText.setText("ابدأ رحلتك معنا");
            fullNameEditText.setHint("الاسم الكامل");
            emailEditText.setHint("البريد الإلكتروني أو رقم الهاتف");
            passwordEditText.setHint("كلمة المرور");
            confirmPasswordEditText.setHint("تأكيد كلمة المرور");
            signUpButton.setText("إنشاء حساب");
            socialLabelText.setText("أو سجّل باستخدام");
            haveAccountText.setText("لديك حساب بالفعل؟");
            signInButton.setText("تسجيل الدخول");
        } else {
            languageText.setText("Francais v");
            titleText.setText("Creer votre compte Travelin");
            subtitleText.setText("Commencez votre voyage avec nous");
            fullNameEditText.setHint("Nom complet");
            emailEditText.setHint("E-mail ou numero de telephone");
            passwordEditText.setHint("Mot de passe");
            confirmPasswordEditText.setHint("Confirmer le mot de passe");
            signUpButton.setText("Creer un compte");
            socialLabelText.setText("ou s'inscrire avec");
            haveAccountText.setText("Vous avez deja un compte ?");
            signInButton.setText("Se connecter");
        }
    }

    private String getSavedLanguage() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return preferences.getString(KEY_LANGUAGE, LANG_FR);
    }

    private void saveLanguage(String language) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_LANGUAGE, language)
                .apply();
    }
}
