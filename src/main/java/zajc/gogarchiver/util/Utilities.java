//SPDX-License-Identifier: GPL-3.0
/*
 * gogarchiver-ng, an archival tool for GOG.com
 * Copyright (C) 2024 Marko Zajc
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program. If not, see <https://www.gnu.org/licenses/>.
 */
package zajc.gogarchiver.util;

import static java.lang.System.err;
import static picocli.CommandLine.Help.defaultColorScheme;

import java.util.stream.*;

import javax.annotation.*;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.json.JSONArray;
import picocli.CommandLine.Help.*;

public class Utilities {

	private static boolean enableVerbose = false;
	private static ColorScheme colorScheme;

	public static void setVerbose(boolean verbose) {
		enableVerbose = verbose;
	}

	public static void setColorMode(@Nonnull Ansi ansi) {
		colorScheme = defaultColorScheme(ansi);
	}

	public static void println(@Nullable Object text) {
		err.println(colorScheme.text(String.valueOf(text)));
	}

	public static void printf(@Nonnull String format, @Nonnull Object... args) {
		err.print(colorScheme.text(format.formatted(args)));
	}

	public static void warn(@Nonnull String text, @Nonnull Object... args) {
		println("@|yellow [W]|@ " + text.formatted(args));
	}

	public static void verbose(@Nonnull String text, @Nonnull Object... args) {
		if (enableVerbose)
			println("@|faint [V]|@ " + text.formatted(args));
	}

	public static <T> HttpResponse<T> checkResponse(String url, HttpResponse<T> resp) {
		if (!resp.isSuccess())
			throw new RuntimeException("Got a bad HTTP response on %s: %d %s".formatted(url, resp.getStatus(),
																						resp.getStatusText()));

		return resp;
	}

	@Nonnull
	@SuppressWarnings("null")
	public static Stream<Object> stream(JSONArray array) {
		return StreamSupport.stream(array.spliterator(), false);
	}

	public static void cursorUp() {
		err.print("\u001b[1A");
	}

	private Utilities() {}

}
