package net.simplyrin.socialdownloader;

import java.io.PrintStream;

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
public class StackTrace extends PrintStream {

	public StackTrace() {
		super(System.out);
	}

	private String content = "";

	@Override
	public void print(String content) {
		this.content += content + "\n";
	}

	public String getContent() {
		return this.content;
	}

}
