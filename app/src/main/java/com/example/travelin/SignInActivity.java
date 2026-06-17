package com.example.travelin;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
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

import java.util.Arrays;

public class SignInActivity extends Activity {
    private static final String PREFS_NAME = "travelin_prefs";
    private static final String KEY_LANGUAGE = "language";
    private static final String LANG_EN = "en";
    private static final String LANG_FR = "fr";
    private static final int RC_GOOGLE_SIGN_IN = 1001;

    private TextView languageText;
    private TextView titleText;
    private TextView subtitleText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private TextView forgotPasswordText;
    private Button signInButton;
    private TextView socialLabelText;
    private TextView noAccountText;
    private Button signUpButton;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private CallbackManager callbackManager;


    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        bindViews();
        auth = getFirebaseAuthOrNull();
        setupGoogleSignIn();
        setupFacebookSignIn();
        applyLanguage(getSavedLanguage());

        languageText.setOnClickListener(v -> showLanguageMenu());
        signInButton.setOnClickListener(v -> signInWithEmail());
        signUpButton.setOnClickListener(v ->
                startActivity(new Intent(SignInActivity.this, SignUpActivity.class)));
        forgotPasswordText.setOnClickListener(v -> sendPasswordResetEmail());
        findViewById(R.id.btn_google).setOnClickListener(v -> signInWithGoogle());
        findViewById(R.id.btn_facebook).setOnClickListener(v -> signInWithFacebook());
    }

    private void bindViews() {
        languageText = findViewById(R.id.txt_language);
        titleText = findViewById(R.id.txt_sign_in_title);
        subtitleText = findViewById(R.id.txt_sign_in_subtitle);
        emailEditText = findViewById(R.id.edt_email);
        passwordEditText = findViewById(R.id.edt_password);
        forgotPasswordText = findViewById(R.id.txt_forgot_password);
        signInButton = findViewById(R.id.btn_sign_in);
        socialLabelText = findViewById(R.id.txt_social_label);
        noAccountText = findViewById(R.id.txt_no_account);
        signUpButton = findViewById(R.id.btn_go_sign_up);
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
        String webClientId = getWebClientId();
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(webClientId)
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
                Toast.makeText(SignInActivity.this, "Connexion Facebook annulee", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(SignInActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void signInWithEmail() {
        if (!isFirebaseReady()) {
            return;
        }

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        if (!validateEmailAndPassword(email, password)) {
            return;
        }

        setLoading(true);
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Connexion reussie", Toast.LENGTH_SHORT).show();
                        openHome();
                    } else {
                        showAuthError(task.getException());
                    }
                });
    }

    private void sendPasswordResetEmail() {
        if (!isFirebaseReady()) {
            return;
        }

        String email = emailEditText.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Entrez d'abord un e-mail valide");
            emailEditText.requestFocus();
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "E-mail de reinitialisation envoye", Toast.LENGTH_SHORT).show();
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
                        openHome();
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
                        openHome();
                    } else {
                        showAuthError(task.getException());
                    }
                });
    }

    private void openHome() {
        Intent intent = new Intent(SignInActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean validateEmailAndPassword(String email, String password) {
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
        signInButton.setEnabled(!isLoading);
        signInButton.setText(isLoading ? getString(R.string.wait) : getString(R.string.sign_in));
    }

    private void showLanguageMenu() {
        PopupMenu menu = new PopupMenu(this, languageText);
        menu.getMenu().add(getString(R.string.english));
        menu.getMenu().add(getString(R.string.french));
        menu.setOnMenuItemClickListener(item -> {
            String selected = item.getTitle().toString();
            if (getString(R.string.french).equals(selected)) {
                saveLanguage(LANG_FR);
            } else {
                saveLanguage(LANG_EN);
            }
            recreate();
            return true;
        });
        menu.show();
    }

    private void applyLanguage(String language) {
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        if (LANG_FR.equals(language)) {
            languageText.setText(getString(R.string.language_french_short));
            setTitleWithPrimaryWord(getString(R.string.sign_in_title), "Travelin");
            subtitleText.setText(getString(R.string.sign_in_subtitle));
            emailEditText.setHint(getString(R.string.email_or_phone));
            passwordEditText.setHint(getString(R.string.password));
            forgotPasswordText.setText(getString(R.string.forgot_password));
            signInButton.setText(getString(R.string.sign_in));
            socialLabelText.setText(getString(R.string.or_sign_in_with));
            noAccountText.setText(getString(R.string.no_account));
            signUpButton.setText(getString(R.string.create_account));
        } else {
            languageText.setText(getString(R.string.language_english_short));
            setTitleWithPrimaryWord("Sign in with Travelin", "Travelin");
            subtitleText.setText("Discover the world with every\nsign-in");
            emailEditText.setHint("Email or phone number");
            passwordEditText.setHint("Password");
            forgotPasswordText.setText("Forgot password?");
            signInButton.setText("Sign in");
            socialLabelText.setText("or sign in with");
            noAccountText.setText("Don’t have an account?");
            signUpButton.setText("Create account");
        }
    }

    private void setTitleWithPrimaryWord(String text, String word) {
        SpannableString styledText = new SpannableString(text);
        int start = text.indexOf(word);
        if (start >= 0) {
            styledText.setSpan(new ForegroundColorSpan(Color.parseColor("#007A8C")),
                    start, start + word.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        titleText.setText(styledText);
    }

    private String getSavedLanguage() {
        return LocaleHelper.getLanguage(this);
    }

    private void saveLanguage(String language) {
        LocaleHelper.setLanguage(this, language);
    }
}
