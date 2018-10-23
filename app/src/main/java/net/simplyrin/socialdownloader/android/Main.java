package net.simplyrin.socialdownloader.android;

import android.Manifest;
import android.content.ClipboardManager;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends AppCompatActivity {

    private Button button;
    private ClipboardManager.OnPrimaryClipChangedListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main);
        this.setSupportActionBar((Toolbar) this.findViewById(R.id.toolbar));
        try {
            PackageInfo packageInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            this.setTitle("SocialDownloader v" + packageInfo.versionName);
        } catch (Exception e) {
        }

        try {
            this.checkPermission();
        } catch (Exception e) {
            this.sendActionBar(e.getMessage());
        }

        final ClipboardManager clipboardManager = (ClipboardManager) this.getSystemService(CLIPBOARD_SERVICE);
        if (this.listener == null) {
            this.listener = new ClipboardManager.OnPrimaryClipChangedListener() {
                @Override
                public void onPrimaryClipChanged() {
                    ThreadPool.run(new Runnable() {
                        @Override
                        public void run() {
                            Main.this.run(String.valueOf(clipboardManager.getText()), false);
                        }
                    });
                }
            };
            clipboardManager.addPrimaryClipChangedListener(this.listener);
        }

        final EditText editText = Main.this.findViewById(R.id.textField);
        this.button = this.findViewById(R.id.button);
        this.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ThreadPool.run(new Runnable() {
                    @Override
                    public void run() {
                        Main.this.run(editText.getText().toString(), true);
                    }
                });
            }
        });
    }

    private void run(String url, boolean alert) {
        if (url.equals("")) {
            if (alert) {
                this.sendActionBar("エラー\nURL を入力する必要があります。");
            }
            return;
        }
        if (!(url.contains("nana-music.com") ||
                url.contains("lispon.moe") ||
                url.contains("tiktok.com") ||
                url.contains("tiktokv.com") ||
                url.contains("store.line.me/stickershop/product/") ||
                url.contains("clips.twitch.tv/"))) {
            if (alert) {
                this.sendActionBar("エラー\n対応している URL を入力する必要があります。\n\n- nana-music.com\n- lispon.moe\n- tiktok.com\n- store.line.me");
            }
            return;
        } else {
            this.sendActionBar("URL を解析しています...。");
        }

        Pattern pattern = Pattern.compile(
                "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
                        + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
                        + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

        Matcher matcher = pattern.matcher(url);
        while (matcher.find()) {
            url = url.substring(matcher.start(1), matcher.end());
        }

        if (url.contains("nana-music.com")) {
            this.downloadNana(url);
        }

        if (url.contains("lispon.moe")) {
            this.downloadLispon(url);
        }

        if (url.contains("tiktok.com") || url.contains("tiktokv.com")) {
            this.downloadTikTok(url);
        }

        if (url.contains("store.line.me/stickershop/product/")) {
            this.downloadLineSticker(url);
        }

        if (url.contains("clips.twitch.tv/")) {
            this.downloadTwitchClip(url);
        }
    }

    private void downloadNana(String url) {
        String content = HttpClient.rawWithAgent(url);
        if (content == null) {
            this.sendActionBar("An error occured!");
            return;
        }
        if (!content.contains("build_sound_url")) {
            this.sendActionBar("エラー\nファイルの検出に失敗しました。");
            return;
        }

        String title;
        try {
            url = "https://storage.nana-music.com/" + content.split(Pattern.quote("build_sound_url('"))[1].split(Pattern.quote("'));"))[0];
            System.out.println("Detected M4A URL: " + url);
            title = content.split("<title>")[1].split("</title>")[0];
        } catch (Exception e) {
            this.sendActionBar("エラー\nURL の解析に失敗しました。");
            return;
        }

        System.out.println("Title: " + title);

        title = title.replace("\\", "");
        title = title.replace("/", "");
        title = title.replace("|", "-");

        File file = new File("/storage/emulated/0/Download", title + ".m4a");
        this.copyURLtoFile(url, file);
    }

    private void downloadLispon(String url) {
        if (!url.contains("qaId=")) {
            this.sendActionBar("エラー\nファイルの検出に失敗しました。");
            return;
        }

        String qaId = url.split(Pattern.quote("qaId="))[1];
        if (qaId.contains("&")) {
            qaId = qaId.split("&")[0];
        }

        System.out.println("Detected qaId: " + qaId);
        url = "http://lispon.moe/lispon/vaqa/getQAInfo?qaId=" + qaId;

        String result = HttpClient.rawWithAgent(url);

        JsonObject jsonObject;
        try {
            jsonObject = new JsonParser().parse(result).getAsJsonObject().get("data").getAsJsonObject();
        } catch (Exception e) {
            this.sendActionBar("エラー\nファイルの解析に失敗しました。");
            return;
        }

        String title = qaId;
        try {
            title = String.valueOf(jsonObject.get("id").getAsInt());
            title += " - " + jsonObject.get("aUserNick").getAsString();
        } catch (Exception e) {
        }

        String mp3;
        try {
            mp3 = jsonObject.get("aVoice").getAsString();
        } catch (Exception e) {
            this.sendActionBar("エラー\nURL の解析に失敗しました。");
            return;
        }

        File file = new File("/storage/emulated/0/Download", title + ".mp3");
        this.copyURLtoFile(mp3, file);
    }

    private void downloadTikTok(String url) {
        String content = HttpClient.rawWithAgent(url);
        if(content.contains("Redirecting")) {
            System.out.println("Redirecting to target link...");

            String reconnectUrl = content.split("<a href=\"")[1].split("&amp;utm_source=copy_link")[0];
            content = HttpClient.rawWithAgent(reconnectUrl);
        }

        content = content.replace("\n", "");
        content = content.replace("\"", "");
        content = content.replace("”", "");
        content = content.replace(" ", "");

        String videoId = content.split("video_id=")[1].split("line=0")[0];
        videoId = videoId.replace("\\u0026", "");

        try {
            HttpURLConnection connection = HttpClient.getHttpURLConnection("https://api.tiktokv.com/aweme/v1/playwm/?video_id=" + videoId);
            url = connection.getHeaderField("Location");
            if(url == null) {
                new Exception();
            }
        } catch (Exception e) {
            this.sendActionBar("エラー\nURL の解析に失敗しました。");
            return;
        }

        File file = new File("/storage/emulated/0/Download", videoId + ".mp4");
        this.copyURLtoFile(url, file);
    }

    private void downloadLineSticker(String url) {
        String content = HttpClient.rawWithAgent(url);

        String title = content.split(" - LINE スタンプ | LINE STORE")[0].split("<title>")[1];
        System.out.println("スタンプ名: " + title);

        if (!content.contains("https://stickershop.line-scdn.net/stickershop/v1/sticker/")) {
            this.sendActionBar("エラー\nスタンプの解析に失敗しました。");
            return;
        }

        List<Integer> ids = new ArrayList<>();
        for (String args : content.split("https://stickershop.line-scdn.net/stickershop/v1/sticker/")) {
            int id = 0;
            try {
                id = Integer.valueOf(args.split("/")[0]).intValue();
            } catch (Exception e) {
            }

            if (id != 0) {
                System.out.println("ID: " + id);
                ids.add(id);
            }
        }

        File file = new File("/storage/emulated/0/Download");
        File directory = new File(file, title);
        if (!directory.exists()) {
            directory.mkdir();
        }

        for (Integer integer : ids) {
            this.copyURLtoFile("https://stickershop.line-scdn.net/stickershop/v1/sticker/" + integer.intValue() + "/ANDROID/sticker.png",
                    new File(directory, integer.intValue() + ".png"), false);
        }

        this.sendActionBar("完了\nファイルのダウンロードが完了しました。\n\nフォルダ名: " + title);
    }

    private void downloadTwitchClip(String url) {
        if (url.contains(Pattern.quote("?"))) {
            url = url.split(Pattern.quote("?"))[0];
        }

        String clipId = url.split(Pattern.quote("clips.twitch.tv/"))[1];

        url = "https://clips.twitch.tv/api/v2/clips/" + clipId + "/status";

        String content = HttpClient.rawWithAgent(url);

        JsonArray jsonArray;
        JsonObject jsonObject;
        try {
            jsonArray = new JsonParser().parse(content).getAsJsonObject().get("quality_options").getAsJsonArray();
            jsonObject = jsonArray.get(0).getAsJsonObject();
        } catch (Exception e) {
            this.sendActionBar("失敗\n不明なエラーが発生しました。");
            return;
        }

        url = jsonObject.get("source").getAsString();

        File file = new File("/storage/emulated/0/Download", clipId + ".mp4");
        this.copyURLtoFile(url, file);
    }

    private boolean copyURLtoFile(String url, File file) {
        return this.copyURLtoFile(url, file, true);
    }

    private boolean copyURLtoFile(String url, File file, boolean showActionBar) {
        try {
            HttpURLConnection connection = HttpClient.getHttpURLConnection(url);
            FileUtils.copyInputStreamToFile(connection.getInputStream(), file);
            System.out.println("ダウンロードが完了しました！");
            if (showActionBar) {
                this.sendActionBar("完了\nファイルのダウンロードが完了しました。\n\nファイル名: " + file.getName());
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (showActionBar) {
                this.sendActionBar("エラー\nファイルのダウンロードに失敗しました。");
            }
            return false;
        }
    }

    private void setEnableButton(final boolean bool) {
        this.runWithMainThread(new Runnable() {
            @Override
            public void run() {
                Main.this.button.setEnabled(bool);
            }
        });
    }

    private void sendActionBar(final String content) {
        this.runWithMainThread(new Runnable() {
            @Override
            public void run() {
                final Toast toast = Toast.makeText(Main.this, content, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP, 0, 500);
                toast.show();
            }
        });
    }

    private void runWithMainThread(Runnable runnable) {
        this.runOnUiThread(runnable);
    }

    private void checkPermission() {
        String[] permissions = { Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE };
        ActivityCompat.requestPermissions(this, permissions, 5);
    }

}
