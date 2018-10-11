package net.simplyrin.socialdownloader;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import club.sk1er.utils.HttpClient;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**
 * Created by SimplyRin on 2018/10/11.
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
public class Controller {

	@FXML
	private Button button;

	@FXML
	private TextField textField;

	@FXML
	private void onAction(ActionEvent event) {
		String url = this.textField.getText();
		if (url.equals("")) {
			this.buildAlert(Alert.AlertType.ERROR, "エラー", "URL を入力する必要があります。", null);
			return;
		}
		if (!(url.contains("nana-music.com") || url.contains("lispon.moe") || url.contains("tiktok.com") || url.contains("tiktokv.com"))) {
			this.buildAlert(Alert.AlertType.ERROR, "エラー", "対応している URL を入力する必要があります。", "- nana-music.com\n- lispon.moe\n- tiktok.com");
			return;
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
	}

	private void downloadNana(String url) {
		String content = HttpClient.rawWithAgent(url);
		if (!content.contains("build_sound_url")) {
			this.buildAlert(Alert.AlertType.ERROR, "エラー", "ファイルの検出に失敗しました。", null);
			return;
		}

		String title;
		try {
			url = "https://storage.nana-music.com/" + content.split(Pattern.quote("build_sound_url('"))[1].split(Pattern.quote("'));"))[0];
			System.out.println("Detected M4A URL: " + url);
			title = content.split("<title>")[1].split("</title>")[0];
		} catch (Exception e) {
			this.buildAlert(Alert.AlertType.ERROR, "エラー", "URL の解析に失敗しました。", null);
			return;
		}

		System.out.println("Title: " + title);

		title = title.replace("\\", "");
		title = title.replace("/", "");
		title = title.replace("|", "-");

		File directory = this.openDirectoryChooser("保存場所を選択してダウンロード");
		if (directory == null) {
			this.buildAlert(Alert.AlertType.ERROR, "エラー", "保存場所の取得に失敗しました", null);
			return;
		}

		File file = new File(directory, title + ".m4a");
		try {
			FileUtils.copyURLToFile(new URL(url), file);
			System.out.println("ダウンロードが完了しました！");
			this.buildAlert(Alert.AlertType.INFORMATION, "完了", "ファイルのダウンロードが完了しました。", "ファイル名: " + file.getName());
		} catch (Exception e) {
			StackTrace stackTrace = new StackTrace();
			e.printStackTrace(stackTrace);
			this.buildAlert(Alert.AlertType.ERROR, "エラー", "ファイルのダウンロードに失敗しました。", "エラー:\n\n" + stackTrace.getContent());
		}
	}

	private void downloadLispon(String url) {
		if (!url.contains("qaId=")) {
			this.buildAlert(Alert.AlertType.ERROR, "エラー", "ファイルの検出に失敗しました。", null);
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
			this.buildAlert(Alert.AlertType.ERROR, "エラー", "URL の解析に失敗しました。", null);
			return;
		}

		File directory = this.openDirectoryChooser("保存場所を選択してダウンロード");
		if (directory == null) {
			this.buildAlert(Alert.AlertType.ERROR, "エラー", "保存場所の取得に失敗しました", null);
			return;
		}

		File file = new File(directory, title + ".mp3");
		try {
			FileUtils.copyURLToFile(new URL(mp3), file);
			System.out.println("ダウンロードが完了しました: " + file.getName());
			this.buildAlert(Alert.AlertType.INFORMATION, "完了", "ファイルのダウンロードが完了しました。", "ファイル名: " + file.getName());
		} catch (Exception e) {
			StackTrace stackTrace = new StackTrace();
			e.printStackTrace(stackTrace);
			this.buildAlert(Alert.AlertType.ERROR, "エラー", "ファイルのダウンロードに失敗しました。", "エラー:\n\n" + stackTrace.getContent());
		}
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
			this.buildAlert(Alert.AlertType.ERROR, "エラー", "URL の解析に失敗しました。", null);
			return;
		}

		File directory = this.openDirectoryChooser("保存場所を選択してダウンロード");
		if (directory == null) {
			this.buildAlert(Alert.AlertType.ERROR, "エラー", "保存場所の取得に失敗しました", null);
			return;
		}

		File file = new File(directory, "tiktok_" + videoId + ".mp4");
		try {
			FileUtils.copyURLToFile(new URL(url), file);
			System.out.println("ダウンロードが完了しました！");
			this.buildAlert(Alert.AlertType.INFORMATION, "完了", "ファイルのダウンロードが完了しました。", "ファイル名: " + file.getName());
		} catch (Exception e) {
			StackTrace stackTrace = new StackTrace();
			e.printStackTrace(stackTrace);
			this.buildAlert(Alert.AlertType.ERROR, "エラー", "ファイルのダウンロードに失敗しました。", "エラー:\n\n" + stackTrace.getContent());
		}
	}

	private void buildAlert(Alert.AlertType alertyType, String title, String headerText, String content) {
		this.textField.clear();

		Stage primaryStage = (Stage) this.button.getScene().getWindow();

		Alert alert = new Alert(alertyType);
		Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();

		try {
			stage.getIcons().add(primaryStage.getIcons().get(0));
		} catch (Exception e) {
		}

		alert.setTitle(title);
		alert.setHeaderText(headerText);
		if (content != null) {
			alert.setContentText(content);
		}
		alert.show();
	}

	private File openDirectoryChooser(String title) {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(title);

		return directoryChooser.showDialog(this.button.getScene().getWindow());
	}

}
