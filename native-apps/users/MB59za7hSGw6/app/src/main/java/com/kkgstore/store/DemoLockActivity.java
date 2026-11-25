package com.kkgstore.store;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class DemoLockActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_lock);

        Button btnWhatsapp = (Button) findViewById(R.id.btn_whatsapp_contact);
        btnWhatsapp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWhatsappContact();
            }
        });
    }

    private void openWhatsappContact() {
        try {
            String phoneNumber = "918171768360";
            String message = "Namaste, mujhe KKG Store app ka full working version chahiye.";
            String url = "https://api.whatsapp.com/send?phone=" + phoneNumber + "&text=" + Uri.encode(message);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:8171768360"));
            startActivity(dialIntent);
        }
    }

    @Override
    public void onBackPressed() {
        // Prevent dismissal of demo lock overlay screen
    }
}