package net.simplyrin.socialdownloader;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import net.simplyrin.socialdownloader.utils.Version;

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
public class Main extends Application {

	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		Parent parent;
		try {
			parent = FXMLLoader.load(this.getClass().getResource("/main.fxml"));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		try {
			stage.getIcons().add(new Image(this.getClass().getResourceAsStream("/icon.png")));
		} catch (Exception e) {
		}

		stage.setTitle("Social Downloader v" + Version.POMVERSION);
		stage.sizeToScene();
		stage.setResizable(false);
		stage.setScene(new Scene(parent, 300, 110));
		stage.show();
	}

}
