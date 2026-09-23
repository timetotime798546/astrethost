package com.offlineaimind.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class MainActivity extends Activity {

    // Tab view models
    private LinearLayout layoutChatContainer;
    private ScrollView layoutTrainContainer;
    private ScrollView layoutLabContainer;

    // Navigation buttons
    private Button btnTabChat;
    private Button btnTabTrain;
    private Button btnTabLab;

    // Chat items
    private ScrollView chatScrollView;
    private LinearLayout chatHistoryContainer;
    private EditText etChatInput;
    private Button btnChatSend;

    // Brain items
    private EditText etTrigger;
    private EditText etResponse;
    private Button btnTrainBrain;
    private LinearLayout brainListContainer;

    // Sentiment items
    private EditText etSentimentInput;
    private Button btnAnalyzeSentiment;
    private TextView tvSentimentResult;

    // Generative art items
    private EditText etArtPrompt;
    private Button btnGenerateArt;
    private ImageView ivArtOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide standard framework action bar to use our custom design
        if (getActionBar() != null) {
            getActionBar().hide();
        }

        setContentView(R.layout.activity_main);

        // Map layout controls
        initViews();

        // Bind interactive event listeners
        bindEvents();

        // Introduce AI inside chat history view
        addChatBubble("Hello! I am your Offline AI Mind. I run entirely on local CPU cycles without any internet access.", false);
        addChatBubble("You can ask me questions, compute expressions (e.g. 25 * 4), test jokes, analyze psychological sentiment, generate symmetric pixel art, or completely customize my responses in the 'Train Brain' section above!", false);

        // Load initially active items
        refreshBrainList();
    }

    private void initViews() {
        layoutChatContainer = (LinearLayout) findViewById(R.id.layoutChatContainer);
        layoutTrainContainer = (ScrollView) findViewById(R.id.layoutTrainContainer);
        layoutLabContainer = (ScrollView) findViewById(R.id.layoutLabContainer);

        btnTabChat = (Button) findViewById(R.id.btnTabChat);
        btnTabTrain = (Button) findViewById(R.id.btnTabTrain);
        btnTabLab = (Button) findViewById(R.id.btnTabLab);

        chatScrollView = (ScrollView) findViewById(R.id.chatScrollView);
        chatHistoryContainer = (LinearLayout) findViewById(R.id.chatHistoryContainer);
        etChatInput = (EditText) findViewById(R.id.etChatInput);
        btnChatSend = (Button) findViewById(R.id.btnChatSend);

        etTrigger = (EditText) findViewById(R.id.etTrigger);
        etResponse = (EditText) findViewById(R.id.etResponse);
        btnTrainBrain = (Button) findViewById(R.id.btnTrainBrain);
        brainListContainer = (LinearLayout) findViewById(R.id.brainListContainer);

        etSentimentInput = (EditText) findViewById(R.id.etSentimentInput);
        btnAnalyzeSentiment = (Button) findViewById(R.id.btnAnalyzeSentiment);
        tvSentimentResult = (TextView) findViewById(R.id.tvSentimentResult);

        etArtPrompt = (EditText) findViewById(R.id.etArtPrompt);
        btnGenerateArt = (Button) findViewById(R.id.btnGenerateArt);
        ivArtOutput = (ImageView) findViewById(R.id.ivArtOutput);
    }

    private void bindEvents() {
        // Handle swap tab navigation actions
        btnTabChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        btnTabTrain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });

        btnTabLab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3);
            }
        });

        // Chat send trigger events
        btnChatSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processUserChatMessage();
            }
        });

        etChatInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    processUserChatMessage();
                    return true;
                }
                return false;
            }
        });

        // Neural trainer submission
        btnTrainBrain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trainCustomSynapse();
            }
        });

        // Sentiment engine execution
        btnAnalyzeSentiment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runSentimentEngine();
            }
        });

        // Pixel generation actions
        btnGenerateArt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runProceduralArtGenerator();
            }
        });
    }

    private void switchTab(int tabIndex) {
        // Reset navigation styles
        btnTabChat.setTextColor(Color.parseColor("#757575"));
        btnTabChat.setTypeface(null, Typeface.NORMAL);
        btnTabTrain.setTextColor(Color.parseColor("#757575"));
        btnTabTrain.setTypeface(null, Typeface.NORMAL);
        btnTabLab.setTextColor(Color.parseColor("#757575"));
        btnTabLab.setTypeface(null, Typeface.NORMAL);

        // Hide layout components
        layoutChatContainer.setVisibility(View.GONE);
        layoutTrainContainer.setVisibility(View.GONE);
        layoutLabContainer.setVisibility(View.GONE);

        // Select and display correct items
        if (tabIndex == 1) {
            btnTabChat.setTextColor(Color.parseColor("#3F51B5"));
            btnTabChat.setTypeface(null, Typeface.BOLD);
            layoutChatContainer.setVisibility(View.VISIBLE);
        } else if (tabIndex == 2) {
            btnTabTrain.setTextColor(Color.parseColor("#3F51B5"));
            btnTabTrain.setTypeface(null, Typeface.BOLD);
            layoutTrainContainer.setVisibility(View.VISIBLE);
            refreshBrainList();
        } else if (tabIndex == 3) {
            btnTabLab.setTextColor(Color.parseColor("#3F51B5"));
            btnTabLab.setTypeface(null, Typeface.BOLD);
            layoutLabContainer.setVisibility(View.VISIBLE);
        }
    }

    // --- Interactive Chat Logic ---

    private void processUserChatMessage() {
        String input = etChatInput.getText().toString().trim();
        if (input.isEmpty()) {
            return;
        }

        // Output user dialog
        addChatBubble(input, true);
        etChatInput.setText("");

        // Process AI offline response with slight delay to mimic cognitive thought processing
        final String query = input;
        chatScrollView.postDelayed(new Runnable() {
            @Override
            public void run() {
                String result = analyzeAndGenerateResponse(query);
                addChatBubble(result, false);
            }
        }, 350);
    }

    private void addChatBubble(String text, boolean isUser) {
        LinearLayout bubbleLayout = new LinearLayout(this);
        bubbleLayout.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams parentParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        parentParams.setMargins(0, 12, 0, 12);
        bubbleLayout.setLayoutParams(parentParams);

        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(15);
        textView.setTextColor(isUser ? Color.parseColor("#1A237E") : Color.parseColor("#212121"));

        int backgroundRes = isUser ? R.drawable.bg_bubble_user : R.drawable.bg_bubble_ai;
        textView.setBackgroundResource(backgroundRes);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        if (isUser) {
            bubbleLayout.setGravity(Gravity.RIGHT);
            textParams.setMargins(80, 0, 4, 0);
        } else {
            bubbleLayout.setGravity(Gravity.LEFT);
            textParams.setMargins(4, 0, 80, 0);
        }

        textView.setLayoutParams(textParams);
        bubbleLayout.addView(textView);
        chatHistoryContainer.addView(bubbleLayout);

        // Smooth scroll implementation
        chatScrollView.post(new Runnable() {
            @Override
            public void run() {
                chatScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private String analyzeAndGenerateResponse(String query) {
        String cleanQuery = query.toLowerCase().trim();

        // 1. Search locally taught database responses first
        SharedPreferences prefs = getSharedPreferences("OfflineAIBrain", MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith("taught_")) {
                String trainedTrigger = entry.getKey().substring(7);
                if (cleanQuery.contains(trainedTrigger)) {
                    return entry.getValue().toString();
                }
            }
        }

        // 2. Perform Mathematical ALU Parsing checks
        String mathSolution = tryParseArithmetic(cleanQuery);
        if (mathSolution != null) {
            return mathSolution;
        }

        // 3. Built-in lexical matching engines
        if (cleanQuery.contains("hello") || cleanQuery.contains("hi ") || cleanQuery.contains("hey") || cleanQuery.contains("greetings") || cleanQuery.equals("hi")) {
            return "Greetings! Standard neural welcome protocol active. How can I assist you in this sandbox offline?";
        }

        if (cleanQuery.contains("name") || cleanQuery.contains("who are you") || cleanQuery.contains("what are you")) {
            return "I am the Offline AI Mind, a high-performance interactive cognitive model running 100% locally on your device with no cloud components.";
        }

        if (cleanQuery.contains("features") || cleanQuery.contains("what can you do") || cleanQuery.contains("help") || cleanQuery.contains("commands")) {
            return "My functional systems include:\n" +
                    "- Built-in conversations (greetings, system info, jokes, game tokens).\n" +
                    "- Dynamic math parsing (e.g. ask me: 'solve 234 * 5').\n" +
                    "- SQLite-equivalent Synapse training (Teach me keywords in the Train Brain tab!).\n" +
                    "- Offline Sentiment Predictive Analytics (in Lab Tab).\n" +
                    "- Seeder-hash procedural graphic sprite rendering (in Lab Tab).";
        }

        if (cleanQuery.contains("joke") || cleanQuery.contains("laugh") || cleanQuery.contains("funny")) {
            String[] jokes = {
                "Why do programmers prefer dark mode? Because light attracts bugs!",
                "There are 10 types of people in this world: those who understand binary, and those who don't.",
                "Why did the database administrator leave the restaurant? Because there were too many tables!",
                "An SQL query walks into a bar, walks up to two tables and asks: 'Can I join you?'"
            };
            return jokes[new Random().nextInt(jokes.length)];
        }

        if (cleanQuery.contains("riddle") || cleanQuery.contains("brain teaser")) {
            return "What has keys but can't open locks, has space but no room, and allows you to enter?\n\nAnswer: A Keyboard! Press enter to keep going.";
        }

        if (cleanQuery.contains("time") || cleanQuery.contains("date") || cleanQuery.contains("clock") || cleanQuery.contains("hour")) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return "Local system clock reports: " + sdf.format(new Date()) + ". Synchronization offline is correct.";
        }

        if (cleanQuery.contains("rock paper scissors") || cleanQuery.contains("play game") || cleanQuery.contains("rps")) {
            String[] rpsOptions = {"rock", "paper", "scissors"};
            String aiSelection = rpsOptions[new Random().nextInt(3)];
            return "Let's play Rock-Paper-Scissors! I select: " + aiSelection.toUpperCase() + ". Did you win? Let me know!";
        }

        if (cleanQuery.contains("how are you") || cleanQuery.contains("are you happy") || cleanQuery.contains("feel")) {
            return "My silicon neural synapses are currently performing at 100% efficiency. No performance errors found. I feel great! Thank you.";
        }

        // 4. Default fallback explaining teach functions
        return "I have scanned my local offline dictionary but haven't found a match for '" + query + "'.\n\n" +
                "You can teach me what to say! Go to the 'Train Brain' tab above, register '" + query.toLowerCase() + "' as a custom synaptic trigger, and I will instantly know how to answer.";
    }

    private String tryParseArithmetic(String input) {
        try {
            // Clean dynamic formulas out of natural text patterns
            String formulaText = input;
            if (formulaText.contains("calculate")) {
                formulaText = formulaText.substring(formulaText.indexOf("calculate") + 9);
            } else if (formulaText.contains("solve")) {
                formulaText = formulaText.substring(formulaText.indexOf("solve") + 5);
            }

            // Clean irrelevant symbols
            String clean = formulaText.replaceAll("[^0-9\\+\\-\\*/\\.]", " ").trim();
            String[] tokens = clean.split("\\s+");

            if (tokens.length >= 2) {
                double val1 = Double.parseDouble(tokens[0]);
                double val2 = Double.parseDouble(tokens[1]);

                char operator = ' ';
                if (formulaText.contains("+")) operator = '+';
                else if (formulaText.contains("-")) operator = '-';
                else if (formulaText.contains("*") || formulaText.contains("x")) operator = '*';
                else if (formulaText.contains("/")) operator = '/';

                if (operator != ' ') {
                    double solution = 0;
                    switch (operator) {
                        case '+': solution = val1 + val2; break;
                        case '-': solution = val1 - val2; break;
                        case '*': solution = val1 * val2; break;
                        case '/':
                            if (val2 == 0) return "Arithmetic Error: Division by zero is undefined in logic cores.";
                            solution = val1 / val2;
                            break;
                    }
                    return "ALU Parser Active. The mathematical output for " + val1 + " " + operator + " " + val2 + " equals: " + solution;
                }
            }
        } catch (Exception e) {
            // Ignore arithmetic failures and fall back to standard conversational layers
        }
        return null;
    }

    // --- Brain Synaptic custom training ---

    private void trainCustomSynapse() {
        String trigger = etTrigger.getText().toString().trim().toLowerCase();
        String response = etResponse.getText().toString().trim();

        if (trigger.isEmpty() || response.isEmpty()) {
            Toast.makeText(this, "Please write both a trigger phrase and its reply!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save entry into SharedPreferences database layers
        SharedPreferences.Editor editor = getSharedPreferences("OfflineAIBrain", MODE_PRIVATE).edit();
        editor.putString("taught_" + trigger, response);
        editor.apply();

        Toast.makeText(this, "Cognitive connection trained successfully!", Toast.LENGTH_SHORT).show();
        
        // Reset inputs
        etTrigger.setText("");
        etResponse.setText("");

        // Refresh database lists
        refreshBrainList();
    }

    private void refreshBrainList() {
        brainListContainer.removeAllViews();
        SharedPreferences prefs = getSharedPreferences("OfflineAIBrain", MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        boolean empty = true;
        for (final Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith("taught_")) {
                empty = false;
                final String trigger = entry.getKey().substring(7);
                final String response = entry.getValue().toString();

                LinearLayout rowLayout = new LinearLayout(this);
                rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                rowLayout.setPadding(8, 12, 8, 12);

                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                rowLayout.setLayoutParams(rowParams);

                TextView tvRule = new TextView(this);
                tvRule.setText("• IF USER SAYS: \"" + trigger + "\"\n  THEN AI REPLIES: \"" + response + "\"");
                tvRule.setTextSize(13);
                tvRule.setTextColor(Color.parseColor("#212121"));

                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );
                tvRule.setLayoutParams(textParams);
                rowLayout.addView(tvRule);

                Button btnDelete = new Button(this);
                btnDelete.setText("Forget");
                btnDelete.setTextSize(10);
                btnDelete.setTextColor(Color.WHITE);
                btnDelete.setBackgroundColor(Color.parseColor("#D32F2F"));

                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        36 * (int)getResources().getDisplayMetrics().density
                );
                btnParams.setMargins(12, 0, 0, 0);
                btnDelete.setLayoutParams(btnParams);

                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SharedPreferences.Editor editor = getSharedPreferences("OfflineAIBrain", MODE_PRIVATE).edit();
                        editor.remove("taught_" + trigger);
                        editor.apply();
                        Toast.makeText(MainActivity.this, "Connection removed.", Toast.LENGTH_SHORT).show();
                        refreshBrainList();
                    }
                });

                rowLayout.addView(btnDelete);

                // Add bottom border line
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(Color.parseColor("#E0E0E0"));

                brainListContainer.addView(rowLayout);
                brainListContainer.addView(divider);
            }
        }

        if (empty) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No custom synapses trained. Use the editor fields above to program custom triggers!");
            tvEmpty.setTextSize(13);
            tvEmpty.setTextColor(Color.parseColor("#9E9E9E"));
            tvEmpty.setGravity(Gravity.CENTER);
            tvEmpty.setPadding(0, 30, 0, 30);
            brainListContainer.addView(tvEmpty);
        }
    }

    // --- Dynamic Sentiment Predictive Analytics ---

    private void runSentimentEngine() {
        String text = etSentimentInput.getText().toString().trim();
        if (text.isEmpty()) {
            tvSentimentResult.setText("Warning: Input text field is currently blank.");
            return;
        }

        String cleanText = text.toLowerCase().replaceAll("[^a-z ]", " ");
        String[] words = cleanText.split("\\s+");

        int finalScore = 0;

        // Custom Lexicon sets
        String[] positiveSet = {"good", "great", "awesome", "love", "happy", "beautiful", "excellent", "amazing", "yes", "cool", "superb", "nice", "friendly", "helpful", "smart", "outstanding", "glad", "joy", "fun", "perfect"};
        String[] negativeSet = {"bad", "sad", "hate", "angry", "terrible", "worst", "broken", "useless", "no", "poor", "pain", "annoying", "stupid", "dumb", "ugly", "mad", "awful", "horrible", "dislike", "fail"};

        for (int i = 0; i < words.length; i++) {
            String targetWord = words[i];

            boolean isPositiveMatch = false;
            for (String pos : positiveSet) {
                if (pos.equals(targetWord)) {
                    isPositiveMatch = true;
                    break;
                }
            }

            boolean isNegativeMatch = false;
            for (String neg : negativeSet) {
                if (neg.equals(targetWord)) {
                    isNegativeMatch = true;
                    break;
                }
            }

            if (isPositiveMatch) {
                // Check local negations inside preceding word arrays
                if (i > 0 && (words[i-1].equals("not") || words[i-1].equals("never") || words[i-1].equals("no") || words[i-1].equals("dont"))) {
                    finalScore -= 1; // "not awesome" yields negative output
                } else {
                    finalScore += 1;
                }
            } else if (isNegativeMatch) {
                if (i > 0 && (words[i-1].equals("not") || words[i-1].equals("never") || words[i-1].equals("dont"))) {
                    finalScore += 1; // "not bad" yields positive output
                } else {
                    finalScore -= 1;
                }
            }
        }

        String moodResult;
        String emoji;
        int colorTint;

        if (finalScore > 0) {
            moodResult = "POSITIVE VALENCE DETECTED";
            colorTint = Color.parseColor("#E8F5E9"); // Light green
            emoji = "😊";
        } else if (finalScore < 0) {
            moodResult = "NEGATIVE VALENCE DETECTED";
            colorTint = Color.parseColor("#FFEBEE"); // Light red
            emoji = "😢";
        } else {
            moodResult = "NEUTRAL VALENCE DETECTED";
            colorTint = Color.parseColor("#F5F5F5"); // Light gray
            emoji = "😐";
        }

        tvSentimentResult.setBackgroundColor(colorTint);
        tvSentimentResult.setPadding(16, 16, 16, 16);
        tvSentimentResult.setText(emoji + " sentiment: " + moodResult + "\nNumeric Score: " + finalScore + "\nMatched elements parsed.");
    }

    // --- Procedural generative sprite rendering engine ---

    private void runProceduralArtGenerator() {
        String prompt = etArtPrompt.getText().toString().trim();
        if (prompt.isEmpty()) {
            Toast.makeText(this, "Please enter a descriptive prompt to seed the generator!", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap procedurallyGeneratedBitmap = renderSymmetricPixelSprite(prompt);
        ivArtOutput.setImageBitmap(procedurallyGeneratedBitmap);
    }

    private Bitmap renderSymmetricPixelSprite(String inputPrompt) {
        int width = 16;
        int height = 16;
        Bitmap pixelGridBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // Generate deterministic seed using prompt string hashCode
        long numericSeed = inputPrompt.trim().toLowerCase().hashCode();
        Random randomEngine = new Random(numericSeed);

        // Choose beautiful matching color pallet configurations based on prompt properties
        int primaryR = 80 + randomEngine.nextInt(176);
        int primaryG = 80 + randomEngine.nextInt(176);
        int primaryB = 80 + randomEngine.nextInt(176);

        String lowerPrompt = inputPrompt.toLowerCase();
        if (lowerPrompt.contains("fire") || lowerPrompt.contains("burn") || lowerPrompt.contains("lava") || lowerPrompt.contains("sun") || lowerPrompt.contains("red")) {
            primaryR = 210 + randomEngine.nextInt(45);
            primaryG = 40 + randomEngine.nextInt(100);
            primaryB = 10;
        } else if (lowerPrompt.contains("forest") || lowerPrompt.contains("tree") || lowerPrompt.contains("grass") || lowerPrompt.contains("nature") || lowerPrompt.contains("green")) {
            primaryR = 20;
            primaryG = 160 + randomEngine.nextInt(95);
            primaryB = 40;
        } else if (lowerPrompt.contains("water") || lowerPrompt.contains("sea") || lowerPrompt.contains("ocean") || lowerPrompt.contains("ice") || lowerPrompt.contains("blue")) {
            primaryR = 15;
            primaryG = 60 + randomEngine.nextInt(90);
            primaryB = 210 + randomEngine.nextInt(45);
        } else if (lowerPrompt.contains("space") || lowerPrompt.contains("star") || lowerPrompt.contains("galaxy") || lowerPrompt.contains("purple")) {
            primaryR = 180 + randomEngine.nextInt(75);
            primaryG = 20;
            primaryB = 180 + randomEngine.nextInt(75);
        }

        int[][] colorMatrix = new int[width][height];
        int centerAxis = width / 2;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x <= centerAxis; x++) {
                // Decide pixel density ratios based on distance values to center line
                double axisOffset = Math.abs(centerAxis - x) / (double) centerAxis;
                double fillChance = 0.75 - (axisOffset * 0.45);

                if (randomEngine.nextDouble() < fillChance) {
                    // Generate subtle hue variations
                    int varR = Math.max(0, Math.min(255, primaryR + randomEngine.nextInt(50) - 25));
                    int varG = Math.max(0, Math.min(255, primaryG + randomEngine.nextInt(50) - 25));
                    int varB = Math.max(0, Math.min(255, primaryB + randomEngine.nextInt(50) - 25));
                    int finalColor = Color.rgb(varR, varG, varB);

                    colorMatrix[x][y] = finalColor;
                    colorMatrix[width - 1 - x][y] = finalColor; // Horizontally mirror color pixels
                } else {
                    // Randomly assign structured dark space border blocks
                    if (randomEngine.nextDouble() < 0.15) {
                        int borderCol = Color.rgb(40, 40, 48);
                        colorMatrix[x][y] = borderCol;
                        colorMatrix[width - 1 - x][y] = borderCol;
                    } else {
                        colorMatrix[x][y] = Color.TRANSPARENT;
                        colorMatrix[width - 1 - x][y] = Color.TRANSPARENT;
                    }
                }
            }
        }

        // Apply grid coordinates to target bitmap pixels
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixelGridBitmap.setPixel(x, y, colorMatrix[x][y]);
            }
        }

        // Scale up bitmap with nearest-neighbor scaling to maintain retro-style pixel sharpness
        return Bitmap.createScaledBitmap(pixelGridBitmap, 400, 400, false);
    }
}