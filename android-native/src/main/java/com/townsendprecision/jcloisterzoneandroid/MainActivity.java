package com.townsendprecision.jcloisterzoneandroid;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Base64;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class MainActivity extends Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private EditText host;
    private EditText port;
    private EditText outgoing;
    private Spinner mode;
    private TextView log;
    private Button send;
    private Button close;
    private BoardView board;
    private volatile RawTcpClient rawClient;
    private volatile SimpleWebSocket wsClient;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
    }

    @Override protected void onDestroy() {
        closeClients();
        super.onDestroy();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 16, 16, 16);
        root.setBackgroundColor(Color.rgb(244, 247, 241));

        TextView title = text("JCloisterZone Android", 22, true);
        TextView subtitle = text("First Java APK milestone: touch board shell + remote game connection plumbing.", 13, false);
        root.addView(title);
        root.addView(subtitle);

        board = new BoardView(this);
        root.addView(board, new LinearLayout.LayoutParams(-1, 0, 1.15f));

        LinearLayout playButtons = new LinearLayout(this);
        playButtons.setOrientation(LinearLayout.HORIZONTAL);
        Button undo = new Button(this);
        undo.setText("Undo");
        Button rotate = new Button(this);
        rotate.setText("Rotate");
        Button fit = new Button(this);
        fit.setText("Fit");
        playButtons.addView(undo, new LinearLayout.LayoutParams(0, -2, 1));
        playButtons.addView(rotate, new LinearLayout.LayoutParams(0, -2, 1));
        playButtons.addView(fit, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(playButtons);

        host = input("PC host/IP, e.g. 192.168.1.20");
        host.setSingleLine(true);
        host.setText("192.168.1.");
        port = input("Port");
        port.setSingleLine(true);
        port.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        port.setText("9000");
        mode = new Spinner(this);
        mode.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Raw Java engine TCP", "JCZ desktop WebSocket"}));
        outgoing = input("Protocol message/command to send");
        outgoing.setMinLines(2);
        outgoing.setText("%version");
        Button connect = new Button(this);
        connect.setText("Connect");
        send = new Button(this);
        send.setText("Send");
        send.setEnabled(false);
        close = new Button(this);
        close.setText("Close");
        close.setEnabled(false);
        LinearLayout networkButtons = new LinearLayout(this);
        networkButtons.setOrientation(LinearLayout.HORIZONTAL);
        networkButtons.addView(connect, new LinearLayout.LayoutParams(0, -2, 1));
        networkButtons.addView(send, new LinearLayout.LayoutParams(0, -2, 1));
        networkButtons.addView(close, new LinearLayout.LayoutParams(0, -2, 1));

        root.addView(label("Host"));
        root.addView(host);
        root.addView(label("Port"));
        root.addView(port);
        root.addView(label("Connection mode"));
        root.addView(mode);
        root.addView(label("Outgoing"));
        root.addView(outgoing);
        root.addView(networkButtons);

        log = text("Ready. Drag board to pan. Pinch to zoom. Long-press tile to rotate.\n", 12, false);
        log.setTextIsSelectable(true);
        ScrollView scroller = new ScrollView(this);
        scroller.addView(log);
        root.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 0.85f));

        setContentView(root);

        connect.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { doConnect(); }});
        send.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { doSend(); }});
        close.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { closeClients(); append("Closed."); }});
        undo.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { board.undo(); sendProtocol("UNDO"); }});
        rotate.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { board.rotateCurrentTile(); sendProtocol("ROTATE"); }});
        fit.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { board.resetView(); }});
    }

    private TextView label(String s) {
        TextView v = text(s, 12, true);
        v.setPadding(0, 10, 0, 0);
        return v;
    }

    private TextView text(String s, int sp, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(Color.rgb(20, 35, 30));
        if (bold) v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return v;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(false);
        return e;
    }

    private Logger logger() {
        return new Logger() { public void log(String s) { append(s); }};
    }

    private void doConnect() {
        closeClients();
        final String h = host.getText().toString().trim();
        final int p;
        try {
            p = Integer.parseInt(port.getText().toString().trim());
        } catch (NumberFormatException ex) {
            append("Port must be a number.");
            return;
        }
        append("Connecting to " + h + ":" + p + " as " + mode.getSelectedItem() + "...");
        new Thread(new Runnable() { public void run() {
            try {
                if (mode.getSelectedItemPosition() == 0) {
                    RawTcpClient c = new RawTcpClient(h, p, logger());
                    c.connect();
                    rawClient = c;
                } else {
                    SimpleWebSocket c = new SimpleWebSocket(h, p, logger());
                    c.connect();
                    wsClient = c;
                    c.send(defaultHello());
                }
                ui.post(new Runnable() { public void run() { send.setEnabled(true); close.setEnabled(true); }});
                append("Connected.");
            } catch (Exception ex) {
                append("CONNECT ERROR: " + ex);
            }
        }}).start();
    }

    private String defaultHello() {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String clientId = UUID.randomUUID().toString();
        return "{\"id\":\"" + id + "\",\"type\":\"HELLO\",\"payload\":{" +
                "\"appVersion\":\"5.12.1\",\"engineVersion\":\"5-SNAPSHOT\"," +
                "\"protocolVersion\":\"5.8.0\",\"name\":\"Android\",\"clientId\":\"" + clientId + "\",\"secret\":null}}";
    }

    private void doSend() {
        sendProtocol(outgoing.getText().toString());
    }

    private void sendProtocol(final String msg) {
        new Thread(new Runnable() { public void run() {
            try {
                if (rawClient != null) rawClient.send(msg);
                else if (wsClient != null) wsClient.send(msg);
                else append("Not connected; local UI action recorded only: " + msg);
            } catch (Exception ex) {
                append("SEND ERROR: " + ex);
            }
        }}).start();
    }

    private void closeClients() {
        try { if (rawClient != null) rawClient.close(); } catch (Exception ignored) {}
        try { if (wsClient != null) wsClient.close(); } catch (Exception ignored) {}
        rawClient = null;
        wsClient = null;
        ui.post(new Runnable() { public void run() { if (send != null) send.setEnabled(false); if (close != null) close.setEnabled(false); }});
    }

    private void append(final String s) {
        ui.post(new Runnable() { public void run() { if (log != null) log.append("\n" + s); }});
    }

    public static class BoardView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final ScaleGestureDetector scaleDetector;
        private final GestureDetector gestureDetector;
        private final List<Integer> rotations = new ArrayList<Integer>();
        private float scale = 1.0f;
        private float panX = 0f;
        private float panY = 0f;
        private float lastX;
        private float lastY;
        private boolean dragging;
        private int currentRotation;

        public BoardView(final Activity context) {
            super(context);
            setBackgroundColor(Color.rgb(228, 235, 220));
            scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScale(ScaleGestureDetector detector) {
                    scale *= detector.getScaleFactor();
                    if (scale < 0.35f) scale = 0.35f;
                    if (scale > 4.0f) scale = 4.0f;
                    invalidate();
                    return true;
                }
            });
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override public void onLongPress(MotionEvent e) {
                    rotateCurrentTile();
                }
                @Override public boolean onDown(MotionEvent e) { return true; }
            });
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            scaleDetector.onTouchEvent(event);
            gestureDetector.onTouchEvent(event);
            if (event.getPointerCount() > 1) {
                dragging = false;
                return true;
            }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = event.getX();
                    lastY = event.getY();
                    dragging = true;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (dragging) {
                        float x = event.getX();
                        float y = event.getY();
                        panX += x - lastX;
                        panY += y - lastY;
                        lastX = x;
                        lastY = y;
                        invalidate();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    dragging = false;
                    return true;
                default:
                    return true;
            }
        }

        public void rotateCurrentTile() {
            rotations.add(Integer.valueOf(currentRotation));
            currentRotation = (currentRotation + 90) % 360;
            invalidate();
        }

        public void undo() {
            if (!rotations.isEmpty()) {
                currentRotation = rotations.remove(rotations.size() - 1).intValue();
            }
            invalidate();
        }

        public void resetView() {
            scale = 1.0f;
            panX = 0f;
            panY = 0f;
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.save();
            canvas.translate(getWidth() / 2f + panX, getHeight() / 2f + panY);
            canvas.scale(scale, scale);
            drawGrid(canvas);
            drawPlacedTiles(canvas);
            drawCurrentTile(canvas);
            canvas.restore();
            paint.setColor(Color.rgb(40, 65, 50));
            paint.setTextSize(28f);
            canvas.drawText("Pinch zoom: " + String.format(java.util.Locale.US, "%.2f", scale) + "x  |  Current rotation: " + currentRotation + "°", 18, 34, paint);
        }

        private void drawGrid(Canvas canvas) {
            paint.setStrokeWidth(2f);
            paint.setColor(Color.rgb(190, 205, 180));
            for (int i = -8; i <= 8; i++) {
                canvas.drawLine(i * 96, -8 * 96, i * 96, 8 * 96, paint);
                canvas.drawLine(-8 * 96, i * 96, 8 * 96, i * 96, paint);
            }
        }

        private void drawPlacedTiles(Canvas canvas) {
            int[][] pts = new int[][]{{0,0},{1,0},{0,1},{-1,0},{0,-1}};
            for (int i = 0; i < pts.length; i++) {
                drawTile(canvas, pts[i][0] * 96, pts[i][1] * 96, (i * 90) % 360, false);
            }
        }

        private void drawCurrentTile(Canvas canvas) {
            drawTile(canvas, 2 * 96, 0, currentRotation, true);
        }

        private void drawTile(Canvas canvas, int x, int y, int rotation, boolean current) {
            canvas.save();
            canvas.translate(x, y);
            canvas.rotate(rotation);
            RectF r = new RectF(-44, -44, 44, 44);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(current ? Color.rgb(246, 214, 116) : Color.rgb(238, 229, 189));
            canvas.drawRoundRect(r, 8, 8, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f);
            paint.setColor(Color.rgb(70, 94, 65));
            canvas.drawRoundRect(r, 8, 8, paint);
            paint.setStrokeWidth(10f);
            paint.setColor(Color.rgb(90, 120, 72));
            canvas.drawLine(-44, 0, 44, 0, paint);
            paint.setColor(Color.rgb(120, 120, 120));
            canvas.drawLine(0, -44, 0, 44, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(60, 70, 120));
            canvas.drawCircle(0, 0, 10, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.restore();
        }
    }

    private static class RawTcpClient {
        private final String host;
        private final int port;
        private final Logger logger;
        private Socket socket;
        private BufferedWriter out;
        RawTcpClient(String host, int port, Logger logger) { this.host = host; this.port = port; this.logger = logger; }
        void connect() throws Exception {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 5000);
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
            new Thread(new Runnable() { public void run() { readLoop(); }}).start();
        }
        void send(String msg) throws Exception { out.write(msg); out.write("\n"); out.flush(); logger.log("> " + msg); }
        void readLoop() {
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                String line;
                while ((line = r.readLine()) != null) logger.log("< " + line);
            } catch (Exception e) { logger.log("TCP closed: " + e.getMessage()); }
        }
        void close() throws Exception { if (socket != null) socket.close(); }
    }

    private static class SimpleWebSocket {
        private final String host;
        private final int port;
        private final Logger logger;
        private Socket socket;
        private InputStream in;
        private OutputStream out;
        SimpleWebSocket(String host, int port, Logger logger) { this.host = host; this.port = port; this.logger = logger; }
        void connect() throws Exception {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 5000);
            in = socket.getInputStream();
            out = socket.getOutputStream();
            String key = Base64.encodeToString(UUID.randomUUID().toString().getBytes("UTF-8"), Base64.NO_WRAP);
            String req = "GET / HTTP/1.1\r\nHost: " + host + ":" + port + "\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: " + key + "\r\nSec-WebSocket-Version: 13\r\n\r\n";
            out.write(req.getBytes("UTF-8"));
            out.flush();
            ByteArrayOutputStream head = new ByteArrayOutputStream();
            int b;
            int matched = 0;
            byte[] end = new byte[]{13,10,13,10};
            while ((b = in.read()) != -1) {
                head.write(b);
                matched = (b == end[matched]) ? matched + 1 : 0;
                if (matched == 4) break;
            }
            String hs = head.toString("UTF-8");
            if (hs.indexOf(" 101 ") < 0) throw new IOException("WebSocket handshake failed: " + hs.split("\\r?\\n")[0]);
            new Thread(new Runnable() { public void run() { readLoop(); }}).start();
        }
        void send(String msg) throws Exception {
            byte[] data = msg.getBytes("UTF-8");
            ByteArrayOutputStream frame = new ByteArrayOutputStream();
            frame.write(0x81);
            int len = data.length;
            if (len < 126) frame.write(0x80 | len);
            else if (len <= 65535) { frame.write(0x80 | 126); frame.write((len >> 8) & 255); frame.write(len & 255); }
            else throw new IOException("Frame too large");
            byte[] mask = new byte[4];
            new Random().nextBytes(mask);
            frame.write(mask);
            for (int i = 0; i < data.length; i++) frame.write(data[i] ^ mask[i % 4]);
            out.write(frame.toByteArray());
            out.flush();
            logger.log("> " + msg);
        }
        void readLoop() {
            try {
                while (true) {
                    int b0 = in.read();
                    if (b0 < 0) break;
                    int b1 = in.read();
                    if (b1 < 0) break;
                    int opcode = b0 & 15;
                    int len = b1 & 127;
                    if (len == 126) len = (in.read() << 8) | in.read();
                    else if (len == 127) throw new IOException("Large frame unsupported");
                    byte[] payload = new byte[len];
                    int off = 0;
                    while (off < len) {
                        int n = in.read(payload, off, len - off);
                        if (n < 0) throw new EOFException();
                        off += n;
                    }
                    if (opcode == 1) logger.log("< " + new String(payload, "UTF-8"));
                    else if (opcode == 8) break;
                }
            } catch (Exception e) { logger.log("WS closed: " + e.getMessage()); }
        }
        void close() throws Exception { if (socket != null) socket.close(); }
    }

    private interface Logger { void log(String s); }
}
