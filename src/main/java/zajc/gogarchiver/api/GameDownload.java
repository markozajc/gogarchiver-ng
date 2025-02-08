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
package zajc.gogarchiver.api;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.hash;
import static java.util.regex.Pattern.compile;
import static zajc.gogarchiver.api.GameDownload.Type.*;
import static zajc.gogarchiver.util.Utilities.warn;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.annotation.*;

import kong.unirest.core.json.JSONObject;
import me.tongfei.progressbar.ProgressBar;
import zajc.gogarchiver.util.LazyValue;

public record GameDownload(@Nonnull Game game, @Nonnull String originalUrl, @Nonnull LazyValue<String> resolvedUrl,
	@Nonnull Platform platform, @Nullable String name, @Nullable String version, @Nonnull Type type, int part) {

	private static final Pattern TYPE_PATTERN = compile("\\d+(\\p{IsLatin}+)");
	private static final Pattern PART_PATTERN = compile("\\d+$");

	@Nonnull
	public static GameDownload fromJson(@Nonnull Game game, @Nonnull JSONObject download, @Nonnull Platform platform) {
		var url = "https://www.gog.com/" + download.getString("manualUrl").substring(1);
		var name = download.optString("name");
		var version = download.optString("version");
		var type = parseType(url);
		var part = parsePart(url);

		return new GameDownload(game, url, new LazyValue<>(), platform, name, version, type, part);
	}

	@Nonnull
	private static Type parseType(@Nonnull String url) {
		var m = TYPE_PATTERN.matcher(url.substring(url.lastIndexOf('/') + 1));
		if (!m.find()) {
			warn("Could not extract download type from the url: %s. Please report this to marko@zajc.tel.", url);
			return UNKNOWN;
		}

		return switch (m.group(1)) {
			case "installer" -> INSTALLER;
			case "patch" -> PATCH;
			default -> {
				warn("Unknown download type: %s. Please report this to marko@zajc.tel.", m.group(1));
				yield UNKNOWN;
			}
		};
	}

	private static int parsePart(@Nonnull String url) {
		var m = PART_PATTERN.matcher(url);
		if (!m.find()) {
			warn("Could not extract part number from the url: %s. Please report this to marko@zajc.tel.", url);
			return 0;

		} else {
			return parseInt(m.group());
		}
	}

	@SuppressWarnings("null")
	public void downloadTo(@Nonnull Path outputDirectory, @Nullable ProgressBar monitor) throws IOException {
		game().getUser().downloadTo(this, outputDirectory.resolve(path()), monitor);
	}

	@Nonnull
	@SuppressWarnings("null")
	public Path path() {
		return Path.of(this.game.getTitle(), this.platform.toString().toLowerCase(), this.type.toString().toLowerCase(),
					   URLDecoder.decode(url().substring(url().lastIndexOf('/') + 1), UTF_8));
	}

	@Nonnull
	@SuppressWarnings("null")
	public String url() {
		return this.resolvedUrl.get(() -> game().getUser().resolveUrl(this));
	}

	@Nonnull
	@SuppressWarnings("null")
	public String getProgressTitle() {
		return format("%s (%s%s%s, %s)", game().getTitle(), this.part != 0 ? "part " + (part() + 1) + ", " : "",
					  version() != null ? "ver. " + version() + ", " : "", platform().toString().toLowerCase(),
					  type().toString().toLowerCase());
	}

	@Override
	public int hashCode() {
		return hash(this.game, this.name, this.originalUrl, this.platform, this.type, this.version);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof GameDownload other)
			return Objects.equals(this.game, other.game) && Objects.equals(this.name, other.name)
				&& Objects.equals(this.originalUrl, other.originalUrl) && this.platform == other.platform
				&& this.type == other.type && Objects.equals(this.version, other.version);
		else
			return false;
	}

	public enum Platform {

		LINUX,
		WINDOWS,
		MAC;

		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	public enum Type {

		INSTALLER,
		PATCH,
		UNKNOWN;

	}

}
