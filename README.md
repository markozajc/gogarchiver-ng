# gogarchiver-ng
A Java rewrite of [gogarchiver](https://github.com/markozajc/gogarchiver), the GOG.com archival tool written in bash, with many improvements:

* a better, more flexible CLI
* parallel download support
* DLC support
* account library retrieval
* a much improved interface

## Building
Prerequisites:

* JDK (>= 17)
* Maven (>= 3.2.5)

Run the following:
```
$ mvn clean package
```
The output runnable JAR will be written to `target/gogarchiver-ng.jar`. There will be another JAR with the version number in that directory - ignore it.

## Finding the authorization token
1. Open firefox
2. Log into GOG.COM
3. Open developer tools (Ctrl + Shift + Alt)
4. Navigate to the *Storage* tab
5. Select *Cookies*, and then *https://www.gog.com/* on the left sidebar
6. Locate the `gog-al` cookie, and copy its value

The cookie can be passed to gogarchiver-ng directly (via `-k`) or through a file for better security (via `-K`).

## Available on:
* [https://git.zajc.eu.org/gogarchiver-ng.git/](https://git.zajc.eu.org/gogarchiver-ng.git/)
* [https://github.com/markozajc/gogarchiver-ng/](https://github.com/markozajc/gogarchiver-ng/)

Prebuilt binaries are available at [https://files.zajc.eu.org/builds/gogarchiver-ng/](https://files.zajc.eu.org/builds/gogarchiver-ng/).

<div class="sect1">
<h2 id="_options">Usage</h2>
<div class="sectionbody">
<div class="dlist">
<dl>
<dt class="hdlist1"><strong>-k</strong>, <strong>--token</strong>=<em>TOKEN</em></dt>
<dd>
<p>GOG token, which can be extracted from the gog-al cookie</p>
</dd>
<dt class="hdlist1"><strong>-K</strong>, <strong>--token-file</strong>=<em>PATH</em></dt>
<dd>
<p>read GOG token from a file</p>
</dd>
<dt class="hdlist1"><strong>-o</strong>, <strong>--output</strong>=<em>PATH</em></dt>
<dd>
<p>directory to write downloaded games to</p>
</dd>
<dt class="hdlist1"><strong>-t</strong>, <strong>--threads</strong>=<em>THREADS</em></dt>
<dd>
<p>number of download threads</p>
<div class="literalblock">
Default: (same as machine's thread count)
</div>
</dd>
<dt class="hdlist1"><strong>-q</strong>, <strong>--quiet</strong></dt>
<dd>
<p>disable progress bars</p>
</dd>
<dt class="hdlist1"><strong>-c</strong>, <strong>--color</strong>=<em>MODE</em></dt>
<dd>
<p>control output color. Supported are auto, on, off</p>
</dd>
<dt class="hdlist1"><strong>-h</strong>, <strong>--help</strong></dt>
<dd>
<p>Show this help message and exit.</p>
</dd>
<dt class="hdlist1"><strong>-V</strong>, <strong>--version</strong></dt>
<dd>
<p>Print version information and exit.</p>
</dd>
</dl>
</div>
</div>
</div>
<div class="sect1">
<h2 id="_filter_options">Filter options</h2>
<div class="sectionbody">
<div class="dlist">
<dl>
<dt class="hdlist1"><strong>--[no-]installers</strong></dt>
<dd>
<p>download installers</p>
<div class="literalblock">
Default: true
</div>
</dd>
<dt class="hdlist1"><strong>--[no-]patches</strong></dt>
<dd>
<p>download version patches</p>
<div class="literalblock">
Default: true
</div>
</dd>
<dt class="hdlist1"><strong>--[no-]dlcs</strong></dt>
<dd>
<p>download available DLCs</p>
<div class="literalblock">
Default: true
</div>
</dd>
<dt class="hdlist1"><strong>-i</strong>, <strong>--include-game</strong>=<em>ID</em>[,<em>ID</em>&#8230;&#8203;]</dt>
<dd>
<p>only the listed game IDs will be downloaded. Game IDs can be obtained from <a href="https://www.gogdb.org/" class="bare">https://www.gogdb.org/</a></p>
</dd>
<dt class="hdlist1"><strong>-e</strong>, <strong>--exclude-game</strong>=<em>ID</em>[,<em>ID</em>&#8230;&#8203;]</dt>
<dd>
<p>all games owned by the account except those listed will be downloaded</p>
</dd>
<dt class="hdlist1"><strong>--include-platform</strong>=<em>PLATFORM</em>[,<em>PLATFORM</em>&#8230;&#8203;]</dt>
<dd>
<p>platforms to download for. Supported are linux, windows, mac</p>
</dd>
<dt class="hdlist1"><strong>--exclude-platform</strong>=<em>PLATFORM</em>[,<em>PLATFORM</em>&#8230;&#8203;]</dt>
<dd>
<p>platforms to not download for</p>
</dd>
</dl>
</div>
</div>
</div>
<div class="sect1">
<h2 id="_advanced_options">Advanced options</h2>
<div class="sectionbody">
<div class="dlist">
<dl>
<dt class="hdlist1"><strong>-v</strong>, <strong>--verbose</strong></dt>
<dd>
<p>display verbose log messages</p>
</dd>
<dt class="hdlist1"><strong>--[no-]unknown-types</strong></dt>
<dd>
<p>download unknown download types</p>
<div class="literalblock">
Default: false
</div>
</dd>
</dl>
</div>
</div>
</div>
