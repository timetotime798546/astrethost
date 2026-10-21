package com.geradorvideoimagem.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int TAB_IMAGE = 0;
    private static final int TAB_VIDEO = 1;
    private static final int TAB_GALLERY = 2;

    private int currentTab = TAB_IMAGE;

    private Button btnTabImage;
    private Button btnTabVideo;
    private Button btnTabGallery;

    private LinearLayout panelControls;
    private LinearLayout layoutVideoSettings;
    private LinearLayout cardPreview;
    private LinearLayout layoutGallery;
    private LinearLayout galleryItemsContainer;
    private LinearLayout layoutMediaActions;

    private TextView lblModeTitle;
    private EditText editPrompt;
    private Spinner spinnerStyle;
    private Spinner spinnerDuration;
    private Button btnMainAction;
    private ProgressBar progressBar;
    private TextView txtProgressStatus;

    private ImageView imgPreview;
    private VideoView videoPreview;
    private TextView txtEmptyPlaceholder;
    private Button btnPlayVideo;
    private Button btnShare;

    private File currentMediaFile = null;
    private boolean isCurrentMediaVideo = false;
    private final List<MediaItem> creationHistory = new ArrayList<MediaItem>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupSpinners();
        setupTabs();
        setupActions();
    }

    private void initViews() {
        btnTabImage = (Button) findViewById(R.id.btnTabImage);
        btnTabVideo = (Button) findViewById(R.id.btnTabVideo);
        btnTabGallery = (Button) findViewById(R.id.btnTabGallery);

        panelControls = (LinearLayout) findViewById(R.id.panelControls);
        layoutVideoSettings = (LinearLayout) findViewById(R.id.layoutVideoSettings);
        cardPreview = (LinearLayout) findViewById(R.id.cardPreview);
        layoutGallery = (LinearLayout) findViewById(R.id.layoutGallery);
        galleryItemsContainer = (LinearLayout) findViewById(R.id.galleryItemsContainer);
        layoutMediaActions = (LinearLayout) findViewById(R.id.layoutMediaActions);

        lblModeTitle = (TextView) findViewById(R.id.lblModeTitle);
        editPrompt = (EditText) findViewById(R.id.editPrompt);
        spinnerStyle = (Spinner) findViewById(R.id.spinnerStyle);
        spinnerDuration = (Spinner) findViewById(R.id.spinnerDuration);
        btnMainAction = (Button) findViewById(R.id.btnMainAction);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        txtProgressStatus = (TextView) findViewById(R.id.txtProgressStatus);

        imgPreview = (ImageView) findViewById(R.id.imgPreview);
        videoPreview = (VideoView) findViewById(R.id.videoPreview);
        txtEmptyPlaceholder = (TextView) findViewById(R.id.txtEmptyPlaceholder);
        btnPlayVideo = (Button) findViewById(R.id.btnPlayVideo);
        btnShare = (Button) findViewById(R.id.btnShare);
    }

    private void setupSpinners() {
        String[] styles = new String[]{
                "Neon Cyberpunk",
                "Cosmic Nebula",
                "Natureza Fantasia",
                "Abstrato Futurista"
        };
        ArrayAdapter<String> styleAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, styles);
        spinnerStyle.setAdapter(styleAdapter);

        String[] durations = new String[]{
                "3 Segundos (Rápido)",
                "5 Segundos (Padrão)",
                "10 Segundos (HD Completo)"
        };
        ArrayAdapter<String> durationAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, durations);
        spinnerDuration.setAdapter(durationAdapter);
    }

    private void setupTabs() {
        btnTabImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(TAB_IMAGE);
            }
        });

        btnTabVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(TAB_VIDEO);
            }
        });

        btnTabGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(TAB_GALLERY);
            }
        });

        switchTab(TAB_IMAGE);
    }

    private void switchTab(int tab) {
        currentTab = tab;

        int activeColor = getResources().getColor(R.color.accent);
        int inactiveColor = getResources().getColor(R.color.text_secondary);

        btnTabImage.setTextColor(tab == TAB_IMAGE ? activeColor : inactiveColor);
        btnTabVideo.setTextColor(tab == TAB_VIDEO ? activeColor : inactiveColor);
        btnTabGallery.setTextColor(tab == TAB_GALLERY ? activeColor : inactiveColor);

        if (tab == TAB_IMAGE) {
            panelControls.setVisibility(View.VISIBLE);
            cardPreview.setVisibility(View.VISIBLE);
            layoutGallery.setVisibility(View.GONE);
            layoutVideoSettings.setVisibility(View.GONE);
            lblModeTitle.setText("Gerador de Imagens AI de Alta Definição");
            btnMainAction.setText(R.string.btn_generate_image);
            btnMainAction.setBackgroundColor(getResources().getColor(R.color.button_purple));
        } else if (tab == TAB_VIDEO) {
            panelControls.setVisibility(View.VISIBLE);
            cardPreview.setVisibility(View.VISIBLE);
            layoutGallery.setVisibility(View.GONE);
            layoutVideoSettings.setVisibility(View.VISIBLE);
            lblModeTitle.setText("Gerador de Vídeo MP4 com Hardware AI");
            btnMainAction.setText(R.string.btn_generate_video);
            btnMainAction.setBackgroundColor(getResources().getColor(R.color.button_green));
        } else {
            panelControls.setVisibility(View.GONE);
            cardPreview.setVisibility(View.GONE);
            layoutGallery.setVisibility(View.VISIBLE);
            refreshGalleryList();
        }
    }

    private void setupActions() {
        btnMainAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String prompt = editPrompt.getText().toString().trim();
                if (prompt.length() == 0) {
                    prompt = "Arte Cósmica Futurista";
                }
                String style = (String) spinnerStyle.getSelectedItem();

                if (currentTab == TAB_IMAGE) {
                    processImageGeneration(prompt, style);
                } else if (currentTab == TAB_VIDEO) {
                    int duration = 5;
                    int selected = spinnerDuration.getSelectedItemPosition();
                    if (selected == 0) duration = 3;
                    if (selected == 2) duration = 10;
                    processVideoGeneration(prompt, style, duration);
                }
            }
        });

        btnPlayVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentMediaFile != null && isCurrentMediaVideo) {
                    videoPreview.setVisibility(View.VISIBLE);
                    imgPreview.setVisibility(View.GONE);
                    txtEmptyPlaceholder.setVisibility(View.GONE);
                    videoPreview.setVideoURI(Uri.fromFile(currentMediaFile));
                    videoPreview.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mp.setLooping(true);
                            videoPreview.start();
                        }
                    });
                }
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentMediaFile != null && currentMediaFile.exists()) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType(isCurrentMediaVideo ? "video/mp4" : "image/png");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(currentMediaFile));
                    startActivity(Intent.createChooser(shareIntent, "Compartilhar Mídia Criada"));
                } else {
                    Toast.makeText(MainActivity.this, "Nenhum arquivo para compartilhar ainda.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void processImageGeneration(final String prompt, final String style) {
        setProcessingState(true, "Renderizando imagem AI em alta resolução...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Bitmap result = ArtEngine.generateArtwork(prompt, style, 720, 720, 0.5f);

                    File storageDir = getExternalFilesDir("Images");
                    if (storageDir == null) {
                        storageDir = getFilesDir();
                    }
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                    final File imageFile = new File(storageDir, "AI_IMG_" + timeStamp + ".png");

                    FileOutputStream fos = new FileOutputStream(imageFile);
                    result.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                    fos.close();

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            setProcessingState(false, null);
                            currentMediaFile = imageFile;
                            isCurrentMediaVideo = false;

                            videoPreview.stopPlayback();
                            videoPreview.setVisibility(View.GONE);
                            imgPreview.setVisibility(View.VISIBLE);
                            imgPreview.setImageBitmap(result);
                            txtEmptyPlaceholder.setVisibility(View.GONE);

                            btnPlayVideo.setVisibility(View.GONE);
                            layoutMediaActions.setVisibility(View.VISIBLE);

                            creationHistory.add(0, new MediaItem(prompt, imageFile, MediaItem.TYPE_IMAGE));
                            Toast.makeText(MainActivity.this, "Imagem gerada com sucesso sem limites!", Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (final Exception ex) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            setProcessingState(false, null);
                            Toast.makeText(MainActivity.this, "Erro: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void processVideoGeneration(final String prompt, final String style, final int duration) {
        setProcessingState(true, "Iniciando codificador de hardware MP4...");

        File storageDir = getExternalFilesDir("Videos");
        if (storageDir == null) {
            storageDir = getFilesDir();
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        final File videoFile = new File(storageDir, "AI_VID_" + timeStamp + ".mp4");

        Mp4VideoGenerator.createMp4Video(videoFile, prompt, style, 480, 480, duration, 24, new Mp4VideoGenerator.VideoCallback() {
            @Override
            public void onProgress(final int percent) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setProgress(percent);
                        txtProgressStatus.setText("Codificando frames MP4 (" + percent + "%)...");
                    }
                });
            }

            @Override
            public void onSuccess(final File outputFile) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setProcessingState(false, null);
                        currentMediaFile = outputFile;
                        isCurrentMediaVideo = true;

                        imgPreview.setVisibility(View.GONE);
                        txtEmptyPlaceholder.setVisibility(View.GONE);
                        videoPreview.setVisibility(View.VISIBLE);

                        btnPlayVideo.setVisibility(View.VISIBLE);
                        layoutMediaActions.setVisibility(View.VISIBLE);

                        videoPreview.setVideoURI(Uri.fromFile(outputFile));
                        videoPreview.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                mp.setLooping(true);
                                videoPreview.start();
                            }
                        });

                        creationHistory.add(0, new MediaItem(prompt, outputFile, MediaItem.TYPE_VIDEO));
                        Toast.makeText(MainActivity.this, "Vídeo MP4 gerado com sucesso!", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setProcessingState(false, null);
                        Toast.makeText(MainActivity.this, "Falha na codificação: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void setProcessingState(boolean processing, String message) {
        btnMainAction.setEnabled(!processing);
        progressBar.setVisibility(processing ? View.VISIBLE : View.GONE);
        txtProgressStatus.setVisibility(processing ? View.VISIBLE : View.GONE);
        if (message != null) {
            txtProgressStatus.setText(message);
        }
        if (processing) {
            progressBar.setProgress(0);
        }
    }

    private void refreshGalleryList() {
        galleryItemsContainer.removeAllViews();

        if (creationHistory.isEmpty()) {
            TextView emptyTxt = new TextView(this);
            emptyTxt.setText("Nenhuma criação feita ainda. Crie imagens ou vídeos na barra superior!");
            emptyTxt.setTextColor(getResources().getColor(R.color.text_secondary));
            emptyTxt.setPadding(20, 40, 20, 20);
            galleryItemsContainer.addView(emptyTxt);
            return;
        }

        for (int i = 0; i < creationHistory.size(); i++) {
            final MediaItem item = creationHistory.get(i);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setBackgroundColor(getResources().getColor(R.color.card_bg));
            card.setPadding(16, 16, 16, 16);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.match_parent,
                    ViewGroup.LayoutParams.wrap_content
            );
            lp.setMargins(0, 0, 0, 16);
            card.setLayoutParams(lp);

            ImageView icon = new ImageView(this);
            icon.setLayoutParams(new LinearLayout.LayoutParams(60, 60));
            icon.setBackgroundColor(item.getType() == MediaItem.TYPE_VIDEO ?
                    getResources().getColor(R.color.button_green) : getResources().getColor(R.color.button_purple));
            card.addView(icon);

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textColLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.wrap_content, 1f);
            textColLp.setMargins(16, 0, 16, 0);
            textCol.setLayoutParams(textColLp);

            TextView title = new TextView(this);
            title.setText(item.getTitle());
            title.setTextColor(getResources().getColor(R.color.text_primary));
            title.setTextSize(14sp);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            textCol.addView(title);

            TextView subtitle = new TextView(this);
            subtitle.setText((item.getType() == MediaItem.TYPE_VIDEO ? "Vídeo MP4 • " : "Imagem PNG • ") + item.getFile().getName());
            subtitle.setTextColor(getResources().getColor(R.color.text_secondary));
            subtitle.setTextSize(12sp);
            textCol.addView(subtitle);

            card.addView(textCol);

            Button openBtn = new Button(this);
            openBtn.setText("Abrir");
            openBtn.setTextColor(getResources().getColor(R.color.text_primary));
            openBtn.setBackgroundColor(getResources().getColor(R.color.card_secondary));
            openBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentMediaFile = item.getFile();
                    isCurrentMediaVideo = (item.getType() == MediaItem.TYPE_VIDEO);

                    if (isCurrentMediaVideo) {
                        switchTab(TAB_VIDEO);
                        imgPreview.setVisibility(View.GONE);
                        txtEmptyPlaceholder.setVisibility(View.GONE);
                        videoPreview.setVisibility(View.VISIBLE);
                        videoPreview.setVideoURI(Uri.fromFile(currentMediaFile));
                        videoPreview.start();
                        btnPlayVideo.setVisibility(View.VISIBLE);
                    } else {
                        switchTab(TAB_IMAGE);
                        videoPreview.setVisibility(View.GONE);
                        txtEmptyPlaceholder.setVisibility(View.GONE);
                        imgPreview.setVisibility(View.VISIBLE);
                        Bitmap bmp = BitmapFactory.decodeFile(currentMediaFile.getAbsolutePath());
                        imgPreview.setImageBitmap(bmp);
                        btnPlayVideo.setVisibility(View.GONE);
                    }
                    layoutMediaActions.setVisibility(View.VISIBLE);
                }
            });
            card.addView(openBtn);

            galleryItemsContainer.addView(card);
        }
    }
}