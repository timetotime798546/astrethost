package com.htmltoapkconverterstudio.app;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    // Primary UI Tabs
    private Button mTabEditor, mTabPreview, mTabTemplates, mTabGuide;
    private View mIndEditor, mIndPreview, mIndTemplates, mIndGuide;
    private View mContEditor, mContPreview, mContTemplates, mContGuide;

    // Interactive Fields
    private EditText mEditorField;
    private WebView mWebView;
    private TextView mPreviewStatus;
    private ListView mTemplatesList;

    // Presets Template Storage
    private static final String[] TEMPLATE_NAMES = {
        "🎮 Coin Clicker Web Game",
        "📝 Responsive Access Form",
        "✨ Physics Particles Canvas",
        "📱 Flexbox Device Grid Layout"
    };

    private static final String[] TEMPLATE_DESCRIPTIONS = {
        "Fun custom coin clicker built with responsive V8 runtime listeners, localized scale changes, and custom styling.",
        "Beautiful login widget showing dark system gradients, validation handlers, and simulated real-time checks.",
        "Interactive canvas background animation loop configured to run smoothly inside web sandbox layers.",
        "Modern grid layout that fluidly snaps cards cleanly on various device viewports."
    };

    private static final String DEFAULT_EDITOR_CODE = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "  <style>\n" +
            "    body {\n" +
            "      font-family: system-ui, sans-serif;\n" +
            "      background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);\n" +
            "      color: white;\n" +
            "      text-align: center;\n" +
            "      padding: 30px 15px;\n" +
            "      margin: 0;\n" +
            "      min-height: 100vh;\n" +
            "      box-sizing: border-box;\n" +
            "    }\n" +
            "    .card {\n" +
            "      background: rgba(255, 255, 255, 0.12);\n" +
            "      backdrop-filter: blur(12px);\n" +
            "      border-radius: 16px;\n" +
            "      padding: 25px;\n" +
            "      max-width: 380px;\n" +
            "      margin: 0 auto;\n" +
            "      box-shadow: 0 8px 32px 0 rgba(0,0,0,0.3);\n" +
            "      border: 1px solid rgba(255,255,255,0.2);\n" +
            "    }\n" +
            "    h1 { margin: 0 0 10px 0; font-size: 24px; }\n" +
            "    p { font-size: 14px; opacity: 0.95; line-height: 1.4; }\n" +
            "    .btn {\n" +
            "      background: #00E5FF;\n" +
            "      color: #0f172a;\n" +
            "      border: none;\n" +
            "      padding: 12px 30px;\n" +
            "      font-size: 15px;\n" +
            "      font-weight: bold;\n" +
            "      border-radius: 25px;\n" +
            "      cursor: pointer;\n" +
            "      margin-top: 20px;\n" +
            "      transition: transform 0.1s;\n" +
            "    }\n" +
            "    .btn:active { transform: scale(0.95); }\n" +
            "    .counter { font-size: 48px; font-weight: bold; margin: 15px 0; color: #00E5FF; }\n" +
            "  </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "  <div class=\"card\">\n" +
            "    <h1>HTML to App Simulator</h1>\n" +
            "    <p>This is a simulated Android WebView output container! Modify the HTML code on the left tab and hit Run!</p>\n" +
            "    <div class=\"counter\" id=\"count_val\">0</div>\n" +
            "    <button class=\"btn\" onclick=\"increment()\">Perform Action</button>\n" +
            "  </div>\n" +
            "  <script>\n" +
            "    var value = 0;\n" +
            "    function increment() {\n" +
            "      value++;\n" +
            "      document.getElementById('count_val').innerText = value;\n" +
            "    }\n" +
            "  </script>\n" +
            "</body>\n" +
            "</html>";

    private static final String TEMPLATE_CLICKER = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "  <style>\n" +
            "    body {\n" +
            "      background: #0f172a;\n" +
            "      color: #f8fafc;\n" +
            "      font-family: system-ui, sans-serif;\n" +
            "      text-align: center;\n" +
            "      margin: 0;\n" +
            "      padding: 20px;\n" +
            "    }\n" +
            "    .game-container {\n" +
            "      max-width: 400px;\n" +
            "      margin: 30px auto;\n" +
            "      background: #1e293b;\n" +
            "      padding: 25px;\n" +
            "      border-radius: 20px;\n" +
            "      box-shadow: 0 10px 25px rgba(0,0,0,0.5);\n" +
            "    }\n" +
            "    .coin {\n" +
            "      width: 140px;\n" +
            "      height: 140px;\n" +
            "      background: radial-gradient(circle, #facc15 0%, #ca8a04 100%);\n" +
            "      border-radius: 50%;\n" +
            "      margin: 30px auto;\n" +
            "      box-shadow: 0 0 20px #eab308;\n" +
            "      cursor: pointer;\n" +
            "      display: flex;\n" +
            "      justify-content: center;\n" +
            "      align-items: center;\n" +
            "      font-size: 50px;\n" +
            "      user-select: none;\n" +
            "      transition: transform 0.1s;\n" +
            "      border: 6px solid #fef08a;\n" +
            "    }\n" +
            "    .coin:active {\n" +
            "      transform: scale(0.9);\n" +
            "    }\n" +
            "    h1 { margin: 0; font-size: 26px; color: #fbbf24; }\n" +
            "    .score-board { font-size: 22px; font-weight: bold; margin-top: 10px; }\n" +
            "  </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "  <div class=\"game-container\">\n" +
            "    <h1>Gold Clicker App</h1>\n" +
            "    <div class=\"score-board\">Coins Collected: <span id=\"coins\">0</span></div>\n" +
            "    <div class=\"coin\" onclick=\"clickCoin()\">🪙</div>\n" +
            "    <p style=\"color: #94a3b8; font-size: 13px;\">Tap the gold coin rapidly inside this sandboxed environment.</p>\n" +
            "  </div>\n" +
            "  <script>\n" +
            "    let coinCount = 0;\n" +
            "    function clickCoin() {\n" +
            "      coinCount++;\n" +
            "      document.getElementById('coins').innerText = coinCount;\n" +
            "    }\n" +
            "  </script>\n" +
            "</body>\n" +
            "</html>";

    private static final String TEMPLATE_FORM = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "  <style>\n" +
            "    body {\n" +
            "      background: #111827;\n" +
            "      color: #f3f4f6;\n" +
            "      font-family: system-ui, sans-serif;\n" +
            "      margin: 0;\n" +
            "      display: flex;\n" +
            "      justify-content: center;\n" +
            "      align-items: center;\n" +
            "      min-height: 100vh;\n" +
            "    }\n" +
            "    .card {\n" +
            "      background: #1f2937;\n" +
            "      border-radius: 12px;\n" +
            "      padding: 30px;\n" +
            "      width: 90%;\n" +
            "      max-width: 380px;\n" +
            "      box-shadow: 0 4px 15px rgba(0,0,0,0.3);\n" +
            "    }\n" +
            "    h2 { margin-top: 0; color: #10b981; }\n" +
            "    input {\n" +
            "      width: 100%;\n" +
            "      padding: 10px;\n" +
            "      margin: 8px 0 16px 0;\n" +
            "      border: 1px solid #4b5563;\n" +
            "      border-radius: 6px;\n" +
            "      background: #374151;\n" +
            "      color: white;\n" +
            "      box-sizing: border-box;\n" +
            "    }\n" +
            "    button {\n" +
            "      width: 100%;\n" +
            "      padding: 12px;\n" +
            "      background: #10b981;\n" +
            "      border: none;\n" +
            "      color: white;\n" +
            "      font-weight: bold;\n" +
            "      border-radius: 6px;\n" +
            "      cursor: pointer;\n" +
            "    }\n" +
            "    button:hover { background: #059669; }\n" +
            "  </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "  <div class=\"card\">\n" +
            "    <h2>Register Account</h2>\n" +
            "    <label>User Handle</label>\n" +
            "    <input type=\"text\" id=\"username\" placeholder=\"developer\">\n" +
            "    <label>Email Address</label>\n" +
            "    <input type=\"email\" id=\"email\" placeholder=\"dev@htmltoapk.com\">\n" +
            "    <button onclick=\"validate()\">Register</button>\n" +
            "  </div>\n" +
            "  <script>\n" +
            "    function validate() {\n" +
            "      let user = document.getElementById('username').value;\n" +
            "      let email = document.getElementById('email').value;\n" +
            "      if (!user || !email) {\n" +
            "        alert('Fields cannot be blank!');\n" +
            "        return;\n" +
            "      }\n" +
            "      if (!email.includes('@')) {\n" +
            "        alert('Invalid email syntax!');\n" +
            "        return;\n" +
            "      }\n" +
            "      alert('Success! Registration recorded for ' + user);\n" +
            "    }\n" +
            "  </script>\n" +
            "</body>\n" +
            "</html>";

    private static final String TEMPLATE_PARTICLES = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "  <style>\n" +
            "    body, html {\n" +
            "      margin: 0;\n" +
            "      padding: 0;\n" +
            "      background: #000000;\n" +
            "      overflow: hidden;\n" +
            "      color: white;\n" +
            "      font-family: sans-serif;\n" +
            "    }\n" +
            "    #canvas {\n" +
            "      display: block;\n" +
            "      position: absolute;\n" +
            "      top: 0;\n" +
            "      left: 0;\n" +
            "    }\n" +
            "    .overlay {\n" +
            "      position: absolute;\n" +
            "      top: 20px;\n" +
            "      left: 20px;\n" +
            "      pointer-events: none;\n" +
            "    }\n" +
            "  </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "  <canvas id=\"canvas\"></canvas>\n" +
            "  <div class=\"overlay\">\n" +
            "    <h3 style=\"margin:0;\">HTML5 Canvas Physics</h3>\n" +
            "    <p style=\"font-size:12px; opacity:0.75; margin:5px 0 0 0;\">Fluid animation drawing engine inside webview</p>\n" +
            "  </div>\n" +
            "  <script>\n" +
            "    const canvas = document.getElementById('canvas');\n" +
            "    const ctx = canvas.getContext('2d');\n" +
            "    canvas.width = window.innerWidth;\n" +
            "    canvas.height = window.innerHeight;\n" +
            "    \n" +
            "    let particles = [];\n" +
            "    class Particle {\n" +
            "      constructor() {\n" +
            "        this.x = Math.random() * canvas.width;\n" +
            "        this.y = Math.random() * canvas.height;\n" +
            "        this.size = Math.random() * 4 + 1.5;\n" +
            "        this.speedX = Math.random() * 2.2 - 1.1;\n" +
            "        this.speedY = Math.random() * 2.2 - 1.1;\n" +
            "      }\n" +
            "      update() {\n" +
            "        this.x += this.speedX;\n" +
            "        this.y += this.speedY;\n" +
            "        if (this.x > canvas.width || this.x < 0) this.speedX *= -1;\n" +
            "        if (this.y > canvas.height || this.y < 0) this.speedY *= -1;\n" +
            "      }\n" +
            "      draw() {\n" +
            "        ctx.fillStyle = '#00E5FF';\n" +
            "        ctx.beginPath();\n" +
            "        ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);\n" +
            "        ctx.fill();\n" +
            "      }\n" +
            "    }\n" +
            "    for (let i = 0; i < 45; i++) {\n" +
            "      particles.push(new Particle());\n" +
            "    }\n" +
            "    function animate() {\n" +
            "      ctx.clearRect(0,0, canvas.width, canvas.height);\n" +
            "      particles.forEach(p => {\n" +
            "        p.update();\n" +
            "        p.draw();\n" +
            "      });\n" +
            "      requestAnimationFrame(animate);\n" +
            "    }\n" +
            "    animate();\n" +
            "  </script>\n" +
            "</body>\n" +
            "</html>";

    private static final String TEMPLATE_GRID = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "  <style>\n" +
            "    body {\n" +
            "      background: #f1f5f9;\n" +
            "      color: #334155;\n" +
            "      font-family: system-ui, sans-serif;\n" +
            "      margin: 0;\n" +
            "      padding: 15px;\n" +
            "    }\n" +
            "    .header {\n" +
            "      background: #1e293b;\n" +
            "      color: white;\n" +
            "      padding: 15px;\n" +
            "      border-radius: 12px;\n" +
            "      margin-bottom: 15px;\n" +
            "    }\n" +
            "    .grid {\n" +
            "      display: flex;\n" +
            "      flex-wrap: wrap;\n" +
            "      gap: 15px;\n" +
            "    }\n" +
            "    .card {\n" +
            "      flex: 1 1 calc(50% - 15px);\n" +
            "      background: white;\n" +
            "      padding: 15px;\n" +
            "      border-radius: 10px;\n" +
            "      box-shadow: 0 4px 10px rgba(0,0,0,0.04);\n" +
            "      box-sizing: border-box;\n" +
            "    }\n" +
            "    @media (max-width: 480px) {\n" +
            "      .card { flex: 1 1 100%; }\n" +
            "    }\n" +
            "    h3 { margin-top: 0; color: #1e293b; }\n" +
            "  </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "  <div class=\"header\">\n" +
            "    <h2 style=\"margin:0;\">Dashboard App</h2>\n" +
            "    <span style=\"font-size:12px;opacity:0.8;\">Responsive grid container layout</span>\n" +
            "  </div>\n" +
            "  <div class=\"grid\">\n" +
            "    <div class=\"card\">\n" +
            "      <h3>V8 Service Engine</h3>\n" +
            "      <p>Performance: Optimal</p>\n" +
            "      <p>Network Link: Active</p>\n" +
            "    </div>\n" +
            "    <div class=\"card\">\n" +
            "      <h3>Hardware Profile</h3>\n" +
            "      <p>Allocated RAM: 14MB</p>\n" +
            "      <p>Platform status: SIMULATION</p>\n" +
            "    </div>\n" +
            "  </div>\n" +
            "</body>\n" +
            "</html>";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind Nav Tabs
        mTabEditor = findViewById(R.id.tab_editor);
        mTabPreview = findViewById(R.id.tab_preview);
        mTabTemplates = findViewById(R.id.tab_templates);
        mTabGuide = findViewById(R.id.tab_guide);

        mIndEditor = findViewById(R.id.indicator_editor);
        mIndPreview = findViewById(R.id.indicator_preview);
        mIndTemplates = findViewById(R.id.indicator_templates);
        mIndGuide = findViewById(R.id.indicator_guide);

        // Bind Screen Containers
        mContEditor = findViewById(R.id.container_editor);
        mContPreview = findViewById(R.id.container_preview);
        mContTemplates = findViewById(R.id.container_templates);
        mContGuide = findViewById(R.id.container_guide);

        // Bind Input/View Blocks
        mEditorField = findViewById(R.id.editor_field);
        mWebView = findViewById(R.id.preview_webview);
        mPreviewStatus = findViewById(R.id.preview_status);
        mTemplatesList = findViewById(R.id.templates_list);

        // Populate Sandbox WebView
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        mWebView.setWebViewClient(new WebViewClient());
        mWebView.setWebChromeClient(new WebChromeClient());

        // Fill Default Editor Space
        mEditorField.setText(DEFAULT_EDITOR_CODE);

        // Configure Programmatic Controls
        initSnippetBar();
        setupTabs();
        setupTemplates();

        // Bind Operation Buttons
        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditorField.setText("");
                Toast.makeText(MainActivity.this, "Editor space cleared", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_run).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String htmlCode = mEditorField.getText().toString();
                if (htmlCode.trim().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please write HTML code first!", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Use requested CARTO base url for map layer access compatibility
                mWebView.loadDataWithBaseURL("https://carto.com", htmlCode, "text/html", "UTF-8", null);
                mPreviewStatus.setText("ACTIVE SANDBOX");
                mPreviewStatus.setTextColor(0xFF00E676);
                
                // Render view to preview space
                switchTab(1);
            }
        });
    }

    private void setupTabs() {
        mTabEditor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });

        mTabPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        mTabTemplates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
                hideSoftKeyboard();
            }
        });

        mTabGuide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3);
                hideSoftKeyboard();
            }
        });
    }

    private void switchTab(int index) {
        // Clear all states
        mContEditor.setVisibility(View.GONE);
        mContPreview.setVisibility(View.GONE);
        mContTemplates.setVisibility(View.GONE);
        mContGuide.setVisibility(View.GONE);

        mIndEditor.setBackgroundColor(0xFF283593);
        mIndPreview.setBackgroundColor(0xFF283593);
        mIndTemplates.setBackgroundColor(0xFF283593);
        mIndGuide.setBackgroundColor(0xFF283593);

        mTabEditor.setTextColor(0xFF8C9EFF);
        mTabPreview.setTextColor(0xFF8C9EFF);
        mTabTemplates.setTextColor(0xFF8C9EFF);
        mTabGuide.setTextColor(0xFF8C9EFF);

        switch (index) {
            case 0:
                mContEditor.setVisibility(View.VISIBLE);
                mIndEditor.setBackgroundColor(0xFF00E5FF);
                mTabEditor.setTextColor(0xFFFFFFFF);
                break;
            case 1:
                mContPreview.setVisibility(View.VISIBLE);
                mIndPreview.setBackgroundColor(0xFF00E5FF);
                mTabPreview.setTextColor(0xFFFFFFFF);
                break;
            case 2:
                mContTemplates.setVisibility(View.VISIBLE);
                mIndTemplates.setBackgroundColor(0xFF00E5FF);
                mTabTemplates.setTextColor(0xFFFFFFFF);
                break;
            case 3:
                mContGuide.setVisibility(View.VISIBLE);
                mIndGuide.setBackgroundColor(0xFF00E5FF);
                mTabGuide.setTextColor(0xFFFFFFFF);
                break;
        }
    }

    private void initSnippetBar() {
        LinearLayout snippetsLayout = findViewById(R.id.snippets_layout);

        final String[] tags = {"<html>", "<body>", "<script>", "<style>", "<h1>", "<button>", "<div>", "CSS Grid"};
        final String[] insertValues = {
            "<html>\n  \n</html>",
            "<body>\n  \n</body>",
            "<script>\n  \n</script>",
            "<style>\n  \n</style>",
            "<h1>Heading</h1>",
            "<button class=\"btn\" onclick=\"\">Button</button>",
            "<div>\n  \n</div>",
            "display: grid;\ngrid-template-columns: repeat(2, 1fr);\ngap: 10px;"
        };

        for (int i = 0; i < tags.length; i++) {
            final String valueToInsert = insertValues[i];
            Button button = new Button(this);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(6, 2, 6, 2);
            button.setLayoutParams(params);
            button.setText(tags[i]);
            button.setTextSize(10);
            button.setTransformationMethod(null);
            button.setBackgroundColor(0xFF37474F);
            button.setTextColor(0xFFFFFFFF);
            button.setPadding(12, 2, 12, 2);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int startSelection = mEditorField.getSelectionStart();
                    int endSelection = mEditorField.getSelectionEnd();
                    mEditorField.getText().replace(
                        Math.min(startSelection, endSelection),
                        Math.max(startSelection, endSelection),
                        valueToInsert, 0, valueToInsert.length()
                    );
                }
            });

            snippetsLayout.addView(button);
        }
    }

    private void setupTemplates() {
        mTemplatesList.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return TEMPLATE_NAMES.length;
            }

            @Override
            public Object getItem(int position) {
                return TEMPLATE_NAMES[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                LinearLayout layout;
                if (convertView == null) {
                    layout = new LinearLayout(MainActivity.this);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setPadding(24, 20, 24, 20);
                    layout.setBackgroundColor(0xFFFFFFFF);

                    TextView titleView = new TextView(MainActivity.this);
                    titleView.setTag("title");
                    titleView.setTextSize(15);
                    titleView.setTextColor(0xFF1A237E);
                    titleView.setTypeface(null, Typeface.BOLD);

                    TextView descView = new TextView(MainActivity.this);
                    descView.setTag("desc");
                    descView.setTextSize(12);
                    descView.setTextColor(0xFF546E7A);
                    descView.setPadding(0, 4, 0, 0);

                    layout.addView(titleView);
                    layout.addView(descView);
                } else {
                    layout = (LinearLayout) convertView;
                }

                TextView title = (TextView) layout.findViewWithTag("title");
                TextView desc = (TextView) layout.findViewWithTag("desc");

                title.setText(TEMPLATE_NAMES[position]);
                desc.setText(TEMPLATE_DESCRIPTIONS[position]);

                return layout;
            }
        });

        mTemplatesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String templateCode = "";
                switch (position) {
                    case 0:
                        templateCode = TEMPLATE_CLICKER;
                        break;
                    case 1:
                        templateCode = TEMPLATE_FORM;
                        break;
                    case 2:
                        templateCode = TEMPLATE_PARTICLES;
                        break;
                    case 3:
                        templateCode = TEMPLATE_GRID;
                        break;
                }
                mEditorField.setText(templateCode);
                Toast.makeText(MainActivity.this, TEMPLATE_NAMES[position] + " injected to IDE", Toast.LENGTH_SHORT).show();
                switchTab(0);
            }
        });
    }

    private void hideSoftKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View currentFocus = getCurrentFocus();
        if (currentFocus != null && inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
    }
}