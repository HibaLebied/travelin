package com.example.travelin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class HelpSupportActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_support);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        bindAction(findViewById(R.id.row_faq), R.drawable.ic_settings_help, "FAQ", "Consultez les questions fréquemment posées");
        bindAction(findViewById(R.id.row_contact), R.drawable.ic_settings_contact, "Nous contacter", "Contactez notre équipe de support");
        bindAction(findViewById(R.id.row_help_center), R.drawable.ic_settings_book, "Centre d’aide", "Guides, conseils et ressources utiles");
        bindAction(findViewById(R.id.row_privacy_policy), R.drawable.ic_settings_shield, "Politique de confidentialité", "Découvrez comment nous protégeons vos données");
        bindAction(findViewById(R.id.row_terms), R.drawable.ic_settings_document, "Conditions d’utilisation", "Consultez les conditions d’utilisation de Travelin");
        findViewById(R.id.row_faq).setOnClickListener(v -> toast("FAQ"));
        findViewById(R.id.row_contact).setOnClickListener(v -> sendSupportEmail());
        findViewById(R.id.row_help_center).setOnClickListener(v -> toast("Centre d’aide"));
        findViewById(R.id.row_privacy_policy).setOnClickListener(v -> toast("Politique de confidentialité"));
        findViewById(R.id.row_terms).setOnClickListener(v -> toast("Conditions d’utilisation"));
        findViewById(R.id.btn_contact_support).setOnClickListener(v -> sendSupportEmail());
    }

    private void sendSupportEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:support@travelin.app"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Support Travelin");
        startActivity(Intent.createChooser(intent, "Contacter le support"));
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void bindAction(View root, int iconRes, String title, String subtitle) {
        ((ImageView) root.findViewById(R.id.img_action_icon)).setImageResource(iconRes);
        ((TextView) root.findViewById(R.id.txt_action_title)).setText(title);
        ((TextView) root.findViewById(R.id.txt_action_subtitle)).setText(subtitle);
    }
}
