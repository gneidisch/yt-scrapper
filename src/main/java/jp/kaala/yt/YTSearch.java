package jp.kaala.yt;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Thumbnail;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Return a list of videos matching a search.
 */

public class YTSearch {

    private static final String PROPERTIES_FILENAME = "kaala.properties";

    private static final long DEFAULT_NUMBER_OF_VIDEOS_RETURNED = 25;

    private YouTube mYoutube;

    private long mResultSize = DEFAULT_NUMBER_OF_VIDEOS_RETURNED;

    public YTSearch() {
    }

    public void setResultSize(int resultSize) {
        mResultSize = resultSize;
    }

    public String go(String json) {

        // Load the properties file.
        Properties properties = new Properties();
        try {
            InputStream in = YTSearch.class.getResourceAsStream("/" + PROPERTIES_FILENAME);
            properties.load(in);

        } catch (IOException e) {
            System.err.println("There was an error reading " + PROPERTIES_FILENAME + ": " + e.getCause()
                    + " : " + e.getMessage());
            System.exit(1);
        }

        try {
            String appName = properties.getProperty("app.name");

            // This object is used to make YouTube Data API requests. The last
            // argument is required, but since we don't need anything
            // initialized when the HttpRequest is initialized, we override
            // the interface and provide a no-op function.
            mYoutube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, new HttpRequestInitializer() {
                public void initialize(HttpRequest request) throws IOException {
                }
            }).setApplicationName(appName).build();

            // Define the API request for retrieving search results.
            YouTube.Search.List search = mYoutube.search().list("id,snippet");

            // Set your developer key from the {{ Google Cloud Console }} for non-authenticated requests.
            // See: {{ https://cloud.google.com/console }}
            String apiKey = properties.getProperty("youtube.apikey");
            search.setKey(apiKey);

            Query query = parseJson(json);
            StringBuilder sb = new StringBuilder();
            sb.append(query.band);
            if (query.genres != null) {
                for (String g : query.genres) {
                    sb.append(" ").append(g);
                }
            }
            search.setQ(sb.toString());
            search.setRegionCode(query.location);
            if (null != query.channelId && query.channelId.length() > 0) {
                search.setChannelId(query.channelId);
            }

            // Restrict the search results to only include videos. See:
            // https://developers.google.com/youtube/v3/docs/search/list#type
            search.setType("video");

            // To increase efficiency, only retrieve the fields that the application uses.
            search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
            search.setMaxResults(mResultSize);

            // Call the API and print results.
            SearchListResponse searchResponse = search.execute();
            List<SearchResult> searchResultList = searchResponse.getItems();
            json = writeJson(searchResultList);
        } catch (GoogleJsonResponseException e) {
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
        } catch (IOException e) {
            System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return json;

    }

    private static class Query {
        String band;
        String[] genres;
        String location;
        String channelId;
    }

    private static Query parseJson(String json) {
        Query query = new Query();
        JSONParser parser = new JSONParser();

        try {
            Object obj = parser.parse(json);

            JSONObject jsonObject = (JSONObject)obj;

            query.band = (String)jsonObject.get("band");

            // loop array
            JSONArray genres = (JSONArray)jsonObject.get("genres");
            if (genres != null && genres.size() > 0) {
                query.genres = new String[genres.size()];
                int i = 0;
                Iterator<String> iterator = genres.iterator();
                while (iterator.hasNext()) {
                    query.genres[i++] = iterator.next();
                }
            }

            query.location = (String)jsonObject.get("location");

            query.channelId = (String)jsonObject.get("channelId");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return query;
    }

    private static String writeJson(List<SearchResult> list) {
        Iterator<SearchResult> it = list.iterator();

        JSONObject obj = new JSONObject();

        if (!it.hasNext()) {
            return obj.toString();
        }

        JSONArray array = new JSONArray();

        while (it.hasNext()) {

            SearchResult singleVideo = it.next();
            ResourceId rId = singleVideo.getId();

            // Confirm that the result represents a video. Otherwise, the item will not contain a video ID.
            if (rId.getKind().equals("youtube#video")) {
                Thumbnail thumbnail = singleVideo.getSnippet().getThumbnails().getDefault();

                JSONObject sub = new JSONObject();
                sub.put("videoId", rId.getVideoId());
                sub.put("Title", singleVideo.getSnippet().getTitle());
                sub.put("Thumbnail", thumbnail.getUrl());
                array.add(sub);
            }
        }

        obj.put("result", array);

        return obj.toString();
    }

    public static void main(String[] args) {

        String json = "{\"band\": \"aspirin\", \"genres\": [\"hardcore\",\"punk\"], \"location\": \"JP\", \"channelId\": \"UCy_BjjSacnwR-kEVPU5KOWA\"}";

        if (args != null && args.length > 0) {
            json = args[0];
        }

        String result = new YTSearch().go(json);

        System.out.println(result);
    }

}
