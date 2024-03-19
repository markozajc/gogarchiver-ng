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

import static java.lang.Long.MAX_VALUE;
import static java.lang.Math.max;
import static java.lang.System.*;
import static java.nio.file.Files.createDirectories;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static java.util.stream.Stream.concat;
import static me.tongfei.progressbar.ProgressBarStyle.*;
import static picocli.CommandLine.Help.Ansi.OFF;
import static zajc.gogarchiver.util.Utilities.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

import javax.annotation.*;

import org.eu.zajc.ef.runnable.except.all.AERunnable;

import me.tongfei.progressbar.*;
import picocli.CommandLine;
import picocli.CommandLine.*;
import sun.misc.Signal; // NOSONAR it's just quality of life
import zajc.gogarchiver.api.*;
import zajc.gogarchiver.exception.NotLoggedInException;

@Command(name = "gogarchiver", description = "an archival tool for GOG.com", version = "gogarchiver 1.0",
		 mixinStandardHelpOptions = true, sortSynopsis = false, sortOptions = false)
public class Main implements Callable<Integer> {

	@Mixin private Arguments arguments;

	private void run() throws Exception {
		createDirectories(this.arguments.getOutputPath());
		var downloads = getDownloadList();
		if (downloads.isEmpty()) {
			if (!this.arguments.isQuiet())
				out.println("\u001b[2KNothing to do");

		} else {
			executeDownloads(downloads);

			if (!this.arguments.isQuiet())
				out.println("Done");
		}
	}

	@SuppressWarnings({ "resource", "null" })
	private void executeDownloads(@Nonnull List<GameDownload> downloads) throws InterruptedException {
		var progressTitleWidth =
			downloads.stream().map(GameDownload::getProgressTitle).mapToInt(String::length).max().orElse(-1);

		Map<GameDownload, ProgressBar> progressBars;
		if (this.arguments.isQuiet()) {
			progressBars = null;

		} else {
			progressBars = new HashMap<>();
			downloads.stream().forEachOrdered(d -> {
				progressBars.put(d, downloadProgress(d.getProgressTitle(), progressTitleWidth));
			});
		}

		var pool = newFixedThreadPool(this.arguments.getThreads());
		downloads.stream().forEachOrdered(d -> {
			startDownload(d, pool, progressBars);
		});

		pool.shutdown();
		pool.awaitTermination(MAX_VALUE, NANOSECONDS);
	}

	@SuppressWarnings({ "null", "resource" })
	private void startDownload(@Nonnull GameDownload download, @Nonnull ExecutorService service,
							   @Nullable Map<GameDownload, ProgressBar> progressBars) {
		service.submit((AERunnable) () -> {
			var progress = progressBars == null ? null : progressBars.get(download);

			download.downloadTo(this.arguments.getOutputPath(), progress);

			if (progress != null) {
				progress.stepTo(progress.getMax());
				progress.refresh();
				progress.pause();
			}
		});
	}

	@Nonnull
	@SuppressWarnings({ "null", "resource" })
	public List<GameDownload> getDownloadList() throws IOException, NotLoggedInException {
		ForkJoinPool pool = null;
		try (var p = this.arguments.isQuiet() ? null : createGameLoadingProgress()) {
			if (p != null)
				p.setExtraMessage("Loading user library");

			var ids = this.arguments.getGameIds();
			if (p != null)
				p.maxHint(ids.size());

			var user = this.arguments.getUser();
			pool = new ForkJoinPool(ids.size() + 1); // metadata requests take a while so it doesn't hurt to parallelize
			var games = pool.submit(() -> { // this is a hack to increase parallelStream()'s parallelism
				return ids.parallelStream().map(user::resolveGame).filter(Objects::nonNull).peek(g -> { // NOSONAR
					if (p != null) {
						p.setExtraMessage(g.getTitle());
						p.step();
					}
				}).collect(toUnmodifiableSet());
			}).join();

			if (p != null) {
				p.stepTo(ids.size());
				p.setExtraMessage("Processing games");
			}
			return createDownloadList(games, pool);

		} finally {
			if (pool != null)
				pool.shutdown();
			if (!this.arguments.isQuiet())
				cursorUp();
		}
	}

