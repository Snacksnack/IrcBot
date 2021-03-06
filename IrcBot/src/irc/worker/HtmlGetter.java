package irc.worker;

import irc.model.FThread;
import irc.settings.Config;

import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;

import org.jsoup.Jsoup;

/**
 * Get's the title from a http(s) link, if there is no title it tries to
 * determine the type of file or gives back an errorcode.
 * 
 * @author snack
 * 
 */
public class HtmlGetter {
	public static String getTitle(String urlString) {
		try {
			// Module that catches 4chan threads and returns their info
			if ((urlString.startsWith("http://boards.4chan.org/") || urlString.startsWith("https://boards.4chan.org/")) && urlString.length() > "https://boards.4chan.org/".length() + 2) {
				try {
					String board = urlString.split("boards.4chan.org/")[1].split("/")[0];
					String threadNumber = urlString.substring(urlString.lastIndexOf("/") + 1);
					FThread thread = FourChanAPI.retrieve(board, threadNumber);
					return Config.speech.get("SE_SUC").replace("<board>", board).replace("<subject>", thread.subject).replace("<url>", thread.url)
							.replace("<replies>", thread.replies).replace("<comment>", thread.comment);
				} catch (Exception e) {
					return Config.speech.get("SE_ERR");
				}
			}
			// Module that catches youtube urls and returns their info
			String youtube = urlString.substring(urlString.indexOf("://") + 3);
			if ((youtube.startsWith("youtube.") || youtube.startsWith("www.youtube.")) && youtube.contains("watch?v=") && youtube.split("/").length == 2 || (youtube.startsWith("youtu.be/") || youtube.startsWith("www.youtu.be/")) && youtube.split("/").length == 2) {
				String query = youtube;
				if (query.contains("&feature")) {
					query = query.split("&feature")[0];
				}
				if (query.contains("watch?v=")) {
					query = query.split("watch")[1].substring(3);
				}
				if (query.contains("/")) {
					query = query.split("/")[1];
				}
				return YoutubeAPI.getVideoDescription(query);
			}
			// Try to get the title
			String title = Jsoup.connect(urlString).timeout(5000).userAgent("Mozilla").followRedirects(true).get().title();
			title = title.replaceAll("\n", "");
			title = title.replaceAll("  ", " ");
			title = title.replaceAll("  ", " ");
			title = title.replaceAll("  ", " ");
			if (title.length() != 0) {
				if (title.length() > 100) {
					title = title.substring(0, 59) + "...";
				}
				return title;
			} else {
				throw new Exception();
			}
		} catch (Exception e) {
			e.printStackTrace();
			try {
				// Try to determine filesize/type/encoding
				URL url = new URL(urlString);
				URLConnection conn = url.openConnection();
				conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
				conn.setReadTimeout(10000);
				int size = conn.getContentLength();
				String sizeString, output;
				double filesize;
				if (size <= 0) {
					output = Config.speech.get("HT_SUC_SIZE0");
				} else {
					double bytes = Double.valueOf(size);
					double kilobytes = (bytes / 1024);
					double megabytes = (kilobytes / 1024);
					double gigabytes = (megabytes / 1024);

					if (kilobytes < 1) {
						sizeString = "B";
						filesize = bytes;
					} else if (megabytes < 1) {
						sizeString = "KiB";
						filesize = kilobytes;
					} else if (gigabytes < 1) {
						sizeString = "MiB";
						filesize = megabytes;
					} else {
						sizeString = "GiB";
						filesize = gigabytes;
					}

					filesize = new BigDecimal(filesize).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
					output = Config.speech.get("HT_SUC_!SIZE0").replace("<size>", Double.toString(filesize)).replace("<format>", sizeString);
				}

				String encoding = "";
				conn.getInputStream().close();
				try {
					conn.getContentEncoding().isEmpty();
					encoding = Config.speech.get("HT_SUC_ENC").replace("<content-type>", conn.getContentType()).replace("<content-encoding>", conn.getContentEncoding());
				} catch (Exception conEx) {
					encoding = Config.speech.get("HT_SUC_!ENC").replace("<content-type>", conn.getContentType());
				}
				return Config.speech.get("HT_SUC").replace("<encoding>", encoding).replace("<size>", output);
			} catch (Exception ex) {
				// Only got errors? Return it.
				if (ex.getMessage().contains("403")) {
					return Config.speech.get("HT_ERR_403");
				} else if (ex.getMessage().contains("404")) {
					return Config.speech.get("HT_ERR_404");
				} else {
					return Config.speech.get("HT_ERR_?");
				}
			}
		}
	}
}
