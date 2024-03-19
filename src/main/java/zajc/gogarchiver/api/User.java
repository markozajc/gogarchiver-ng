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

import static java.io.File.separatorChar;
import static java.nio.file.Files.createDirectories;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static zajc.gogarchiver.api.GameDownload.Platform.LINUX;
import static zajc.gogarchiver.util.Utilities.*;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.*;

import kong.unirest.*;
import kong.unirest.json.JSONObject;
import me.tongfei.progressbar.ProgressBar;
import zajc.gogarchiver.exception.NotLoggedInException;
import zajc.gogarchiver.util.LazyValue;

public class User {

	private static final UnirestInstance UNIREST_NO_REDIRECT = new UnirestInstance(new Config().followRedirects(false));

	private static final String URL_USER = "https://www.gog.com/userData.json";
	private static final String URL_LIBRARY = "https://menu.gog.com/v1/account/licences";
	private static final String URL_GAME_DETAILS = "https://www.gog.com/account/gameDetails/%s.json";

	@Nonnull private final String token;
	@Nonnull private final LazyValue<Set<String>> libraryIds = new LazyValue<>();
	@Nonnull private final LazyValue<JSONObject> userData = new LazyValue<>();
	@Nonnull private final Map<String, Game> games = new ConcurrentHashMap<>();

	public User(@Nonnull String token) throws NotLoggedInException {
		this.token = token;

		if (!isLoggedIn())
			throw new NotLoggedInException();
	}

	private boolean isLoggedIn() {
		return getUserData().getBoolean("isLoggedIn");
	}

	@Nonnull
	@SuppressWarnings("null")
	public String getUsername() {
		return getUserData().getString("username");
	}

	@Nonnull
	@SuppressWarnings("null")
	private JSONObject getUserData() {
		return this.userData.get(() -> getJson(URL_USER).getObject());
	}

	@Nonnull
	@SuppressWarnings("null")
	public Set<String> getLibraryIds() {
		return this.libraryIds
			.get(() -> stream(getJson(URL_LIBRARY).getArray()).map(Object::toString).collect(toUnmodifiableSet()));
	}

	@Nullable
	public Game resolveGame(@Nonnull String id) {
		return this.games.computeIfAbsent(id, this::resolveGameDirectly);
	}

	@Nullable
	@SuppressWarnings("null")
	public Game resolveGameDirectly(@Nonnull String id) {
		if (!getLibraryIds().contains(id)) {
			warn("User @|bold %s|@ does not own game @|bold %s|@.", getUsername(), id);
			return null;
		}

		var json = getJson(URL_GAME_DETAILS.formatted(id));
		if (json.isArray() && json.getArray().isEmpty()) // is a dlc
			return null;
		else
			return Game.fromJson(this, json.getObject(), id);
	}

	@Nonnull
	@SuppressWarnings("null")
	public JsonNode getJson(@Nonnull String url) {
		return checkResponse(url, get(url).asJson()).getBody();
	}

	@SuppressWarnings("resource")
	public void downloadTo(@Nonnull GameDownload download, @Nonnull Path output,
						   @Nullable ProgressBar monitor) throws IOException {
		var parent = output.getParent();
		if (parent != null)
			createDirectories(parent);

		var temp = new File((parent == null ? "" : parent.toString() + separatorChar) + '.' +
			output.getFileName().toString() +
			".part");
		temp.deleteOnExit(); // NOSONAR it's good enough

		var req = get(download.url());
		if (monitor != null) {
			req.downloadMonitor((_1, _2, downloaded, total) -> {
				if (monitor.getMax() == 1)
					monitor.maxHint(total);
				monitor.stepTo(downloaded);
			});
		}

		var outputFile = checkResponse(download.originalUrl(), req.asFile(temp.getPath())).getBody();
		if (download.platform() == LINUX)
			outputFile.setExecutable(true, false); // NOSONAR doesn't matter much

		if (!outputFile.renameTo(output.toFile()))
			throw new IOException("Couldn't rename the part file");
	}

	@Nonnull
	@SuppressWarnings("null")
	public String resolveUrl(@Nonnull GameDownload download) {
		var location = download.originalUrl();
		try (var unirest = Unirest.spawnInstance()) {
			unirest.config().followRedirects(false);

			for (int i = 0; i < 10; i++) {
				var newLocation = UNIREST_NO_REDIRECT.get(location)
					.cookie("gog-al", this.token)
					.asEmpty()
					.getHeaders()
					.all()
					.stream()
					.filter(h -> h.getName().equalsIgnoreCase("location"))
					.findFirst()
					.map(Header::getValue);

				if (newLocation.isPresent())
					location = newLocation.get();
				else
					return location;
			}
		}

		throw new RuntimeException("Encountered a redirect loop on " + download.originalUrl());
	}

	public GetRequest get(@Nonnull String url) {
		return Unirest.get(url).cookie("gog-al", this.token);
	}

}
