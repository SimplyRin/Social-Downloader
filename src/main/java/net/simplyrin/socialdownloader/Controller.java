package net.simplyrin.socialdownloader;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import net.simplyrin.httpclient.HttpClient;
import net.simplyrin.multiprocess.MultiProcess;

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

	private final String NANA_MUSIC = "nana-music.com";
	private final String LISPON = "lispon.moe";
	private final String TIKTOK = "tiktok.com";
	private final String TIKTOKV = "tiktokv.com";
	private final String LINE = "store.line.me/stickershop/product";
	private final String TWITCH_CLIP = "clips.twitch.tv";
	private final String PIXIV = "https://www.pixiv.net";

	@FXML
	private void onAction(ActionEvent event) {
		String url = this.textField.getText().trim();
		if (url.equals("")) {
			this.buildAlert(Alert.AlertType.ERROR, "エラー", "URL を入力する必要があります。", null);
			return;
		}

		if (url.contains(this.NANA_MUSIC)) {
			this.downloadNana(url);
			return;
		}

		if (url.contains(this.LISPON)) {
			this.downloadLispon(url);
			return;
		}

		if (url.contains(this.TIKTOK) || url.contains(this.TIKTOKV)) {
			this.downloadTikTok(url);
			return;
		}

		if (url.contains(this.LINE)) {
			this.downloadLineSticker(url);
			return;
		}

		if (url.contains(this.TWITCH_CLIP)) {
			this.downloadTwitchClip(url);
			return;
		}

		if (url.contains(this.PIXIV)) {
			this.downloadPixiv(url);
			return;
		}

		this.buildAlert(Alert.AlertType.ERROR, "エラー", "このサービスは現在サポートされていません。", "- nana-music.com\n- lispon.moe\n- tiktok.com\n- store.line.me\n- clips.twitch.tv\n- pixiv.net");
	}

	private void downloadNana(String url) {
		String content = HttpClient.fetch(url);
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

		String result = HttpClient.fetch(url);

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
		String content = HttpClient.fetch(url);
		if(content.contains("Redirecting")) {
			System.out.println("Redirecting to target link...");

			String reconnectUrl = content.split("<a href=\"")[1].split("&amp;utm_source=copy_link")[0];
			content = HttpClient.fetch(reconnectUrl);
		}

		content = content.replace("\n", "");
		content = content.replace("\"", "");
		content = content.replace("”", "");
		content = content.replace(" ", "");

		String videoId = content.split("video_id=")[1].split("line=0")[0];
		videoId = videoId.replace("\\u0026", "");

		try {
			HttpURLConnection connection = HttpClient.getHttpURLConnection("https://api.tiktokv.com/aweme/v1/playwm/?video_id=" + videoId, null, null);
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

	private void downloadLineSticker(String url) {
		String content = HttpClient.fetch(url);

		String title = content.split(" - LINE スタンプ | LINE STORE")[0].split("<title>")[1];
		System.out.println("スタンプ名: " + title);

		if (!content.contains("https://stickershop.line-scdn.net/stickershop/v1/sticker/")) {
			this.buildAlert(Alert.AlertType.ERROR, "エラー", "スタンプの解析に失敗しました。", null);
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

		File directory = this.openDirectoryChooser("保存場所を選択してダウンロード");
		if (directory == null) {
			this.buildAlert(Alert.AlertType.ERROR, "エラー", "保存場所の取得に失敗しました", null);
			return;
		}

		directory = new File(directory, title);
		if (!directory.exists()) {
			directory.mkdir();
		}

		for (Integer integer : ids) {
			try {
				URL _Url = new URL("https://stickershop.line-scdn.net/stickershop/v1/sticker/" + integer.intValue() + "/ANDROID/sticker.png");
				FileUtils.copyURLToFile(_Url, new File(directory, integer.intValue() + ".png"));
				System.out.println(integer.intValue() + " のダウンロードに成功しました。");
			} catch (Exception e) {
				System.out.println(integer.intValue() + " のダウンロードに失敗しました。");
			}
		}

		this.buildAlert(Alert.AlertType.INFORMATION, "完了", "ファイルのダウンロードが完了しました。", "フォルダ名: " + title);
	}

	private void downloadTwitchClip(String url) {
		if (url.contains(Pattern.quote("?"))) {
			url = url.split(Pattern.quote("?"))[0];
		}

		String clipId = url.split(Pattern.quote("clips.twitch.tv/"))[1];

		url = "https://clips.twitch.tv/api/v2/clips/" + clipId + "/status";

		String content = HttpClient.fetch(url);

		JsonArray jsonArray = new JsonParser().parse(content).getAsJsonObject().get("quality_options").getAsJsonArray();
		JsonObject jsonObject = jsonArray.get(0).getAsJsonObject();

		url = jsonObject.get("source").getAsString();

		File directory = this.openDirectoryChooser("保存場所を選択してダウンロード");
		if (directory == null) {
			this.buildAlert(Alert.AlertType.ERROR, "エラー", "保存場所の取得に失敗しました", null);
			return;
		}

		File file = new File(directory, clipId + ".mp4");
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

	private void downloadPixiv(String url) {
		List<Integer> list = new ArrayList<>();

		int memberId = 0;

		if (url.contains("illust_id=")) {
			int id;
			try {
				id = Integer.valueOf(url.split("illust_id=")[1]);
			} catch (Exception e) {
				this.buildAlert(Alert.AlertType.ERROR, "エラー", "リンクの解析に失敗しました。", null);
				return;
			}
			list.add(id);
			System.out.println("Detected: " + id);
		} else if (url.replace("illust_id=", "").contains("id=")) {
			try {
				memberId = Integer.valueOf(url.split("id=")[1]);
			} catch (Exception e) {
				this.buildAlert(Alert.AlertType.ERROR, "エラー", "リンクの解析に失敗しました。", null);
				return;
			}
			JsonObject jsonObject = HttpClient.getAsJsonObject("https://www.pixiv.net/ajax/user/" + memberId + "/profile/all");
			JsonObject body = jsonObject.get("body").getAsJsonObject();
			Set<String> keys = body.get("illusts").getAsJsonObject().keySet();

			for (String key : keys) {
				try {
					list.add(Integer.valueOf(key));
				} catch (Exception e) {
				}
			}
		} else {
			this.buildAlert(Alert.AlertType.ERROR, "エラー", "イラストIDまたはメンバーIDのURLを貼り付けてください。", null);
			return;
		}

		File directory = this.openDirectoryChooser("保存場所を選択してダウンロード");
		if (directory == null) {
			this.buildAlert(Alert.AlertType.ERROR, "エラー", "保存場所の取得に失敗しました", null);
			return;
		}

		if (list.size() != 1) {
			directory = new File(directory, "pixiv_" + memberId);
		}

		List<Integer> failedDlImages = new ArrayList<>();

		File pixiv = new File("pixiv.json");

		boolean useProxy = false;
		File proxyFile = null;
		List<String> proxies = null;
		int uses = 0;

		if (pixiv.exists()) {
			JsonObject jsonObject = new JsonParser().parse(this.readAll(pixiv)).getAsJsonObject();

			useProxy = jsonObject.get("useProxy").getAsBoolean();
			if (useProxy) {
				proxyFile = new File(jsonObject.get("file").getAsString());
				if (proxyFile.exists()) {
					proxies = this.readLines(proxyFile);
				}
			}
		}

		MultiProcess multiProcess = new MultiProcess();

		for (Integer key : list) {
			HttpClient httpClient = new HttpClient("https://www.pixiv.net/member_illust.php?mode=medium&illust_id=" + key.intValue());
			httpClient.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36");
			String result = httpClient.getResult();

			result = result.replace("\"", "");
			result = result.replace("\\/", "/");

			result = result.replace(",", "");

			String imageUrl = null;
			for (String imgUrl : result.split("<img src=")) {
				imgUrl = imgUrl.split(" ")[0];

				if (imgUrl.contains(key + "_p0_")) {
					imageUrl = imgUrl;
				}
			}

			// https://i.pximg.net/c/600x600/img-master/img/2019/05/02/00/00/06/74502133_p0_master1200.jpg
			imageUrl = imageUrl.split("/img-master/")[1].split("_p0_")[0];
			imageUrl = "https://i.pximg.net/img-original/" + imageUrl + "_p0.png";

			System.out.println("Img: " + imageUrl);

			File file = new File(directory, key + ".png");

			final String u = imageUrl;
			final boolean us = useProxy;
			final List<String> pro = proxies;

			int added = 0;
			while (true) {
				if (added == 10) {
					break;
				}
				final int adf = added;
				multiProcess.addProcess(() -> this.dlTest(httpClient, u, us, pro, adf, file));
				added++;
			}

			/* try {
				httpClient = new HttpClient(imageUrl);
				httpClient.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36");

				if (useProxy) {
					String proxy = proxies.get(uses);
					System.out.println("with Proxy: " + proxy);
					httpClient.setProxy(new Proxy(Type.HTTP, new InetSocketAddress(proxy.split(":")[0], Integer.valueOf(proxy.split(":")[1]))));
					uses++;
				}

				FileUtils.copyInputStreamToFile(httpClient.getInputStream(), file);
			} catch (Exception e) {
				// e.printStackTrace();
				// failedDlImages.add(key.intValue());
			} */
		}

		multiProcess.updateMaxThread(32);
		multiProcess.setFinishedTask(() -> this.buildAlert(Alert.AlertType.INFORMATION, "完了", "ファイルのダウンロードが完了しました。", "ダウンロードできなかったファイル:\n\n" + failedDlImages.toString()) );
		multiProcess.start();

		if (failedDlImages.size() > 0) {
			//this.buildAlert(Alert.AlertType.ERROR, "エラー", "ファイルのダウンロードに失敗しました。", );
			return;
		} else {
			//this.buildAlert(Alert.AlertType.INFORMATION, "完了", "ファイルのダウンロードが完了しました。", null);
		}
	}

	private void dlTest(HttpClient httpClient, String imageUrl, boolean useProxy, List<String> proxies, int uses, File file) {
		try {
			httpClient = new HttpClient(imageUrl);
			httpClient.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36");

			if (useProxy) {
				String proxy = proxies.get(uses);
				System.out.println("with Proxy: " + proxy);
				httpClient.setProxy(new Proxy(Type.HTTP, new InetSocketAddress(proxy.split(":")[0], Integer.valueOf(proxy.split(":")[1]))));
				uses++;
			}

			FileUtils.copyInputStreamToFile(httpClient.getInputStream(), file);
		} catch (Exception e) {
			System.out.println("Failed. continue...");
			this.dlTest(httpClient, imageUrl, useProxy, proxies, uses, file);
			// e.printStackTrace();
			// failedDlImages.add(key.intValue());
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

	/**
	 * Created by @penguinshunya
	 * URL: https://qiita.com/penguinshunya/items/353bb1c555f337b0cf6d
	 */
	private String readAll(File file) {
		try {
			return Files.lines(Paths.get(file.getPath()), Charset.forName("UTF-8"))
					.collect(Collectors.joining(System.getProperty("line.separator")));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private List<String> readLines(File file) {
		Scanner scanner = null;
		try {
			scanner = new Scanner(file);
		} catch (Exception e) {
		}

		List<String> list = new ArrayList<>();
		while (scanner.hasNext()) {
			list.add(scanner.nextLine());
		}
		scanner.close();
		return list;
	}

}
