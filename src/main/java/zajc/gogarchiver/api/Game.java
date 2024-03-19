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

import static java.util.Objects.hash;
import static zajc.gogarchiver.util.Utilities.stream;

import java.util.*;

import javax.annotation.Nonnull;

import kong.unirest.json.*;
import zajc.gogarchiver.api.GameDownload.Platform;

public class Game {

	@Nonnull private final User user;
	@Nonnull private final String id;
	@Nonnull private final String title;
	@Nonnull private final List<GameDownload> downloads;
	@Nonnull private final List<GameDlc> dlcs;

	@SuppressWarnings("null")
	protected Game(@Nonnull User user, @Nonnull String id, @Nonnull String title, @Nonnull JSONObject downloads,
				@Nonnull JSONArray dlcs) {
		this.user = user;
		this.id = id;
		this.title = title;

		this.downloads = downloads.keySet().stream().flatMap(p -> {
			var platform = Platform.valueOf(p.toUpperCase());
			return stream(downloads.getJSONArray(p)).map(JSONObject.class::cast)
				.map(d -> GameDownload.fromJson(this, d, platform)); // NOSONAR
		}).toList();

		this.dlcs = stream(dlcs).map(JSONObject.class::cast).map(j -> GameDlc.fromJson(this, j)).toList();
	}

	@Nonnull
	@SuppressWarnings("null")
	public static Game fromJson(@Nonnull User user, @Nonnull JSONObject json, @Nonnull String id) {
		var title = json.getString("title");
		var downloads = json.getJSONArray("downloads").getJSONArray(0).getJSONObject(1);
		var dlcs = json.getJSONArray("dlcs");

		return new Game(user, id, title, downloads, dlcs);
	}

	@Nonnull
	public User getUser() {
		return this.user;
	}

	@Nonnull
	public String getId() {
		return this.id;
	}

	@Nonnull
	public String getTitle() {
		return this.title;
	}

	@Nonnull
	public List<GameDownload> getDownloads() {
		return this.downloads;
	}

	@Nonnull
	public List<GameDlc> getDlcs() {
		return this.dlcs;
	}

	@Override
	public int hashCode() {
		return hash(this.id, this.title);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof Game other)
			return Objects.equals(this.id, other.id) && Objects.equals(this.title, other.title);
		else
			return false;
	}

}
