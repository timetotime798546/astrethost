package com.javawebserver.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    // Server Config
    private static final int SERVER_PORT = 3399;
    private ServerSocket mServerSocket;
    private ExecutorService mThreadPool;
    private boolean mIsServerRunning = false;
    private File mWebRootDir;

    // GUI UI Components
    private TextView mTxtIp;
    private TextView mTxtStatus;
    private Button mBtnStartStop;
    private TextView mTxtLogs;
    private ScrollView mScrollLogs;

    // Tabs & Dynamic Panels
    private Button mTabServer;
    private Button mTabFiles;
    private LinearLayout mPanelServer;
    private LinearLayout mPanelFiles;

    // File Manager UI
    private ListView mListFiles;
    private Button mBtnNewFile;
    private ArrayList<File> mFileList;
    private FileAdapter mFileAdapter;

    // Code Editor UI
    private LinearLayout mPanelEditor;
    private TextView mEditorFileName;
    private EditText mEditFileContent;
    private Button mBtnEditorSave;
    private Button mBtnEditorCancel;
    private File mCurrentEditingFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWebRootDir = new File(getFilesDir(), "web_root");
        if (!mWebRootDir.exists()) {
            mWebRootDir.mkdirs();
            createInitialMockupFiles();
        }

        initializeViews();
        setupActionListeners();
        updateNetworkIp();
        refreshFilesList();
    }

    private void initializeViews() {
        mTxtIp = (TextView) findViewById(R.id.txt_ip);
        mTxtStatus = (TextView) findViewById(R.id.txt_status);
        mBtnStartStop = (Button) findViewById(R.id.btn_start_stop);
        mTxtLogs = (TextView) findViewById(R.id.txt_logs);
        mScrollLogs = (ScrollView) findViewById(R.id.scroll_logs);

        mTabServer = (Button) findViewById(R.id.tab_server);
        mTabFiles = (Button) findViewById(R.id.tab_files);
        mPanelServer = (LinearLayout) findViewById(R.id.panel_server);
        mPanelFiles = (LinearLayout) findViewById(R.id.panel_files);

        mListFiles = (ListView) findViewById(R.id.list_files);
        mBtnNewFile = (Button) findViewById(R.id.btn_new_file);

        mPanelEditor = (LinearLayout) findViewById(R.id.panel_editor);
        mEditorFileName = (TextView) findViewById(R.id.editor_file_name);
        mEditFileContent = (EditText) findViewById(R.id.edit_file_content);
        mBtnEditorSave = (Button) findViewById(R.id.btn_editor_save);
        mBtnEditorCancel = (Button) findViewById(R.id.btn_editor_cancel);

        mFileList = new ArrayList<>();
        mFileAdapter = new FileAdapter();
        mListFiles.setAdapter(mFileAdapter);
    }

    private void setupActionListeners() {
        // Toggle Server Button
        mBtnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsServerRunning) {
                    stopServer();
                } else {
                    startServer();
                }
            }
        });

        // Tab Switching Actions
        mTabServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(true);
            }
        });

        mTabFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(false);
            }
        });

        // Create File Dialog Action
        mBtnNewFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptCreateFileDialog();
            }
        });

        // Code Editor Save
        mBtnEditorSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFileContent();
            }
        });

        // Code Editor Cancel
        mBtnEditorCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPanelEditor.setVisibility(View.GONE);
                mCurrentEditingFile = null;
            }
        });
    }

    private void switchTab(boolean showServer) {
        if (showServer) {
            mPanelServer.setVisibility(View.VISIBLE);
            mPanelFiles.setVisibility(View.GONE);
            mTabServer.setTextColor(0xFF1A237E);
            mTabServer.setTypeface(null, android.graphics.Typeface.BOLD);
            mTabFiles.setTextColor(0xFF757575);
            mTabFiles.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else {
            mPanelServer.setVisibility(View.GONE);
            mPanelFiles.setVisibility(View.VISIBLE);
            mTabFiles.setTextColor(0xFF1A237E);
            mTabFiles.setTypeface(null, android.graphics.Typeface.BOLD);
            mTabServer.setTextColor(0xFF757575);
            mTabServer.setTypeface(null, android.graphics.Typeface.NORMAL);
            refreshFilesList();
        }
    }

    // Dynamic Server Actions
    private synchronized void startServer() {
        if (mIsServerRunning) return;

        mIsServerRunning = true;
        mTxtStatus.setText("ONLINE (PORT " + SERVER_PORT + ")");
        mTxtStatus.setTextColor(0xFF2E7D32);
        mBtnStartStop.setText("STOP");
        mBtnStartStop.setBackgroundColor(0xFFD32F2F);

        // Scale thread pool to handle peak traffic load securely
        mThreadPool = Executors.newCachedThreadPool();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mServerSocket = new ServerSocket(SERVER_PORT);
                    logMessage("Server listening on port " + SERVER_PORT);

                    while (mIsServerRunning) {
                        try {
                            final Socket clientSocket = mServerSocket.accept();
                            mThreadPool.execute(new Runnable() {
                                @Override
                                public void run() {
                                    handleClientRequest(clientSocket);
                                }
                            });
                        } catch (Exception e) {
                            // Socket closed or exception triggered by closing loop
                        }
                    }
                } catch (Exception e) {
                    logMessage("Server exception: " + e.getMessage());
                }
            }
        }).start();

        logMessage("Server started successfully.");
    }

    private synchronized void stopServer() {
        if (!mIsServerRunning) return;

        mIsServerRunning = false;
        mTxtStatus.setText("OFFLINE (PORT " + SERVER_PORT + ")");
        mTxtStatus.setTextColor(0xFFD32F2F);
        mBtnStartStop.setText("START");
        mBtnStartStop.setBackgroundColor(0xFF2E7D32);

        try {
            if (mServerSocket != null) {
                mServerSocket.close();
            }
        } catch (Exception e) {
            // silent close
        }

        if (mThreadPool != null) {
            mThreadPool.shutdownNow();
        }

        logMessage("Server stopped.");
    }

    private void handleClientRequest(Socket socket) {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = socket.getInputStream();
            output = socket.getOutputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String firstLine = reader.readLine();
            if (firstLine == null) return;

            String[] requestParts = firstLine.split(" ");
            if (requestParts.length < 2) return;

            String method = requestParts[0];
            String path = requestParts[1];

            logMessage("GET -> " + path + " from " + socket.getInetAddress().getHostAddress());

            // Clean path
            if (path.contains("?")) {
                path = path.substring(0, path.indexOf("?"));
            }
            if (path.equals("/")) {
                path = "/index.html";
            }

            // Secure validation against Directory Traversal attacks
            if (path.contains("..") || path.contains("//")) {
                sendErrorResponse(output, 403, "Forbidden", "Access is restricted.");
                return;
            }

            File targetFile = new File(mWebRootDir, path.substring(1));
            if (!targetFile.exists() || targetFile.isDirectory()) {
                sendErrorResponse(output, 404, "Not Found", "File '" + path + "' could not be found on server.");
            } else {
                sendFileResponse(output, targetFile);
            }

        } catch (Exception e) {
            // connection drop or parser failure
        } finally {
            try {
                if (output != null) output.close();
                if (input != null) input.close();
                if (socket != null) socket.close();
            } catch (Exception e) {
                // silent close
            }
        }
    }

    private void sendFileResponse(OutputStream out, File file) {
        FileInputStream fileInput = null;
        try {
            fileInput = new FileInputStream(file);
            long fileLength = file.length();
            String mimeType = getContentType(file.getName());

            // Build HTTP headers
            String headers = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: " + mimeType + "\r\n" +
                    "Content-Length: " + fileLength + "\r\n" +
                    "Connection: close\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "\r\n";

            out.write(headers.getBytes("UTF-8"));

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fileInput.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        } catch (Exception e) {
            // failed mid-stream write
        } finally {
            if (fileInput != null) {
                try {
                    fileInput.close();
                } catch (Exception e) {
                    // silent close
                }
            }
        }
    }

    private void sendErrorResponse(OutputStream out, int statusCode, String statusText, String message) {
        try {
            String errorPage = "<!DOCTYPE html><html><head><title>" + statusCode + " " + statusText + "</title>" +
                    "<style>body{font-family:sans-serif;text-align:center;padding:50px;background:#FAFAFA;}h1{color:#D32F2F;}</style></head>" +
                    "<body><h1>" + statusCode + " " + statusText + "</h1><p>" + message + "</p><hr><p>Android Dynamic Server System</p></body></html>";

            byte[] errorBytes = errorPage.getBytes("UTF-8");
            String headers = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Content-Length: " + errorBytes.length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";

            out.write(headers.getBytes("UTF-8"));
            out.write(errorBytes);
            out.flush();
        } catch (Exception e) {
            // silent close
        }
    }

    private String getContentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=utf-8";
        if (lower.endsWith(".css")) return "text/css; charset=utf-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (lower.endsWith(".json")) return "application/json; charset=utf-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".txt")) return "text/plain; charset=utf-8";
        return "application/octet-stream";
    }

    // Logging helper with thread safety redirection
    private void logMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mTxtLogs != null) {
                    SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
                    String timestamp = timeFormatter.format(new Date());
                    mTxtLogs.append("[" + timestamp + "] " + message + "\n");

                    // Keep scrolled down automatically
                    mScrollLogs.post(new Runnable() {
                        @Override
                        public void run() {
                            mScrollLogs.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            }
        });
    }

    // Dynamic Network IP extraction
    private void updateNetworkIp() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                int ipAddress = connectionInfo.getIpAddress();
                String formattedIp = Formatter.formatIpAddress(ipAddress);
                if ("0.0.0.0".equals(formattedIp) || formattedIp.isEmpty()) {
                    mTxtIp.setText("IP: Loopback Only");
                } else {
                    mTxtIp.setText("IP: " + formattedIp);
                }
            }
        } catch (Exception e) {
            mTxtIp.setText("IP: Unavailable");
        }
    }

    // File Operations Helpers
    private void refreshFilesList() {
        mFileList.clear();
        File[] files = mWebRootDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    mFileList.add(file);
                }
            }
        }
        mFileAdapter.notifyDataSetChanged();
    }

    private void promptCreateFileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Web File");

        final EditText input = new EditText(this);
        input.setHint("e.g. index.html, style.css, app.js");
        input.setSingleLine(true);
        builder.setView(input);

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String fileName = input.getText().toString().trim();
                if (fileName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Filename cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                File newFile = new File(mWebRootDir, fileName);
                if (newFile.exists()) {
                    Toast.makeText(MainActivity.this, "File already exists", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    if (newFile.createNewFile()) {
                        Toast.makeText(MainActivity.this, "File created", Toast.LENGTH_SHORT).show();
                        refreshFilesList();
                        openFileInEditor(newFile);
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to create file", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void openFileInEditor(File file) {
        mCurrentEditingFile = file;
        mEditorFileName.setText("editing: " + file.getName());

        // Read Content
        try {
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int read;
            while ((read = fis.read(buf)) != -1) {
                bos.write(buf, 0, read);
            }
            fis.close();
            mEditFileContent.setText(bos.toString("UTF-8"));
        } catch (Exception e) {
            mEditFileContent.setText("");
            Toast.makeText(this, "Could not open file", Toast.LENGTH_SHORT).show();
        }

        mPanelEditor.setVisibility(View.VISIBLE);
    }

    private void saveFileContent() {
        if (mCurrentEditingFile == null) return;

        try {
            String content = mEditFileContent.getText().toString();
            FileOutputStream fos = new FileOutputStream(mCurrentEditingFile);
            fos.write(content.getBytes("UTF-8"));
            fos.close();
            Toast.makeText(this, "Saved successfully!", Toast.LENGTH_SHORT).show();
            mPanelEditor.setVisibility(View.GONE);
            mCurrentEditingFile = null;
            refreshFilesList();
        } catch (Exception e) {
            Toast.makeText(this, "Save error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void deleteFile(final File file) {
        new AlertDialog.Builder(this)
                .setTitle("Delete File")
                .setMessage("Are you sure you want to delete '" + file.getName() + "'?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (file.delete()) {
                            Toast.makeText(MainActivity.this, "Deleted file", Toast.LENGTH_SHORT).show();
                            refreshFilesList();
                        } else {
                            Toast.makeText(MainActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Default static template population inside root
    private void createInitialMockupFiles() {
        try {
            File index = new File(mWebRootDir, "index.html");
            if (!index.exists()) {
                FileOutputStream fos = new FileOutputStream(index);
                String template = "<!DOCTYPE html>\n" +
                        "<html>\n" +
                        "<head>\n" +
                        "    <meta charset=\"UTF-8\">\n" +
                        "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                        "    <title>Java Local Server</title>\n" +
                        "    <link rel=\"stylesheet\" href=\"style.css\">\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "    <div class=\"card\">\n" +
                        "        <h1>Hello from Native Android Server!</h1>\n" +
                        "        <p>This web page is hosted directly on your Android device on port <b>3399</b>.</p>\n" +
                        "        <p>You can edit these files dynamically from within the app and reload to see immediate updates.</p>\n" +
                        "        <button id=\"actionBtn\">Test Javascript Connection</button>\n" +
                        "        <p id=\"feedback\"></p>\n" +
                        "    </div>\n" +
                        "    <script src=\"script.js\"></script>\n" +
                        "</body>\n" +
                        "</html>";
                fos.write(template.getBytes("UTF-8"));
                fos.close();
            }

            File css = new File(mWebRootDir, "style.css");
            if (!css.exists()) {
                FileOutputStream fos = new FileOutputStream(css);
                String template = "body {\n" +
                        "    background: #eaeff2;\n" +
                        "    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;\n" +
                        "    display: flex;\n" +
                        "    align-items: center;\n" +
                        "    justify-content: center;\n" +
                        "    height: 100vh;\n" +
                        "    margin: 0;\n" +
                        "}\n" +
                        ".card {\n" +
                        "    background: #ffffff;\n" +
                        "    padding: 30px;\n" +
                        "    border-radius: 12px;\n" +
                        "    box-shadow: 0 4px 15px rgba(0,0,0,0.1);\n" +
                        "    text-align: center;\n" +
                        "    max-width: 450px;\n" +
                        "}\n" +
                        "h1 {\n" +
                        "    color: #1a237e;\n" +
                        "    font-size: 24px;\n" +
                        "    margin-bottom: 12px;\n" +
                        "}\n" +
                        "p {\n" +
                        "    color: #555555;\n" +
                        "    line-height: 1.6;\n" +
                        "}\n" +
                        "button {\n" +
                        "    background: #1a237e;\n" +
                        "    color: #ffffff;\n" +
                        "    border: none;\n" +
                        "    padding: 10px 20px;\n" +
                        "    border-radius: 6px;\n" +
                        "    font-size: 14px;\n" +
                        "    cursor: pointer;\n" +
                        "    transition: background 0.3s;\n" +
                        "}\n" +
                        "button:hover {\n" +
                        "    background: #0d1b2a;\n" +
                        "}\n" +
                        "#feedback {\n" +
                        "    margin-top: 15px;\n" +
                        "    font-weight: bold;\n" +
                        "    color: #2e7d32;\n" +
                        "}";
                fos.write(template.getBytes("UTF-8"));
                fos.close();
            }

            File js = new File(mWebRootDir, "script.js");
            if (!js.exists()) {
                FileOutputStream fos = new FileOutputStream(js);
                String template = "document.getElementById('actionBtn').addEventListener('click', function() {\n" +
                        "    document.getElementById('feedback').innerText = 'JavaScript connection confirmed and running seamlessly!';\n" +
                        "});";
                fos.write(template.getBytes("UTF-8"));
                fos.close();
            }
        } catch (Exception e) {
            // template initialization fallback
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopServer();
    }

    // Custom List View Adapter for File Manager Items
    private class FileAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mFileList.size();
        }

        @Override
        public Object getItem(int position) {
            return mFileList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            final File file = mFileList.get(position);
            TextView txtName = (TextView) convertView.findViewById(android.R.id.text1);
            TextView txtDetails = (TextView) convertView.findViewById(android.R.id.text2);

            txtName.setText(file.getName());
            txtName.setTextSize(16);
            txtName.setTextColor(0xFF212121);

            long bytes = file.length();
            txtDetails.setText(bytes + " Bytes - Long tap to Delete, Single tap to Edit");
            txtDetails.setTextColor(0xFF757575);

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openFileInEditor(file);
                }
            });

            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    deleteFile(file);
                    return true;
                }
            });

            return convertView;
        }
    }
}