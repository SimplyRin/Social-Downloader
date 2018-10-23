using Microsoft.WindowsAPICodePack.Dialogs;
using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Windows.Forms;

/**
 * Created by SimplyRin on 2018/10/14.
 * 
 * Copyright (C) 2018 SimplyRin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
namespace SocialDownloader {

    public partial class Form1 : Form {
   
        public Form1() {
            InitializeComponent();

            this.MaximumSize = this.Size;
            this.MinimumSize = this.Size;

            this.MaximizeBox = false;

            String staticUrl = "https://static.simplyrin.net/Microsoft.WindowsAPICodePack/";
            this.copyURLtoFile(staticUrl + "Microsoft.WindowsAPICodePack.dll", "Microsoft.WindowsAPICodePack.dll");
            this.copyURLtoFile(staticUrl + "Microsoft.WindowsAPICodePack.pdb", "Microsoft.WindowsAPICodePack.pdb");
            this.copyURLtoFile(staticUrl + "Microsoft.WindowsAPICodePack.Shell.dll", "Microsoft.WindowsAPICodePack.Shell.dll");
            this.copyURLtoFile(staticUrl + "Microsoft.WindowsAPICodePack.Shell.pdb", "Microsoft.WindowsAPICodePack.Shell.pdb");
        }

        private void textBox1_TextChanged(object sender, EventArgs e) {
        }

        private void button1_Click(object sender, EventArgs e) {
            String url = textBox1.Text;
            if (url.Equals("")) {
                this.buildAlert(MessageBoxIcon.Error, "エラー", "URL を入力する必要があります。");
                return;
            }
            if (!(url.Contains("nana-music.com") || 
                url.Contains("lispon.moe") || 
                url.Contains("tiktok.com") || 
                url.Contains("tiktokv.com") || 
                url.Contains("store.line.me/stickershop/product/") ||
                url.Contains("clips.twitch.tv/"))) {
                this.buildAlert(MessageBoxIcon.Error, "エラー", "対応している URL を入力する必要があります。\n\n- nana-music.com\n- lispon.moe\n- tiktok.com\n- store.line.me");
                return;
            }

            if (url.Contains("nana-music.com")) {
                this.downloadNana(url);
            }

            if (url.Contains("lispon.moe")) {
                this.downloadLispon(url);
            }

            if (url.Contains("tiktok.com") || url.Contains("tiktokv.com")) {
                this.downloadTikTok(url);
            }

            if (url.Contains("store.line.me/stickershop/product/")) {
                this.downloadLineSticker(url);
            }

            if (url.Contains("clips.twitch.tv/")) {
                this.downloadTwitchClip(url);
            }
        }

        private void downloadNana(String url) {
            String content = HttpClient.rawWithAgent(url);
            if (!content.Contains("build_sound_url")) {
                this.buildAlert(MessageBoxIcon.Error, "エラー", "ファイルの検出に失敗しました。");
                return;
            }

            String title;
            try {
                String[] a1 = this.split(content, "build_sound_url('");
                String[] a2 = this.split(a1[1], "'));");
                url = "https://storage.nana-music.com/" + a2[0];
                title = this.split(content, "<title>")[1];
                title = this.split(title, "</title>")[0];
            } catch (Exception) {
                this.buildAlert(MessageBoxIcon.Error, "エラー", "URL の解析に失敗しました。");
                return;
            }

            Console.WriteLine("Title: " + title);

            title = title.Replace("\\", "");
            title = title.Replace("/", "");
            title = title.Replace("|", "-");

            String directory = this.openDirectoryChooser();
            if (directory == null) {
                this.buildAlert(MessageBoxIcon.Error, "エラー", "保存場所の取得に失敗しました");
                return;
            }

            bool response = this.copyURLtoFile(url, directory + "\\" + title + ".m4a");
            if (response) {
                this.buildAlert(MessageBoxIcon.Information, "完了", "ファイルのダウンロードが完了しました。\n\nファイル名: " + title + ".m4a");
            } else {
                this.buildAlert(MessageBoxIcon.Information, "エラー", "ファイルのダウンロードに失敗しました。");
            }
        }

        private void downloadLispon(String url) {
            if (!url.Contains("qaId=")) {
                this.buildAlert(MessageBoxIcon.Error, "エラー", "ファイルの検出に失敗しました。");
                return;
            }

            String qaId = this.split(url, "qaId=")[1];
            if (qaId.Contains("&")) {
                qaId = this.split(qaId, "&")[0];
            }

            Console.WriteLine("Detected qaId: " + qaId);
            url = "http://lispon.moe/lispon/vaqa/getQAInfo?qaId=" + qaId;

            String result = HttpClient.rawWithAgent(url);

            String title = qaId;
            try {
                title += " - " + this.split(result, "aUserNick\":\"")[1];
                title = this.split(title, "\",\"")[0];
            } catch (Exception) {
            }

            String mp3;
            try {
                mp3 = this.split(result, "aVoice\":\"")[1];
                mp3 = this.split(mp3, "\",\"")[0];
            } catch (Exception) {
                this.buildAlert(MessageBoxIcon.Error, "エラー", "URL の解析に失敗しました。");
                return;
            }
            
            String directory = this.openDirectoryChooser();
            if (directory == null) {
                this.buildAlert(MessageBoxIcon.Error, "エラー", "保存場所の取得に失敗しました");
                return;
            }

            bool response = this.copyURLtoFile(mp3, directory + "\\" + title + ".mp3");
            if (response) {
                this.buildAlert(MessageBoxIcon.Information, "完了", "ファイルのダウンロードが完了しました。\n\nファイル名: " + title + ".mp3");
            } else {
                this.buildAlert(MessageBoxIcon.Information, "エラー", "ファイルのダウンロードに失敗しました。");
            }
        }

        private void downloadTikTok(String url) {
            String content = HttpClient.rawWithAgent(url);

            content = content.Replace("\n", "");
            content = content.Replace("\"", "");
            content = content.Replace("”", "");
            content = content.Replace(" ", "");

            String videoId = this.split(content, "video_id=")[1];
            videoId = this.split(videoId, "line=0")[0];
            videoId = videoId.Replace("\\u0026", "");

            String mp4 = "https://api.tiktokv.com/aweme/v1/playwm/?video_id=" + videoId;

            String directory = this.openDirectoryChooser();
            if (directory == null) {
                this.buildAlert(MessageBoxIcon.Error, "エラー", "保存場所の取得に失敗しました");
                return;
            }

            bool response = this.copyURLtoFile(mp4, directory + "\\" + videoId + ".mp4");
            if (response) {
                this.buildAlert(MessageBoxIcon.Information, "完了", "ファイルのダウンロードが完了しました。\n\nファイル名: " + videoId + ".mp4");
            } else {
                this.buildAlert(MessageBoxIcon.Information, "エラー", "ファイルのダウンロードに失敗しました。");
            }
        }

        private void downloadLineSticker(String url) {
            String content = HttpClient.rawWithAgent(url);

            String title = this.split(content, " - LINE スタンプ | LINE STORE")[0];
            title = this.split(title, "<title>")[1];

            if (!content.Contains("https://stickershop.line-scdn.net/stickershop/v1/sticker/")) {
                this.buildAlert(MessageBoxIcon.Error, "エラー", "スタンプの解析に失敗しました。");
                return;
            }

            List<String> ids = new List<String>();
            foreach (String args in this.split(content, "https://stickershop.line-scdn.net/stickershop/v1/sticker/")) {
                String id = "0";
                try {
                    id = this.split(args, "/")[0];
                } catch (Exception) {
                }
                
                if (!id.Equals("0")) {
                    Console.WriteLine("ID: " + id);
                    ids.Add(this.split(args, "/")[0]);
                }
            }

            String directory = this.openDirectoryChooser();
            if (directory == null) {
                this.buildAlert(MessageBoxIcon.Error, "エラー", "保存場所の取得に失敗しました");
                return;
            }
            directory += "\\" + title;
            Directory.CreateDirectory(directory);

            foreach (String id in ids) {
                url = "https://stickershop.line-scdn.net/stickershop/v1/sticker/" + id + "/ANDROID/sticker.png";
                this.copyURLtoFile(url, directory + "\\" + id + ".png");
            }

            this.buildAlert(MessageBoxIcon.Information, "完了", "ファイルのダウンロードが完了しました。\n\nフォルダ名: " + title);
        }

        private void downloadTwitchClip(String url) {
            if (url.Contains("?")) {
                url = this.split(url, "?")[0];
            }

            String clipId = this.split(url, "clips.twitch.tv/")[1];

            url = "https://clips.twitch.tv/api/v2/clips/" + clipId + "/status";

            String content = HttpClient.rawWithAgent(url);

            url = this.split(content, "source\":\"")[1];
            url = this.split(url, "\",\"")[0];

            String directory = this.openDirectoryChooser();
            if (directory == null) {
                this.buildAlert(MessageBoxIcon.Error, "エラー", "保存場所の取得に失敗しました");
                return;
            }

            bool response = this.copyURLtoFile(url, directory + "\\" + clipId + ".mp4");
            if (response) {
                this.buildAlert(MessageBoxIcon.Information, "完了", "ファイルのダウンロードが完了しました。\n\nファイル名: " + clipId + ".mp4");
            } else {
                this.buildAlert(MessageBoxIcon.Information, "エラー", "ファイルのダウンロードに失敗しました。");
            }
        }

        private String[] split(String args, String split) {
            String[] a1 = { split };
            return args.Split(a1, StringSplitOptions.None);
        }

        private void buildAlert(MessageBoxIcon messageBoxIcon, String title, String message) {
            this.textBox1.Clear();
            MessageBox.Show(message, title, MessageBoxButtons.OK, messageBoxIcon);
        }

        private String openDirectoryChooser() {
            CommonOpenFileDialog commonOpenFileDialog = new CommonOpenFileDialog("保存場所を選択してダウンロード");
            commonOpenFileDialog.IsFolderPicker = true;
            if (commonOpenFileDialog.ShowDialog() == CommonFileDialogResult.Ok) {
                return commonOpenFileDialog.FileName;
            } else {
                return null;
            }
        }

        private bool copyURLtoFile(String url, String path) {
            WebClient webClient = new WebClient();
            webClient.Headers.Add("User-Agent", "Mozilla/5.0");
            try {
                webClient.DownloadFile(url, path);
                webClient.Dispose();
                return true;
            } catch (Exception e) {
                Console.WriteLine("URL: " + url + "\nPath: " + path);
                Console.WriteLine("Error: " + e.Message);
                return false;
            }
        }

    }

    public class HttpClient {

        public static String rawWithAgent(String url) {
            return rawWithAgent(url, "Mozilla/5.0");
        }

        public static String rawWithAgent(String url, String userAgent) {
            Console.WriteLine("Connecting to " + url + ".");

            if (!url.StartsWith("http://") && !url.StartsWith("https://")) {
                url = "http://" + url;
            }

            HttpWebRequest httpWebRequest = (HttpWebRequest)WebRequest.Create(url);
            httpWebRequest.UserAgent = userAgent;
            httpWebRequest.Method = "GET";

            HttpWebResponse httpWebResponse = (HttpWebResponse)httpWebRequest.GetResponse();

            Stream stream = httpWebResponse.GetResponseStream();
            StreamReader streamReader = new StreamReader(stream);

            return streamReader.ReadToEnd();
        }

    }

}
