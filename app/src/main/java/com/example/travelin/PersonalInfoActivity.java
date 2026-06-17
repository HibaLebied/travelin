package com.example.travelin;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

public class PersonalInfoActivity extends AppCompatActivity {
    private static final int REQUEST_GALLERY = 2001;
    private static final int REQUEST_CAMERA = 2002;

    private ProfilePreferences profilePreferences;
    private ImageView avatarImage;
    private TextView initialsText;
    private EditText fullNameInput;
    private EditText emailInput;
    private EditText phoneInput;
    private EditText cityInput;
    private EditText birthDateInput;
    private String photoUri;
    private String originalEmail;


    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);
        profilePreferences = new ProfilePreferences(this);

        avatarImage = findViewById(R.id.img_personal_avatar);
        initialsText = findViewById(R.id.txt_personal_initials);
        fullNameInput = findViewById(R.id.input_full_name);
        emailInput = findViewById(R.id.input_email);
        phoneInput = findViewById(R.id.input_phone);
        cityInput = findViewById(R.id.input_city);
        birthDateInput = findViewById(R.id.input_birth_date);

        bindValues();
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_change_photo).setOnClickListener(v -> showPhotoPicker());
        birthDateInput.setOnClickListener(v -> showDatePicker());
        findViewById(R.id.btn_save_profile).setOnClickListener(v -> saveProfile());
    }

    private void bindValues() {
        fullNameInput.setText(profilePreferences.getFullName());
        originalEmail = profilePreferences.getEmail();
        emailInput.setText(originalEmail);
        phoneInput.setText(profilePreferences.getPhone());
        cityInput.setText(profilePreferences.getCity());
        birthDateInput.setText(profilePreferences.getBirthDate());
        photoUri = profilePreferences.getPhotoUri();
        if (!TextUtils.isEmpty(photoUri)) {
            setAvatarSafely(Uri.parse(photoUri));
        } else {
            showInitialsAvatar();
        }
    }


    private void showPhotoPicker() {
        boolean hasPhoto = !TextUtils.isEmpty(photoUri);
        CharSequence[] items = hasPhoto
                ? new CharSequence[]{"Galerie", "Camera", "Supprimer la photo"}
                : new CharSequence[]{"Galerie", "Camera"};
        new AlertDialog.Builder(this)
                .setTitle("Photo de profil")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(intent, REQUEST_GALLERY);
                    } else if (which == 1) {
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(intent, REQUEST_CAMERA);
                    } else {
                        removeProfilePhoto();
                    }
                })
                .show();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String monthName = new DateFormatSymbols(Locale.FRANCE).getMonths()[month];
            birthDateInput.setText(dayOfMonth + " " + monthName + " " + year);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void saveProfile() {
        String newEmail = emailInput.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            emailInput.setError("Adresse e-mail invalide");
            emailInput.requestFocus();
            return;
        }

        if (isEmailChanged(newEmail)) {
            requestPasswordAndUpdateEmail(newEmail);
            return;
        }

        saveProfileLocally(newEmail, true);
    }

    private boolean isEmailChanged(String newEmail) {
        return !TextUtils.equals(originalEmail == null ? "" : originalEmail.trim(), newEmail);
    }

    private void requestPasswordAndUpdateEmail(String newEmail) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || TextUtils.isEmpty(user.getEmail())) {
            Toast.makeText(this, "Aucun utilisateur connecté", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasPasswordProvider(user)) {
            showUnsupportedProviderDialog();
            return;
        }

        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Mot de passe actuel");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(this)
                .setTitle("Confirmer votre identité")
                .setMessage("Entrez votre mot de passe pour modifier l'adresse e-mail du compte.")
                .setView(passwordInput)
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Confirmer", (dialog, which) -> {
                    String password = passwordInput.getText().toString();
                    if (password.length() < 6) {
                        Toast.makeText(this, "Mot de passe invalide", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    reauthenticateAndSendVerification(user, password, newEmail);
                })
                .show();
    }

    private boolean hasPasswordProvider(FirebaseUser user) {
        for (UserInfo info : user.getProviderData()) {
            if (EmailAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                return true;
            }
        }
        return false;
    }

    private void showUnsupportedProviderDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Compte Google/Facebook")
                .setMessage("Ce compte n'utilise pas un mot de passe Firebase. Change l'adresse e-mail depuis le fournisseur utilisé pour la connexion, ou connecte ce compte au provider Email/Password dans Firebase.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void reauthenticateAndSendVerification(FirebaseUser user, String password, String newEmail) {
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
        user.reauthenticate(credential)
                .addOnSuccessListener(unused -> sendEmailChangeVerification(user, newEmail))
                .addOnFailureListener(e -> {
                    e.printStackTrace(); // Vérifiez ici dans le Logcat
                    Toast.makeText(this, "Erreur auth: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void sendEmailChangeVerification(FirebaseUser user, String newEmail) {
        FirebaseAuth.getInstance().setLanguageCode("fr");
        Log.d("DEBUG_EMAIL", "Email envoyé à : " + newEmail);
        user.verifyBeforeUpdateEmail(newEmail, createEmailActionSettings())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this,
                            "Email de vérification envoyé",
                            Toast.LENGTH_LONG).show();

                    saveProfileData(newEmail);
                    profilePreferences.putString("pending_email", newEmail);
                    showVerificationSentDialog(user, newEmail);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();

                    Toast.makeText(
                            this,
                            "Erreur : " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private ActionCodeSettings createEmailActionSettings() {
        return ActionCodeSettings.newBuilder()
                .setUrl("https://travelin-5ff4a.firebaseapp.com")
                .setHandleCodeInApp(true)
                .setAndroidPackageName(
                        getPackageName(),
                        true,
                        null
                )
                .build();
    }
    private void showVerificationSentDialog(FirebaseUser user, String newEmail) {
        new AlertDialog.Builder(this)
                .setTitle("Vérification demandée")
                .setMessage("Un lien de vérification a été demandé pour :\n\n" + newEmail
                        + "\n\nSi tu ne le vois pas, vérifie Spam/Promotions et attends 1 à 2 minutes. Tu peux aussi renvoyer le lien.")
                .setNegativeButton("Renvoyer", (dialog, which) -> {
                    Toast.makeText(this, "Renvoi du lien...", Toast.LENGTH_SHORT).show();
                    sendEmailChangeVerification(user, newEmail);
                })
                .setPositiveButton("OK", (dialog, which) -> finish())
                .show();
    }

    private void showFirebaseEmailError(Exception exception) {
        String message = exception == null ? "" : exception.getMessage();
        String lowerMessage = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (lowerMessage.contains("operation is not allowed")) {
            new AlertDialog.Builder(this)
                    .setTitle("Modification refusée par Firebase")
                    .setMessage("Firebase refuse cette opération pour ce compte.\n\nVérifie que l'utilisateur s'est bien inscrit avec Email/Password, pas Google ou Facebook. Si le provider est déjà activé, reconnecte-toi puis réessaie.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        if (lowerMessage.contains("domain") || lowerMessage.contains("continue url")) {
            new AlertDialog.Builder(this)
                    .setTitle("Domaine Firebase non autorisé")
                    .setMessage("Firebase refuse le lien de vérification.\n\nAjoute ce domaine dans Firebase Console > Authentication > Settings > Authorized domains :\n\ntravelin.app\n\nOu remplace l'URL utilisée dans l'app par un domaine déjà autorisé.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        if (lowerMessage.contains("provider")) {
            new AlertDialog.Builder(this)
                    .setTitle("Provider non compatible")
                    .setMessage("Ce compte utilise probablement Google ou Facebook. L'e-mail doit être changé depuis ce provider, ou le compte doit être lié au provider Email/Password.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        Toast.makeText(
                this,
                TextUtils.isEmpty(message) ? "Impossible de modifier l'e-mail" : message,
                Toast.LENGTH_LONG
        ).show();
    }

    private void saveProfileLocally(String email, boolean showToast) {
        saveProfileData(email);
        if (showToast) {
            Toast.makeText(this, "Modifications enregistrées", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private void saveProfileData(String email) {
        profilePreferences.saveProfile(
                fullNameInput.getText().toString().trim(),
                email,
                phoneInput.getText().toString().trim(),
                cityInput.getText().toString().trim(),
                birthDateInput.getText().toString().trim(),
                photoUri == null ? "" : photoUri
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        if (requestCode == REQUEST_GALLERY && data.getData() != null) {
            saveGalleryImage(data.getData());
        } else if (requestCode == REQUEST_CAMERA && data.getExtras() != null) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            if (bitmap != null) {
                saveCameraThumbnail(bitmap);
            }
        }
    }

    private void saveCameraThumbnail(Bitmap bitmap) {
        File file = new File(getFilesDir(), "profile_photo.jpg");
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            photoUri = Uri.fromFile(file).toString();
            setAvatarSafely(Uri.parse(photoUri));
        } catch (IOException exception) {
            Toast.makeText(this, "Impossible d'enregistrer la photo", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveGalleryImage(Uri sourceUri) {
        File file = new File(getFilesDir(), "profile_photo_gallery.jpg");
        try (InputStream inputStream = getContentResolver().openInputStream(sourceUri);
             OutputStream outputStream = new FileOutputStream(file)) {
            if (inputStream == null) {
                Toast.makeText(this, "Impossible de lire la photo", Toast.LENGTH_SHORT).show();
                return;
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            photoUri = Uri.fromFile(file).toString();
            setAvatarSafely(Uri.parse(photoUri));
        } catch (IOException | SecurityException exception) {
            Toast.makeText(this, "Impossible d'enregistrer la photo", Toast.LENGTH_SHORT).show();
        }
    }

    private void setAvatarSafely(Uri uri) {
        try {
            avatarImage.setPadding(0, 0, 0, 0);
            Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(avatarImage);
            avatarImage.setVisibility(ImageView.VISIBLE);
            initialsText.setVisibility(TextView.GONE);
        } catch (SecurityException exception) {
            photoUri = "";
            showInitialsAvatar();
            Toast.makeText(this, "Photo inaccessible, choisissez-la à nouveau", Toast.LENGTH_SHORT).show();
        }
    }
    private void showInitialsAvatar() {
        avatarImage.setPadding(28, 28, 28, 28);
        avatarImage.setVisibility(ImageView.GONE);
        initialsText.setText(getInitials(fullNameInput.getText().toString()));
        initialsText.setVisibility(TextView.VISIBLE);
    }

    private void removeProfilePhoto() {
        photoUri = "";
        profilePreferences.putString("photo_uri", "");
        showInitialsAvatar();
    }

    private String getInitials(String name) {
        if (TextUtils.isEmpty(name)) {
            String email = emailInput == null ? "" : emailInput.getText().toString();
            if (!TextUtils.isEmpty(email) && email.contains("@")) {
                name = email.substring(0, email.indexOf('@'));
            }
        }
        if (TextUtils.isEmpty(name)) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1))
                .toUpperCase(Locale.ROOT);
    }
}
