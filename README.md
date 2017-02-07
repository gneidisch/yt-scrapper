Before use:

src/resources/kaala.properties:
  replace youtube-api-key



Usage

+ java:
  - import jp.kaala.yt.*
  - new YTSearch().go(String json-content);
  


+ cmd line:
# java -cp KaalaYTScrapper.jar jp.kaala.yt.YTSearch json-content


json-content format:
{
  "band": "band-name",
  "genres": ["genre1", "genre2"...],
  "location": "JP",
  "channelId": "youTubeChannelId"
}

genres and channelId are optional


