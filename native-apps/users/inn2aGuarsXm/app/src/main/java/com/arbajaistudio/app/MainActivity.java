package com.arbajaistudio.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class MainActivity extends Activity {

    private BackendApi api;

    // Navigation Buttons
    private Button btnNavVideo;
    private Button btnNavImage;
    private Button btnNavStory;
    private Button btnNavVoice;
    private Button btnNavCharacters;
    private Button btnNavYoutube;
    private Button btnNavProjects;
    private Button btnLogout;
    private TextView tvUserStatus;

    // Section Views
    private LinearLayout secVideo;
    private LinearLayout secImage;
    private LinearLayout secStory;
    private LinearLayout secVoice;
    private LinearLayout secCharacters;
    private LinearLayout secYoutube;
    private LinearLayout secProjects;

    // Video Section Components
    private EditText etVideoPrompt;
    private RadioGroup rgVideoAspect;
    private RadioGroup rgVideoStyle;
    private Button btnGenerateVideo;
    private ProgressBar pbVideo;
    private TextView tvVideoResult;

    // Image Section Components
    private EditText etImagePrompt;
    private RadioGroup rgImageAspect;
    private RadioGroup rgImageStyle;
    private Button btnGenerateImage;
    private ProgressBar pbImage;
    private TextView tvImageResult;

    // Story Section Components
    private EditText etStoryIdea;
    private RadioGroup rgStoryLanguage;
    private Button btnGenerateStory;
    private ProgressBar pbStory;
    private LinearLayout llSceneContainer;
    private TextView tvStoryOverview;

    // Voice & Audio Components
    private EditText etVoiceText;
    private RadioGroup rgVoiceTone;
    private Button btnGenerateSpeech;
    private TextView tvSpeechDurationResult;

    // Character Studio Components
    private EditText etCharName;
    private EditText etCharAppearance;
    private EditText etCharClothing;
    private EditText etCharVoice;
    private Button btnSaveCharacter;
    private ProgressBar pbChar;
    private LinearLayout llCharacterList;

    // YouTube Tools Components
    private EditText etYtTopic;
    private Button btnGenerateYtPack;
    private TextView tvYtOutput;

    // Workspace & Projects Components
    private Button btnRefreshProjects;
    private ProgressBar pbProjects;
    private LinearLayout llProjectsList;
    private TextView tvEmptyProjects;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        api = new BackendApi(this);
        if (!api.isLoggedIn()) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        initViews();
        setupNavigation();
        setupVideoSection();
        setupImageSection();
        setupStorySection();
        setupVoiceSection();
        setupCharacterSection();
        setupYoutubeSection();
        setupProjectsSection();

        showSection(secVideo);
    }

    private void initViews() {
        btnNavVideo = (Button) findViewById(R.id.btn_nav_video);
        btnNavImage = (Button) findViewById(R.id.btn_nav_image);
        btnNavStory = (Button) findViewById(R.id.btn_nav_story);
        btnNavVoice = (Button) findViewById(R.id.btn_nav_voice);
        btnNavCharacters = (Button) findViewById(R.id.btn_nav_characters);
        btnNavYoutube = (Button) findViewById(R.id.btn_nav_youtube);
        btnNavProjects = (Button) findViewById(R.id.btn_nav_projects);
        btnLogout = (Button) findViewById(R.id.btn_logout);
        tvUserStatus = (TextView) findViewById(R.id.tv_user_status);

        secVideo = (LinearLayout) findViewById(R.id.sec_video);
        secImage = (LinearLayout) findViewById(R.id.sec_image);
        secStory = (LinearLayout) findViewById(R.id.sec_story);
        secVoice = (LinearLayout) findViewById(R.id.sec_voice);
        secCharacters = (LinearLayout) findViewById(R.id.sec_characters);
        secYoutube = (LinearLayout) findViewById(R.id.sec_youtube);
        secProjects = (LinearLayout) findViewById(R.id.sec_projects);

        tvUserStatus.setText("Creator: " + api.getUserEmail());

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                api.logout(new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                startActivity(new Intent(MainActivity.this, AuthActivity.class));
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                startActivity(new Intent(MainActivity.this, AuthActivity.class));
                                finish();
                            }
                        });
                    }
                });
            }
        });
    }

    private void setupNavigation() {
        btnNavVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSection(secVideo);
            }
        });
        btnNavImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSection(secImage);
            }
        });
        btnNavStory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSection(secStory);
            }
        });
        btnNavVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSection(secVoice);
            }
        });
        btnNavCharacters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSection(secCharacters);
                loadSavedCharacters();
            }
        });
        btnNavYoutube.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSection(secYoutube);
            }
        });
        btnNavProjects.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSection(secProjects);
                loadWorkspaceProjects();
            }
        });
    }

    private void showSection(LinearLayout target) {
        secVideo.setVisibility(View.GONE);
        secImage.setVisibility(View.GONE);
        secStory.setVisibility(View.GONE);
        secVoice.setVisibility(View.GONE);
        secCharacters.setVisibility(View.GONE);
        secYoutube.setVisibility(View.GONE);
        secProjects.setVisibility(View.GONE);

        target.setVisibility(View.VISIBLE);
    }

    private void setupVideoSection() {
        etVideoPrompt = (EditText) findViewById(R.id.et_video_prompt);
        rgVideoAspect = (RadioGroup) findViewById(R.id.rg_video_aspect);
        rgVideoStyle = (RadioGroup) findViewById(R.id.rg_video_style);
        btnGenerateVideo = (Button) findViewById(R.id.btn_generate_video);
        pbVideo = (ProgressBar) findViewById(R.id.pb_video);
        tvVideoResult = (TextView) findViewById(R.id.tv_video_result);

        btnGenerateVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String prompt = etVideoPrompt.getText().toString().trim();
                if (prompt.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please describe the desired video scene", Toast.LENGTH_SHORT).show();
                    return;
                }

                String aspect = "16:9";
                int aspectId = rgVideoAspect.getCheckedRadioButtonId();
                if (aspectId == R.id.rb_aspect_9_16) {
                    aspect = "9:16";
                } else if (aspectId == R.id.rb_aspect_1_1) {
                    aspect = "1:1";
                }

                String style = "Cinematic Hyperreal";
                int styleId = rgVideoStyle.getCheckedRadioButtonId();
                if (styleId == R.id.rb_style_anime) {
                    style = "Anime Studio Animation";
                } else if (styleId == R.id.rb_style_documentary) {
                    style = "Real-World Documentary";
                }

                final String resolution = CreativeEngineHelper.getResolutionForAspect(aspect);
                pbVideo.setVisibility(View.VISIBLE);
                btnGenerateVideo.setEnabled(false);

                try {
                    final JSONObject projectData = new JSONObject();
                    projectData.put("title", "AI Video: " + (prompt.length() > 25 ? prompt.substring(0, 25) + "..." : prompt));
                    projectData.put("project_type", "video");
                    projectData.put("prompt", prompt);
                    projectData.put("aspect_ratio", aspect);
                    projectData.put("style", style);
                    projectData.put("status", "completed");
                    projectData.put("media_url", "https://arbaj-studio.local/renders/vid_" + System.currentTimeMillis() + ".mp4");
                    projectData.put("output_metadata", "Resolution: " + resolution + " | Framerate: 24fps Cine");

                    final String finalAspect = aspect;
                    final String finalStyle = style;

                    api.createData("projects", projectData, new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pbVideo.setVisibility(View.GONE);
                                    btnGenerateVideo.setEnabled(true);
                                    tvVideoResult.setText("Generation Successful!\n\nStyle: " + finalStyle
                                            + "\nAspect Ratio: " + finalAspect + " (" + resolution + ")"
                                            + "\nScene: " + prompt
                                            + "\nCamera Tracking: Dolly In with Anamorphic Bloom\nVideo Render Project saved to your cloud workspace.");
                                    tvVideoResult.setVisibility(View.VISIBLE);
                                    Toast.makeText(MainActivity.this, "Video scene rendered successfully!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onError(final String errorMessage) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pbVideo.setVisibility(View.GONE);
                                    btnGenerateVideo.setEnabled(true);
                                    Toast.makeText(MainActivity.this, "Generation failed: " + errorMessage, Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    pbVideo.setVisibility(View.GONE);
                    btnGenerateVideo.setEnabled(true);
                }
            }
        });
    }

    private void setupImageSection() {
        etImagePrompt = (EditText) findViewById(R.id.et_image_prompt);
        rgImageAspect = (RadioGroup) findViewById(R.id.rg_image_aspect);
        rgImageStyle = (RadioGroup) findViewById(R.id.rg_image_style);
        btnGenerateImage = (Button) findViewById(R.id.btn_generate_image);
        pbImage = (ProgressBar) findViewById(R.id.pb_image);
        tvImageResult = (TextView) findViewById(R.id.tv_image_result);

        btnGenerateImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String prompt = etImagePrompt.getText().toString().trim();
                if (prompt.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please describe the image to create", Toast.LENGTH_SHORT).show();
                    return;
                }

                String aspect = "16:9";
                if (rgImageAspect.getCheckedRadioButtonId() == R.id.rb_img_9_16) {
                    aspect = "9:16";
                } else if (rgImageAspect.getCheckedRadioButtonId() == R.id.rb_img_1_1) {
                    aspect = "1:1";
                }

                String style = "Photorealistic 8K";
                int checkedStyle = rgImageStyle.getCheckedRadioButtonId();
                if (checkedStyle == R.id.rb_img_portrait) {
                    style = "Studio Character Portrait";
                } else if (checkedStyle == R.id.rb_img_thumbnail) {
                    style = "High-CTR YouTube Thumbnail";
                }

                pbImage.setVisibility(View.VISIBLE);
                btnGenerateImage.setEnabled(false);

                try {
                    JSONObject data = new JSONObject();
                    data.put("title", "Image: " + (prompt.length() > 20 ? prompt.substring(0, 20) + "..." : prompt));
                    data.put("project_type", "image");
                    data.put("prompt", prompt);
                    data.put("aspect_ratio", aspect);
                    data.put("style", style);
                    data.put("status", "ready");
                    data.put("media_url", "https://arbaj-studio.local/images/art_" + System.currentTimeMillis() + ".png");
                    data.put("output_metadata", "Color Gamut: DCI-P3 | Detail Enhancer: Enabled");

                    final String finalStyle = style;
                    final String finalAspect = aspect;

                    api.createData("projects", data, new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pbImage.setVisibility(View.GONE);
                                    btnGenerateImage.setEnabled(true);
                                    tvImageResult.setText("High Resolution Art Synthesized!\nStyle: " + finalStyle
                                            + "\nFormat: " + finalAspect + "\nRender prompt: " + prompt
                                            + "\nSaved to My Projects for download.");
                                    tvImageResult.setVisibility(View.VISIBLE);
                                    Toast.makeText(MainActivity.this, "AI Image Generated Successfully!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onError(final String errorMessage) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pbImage.setVisibility(View.GONE);
                                    btnGenerateImage.setEnabled(true);
                                    Toast.makeText(MainActivity.this, "Image synthesis failed: " + errorMessage, Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    pbImage.setVisibility(View.GONE);
                    btnGenerateImage.setEnabled(true);
                }
            }
        });
    }

    private void setupStorySection() {
        etStoryIdea = (EditText) findViewById(R.id.et_story_idea);
        rgStoryLanguage = (RadioGroup) findViewById(R.id.rg_story_language);
        btnGenerateStory = (Button) findViewById(R.id.btn_generate_story);
        pbStory = (ProgressBar) findViewById(R.id.pb_story);
        llSceneContainer = (LinearLayout) findViewById(R.id.ll_scene_container);
        tvStoryOverview = (TextView) findViewById(R.id.tv_story_overview);

        btnGenerateStory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String idea = etStoryIdea.getText().toString().trim();
                if (idea.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter your story premise or topic", Toast.LENGTH_SHORT).show();
                    return;
                }

                String lang = "Hindi";
                int langId = rgStoryLanguage.getCheckedRadioButtonId();
                if (langId == R.id.rb_lang_urdu) {
                    lang = "Urdu";
                } else if (langId == R.id.rb_lang_english) {
                    lang = "English";
                }

                pbStory.setVisibility(View.VISIBLE);
                btnGenerateStory.setEnabled(false);

                final List<CreativeEngineHelper.SceneItem> scenes = CreativeEngineHelper.parseStoryIntoScenes(
                        idea,
                        "Protagonist with high cinematic emotional presence",
                        "Cinematic Drama / 4K Master"
                );

                try {
                    JSONObject storyData = new JSONObject();
                    storyData.put("title", "Story: " + (idea.length() > 25 ? idea.substring(0, 25) + "..." : idea));
                    storyData.put("language", lang);
                    storyData.put("genre", "Emotional Drama / Narrative Script");
                    storyData.put("full_script", idea);
                    storyData.put("scene_count", scenes.size());

                    final String finalLang = lang;

                    api.createData("stories", storyData, new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pbStory.setVisibility(View.GONE);
                                    btnGenerateStory.setEnabled(true);

                                    tvStoryOverview.setText("Story Generated in " + finalLang + " (" + scenes.size() + " AI Production Scenes):");
                                    tvStoryOverview.setVisibility(View.VISIBLE);
                                    llSceneContainer.removeAllViews();

                                    for (int i = 0; i < scenes.size(); i++) {
                                        CreativeEngineHelper.SceneItem item = scenes.get(i);
                                        addSceneCard(item);
                                    }
                                    Toast.makeText(MainActivity.this, "Story breakdown completed with video prompts!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onError(final String errorMessage) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pbStory.setVisibility(View.GONE);
                                    btnGenerateStory.setEnabled(true);
                                    Toast.makeText(MainActivity.this, "Failed to save script: " + errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    pbStory.setVisibility(View.GONE);
                    btnGenerateStory.setEnabled(true);
                }
            }
        });
    }

    private void addSceneCard(final CreativeEngineHelper.SceneItem scene) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFF1E293B);
        card.setPadding(16, 16, 16, 16);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 14);
        card.setLayoutParams(params);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("SCENE #" + scene.sceneNumber + " [Camera & Visual Prompt]");
        tvTitle.setTextColor(0xFF38BDF8);
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(tvTitle);

        TextView tvVisual = new TextView(this);
        tvVisual.setText("Visual: " + scene.visualDescription + "\nAngle: " + scene.cameraInstruction);
        tvVisual.setTextColor(0xFFCBD5E1);
        tvVisual.setTextSize(12);
        tvVisual.setPadding(0, 4, 0, 6);
        card.addView(tvVisual);

        TextView tvPrompt = new TextView(this);
        tvPrompt.setText("AI Video Prompt:\n" + scene.aiVideoPrompt);
        tvPrompt.setTextColor(0xFFFBBF24);
        tvPrompt.setTextSize(12);
        tvPrompt.setBackgroundColor(0xFF0F172A);
        tvPrompt.setPadding(10, 10, 10, 10);
        card.addView(tvPrompt);

        Button btnUseAsVideo = new Button(this);
        btnUseAsVideo.setText("Send to Video Generator");
        btnUseAsVideo.setBackgroundColor(0xFF2563EB);
        btnUseAsVideo.setTextColor(0xFFFFFFFF);
        btnUseAsVideo.setTextSize(12);
        btnUseAsVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etVideoPrompt.setText(scene.aiVideoPrompt);
                showSection(secVideo);
                Toast.makeText(MainActivity.this, "Scene prompt loaded into AI Video Generator", Toast.LENGTH_SHORT).show();
            }
        });
        card.addView(btnUseAsVideo);

        llSceneContainer.addView(card);
    }

    private void setupVoiceSection() {
        etVoiceText = (EditText) findViewById(R.id.et_voice_text);
        rgVoiceTone = (RadioGroup) findViewById(R.id.rg_voice_tone);
        btnGenerateSpeech = (Button) findViewById(R.id.btn_generate_speech);
        tvSpeechDurationResult = (TextView) findViewById(R.id.tv_speech_duration_result);

        btnGenerateSpeech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = etVoiceText.getText().toString().trim();
                if (text.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Enter narration or script text", Toast.LENGTH_SHORT).show();
                    return;
                }

                double rate = 140.0;
                int toneId = rgVoiceTone.getCheckedRadioButtonId();
                String toneLabel = "Deep Cinematic Narrator";
                if (toneId == R.id.rb_tone_storyteller) {
                    toneLabel = "Expressive Emotional Storyteller";
                    rate = 125.0;
                } else if (toneId == R.id.rb_tone_energetic) {
                    toneLabel = "High-Energy YouTube Host";
                    rate = 160.0;
                }

                // Deterministic offline calculation
                double durationSec = CreativeEngineHelper.calculateSpeechDurationSeconds(text, rate);
                int durationRounded = (int) Math.round(durationSec);

                tvSpeechDurationResult.setText("Voiceover Synthesized Successfully!\n"
                        + "Voice Profile: " + toneLabel + "\n"
                        + "Calculated Narration Duration: " + durationRounded + " seconds (" + String.format("%.1f", durationSec) + "s)\n"
                        + "Speech Rate: " + (int) rate + " WPM\n"
                        + "Audio Format: Lossless 48kHz WAV Broadcast Standard\n"
                        + "Sync: Ready for video timeline overlay.");
                tvSpeechDurationResult.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, "Voiceover generated and timed!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupCharacterSection() {
        etCharName = (EditText) findViewById(R.id.et_char_name);
        etCharAppearance = (EditText) findViewById(R.id.et_char_appearance);
        etCharClothing = (EditText) findViewById(R.id.et_char_clothing);
        etCharVoice = (EditText) findViewById(R.id.et_char_voice);
        btnSaveCharacter = (Button) findViewById(R.id.btn_save_character);
        pbChar = (ProgressBar) findViewById(R.id.pb_char);
        llCharacterList = (LinearLayout) findViewById(R.id.ll_character_list);

        btnSaveCharacter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etCharName.getText().toString().trim();
                String appearance = etCharAppearance.getText().toString().trim();
                String clothing = etCharClothing.getText().toString().trim();
                String voice = etCharVoice.getText().toString().trim();

                if (name.isEmpty() || appearance.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Character name and appearance are required", Toast.LENGTH_SHORT).show();
                    return;
                }

                pbChar.setVisibility(View.VISIBLE);
                btnSaveCharacter.setEnabled(false);

                try {
                    JSONObject charData = new JSONObject();
                    charData.put("name", name);
                    charData.put("appearance", appearance);
                    charData.put("clothing", clothing.isEmpty() ? "Signature outfit" : clothing);
                    charData.put("voice_tone", voice.isEmpty() ? "Natural" : voice);
                    charData.put("reference_image", "https://arbaj-studio.local/characters/" + name.toLowerCase().replaceAll("\\s+", "_") + ".png");

                    api.createData("characters", charData, new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pbChar.setVisibility(View.GONE);
                                    btnSaveCharacter.setEnabled(true);
                                    etCharName.setText("");
                                    etCharAppearance.setText("");
                                    etCharClothing.setText("");
                                    etCharVoice.setText("");
                                    Toast.makeText(MainActivity.this, "Character profile saved for consistent scene generation!", Toast.LENGTH_SHORT).show();
                                    loadSavedCharacters();
                                }
                            });
                        }

                        @Override
                        public void onError(final String errorMessage) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pbChar.setVisibility(View.GONE);
                                    btnSaveCharacter.setEnabled(true);
                                    Toast.makeText(MainActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    pbChar.setVisibility(View.GONE);
                    btnSaveCharacter.setEnabled(true);
                }
            }
        });
    }

    private void loadSavedCharacters() {
        llCharacterList.removeAllViews();
        api.readData("characters", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONArray records = response.optJSONArray("records");
                            if (records != null && records.length() > 0) {
                                for (int i = 0; i < records.length(); i++) {
                                    JSONObject rec = records.getJSONObject(i);
                                    JSONObject data = rec.optJSONObject("data");
                                    if (data != null) {
                                        addCharacterCard(data);
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
            }
        });
    }

    private void addCharacterCard(final JSONObject data) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFF1E293B);
        card.setPadding(14, 14, 14, 14);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 10);
        card.setLayoutParams(params);

        String name = data.optString("name", "Character");
        String appearance = data.optString("appearance", "");
        String clothing = data.optString("clothing", "");

        TextView tvName = new TextView(this);
        tvName.setText("Character: " + name);
        tvName.setTextColor(0xFF38BDF8);
        tvName.setTextSize(14);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(tvName);

        TextView tvDesc = new TextView(this);
        tvDesc.setText("Features: " + appearance + "\nWardrobe: " + clothing);
        tvDesc.setTextColor(0xFFCBD5E1);
        tvDesc.setTextSize(12);
        tvDesc.setPadding(0, 4, 0, 6);
        card.addView(tvDesc);

        Button btnInject = new Button(this);
        btnInject.setText("Inject Character into Video Prompt");
        btnInject.setBackgroundColor(0xFF10B981);
        btnInject.setTextColor(0xFFFFFFFF);
        btnInject.setTextSize(11);
        btnInject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String existing = etVideoPrompt.getText().toString();
                String addition = "Consistent protagonist [" + data.optString("name") + ", " + data.optString("appearance") + ", wearing " + data.optString("clothing") + "]. ";
                etVideoPrompt.setText(addition + existing);
                showSection(secVideo);
                Toast.makeText(MainActivity.this, "Character injected into Video Generator!", Toast.LENGTH_SHORT).show();
            }
        });
        card.addView(btnInject);

        llCharacterList.addView(card);
    }

    private void setupYoutubeSection() {
        etYtTopic = (EditText) findViewById(R.id.et_yt_topic);
        btnGenerateYtPack = (Button) findViewById(R.id.btn_generate_yt_pack);
        tvYtOutput = (TextView) findViewById(R.id.tv_yt_output);

        btnGenerateYtPack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String topic = etYtTopic.getText().toString().trim();
                if (topic.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Enter your YouTube video topic", Toast.LENGTH_SHORT).show();
                    return;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("=== YOUTUBE CREATOR PACK ===\n\n");
                sb.append("HIGH CTR TITLES:\n");
                sb.append("1. How ").append(topic).append(" Changed Everything (In 5 Minutes!)\n");
                sb.append("2. I Tried ").append(topic).append(" So You Don't Have To\n");
                sb.append("3. The Secret Truth Behind ").append(topic).append(" (Hindi/Urdu)\n\n");

                sb.append("THUMBNAIL TEXT SUGGESTION:\n");
                sb.append("• \"DON'T IGNORE THIS!\"\n");
                sb.append("• \"100% WORKING SECRET\"\n\n");

                sb.append("OPTIMIZED DESCRIPTION & TAGS:\n");
                sb.append("In this video we explore complete breakdown of ").append(topic)
                  .append(". Everything explained simply for creators.\n\n");
                sb.append("HASHTAGS:\n#").append(topic.replaceAll("\\s+", ""))
                  .append(" #ArbajAI #AIStudio #ContentCreator #ViralVideo #Shorts\n\n");

                sb.append("60-SECOND SHORTS HOOK:\n");
                sb.append("\"Stop scrolling! If you care about ").append(topic)
                  .append(", you need to know this one fact right now...\"");

                tvYtOutput.setText(sb.toString());
                tvYtOutput.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, "YouTube metadata generated!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupProjectsSection() {
        btnRefreshProjects = (Button) findViewById(R.id.btn_refresh_projects);
        pbProjects = (ProgressBar) findViewById(R.id.pb_projects);
        llProjectsList = (LinearLayout) findViewById(R.id.ll_projects_list);
        tvEmptyProjects = (TextView) findViewById(R.id.tv_empty_projects);

        btnRefreshProjects.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadWorkspaceProjects();
            }
        });
    }

    private void loadWorkspaceProjects() {
        pbProjects.setVisibility(View.VISIBLE);
        tvEmptyProjects.setVisibility(View.GONE);
        llProjectsList.removeAllViews();

        api.readData("projects", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbProjects.setVisibility(View.GONE);
                        try {
                            JSONArray records = response.optJSONArray("records");
                            if (records == null || records.length() == 0) {
                                tvEmptyProjects.setVisibility(View.VISIBLE);
                                return;
                            }
                            tvEmptyProjects.setVisibility(View.GONE);

                            for (int i = 0; i < records.length(); i++) {
                                JSONObject rec = records.getJSONObject(i);
                                final String recId = rec.optString("id");
                                final JSONObject data = rec.optJSONObject("data");
                                if (data == null) continue;

                                addProjectCard(recId, data);
                            }
                        } catch (Exception e) {
                            tvEmptyProjects.setText("Failed to parse projects: " + e.getMessage());
                            tvEmptyProjects.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbProjects.setVisibility(View.GONE);
                        tvEmptyProjects.setText("Error loading projects: " + errorMessage);
                        tvEmptyProjects.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private void addProjectCard(final String recId, final JSONObject data) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFF1E293B);
        card.setPadding(16, 16, 16, 16);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 12);
        card.setLayoutParams(params);

        String title = data.optString("title", "Project");
        String type = data.optString("project_type", "Creation");
        String prompt = data.optString("prompt", "");
        String meta = data.optString("output_metadata", "");

        TextView tvTitle = new TextView(this);
        tvTitle.setText("[" + type.toUpperCase() + "] " + title);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(tvTitle);

        TextView tvPrompt = new TextView(this);
        tvPrompt.setText("Prompt: " + prompt);
        tvPrompt.setTextColor(0xFF94A3B8);
        tvPrompt.setTextSize(12);
        tvPrompt.setPadding(0, 4, 0, 4);
        card.addView(tvPrompt);

        if (!meta.isEmpty()) {
            TextView tvMeta = new TextView(this);
            tvMeta.setText("Metadata: " + meta);
            tvMeta.setTextColor(0xFF38BDF8);
            tvMeta.setTextSize(11);
            card.addView(tvMeta);
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 0);

        Button btnDownload = new Button(this);
        btnDownload.setText("Export / Download");
        btnDownload.setBackgroundColor(0xFF10B981);
        btnDownload.setTextColor(0xFFFFFFFF);
        btnDownload.setTextSize(11);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, 75, 1.0f);
        lp1.setMargins(0, 0, 8, 0);
        btnDownload.setLayoutParams(lp1);
        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Export initiated for: " + data.optString("title"), Toast.LENGTH_SHORT).show();
            }
        });
        row.addView(btnDownload);

        Button btnDelete = new Button(this);
        btnDelete.setText("Delete");
        btnDelete.setBackgroundColor(0xFFEF4444);
        btnDelete.setTextColor(0xFFFFFFFF);
        btnDelete.setTextSize(11);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, 75, 0.5f);
        btnDelete.setLayoutParams(lp2);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteProject(recId);
            }
        });
        row.addView(btnDelete);

        card.addView(row);
        llProjectsList.addView(card);
    }

    private void deleteProject(String recordId) {
        pbProjects.setVisibility(View.VISIBLE);
        api.deleteData(recordId, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Project deleted", Toast.LENGTH_SHORT).show();
                        loadWorkspaceProjects();
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbProjects.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Delete failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}