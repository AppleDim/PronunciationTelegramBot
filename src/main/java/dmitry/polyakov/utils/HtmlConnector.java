package dmitry.polyakov.utils;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.helper.ValidationException;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Slf4j
@Component
public class HtmlConnector {
    public Document getDocFromUrl(String url) {
        Document doc = null;
        try {
            doc = Jsoup.connect(url).get();
        } catch (IOException | ValidationException | NullPointerException e) {
            log.warn("Error occurred while connecting to URL: " + url);
        }

        return doc;
    }
}
