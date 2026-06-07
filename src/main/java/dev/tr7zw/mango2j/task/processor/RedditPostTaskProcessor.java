package dev.tr7zw.mango2j.task.processor;

import dev.tr7zw.mango2j.task.*;
import dev.tr7zw.mango2j.util.*;
import lombok.extern.java.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.springframework.stereotype.*;

import javax.imageio.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

@Component
@Log
public class RedditPostTaskProcessor implements InternalTaskProcessor {

    @Override
    public String getKey() {
        return "reddit.post";
    }

    @Override
    public String getDisplayName() {
        return "Reddit Post Files";
    }

    @Override
    public List<TaskFormField> getFormFields() {
        return List.of(
                new TaskFormField("url", "Source URL", "url", "https://www.reddit.com/r/comics/comments/1twfnqe/surprise/", true),
                new TaskFormField("targetDir", "Target Chapter", "text", "Comics", true)
        );
    }

    @Override
    public String getFormFragment() {
        return "fragments/task-form-reddit-post :: form";
    }

    @Override
    public void process(TaskExecutionContext context) throws Exception {
        Map<String, Object> payload = context.getPayload();
        String url = String.valueOf(payload.getOrDefault("url", "")).trim().replace("https://www.reddit.com", "https://old.reddit.com");
        String targetDir = String.valueOf(payload.getOrDefault("targetDir", "")).trim();
        if (url.isBlank() || targetDir.isBlank()) {
            context.fail("url and targetDir are required");
            return;
        }
        if (!url.startsWith("https://old.reddit.com")) {
            context.fail("URL must start with https://www.reddit.com or https://old.reddit.com");
            return;
        }
        context.progress(0, 1, "Looking up " + url);
        List<String> galleryUrls = new ArrayList<>();
        Document gallery = Jsoup.connect(url).userAgent(
                        "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3833.99 Safari/537.36")
                .cookie("over18", "1")
                .timeout(20000).get();
        gallery.getElementsByAttributeValueStarting("href", "https://preview.redd.it/").forEach(el -> {
            String imageUrl = el.attr("href");
            if(!imageUrl.contains("blur=")) {
                galleryUrls.add(imageUrl);
            }
        });
        context.progress(0, galleryUrls.size(), "Downloading files");
        Path targetDirectory = Path.of(targetDir);
        Files.createDirectories(targetDirectory.getParent());
        int counter = 0;
        for (String imageUrl : galleryUrls) {
            context.progress(counter, galleryUrls.size(), "Downloading " + imageUrl);
            String title = gallery.title();
            downloadFile(imageUrl, targetDirectory.toFile(), counter, FileUtil.cleanName(title));
            counter++;
        }
        context.finish("Download complete");
    }

    private void downloadFile(String url, File targetfolder, int id, String title) throws IOException {
        if (url.endsWith(".gifv") && url.contains("imgur.com/")) {
            String gifId = url.substring(url.indexOf("imgur.com/")).replace(".gifv", "").replace("imgur.com/", "");
            URLConnection con = new URL("https://imgur.com/download/" + gifId).openConnection();
            con.addRequestProperty("Referer", "https://i.imgur.com/");
            InputStream in = con.getInputStream();
            File out = new File(targetfolder, title + (id != 0 ? id : "") + ".mp4");
            in.transferTo(new FileOutputStream(out));
            log.info("Downloaded gifv/mp4: " + title + " " + url);
            return;
        }
        BufferedImage image = ImageIO.read(new URL(url.replace("https://i.redd.it/", "https://redlib.zaggy.nl/img/")));
        if (image != null) {
            File out = null;
            do {
                out = new File(targetfolder, title + (id != 0 ? id : "") + ".png");
                id++;
            } while (out.exists());
            ImageIO.write(image, "png", out);
            log.info("Downloaded: " + title + " " + url);
        } else {
            throw new IOException("Not able to download: " + url);
        }
        if (url.endsWith(".gif")) {
            File out = null;
            do {
                out = new File(targetfolder, FileUtil.cleanName(title) + (id != 0 ? id : "") + ".gif");
                id++;
            } while (out.exists());
            try (FileOutputStream outStream = new FileOutputStream(out)) {
                try (InputStream instream = new URL(url).openStream()) {
                    instream.transferTo(outStream);
                    log.info("Downloaded: " + title + " " + url);
                }
            }
        }
    }

}
