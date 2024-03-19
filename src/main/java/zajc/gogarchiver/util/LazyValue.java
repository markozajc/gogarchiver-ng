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

import java.util.function.Supplier;

import javax.annotation.*;

public class LazyValue<T> {

	@Nullable private volatile T value;

	public T get(@Nonnull Supplier<T> generator) {
		if (this.value != null)
			return this.value;
		synchronized (this) {
			if (this.value != null) // double checked locking
				return this.value;
			else
				return this.value = generator.get();
		}
	}

	public synchronized void unset() {
		this.value = null;
	}

}
