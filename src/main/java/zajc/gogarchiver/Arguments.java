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
package zajc.gogarchiver;

import static java.lang.Runtime.getRuntime;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static picocli.CommandLine.Help.Ansi.AUTO;
import static picocli.CommandLine.Help.Visibility.ALWAYS;
import static zajc.gogarchiver.api.GameDownload.Type.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import org.eu.zajc.ef.supplier.except.all.AESupplier;

import picocli.CommandLine.*;
import picocli.CommandLine.Help.Ansi;
import zajc.gogarchiver.api.*;
import zajc.gogarchiver.api.GameDownload.Platform;
import zajc.gogarchiver.exception.NotLoggedInException;
import zajc.gogarchiver.util.LazyValue;

public class Arguments {

	private final LazyValue<User> user = new LazyValue<>();

	@ArgGroup(exclusive = true, multiplicity = "1") private Token token;

	private static class Token {

		@Option(names = { "-k", "--token" }, description = "GOG token, which can be extracted from the gog-al cookie",
				paramLabel = "TOKEN") private String tokenString;
		@Option(names = { "-K", "--token-file" }, description = "read GOG token from a file",
				paramLabel = "PATH") private Path tokenFile;

		@Nonnull
		@SuppressWarnings("null")
		public String getTokenString() throws IOException {
			var token = this.tokenString;
			if (token == null)
				token = Files.readString(this.tokenFile);
			return token.strip();
		}

	}

	@Option(names = { "-o", "--output" }, required = true, description = "directory to write downloaded games to",
			paramLabel = "PATH") private Path output;
	@Option(names = { "-t", "--threads" }, description = "number of download threads", paramLabel = "THREADS",
			showDefaultValue = ALWAYS) private int threads = getRuntime().availableProcessors();
	@Option(names = { "-q", "--quiet" }, description = "disable progress bars") private boolean quiet = false;
	@Option(names = { "-c", "--color" }, description = "control output color. Supported are auto, on, off",
			paramLabel = "MODE") private Ansi color = AUTO;

	@ArgGroup(validate = false, heading = "%nFilter options%n") private Filters filters = new Filters();

	private static class Filters {

		@Option(names = { "--no-installers" }, description = "download installers", negatable = true,
				showDefaultValue = ALWAYS) private boolean installers = true;
		@Option(names = { "--no-patches" }, description = "download version patches", negatable = true,
				showDefaultValue = ALWAYS) private boolean patches = true;
		@Option(names = { "--no-dlcs" }, description = "download available DLCs", negatable = true,
				showDefaultValue = ALWAYS) private boolean dlcs = true;

		@ArgGroup(exclusive = true) private GameIds gameIds = new GameIds();

		private static class GameIds {

			@Option(names = { "-i", "--include-game" }, description = """
				only the listed game IDs will be downloaded. Game IDs can be obtained from https://www.gogdb.org/""",
					paramLabel = "ID", split = ",") private Set<String> included;
			@Option(names = { "-e", "--exclude-game" }, description = """
				all games owned by the account except those listed will be downloaded""", paramLabel = "ID",
					split = ",") private Set<String> excluded;

			@Nonnull
			@SuppressWarnings("null")
			public Set<String> getGameIds(@Nonnull User user) {
				if (this.included != null)
					return unmodifiableSet(this.included);

				var library = user.getLibraryIds();

				if (this.excluded == null)
					return library;
				else
					return library.stream().filter(Predicate.not(this.excluded::contains)).collect(toUnmodifiableSet());
			}

		}

		@ArgGroup(exclusive = true) private Platforms platforms = new Platforms();

		private static class Platforms {

			@Option(names = { "--include-platform" }, description = """
				platforms to download for. Supported are linux, windows, mac""", paramLabel = "PLATFORM",
					split = ",") private EnumSet<Platform> included;
			@Option(names = { "--exclude-platform" }, description = """
				platforms to not download for""", paramLabel = "PLATFORM",
					split = ",") private EnumSet<Platform> excluded;

			@Nonnull
			@SuppressWarnings("null")
			public Set<Platform> getPlatforms() {
				if (this.included != null)
					return unmodifiableSet(this.included);
				else if (this.excluded != null)
					return unmodifiableSet(this.excluded);
				else
					return EnumSet.allOf(Platform.class);
			}

		}

	}

	@ArgGroup(validate = false, heading = "%nAdvanced options%n") private AdvancedOptions advanced =
		new AdvancedOptions();

	private static class AdvancedOptions {

		@Option(names = { "-v", "--verbose" }, description = "display verbose log messages") private boolean verbose =
			false;

		@Option(names = { "--unknown-types" }, description = "download unknown download types", negatable = true,
				showDefaultValue = ALWAYS) private boolean unknown = false;

	}

	@Nonnull
	@SuppressWarnings({ "unused", "null" })
	public User getUser() throws IOException, NotLoggedInException {
		return this.user.get((AESupplier<User>) () -> new User(this.token.getTokenString()));
	}

	@Nonnull
	public Set<String> getGameIds() throws IOException, NotLoggedInException {
		return this.filters.gameIds.getGameIds(getUser());
	}

	@Nonnull
	@SuppressWarnings("null")
	public Ansi getColorMode() {
		return this.color;
	}

	public int getThreads() {
		return this.threads;
	}

	public Path getOutputPath() {
		return this.output;
	}

	public Set<Platform> getPlatforms() {
		return this.filters.platforms.getPlatforms();
	}

	public boolean downloadDlcs() {
		return this.filters.dlcs;
	}

	public Set<GameDownload.Type> getTypes() {
		var types = EnumSet.noneOf(GameDownload.Type.class);
		if (this.filters.installers)
			types.add(INSTALLER);

		if (this.filters.patches)
			types.add(PATCH);

		if (this.advanced.unknown)
			types.add(UNKNOWN);

		return types;
	}

	public boolean isVerbose() {
		return this.advanced.verbose;
	}

	public boolean isQuiet() {
		return this.quiet;
	}

}