	@Nonnull
	@SuppressWarnings("null")
	private List<GameDownload> createDownloadList(@Nonnull Set<Game> games, @Nonnull ForkJoinPool pool) {
		var types = this.arguments.getTypes();
		var platforms = this.arguments.getPlatforms();
		var output = this.arguments.getOutputPath();

		return pool.submit(() -> {
			return games.parallelStream().flatMap(g -> concat(Stream.of(g), g.getDlcs().stream())).filter(g -> {
				if (g instanceof GameDlc dlc && !this.arguments.downloadDlcs()) {
					verbose("Downloading DLCs is disabled - skipping DLC @|bold %s|@ of game @|bold %s|@",
							dlc.getTitle(), dlc.getParent().getTitle());

					return false;
				} else {
					return true;
				}
			}).flatMap(g -> g.getDownloads().stream()).filter(d -> {
				if (!platforms.contains(d.platform())) {
					verbose("Downloading for @|bold %s|@ is disabled - not downloading @|bold %s|@",
							d.platform().toString().toLowerCase(), d.getProgressTitle());
					return false;

				} else if (!types.contains(d.type())) {
					verbose("Downloading types of @|bold %s|@ is disabled - not downloading @|bold %s|@",
							d.type().toString().toLowerCase(), d.getProgressTitle());
					return false;

				} else if (output.resolve(d.path()).toFile().exists()) {
					verbose("Not downloading @|bold %s|@ because it is already downloaded",
							d.type().toString().toLowerCase(), d.getProgressTitle());
					return false;

				} else {
					return true;
				}
			})
				.sorted(Comparator.<GameDownload, String>comparing(d -> d.game().getTitle())
					.thenComparing(GameDownload::platform)
					.thenComparing(d -> requireNonNullElse(d.version(), ""))
					.thenComparing(GameDownload::type)
					.thenComparingInt(GameDownload::part))
				.toList();
		}).join();
	}

	@Nonnull
	@SuppressWarnings("null")
	public ProgressBar downloadProgress(@Nonnull String title, int titleMinWidth) {
		return new ProgressBarBuilder().setUpdateIntervalMillis(250)
			.setTaskName(title + ".".repeat(max(0, titleMinWidth - title.length())))
			.setStyle(this.arguments.getColorMode() == OFF ? UNICODE_BLOCK : COLORFUL_UNICODE_BLOCK)
			.setInitialMax(1)
			.setUnit(" MiB", 1024L * 1024L)
			.build();
	}

	@Nonnull
	@SuppressWarnings("null")
	private ProgressBar createGameLoadingProgress() {
		return new ProgressBarBuilder().setUpdateIntervalMillis(250)
			.setTaskName("Loading games")
			.setStyle(this.arguments.getColorMode() == OFF ? UNICODE_BLOCK : COLORFUL_UNICODE_BLOCK)
			.setInitialMax(-1)
			.continuousUpdate()
			.clearDisplayOnFinish()
			.hideEta()
			.build();
	}

	@Override
	public Integer call() throws Exception {
		setVerbose(this.arguments.isVerbose());
		setColorMode(this.arguments.getColorMode());

		try {
			run();
		} catch (NotLoggedInException e) {
			println("""
				@|bold,red Invalid token.|@ Find your token by logging into GOG in your browser, \
				and copying the "gog-al" cookie from its developer tools.""");
			return 1;
		}
		return 0;
	}

	public static void main(String[] args) {
		Signal.handle(new Signal("INT"), s -> {
			out.println();
			exit(0);
		});

		exit(new CommandLine(new Main()).setUsageHelpAutoWidth(true)
			.setUsageHelpLongOptionsMaxWidth(50)
			.setCaseInsensitiveEnumValuesAllowed(true)
			.setOverwrittenOptionsAllowed(true)
			.execute(args));
	}

}
