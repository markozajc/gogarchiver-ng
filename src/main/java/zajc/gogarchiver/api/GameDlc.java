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

import java.util.Objects;

import javax.annotation.Nonnull;

import kong.unirest.core.json.*;

public class GameDlc extends Game {

	@Nonnull private final Game parent;

	private GameDlc(@Nonnull Game parent, @Nonnull String title, @Nonnull JSONObject downloads) {
		super(parent.getUser(), parent.getId(), title, downloads, new JSONArray());

		this.parent = parent;
	}

	@Nonnull
	@SuppressWarnings("null")
	public static GameDlc fromJson(@Nonnull Game parent, @Nonnull JSONObject json) {
		var title = json.getString("title");
		var downloads = json.getJSONArray("downloads").getJSONArray(0).getJSONObject(1);

		return new GameDlc(parent, title, downloads);
	}

	@Nonnull
	public Game getParent() {
		return this.parent;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(this.parent);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		else if (!super.equals(obj) || !(obj instanceof GameDlc other))
			return false;
		else
			return Objects.equals(this.parent, other.parent);
	}

}
